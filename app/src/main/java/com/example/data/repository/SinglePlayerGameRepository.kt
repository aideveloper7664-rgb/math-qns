package com.example.data.repository

import android.util.Log
import com.example.data.remote.GameSessionDto
import com.example.data.remote.QuestionData
import com.example.data.remote.StartGameResponse
import com.example.data.remote.SubmitAnswerResponse
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

sealed class RpcResult<out T> {
    data class Ok<T>(val value: T) : RpcResult<T>()
    data class Err(val message: String, val httpCode: Int? = null, val retryable: Boolean = true) : RpcResult<Nothing>()
}

class SinglePlayerGameRepository {

    private val tag = "SinglePlayerGameRepo"
    private val servedQuestionsBySession = mutableMapOf<String, MutableSet<String>>()

    suspend fun startGame(): RpcResult<StartGameResponse> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.restApi.startGameRpc()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val success = (data["success"] as? Boolean) ?: false
                val sessionId = data["session_id"]?.toString()
                if (success && !sessionId.isNullOrBlank()) {
                    val entryFee = (data["entry_fee"] as? Number)?.toDouble() ?: 10.0
                    val newBalance = (data["new_balance"] as? Number)?.toDouble()
                    servedQuestionsBySession[sessionId] = mutableSetOf()
                    Log.d(tag, "✅ Game started via RPC: session=$sessionId, fee=$entryFee, newBal=$newBalance")
                    return@withContext RpcResult.Ok(
                        StartGameResponse(
                            success = true,
                            sessionId = sessionId,
                            entryFee = entryFee,
                            newBalance = newBalance
                        )
                    )
                } else {
                    val err = data["error"]?.toString() ?: "Failed to start game"
                    Log.e(tag, "rpc=start_game error=$err")
                    return@withContext RpcResult.Err(err, response.code(), retryable = false)
                }
            } else {
                val errBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e(tag, "rpc=start_game code=${response.code()} body=$errBody")
                return@withContext RpcResult.Err("Server error (${response.code()}): $errBody", response.code())
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception in start_game: ${e.message}", e)
            return@withContext RpcResult.Err(e.message ?: "Network error starting game")
        }
    }

    suspend fun getNextQuestion(sessionId: String): RpcResult<QuestionData> = withContext(Dispatchers.IO) {
        val served = servedQuestionsBySession.getOrPut(sessionId) { mutableSetOf() }
        repeat(3) { attempt ->
            try {
                val res = SupabaseClient.restApi.getNextQuestionRpc(mapOf("p_session_id" to sessionId))
                val data = res.body()
                if (res.isSuccessful && data != null) {
                    if ((data["success"] as? Boolean) == false) {
                        return@withContext RpcResult.Err(data["error"]?.toString() ?: "No question available", retryable = false)
                    }
                    val qId = data["question_id"]?.toString()
                    val qText = data["question_text"]?.toString()
                    val opA = data["option_a"]?.toString() ?: ""
                    val opB = data["option_b"]?.toString() ?: ""
                    val opC = data["option_c"]?.toString() ?: ""
                    val opD = data["option_d"]?.toString() ?: ""

                    if (!qId.isNullOrBlank() && !qText.isNullOrBlank() && served.add(qId)) {
                        val qNum = (data["question_number"] as? Number)?.toInt() ?: served.size
                        val diff = data["difficulty"]?.toString() ?: "Easy"
                        val limit = (data["time_limit_ms"] as? Number)?.toLong() ?: 10000L
                        val questionData = QuestionData(
                            questionId = qId,
                            questionNumber = qNum,
                            questionText = qText,
                            optionA = opA,
                            optionB = opB,
                            optionC = opC,
                            optionD = opD,
                            difficulty = diff,
                            timeLimitMs = limit,
                            correctAnswer = null
                        )
                        Log.d(tag, "✅ Got question from RPC: id=$qId text=$qText")
                        return@withContext RpcResult.Ok(questionData)
                    }
                }
            } catch (e: IOException) {
                Log.w(tag, "get_next_question attempt $attempt: ${e.message}")
            } catch (e: Exception) {
                Log.w(tag, "get_next_question attempt $attempt error: ${e.message}")
            }
            delay(300L)
        }
        return@withContext RpcResult.Err("Question load nahi hua. Retry karo.", retryable = true)
    }

    suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answer: String?,
        responseTimeMs: Long
    ): RpcResult<SubmitAnswerResponse> = withContext(Dispatchers.IO) {
        Log.d(tag, "Submitting: session=$sessionId, question=$questionId, answer=$answer, time=$responseTimeMs")
        try {
            val json = JSONObject().apply {
                put("p_session_id", sessionId)
                put("p_question_id", questionId)
                if (answer != null) {
                    put("p_answer", answer)
                } else {
                    put("p_answer", JSONObject.NULL)
                }
                put("p_response_time_ms", responseTimeMs.toInt())
            }
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)
            val response = SupabaseClient.restApi.submitAnswerRpc(requestBody)

            if (!response.isSuccessful || response.body() == null) {
                val err = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
                Log.w(tag, "submit_answer HTTP ${response.code()} body=$err")
                val retryable = response.code() >= 500 || response.code() == 408 || response.code() == 429
                return@withContext RpcResult.Err("Answer submit nahi hua. Dobara try karo.", retryable = retryable)
            }

            val data = response.body()!!
            if ((data["success"] as? Boolean) == false) {
                return@withContext RpcResult.Err(data["error"]?.toString() ?: "Submit failed", retryable = false)
            }

            val isCorrect = (data["correct"] as? Boolean) ?: false
            val points = (data["points_earned"] as? Number)?.toInt() ?: 0
            val total = (data["total_score"] as? Number)?.toInt() ?: 0
            val isGameOver = (data["game_over"] as? Boolean) ?: (!isCorrect)
            val reason = data["reason"]?.toString()
            val prize = (data["prize"] as? Number)?.toDouble() ?: 0.0
            val correctAnswer = data["correct_answer"]?.toString()
            val refund = (data["refund"] as? Number)?.toDouble() ?: 0.0
            val refundApplied = (data["refund_applied"] as? Boolean) ?: false
            val correctAnswers = (data["correct_answers"] as? Number)?.toInt() ?: 0
            val success = (data["success"] as? Boolean) ?: true
            val error = data["error"]?.toString()

            val result = SubmitAnswerResponse(
                success = success,
                correct = isCorrect,
                correctAnswer = correctAnswer,
                pointsEarned = points,
                totalScore = total,
                correctAnswers = correctAnswers,
                gameOver = isGameOver,
                reason = reason,
                prize = prize,
                refund = refund,
                refundApplied = refundApplied,
                error = error
            )
            Log.d(tag, "Submit response from RPC: correct=${result.correct}, gameOver=${result.gameOver}")
            return@withContext RpcResult.Ok(result)
        } catch (e: IOException) {
            Log.e(tag, "Network error submitting answer: ${e.message}")
            return@withContext RpcResult.Err("Network error. Retry karo.", retryable = true)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error submitting answer: ${e.message}")
            return@withContext RpcResult.Err(e.message ?: "Unexpected error", retryable = false)
        }
    }

    suspend fun endGame(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.endGameRpc(mapOf("p_session_id" to sessionId))
            Log.d(tag, "rpc=end_game response=${res.code()}")
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Exception in end_game: ${e.message}")
            return@withContext false
        }
    }

    suspend fun getRecentGameSessions(userId: String): List<GameSessionDto> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.restApi.getGameSessions(userQuery = "eq.$userId", limit = 10)
            if (res.isSuccessful && res.body() != null) {
                return@withContext res.body()!!
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch recent sessions: ${e.message}")
        }
        return@withContext emptyList()
    }
}
