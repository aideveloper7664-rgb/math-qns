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

sealed class RpcResult<out T> {
    data class Ok<T>(val value: T) : RpcResult<T>()
    data class Err(val message: String, val httpCode: Int? = null, val retryable: Boolean = true) : RpcResult<Nothing>()
}

class SinglePlayerGameRepository {

    private val tag = "SinglePlayerGameRepo"

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
        var retries = 0
        var lastErr: RpcResult.Err? = null

        while (retries <= 2) {
            try {
                val response = SupabaseClient.restApi.getNextQuestionRpc(mapOf("p_session_id" to sessionId))
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val success = (data["success"] as? Boolean) ?: true
                    if (!success) {
                        val err = data["error"]?.toString() ?: "No questions available"
                        Log.e(tag, "rpc=get_next_question success=false err=$err")
                        return@withContext RpcResult.Err(err, response.code(), retryable = false)
                    }

                    val qId = data["question_id"]?.toString()
                    val qText = data["question_text"]?.toString()
                    val opA = data["option_a"]?.toString()
                    val opB = data["option_b"]?.toString()
                    val opC = data["option_c"]?.toString()
                    val opD = data["option_d"]?.toString()

                    if (!qId.isNullOrBlank() && !qText.isNullOrBlank() && !opA.isNullOrBlank() && !opB.isNullOrBlank()) {
                        val qNum = (data["question_number"] as? Number)?.toInt() ?: 1
                        val diff = data["difficulty"]?.toString() ?: "Easy"
                        val limit = (data["time_limit_ms"] as? Number)?.toLong() ?: 10000L
                        val correct = data["correct_answer"]?.toString()

                        val questionData = QuestionData(
                            questionId = qId,
                            questionNumber = qNum,
                            questionText = qText,
                            optionA = opA,
                            optionB = opB,
                            optionC = opC ?: "",
                            optionD = opD ?: "",
                            difficulty = diff,
                            timeLimitMs = limit,
                            correctAnswer = correct
                        )
                        return@withContext RpcResult.Ok(questionData)
                    } else {
                        val err = data["error"]?.toString() ?: "No new questions available right now. Please try again later."
                        Log.e(tag, "rpc=get_next_question blank fields returned: data=$data")
                        return@withContext RpcResult.Err(err, response.code(), retryable = false)
                    }
                } else {
                    val errBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    Log.e(tag, "rpc=get_next_question code=${response.code()} body=$errBody")
                    lastErr = RpcResult.Err("Server error (${response.code()}): $errBody", response.code(), retryable = true)
                }
            } catch (e: Exception) {
                Log.e(tag, "rpc=get_next_question exception attempt=${retries + 1}: ${e.message}")
                lastErr = RpcResult.Err(e.message ?: "Network error fetching question", retryable = true)
            }

            retries++
            if (retries <= 2) {
                delay(400L)
            }
        }

        return@withContext lastErr ?: RpcResult.Err("Failed to fetch next question after retries")
    }

    suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answer: String?,
        responseTimeMs: Long
    ): RpcResult<SubmitAnswerResponse> = withContext(Dispatchers.IO) {
        var retries = 0
        var lastErr: RpcResult.Err? = null

        while (retries <= 2) {
            try {
                val rpcPayload = mapOf<String, Any?>(
                    "p_session_id" to sessionId,
                    "p_question_id" to questionId,
                    "p_answer" to answer,
                    "p_response_time_ms" to responseTimeMs.toInt()
                )
                val response = SupabaseClient.restApi.submitAnswerRpc(rpcPayload)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val isCorrect = (data["correct"] as? Boolean) ?: false
                    val points = (data["points_earned"] as? Number)?.toInt() ?: 0
                    val total = (data["total_score"] as? Number)?.toInt() ?: 0
                    val isGameOver = (data["game_over"] as? Boolean) ?: (!isCorrect)
                    val reason = data["reason"]?.toString()
                    val prize = (data["prize"] as? Number)?.toDouble() ?: 0.0
                    val correctAnswer = data["correct_answer"]?.toString()

                    val result = SubmitAnswerResponse(
                        correct = isCorrect,
                        correctAnswer = correctAnswer,
                        pointsEarned = points,
                        totalScore = total,
                        gameOver = isGameOver,
                        reason = reason,
                        prize = prize
                    )
                    return@withContext RpcResult.Ok(result)
                } else {
                    val errBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    Log.e(tag, "rpc=submit_answer code=${response.code()} body=$errBody")
                    lastErr = RpcResult.Err("Server error submitting answer (${response.code()}): $errBody", response.code(), retryable = true)
                }
            } catch (e: Exception) {
                Log.e(tag, "rpc=submit_answer exception attempt=${retries + 1}: ${e.message}")
                lastErr = RpcResult.Err(e.message ?: "Network error submitting answer", retryable = true)
            }

            retries++
            if (retries <= 2) {
                delay(400L)
            }
        }

        return@withContext lastErr ?: RpcResult.Err("Failed to submit answer after retries")
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
