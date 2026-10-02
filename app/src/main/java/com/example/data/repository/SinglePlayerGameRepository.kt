package com.example.data.repository

import android.util.Log
import com.example.data.remote.GameSessionDto
import com.example.data.remote.QuestionData
import com.example.data.remote.StartGameResponse
import com.example.data.remote.SubmitAnswerResponse
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseQuestionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed class RpcResult<out T> {
    data class Ok<T>(val value: T) : RpcResult<T>()
    data class Err(val message: String, val httpCode: Int? = null, val retryable: Boolean = true) : RpcResult<Nothing>()
}

class SinglePlayerGameRepository {

    private val tag = "SinglePlayerGameRepo"
    private val servedQuestionsBySession = mutableMapOf<String, MutableSet<String>>()
    private var cachedSupabaseQuestions: List<SupabaseQuestionDto>? = null
    private var questionGenCounter = 0

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
        val servedSet = servedQuestionsBySession.getOrPut(sessionId) { mutableSetOf() }

        var retries = 0
        var lastErr: RpcResult.Err? = null

        while (retries <= 1) {
            try {
                val response = SupabaseClient.restApi.getNextQuestionRpc(mapOf("p_session_id" to sessionId))
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val success = (data["success"] as? Boolean) ?: true
                    val qId = data["question_id"]?.toString()
                    val qText = data["question_text"]?.toString()
                    val opA = data["option_a"]?.toString() ?: ""
                    val opB = data["option_b"]?.toString() ?: ""
                    val opC = data["option_c"]?.toString() ?: ""
                    val opD = data["option_d"]?.toString() ?: ""

                    if (success && !qId.isNullOrBlank() && !qText.isNullOrBlank() && !servedSet.contains(qId)) {
                        servedSet.add(qId)
                        val qNum = (data["question_number"] as? Number)?.toInt() ?: (servedSet.size)
                        val diff = data["difficulty"]?.toString() ?: "Easy"
                        val limit = (data["time_limit_ms"] as? Number)?.toLong() ?: 10000L
                        val rawCorrect = data["correct_answer"]?.toString()
                            ?: data["correct_option"]?.toString()
                            ?: data["answer"]?.toString()

                        val resolvedCorrect = resolveCorrectAnswer(rawCorrect, qText, opA, opB, opC, opD)

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
                            correctAnswer = resolvedCorrect
                        )
                        Log.d(tag, "✅ Got question from RPC: id=$qId text=$qText correct=$resolvedCorrect")
                        return@withContext RpcResult.Ok(questionData)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "rpc=get_next_question attempt $retries failed: ${e.message}")
            }
            retries++
            delay(200L)
        }

        // Fallback to Supabase REST questions table if RPC returned repeat or failed
        try {
            if (cachedSupabaseQuestions == null) {
                val res = SupabaseClient.restApi.getQuestions(limit = 100)
                if (res.isSuccessful && res.body() != null) {
                    cachedSupabaseQuestions = res.body()
                }
            }

            val available = cachedSupabaseQuestions?.filter { dto ->
                !servedSet.contains(dto.id) && !servedSet.contains(dto.question ?: dto.text ?: "")
            }

            if (!available.isNullOrEmpty()) {
                val dto = available.random()
                val qId = dto.id
                val qText = dto.question ?: dto.text ?: "Solve the equation"
                val opA = dto.optionA ?: ""
                val opB = dto.optionB ?: ""
                val opC = dto.optionC ?: ""
                val opD = dto.optionD ?: ""

                servedSet.add(qId)
                if (qText.isNotBlank()) servedSet.add(qText)

                val resolvedCorrect = resolveCorrectAnswer(dto.correctAnswer, qText, opA, opB, opC, opD)

                val questionData = QuestionData(
                    questionId = qId,
                    questionNumber = servedSet.size,
                    questionText = qText,
                    optionA = opA,
                    optionB = opB,
                    optionC = opC,
                    optionD = opD,
                    difficulty = dto.difficulty ?: "Easy",
                    timeLimitMs = 10000L,
                    correctAnswer = resolvedCorrect
                )
                Log.d(tag, "✅ Got question from Supabase REST DB: id=$qId text=$qText correct=$resolvedCorrect")
                return@withContext RpcResult.Ok(questionData)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load questions from Supabase REST DB: ${e.message}")
        }

        // Fallback: Generate fresh unique Speed Math Question when pool exhausted
        val dynamicQ = generateDynamicMathQuestion(sessionId, servedSet)
        Log.d(tag, "✅ Generated dynamic Speed Math question: text=${dynamicQ.questionText} correct=${dynamicQ.correctAnswer}")
        return@withContext RpcResult.Ok(dynamicQ)
    }

    private fun generateDynamicMathQuestion(sessionId: String, servedSet: MutableSet<String>): QuestionData {
        var num1 = (5..99).random()
        var num2 = (5..99).random()
        val opIndex = (0..3).random()

        var text = ""
        var correctVal = 0

        when (opIndex) {
            0 -> { // Addition
                correctVal = num1 + num2
                text = "$num1 + $num2 = ?"
            }
            1 -> { // Subtraction
                if (num1 < num2) { val tmp = num1; num1 = num2; num2 = tmp }
                correctVal = num1 - num2
                text = "$num1 - $num2 = ?"
            }
            2 -> { // Multiplication
                num1 = (2..12).random()
                num2 = (2..15).random()
                correctVal = num1 * num2
                text = "$num1 × $num2 = ?"
            }
            else -> { // Division
                num2 = (2..12).random()
                val mult = (2..15).random()
                num1 = num2 * mult
                correctVal = mult
                text = "$num1 ÷ $num2 = ?"
            }
        }

        val qId = "gen_${System.currentTimeMillis()}_${questionGenCounter++}"
        servedSet.add(qId)
        servedSet.add(text)

        val wrongOptions = mutableSetOf<Int>()
        while (wrongOptions.size < 3) {
            val delta = (-10..10).random()
            val w = correctVal + delta
            if (w != correctVal && w >= 0 && !wrongOptions.contains(w)) {
                wrongOptions.add(w)
            }
        }

        val wrongList = wrongOptions.toList()
        val correctLetter = listOf("A", "B", "C", "D").random()

        var opA = ""
        var opB = ""
        var opC = ""
        var opD = ""

        var wrongIdx = 0
        when (correctLetter) {
            "A" -> { opA = "$correctVal"; opB = "${wrongList[0]}"; opC = "${wrongList[1]}"; opD = "${wrongList[2]}" }
            "B" -> { opA = "${wrongList[0]}"; opB = "$correctVal"; opC = "${wrongList[1]}"; opD = "${wrongList[2]}" }
            "C" -> { opA = "${wrongList[0]}"; opB = "${wrongList[1]}"; opC = "$correctVal"; opD = "${wrongList[2]}" }
            else -> { opA = "${wrongList[0]}"; opB = "${wrongList[1]}"; opC = "${wrongList[2]}"; opD = "$correctVal" }
        }

        return QuestionData(
            questionId = qId,
            questionNumber = servedSet.size,
            questionText = text,
            optionA = opA,
            optionB = opB,
            optionC = opC,
            optionD = opD,
            difficulty = "Easy",
            timeLimitMs = 10000L,
            correctAnswer = correctLetter
        )
    }

    private fun resolveCorrectAnswer(
        rawAnswer: String?,
        qText: String,
        opA: String,
        opB: String,
        opC: String,
        opD: String
    ): String {
        val raw = rawAnswer?.trim()
        if (!raw.isNullOrBlank()) {
            val upper = raw.uppercase()
            if (upper in listOf("A", "B", "C", "D")) return upper
            if (raw.equals(opA.trim(), ignoreCase = true)) return "A"
            if (raw.equals(opB.trim(), ignoreCase = true)) return "B"
            if (raw.equals(opC.trim(), ignoreCase = true)) return "C"
            if (raw.equals(opD.trim(), ignoreCase = true)) return "D"
        }

        val computed = computeMathAnswer(qText, opA, opB, opC, opD)
        if (computed != null) return computed

        return rawAnswer?.uppercase() ?: "A"
    }

    private fun computeMathAnswer(qText: String, opA: String, opB: String, opC: String, opD: String): String? {
        try {
            val regex = Regex("""(-?\d+)\s*([\+\-\*×x/÷])\s*(-?\d+)""")
            val match = regex.find(qText) ?: return null
            val num1 = match.groupValues[1].toLongOrNull() ?: return null
            val op = match.groupValues[2]
            val num2 = match.groupValues[3].toLongOrNull() ?: return null

            val result: Long = when (op) {
                "+", "plus" -> num1 + num2
                "-", "minus" -> num1 - num2
                "*", "×", "x" -> num1 * num2
                "/", "÷" -> if (num2 != 0L) num1 / num2 else return null
                else -> return null
            }

            val resultStr = result.toString()

            val cleanA = opA.trim().replace(",", "")
            val cleanB = opB.trim().replace(",", "")
            val cleanC = opC.trim().replace(",", "")
            val cleanD = opD.trim().replace(",", "")

            return when {
                cleanA == resultStr -> "A"
                cleanB == resultStr -> "B"
                cleanC == resultStr -> "C"
                cleanD == resultStr -> "D"
                else -> null
            }
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answer: String?,
        responseTimeMs: Long,
        expectedCorrectAnswer: String? = null,
        currentScore: Int = 0
    ): RpcResult<SubmitAnswerResponse> = withContext(Dispatchers.IO) {
        Log.d("Game", "Submitting: session=$sessionId, question=$questionId, answer=$answer, time=$responseTimeMs")
        try {
            val jsonObject = JSONObject().apply {
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
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            val response = SupabaseClient.restApi.submitAnswerRpc(requestBody)

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
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
                Log.d("Game", "Submit response from RPC: correct=${result.correct}, gameOver=${result.gameOver}")
                return@withContext RpcResult.Ok(result)
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
                Log.w(tag, "rpc=submit_answer code=${response.code()} body=$errBody. Using precise local evaluation.")

                val correctOpt = expectedCorrectAnswer?.trim()?.uppercase()
                val userOpt = answer?.trim()?.uppercase()
                val isCorrect = !userOpt.isNullOrBlank() && !correctOpt.isNullOrBlank() && (userOpt == correctOpt)
                val points = if (isCorrect) 10 else 0
                val newScore = currentScore + points

                val fallbackResponse = SubmitAnswerResponse(
                    success = true,
                    correct = isCorrect,
                    correctAnswer = correctOpt ?: "A",
                    pointsEarned = points,
                    totalScore = newScore,
                    correctAnswers = if (isCorrect) 1 else 0,
                    gameOver = !isCorrect,
                    reason = if (!isCorrect) (if (userOpt == null) "Time Out" else "Wrong Answer") else null,
                    prize = 0.0,
                    refund = 0.0,
                    refundApplied = false
                )
                Log.d("Game", "Submit response (Local Eval): userOpt=$userOpt, correctOpt=$correctOpt, isCorrect=${fallbackResponse.correct}, gameOver=${fallbackResponse.gameOver}")
                return@withContext RpcResult.Ok(fallbackResponse)
            }
        } catch (e: Exception) {
            Log.w(tag, "submitAnswer exception: ${e.message}. Using precise local evaluation.")
            val correctOpt = expectedCorrectAnswer?.trim()?.uppercase()
            val userOpt = answer?.trim()?.uppercase()
            val isCorrect = !userOpt.isNullOrBlank() && !correctOpt.isNullOrBlank() && (userOpt == correctOpt)
            val points = if (isCorrect) 10 else 0
            val newScore = currentScore + points

            val fallbackResponse = SubmitAnswerResponse(
                success = true,
                correct = isCorrect,
                correctAnswer = correctOpt ?: "A",
                pointsEarned = points,
                totalScore = newScore,
                correctAnswers = if (isCorrect) 1 else 0,
                gameOver = !isCorrect,
                reason = if (!isCorrect) (if (userOpt == null) "Time Out" else "Wrong Answer") else null,
                prize = 0.0,
                refund = 0.0,
                refundApplied = false
            )
            Log.d("Game", "Submit response (Local Exception Fallback): userOpt=$userOpt, correctOpt=$correctOpt, isCorrect=${fallbackResponse.correct}, gameOver=${fallbackResponse.gameOver}")
            return@withContext RpcResult.Ok(fallbackResponse)
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
