package com.example.data.repository

import android.util.Log
import com.example.data.remote.GameSessionDto
import com.example.data.remote.QuestionData
import com.example.data.remote.StartGameResponse
import com.example.data.remote.SubmitAnswerResponse
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class SinglePlayerGameRepository {

    private val tag = "SinglePlayerGameRepo"

    private val seenQuestions = mutableSetOf<String>()

    suspend fun startGame(): StartGameResponse = withContext(Dispatchers.IO) {
        seenQuestions.clear()
        try {
            val response = SupabaseClient.restApi.startGameRpc()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val success = (data["success"] as? Boolean) ?: false
                if (success) {
                    val sessionId = data["session_id"]?.toString() ?: UUID.randomUUID().toString()
                    val entryFee = (data["entry_fee"] as? Number)?.toDouble() ?: 10.0
                    val newBalance = (data["new_balance"] as? Number)?.toDouble()
                    Log.d(tag, "✅ Game started via RPC: session=$sessionId, fee=$entryFee, newBal=$newBalance")
                    return@withContext StartGameResponse(
                        success = true,
                        sessionId = sessionId,
                        entryFee = entryFee,
                        newBalance = newBalance
                    )
                } else {
                    val err = data["error"]?.toString() ?: "Failed to start game"
                    return@withContext StartGameResponse(success = false, error = err)
                }
            } else {
                val err = response.errorBody()?.string() ?: "Server error (${response.code()})"
                Log.w(tag, "start_game RPC error: $err")
                // Fallback: Check if user has at least ₹10
                val userId = SupabaseClient.currentUserId
                if (userId != null) {
                    val userRes = SupabaseClient.restApi.getUsers(idFilter = "eq.$userId")
                    val user = userRes.body()?.firstOrNull()
                    val balance = user?.walletBalance ?: 0.0
                    if (balance < 10.0) {
                        return@withContext StartGameResponse(success = false, error = "Minimum ₹10 balance required to play")
                    }
                    val newBal = (balance - 10.0).coerceAtLeast(0.0)
                    // Deduct ₹10 locally
                    try {
                        SupabaseClient.restApi.updateUser(
                            idQuery = "eq.$userId",
                            updates = mapOf("wallet_balance" to newBal)
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to deduct balance", e)
                    }
                    val sid = UUID.randomUUID().toString()
                    return@withContext StartGameResponse(
                        success = true,
                        sessionId = sid,
                        entryFee = 10.0,
                        newBalance = newBal
                    )
                }
                return@withContext StartGameResponse(success = false, error = err)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception starting game", e)
            return@withContext StartGameResponse(success = false, error = e.message ?: "Network error")
        }
    }

    suspend fun getNextQuestion(sessionId: String, questionNumber: Int): QuestionData = withContext(Dispatchers.IO) {
        val timeLimitMs = maxOf(8000L, 10000L - (questionNumber - 1) * 200L)

        // Try calling remote RPC first
        try {
            val response = SupabaseClient.restApi.getNextQuestionRpc(mapOf("p_session_id" to sessionId))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val qId = data["question_id"]?.toString()
                val qText = data["question_text"]?.toString()
                val opA = data["option_a"]?.toString()
                val opB = data["option_b"]?.toString()
                val opC = data["option_c"]?.toString()
                val opD = data["option_d"]?.toString()

                if (!qId.isNullOrBlank() && !qText.isNullOrBlank() && !opA.isNullOrBlank() && !opB.isNullOrBlank()) {
                    val diff = data["difficulty"]?.toString() ?: getDifficultyForQuestion(questionNumber)
                    val limit = (data["time_limit_ms"] as? Number)?.toLong() ?: timeLimitMs
                    return@withContext QuestionData(
                        questionId = qId,
                        questionNumber = (data["question_number"] as? Number)?.toInt() ?: questionNumber,
                        questionText = qText,
                        optionA = opA,
                        optionB = opB,
                        optionC = opC ?: "",
                        optionD = opD ?: "",
                        difficulty = diff,
                        timeLimitMs = limit,
                        correctAnswer = data["correct_answer"]?.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "RPC get_next_question fallback: ${e.message}")
        }

        // High quality progressive math generator
        return@withContext generateProgressiveQuestion(questionNumber, timeLimitMs)
    }

    suspend fun submitAnswer(
        sessionId: String,
        question: QuestionData,
        answer: String?,
        responseTimeMs: Long,
        currentScore: Int,
        correctCount: Int
    ): SubmitAnswerResponse = withContext(Dispatchers.IO) {
        // 1. Try remote RPC
        try {
            val rpcPayload = mutableMapOf<String, Any?>(
                "p_session_id" to sessionId,
                "p_question_id" to question.questionId,
                "p_answer" to answer,
                "p_response_time_ms" to responseTimeMs.toInt()
            )
            val response = SupabaseClient.restApi.submitAnswerRpc(rpcPayload)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val isCorrect = (data["correct"] as? Boolean) ?: false
                val points = (data["points_earned"] as? Number)?.toInt() ?: 0
                val total = (data["total_score"] as? Number)?.toInt() ?: (currentScore + points)
                val isGameOver = (data["game_over"] as? Boolean) ?: (!isCorrect)
                val reason = data["reason"]?.toString()
                val prize = (data["prize"] as? Number)?.toDouble() ?: 0.0

                return@withContext SubmitAnswerResponse(
                    correct = isCorrect,
                    correctAnswer = data["correct_answer"]?.toString() ?: question.correctAnswer,
                    pointsEarned = points,
                    totalScore = total,
                    gameOver = isGameOver,
                    reason = reason,
                    prize = prize
                )
            }
        } catch (e: Exception) {
            Log.d(tag, "RPC submit_answer fallback: ${e.message}")
        }

        // 2. Progressive scoring fallback
        val isCorrect = answer != null && answer.trim().equals(question.correctAnswer?.trim(), ignoreCase = true)

        if (isCorrect) {
            val remainingMs = (question.timeLimitMs - responseTimeMs).coerceAtLeast(0L)
            val speedBonus = (remainingMs / 100).toInt() // up to 150 bonus points
            val pointsEarned = 100 + speedBonus
            val newScore = currentScore + pointsEarned

            return@withContext SubmitAnswerResponse(
                correct = true,
                correctAnswer = question.correctAnswer,
                pointsEarned = pointsEarned,
                totalScore = newScore,
                gameOver = false,
                reason = null,
                prize = 0.0
            )
        } else {
            val reason = if (answer == null) "Time Out" else "Wrong Answer"
            // Prize = score / 100, max 10x entry fee (₹100)
            val prize = (currentScore / 100.0).coerceAtMost(100.0)

            // Credit prize to user's wallet if prize > 0
            val userId = SupabaseClient.currentUserId
            if (userId != null && prize > 0) {
                try {
                    val userRes = SupabaseClient.restApi.getUsers(idFilter = "eq.$userId")
                    val curBal = userRes.body()?.firstOrNull()?.walletBalance ?: 0.0
                    val newBal = curBal + prize
                    SupabaseClient.restApi.updateUser(
                        idQuery = "eq.$userId",
                        updates = mapOf(
                            "wallet_balance" to newBal,
                            "total_winnings" to ((userRes.body()?.firstOrNull()?.totalWinnings ?: 0.0) + prize)
                        )
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Failed to credit prize", e)
                }
            }

            // Record completed session in game_sessions
            if (userId != null) {
                try {
                    val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                    val updateMap = mapOf(
                        "status" to if (prize > 0) "COMPLETED" else "FAILED",
                        "total_score" to currentScore,
                        "correct_answers" to correctCount,
                        "highest_question" to question.questionNumber,
                        "ended_at" to nowIso
                    )
                    SupabaseClient.restApi.updateGameSession(
                        idQuery = "eq.$sessionId",
                        body = updateMap
                    )
                } catch (e: Exception) {
                    Log.d(tag, "Failed to update game session row: ${e.message}")
                }
            }

            return@withContext SubmitAnswerResponse(
                correct = false,
                correctAnswer = question.correctAnswer,
                pointsEarned = 0,
                totalScore = currentScore,
                gameOver = true,
                reason = reason,
                prize = prize
            )
        }
    }

    suspend fun endGame(sessionId: String): Unit = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.restApi.endGameRpc(mapOf("p_session_id" to sessionId))
        } catch (e: Exception) {
            Log.d(tag, "end_game error: ${e.message}")
        }
    }

    suspend fun getRecentGameSessions(userId: String): List<GameSessionDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = SupabaseClient.restApi.getGameSessions("eq.$userId")
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getDifficultyForQuestion(qNum: Int): String {
        return when {
            qNum <= 2 -> "easy"
            qNum <= 4 -> "medium"
            qNum <= 7 -> "hard"
            else -> "expert"
        }
    }

    private fun generateProgressiveQuestion(questionNumber: Int, timeLimitMs: Long): QuestionData {
        val qId = "local_q_${questionNumber}_${System.currentTimeMillis()}"
        val difficulty = getDifficultyForQuestion(questionNumber)

        var questionText: String
        var correctVal: String
        var options: List<String>
        var attempts = 0

        do {
            val gen = when (difficulty) {
                "easy" -> generateEasyQuestion(questionNumber)
                "medium" -> generateMediumQuestion(questionNumber)
                "hard" -> generateHardQuestion(questionNumber)
                else -> generateExpertQuestion(questionNumber)
            }
            questionText = gen.first
            correctVal = gen.second
            options = gen.third
            attempts++
        } while (seenQuestions.contains(questionText) && attempts < 8)

        seenQuestions.add(questionText)

        // Shuffle options and assign to A, B, C, D
        val shuffledOptions = options.shuffled()
        val correctLetter = when (shuffledOptions.indexOf(correctVal)) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            else -> "D"
        }

        return QuestionData(
            questionId = qId,
            questionNumber = questionNumber,
            questionText = questionText,
            optionA = shuffledOptions.getOrElse(0) { correctVal },
            optionB = shuffledOptions.getOrElse(1) { "$correctVal 1" },
            optionC = shuffledOptions.getOrElse(2) { "$correctVal 2" },
            optionD = shuffledOptions.getOrElse(3) { "$correctVal 3" },
            difficulty = difficulty,
            timeLimitMs = timeLimitMs,
            correctAnswer = correctLetter
        )
    }


    private fun generateEasyQuestion(qNum: Int): Triple<String, String, List<String>> {
        val op = if (qNum == 1) Random.nextInt(2) else Random.nextInt(3)
        return when (op) {
            0 -> {
                val a = Random.nextInt(12, 50)
                val b = Random.nextInt(9, 45)
                val ans = a + b
                val dist = generateDistractors(ans)
                Triple("What is $a + $b?", "$ans", dist)
            }
            1 -> {
                val a = Random.nextInt(35, 95)
                val b = Random.nextInt(12, a - 5)
                val ans = a - b
                val dist = generateDistractors(ans)
                Triple("What is $a - $b?", "$ans", dist)
            }
            else -> {
                val a = Random.nextInt(4, 12)
                val b = Random.nextInt(4, 12)
                val ans = a * b
                val dist = generateDistractors(ans)
                Triple("What is $a × $b?", "$ans", dist)
            }
        }
    }

    private fun generateMediumQuestion(qNum: Int): Triple<String, String, List<String>> {
        val op = Random.nextInt(4)
        return when (op) {
            0 -> {
                // Two digit multiplication
                val a = Random.nextInt(12, 22)
                val b = Random.nextInt(11, 19)
                val ans = a * b
                val dist = generateDistractors(ans)
                Triple("Calculate $a × $b", "$ans", dist)
            }
            1 -> {
                // Division
                val b = Random.nextInt(7, 18)
                val ans = Random.nextInt(8, 25)
                val a = b * ans
                val dist = generateDistractors(ans)
                Triple("What is $a ÷ $b?", "$ans", dist)
            }
            2 -> {
                // Percentage
                val p = listOf(10, 15, 20, 25, 50).random()
                val total = listOf(120, 160, 200, 240, 300, 450).random()
                val ans = (p * total) / 100
                val dist = generateDistractors(ans)
                Triple("Find $p% of $total", "$ans", dist)
            }
            else -> {
                // Squares
                val a = Random.nextInt(12, 25)
                val ans = a * a
                val dist = generateDistractors(ans)
                Triple("What is $a²?", "$ans", dist)
            }
        }
    }

    private fun generateHardQuestion(qNum: Int): Triple<String, String, List<String>> {
        val op = Random.nextInt(4)
        return when (op) {
            0 -> {
                // Linear equation: a * x + b = c
                val a = Random.nextInt(3, 9)
                val x = Random.nextInt(4, 16)
                val b = Random.nextInt(8, 35)
                val c = a * x + b
                val dist = generateDistractors(x)
                Triple("Solve for x: $a x + $b = $c", "$x", dist)
            }
            1 -> {
                // Order of operations: a * b - c / d
                val d = Random.nextInt(3, 8)
                val cQuot = Random.nextInt(4, 12)
                val c = d * cQuot
                val a = Random.nextInt(7, 15)
                val b = Random.nextInt(6, 12)
                val ans = (a * b) - cQuot
                val dist = generateDistractors(ans)
                Triple("Evaluate: ($a × $b) - ($c ÷ $d)", "$ans", dist)
            }
            2 -> {
                // Powers & Roots
                val roots = listOf(144 to 12, 169 to 13, 196 to 14, 225 to 15, 256 to 16, 289 to 17, 324 to 18)
                val pair = roots.random()
                val mult = Random.nextInt(3, 8)
                val ans = pair.second * mult
                val dist = generateDistractors(ans)
                Triple("Calculate: √${pair.first} × $mult", "$ans", dist)
            }
            else -> {
                // Series pattern
                val start = Random.nextInt(3, 12)
                val diff = Random.nextInt(4, 9)
                val s1 = start
                val s2 = s1 + diff
                val s3 = s2 + diff
                val s4 = s3 + diff
                val ans = s4 + diff
                val dist = generateDistractors(ans)
                Triple("Next number in series: $s1, $s2, $s3, $s4, ?", "$ans", dist)
            }
        }
    }

    private fun generateExpertQuestion(qNum: Int): Triple<String, String, List<String>> {
        val op = Random.nextInt(4)
        return when (op) {
            0 -> {
                val a = Random.nextInt(4, 9)
                val x = Random.nextInt(6, 18)
                val b = Random.nextInt(12, 50)
                val c = a * x - b
                val dist = generateDistractors(x)
                Triple("Solve for x: $a x - $b = $c", "$x", dist)
            }
            1 -> {
                val a = Random.nextInt(3, 8)
                val cube = a * a * a
                val b = Random.nextInt(11, 24)
                val sq = b * b
                val ans = a + b
                val dist = generateDistractors(ans)
                Triple("Evaluate: ∛$cube + √$sq", "$ans", dist)
            }
            2 -> {
                val p = listOf(15, 25, 35, 75).random()
                val total = listOf(200, 300, 400, 600).random()
                val extra = Random.nextInt(15, 45)
                val ans = (p * total) / 100 + extra
                val dist = generateDistractors(ans)
                Triple("Calculate: $p% of $total + $extra", "$ans", dist)
            }
            else -> {
                val start = Random.nextInt(2, 6)
                val r = Random.nextInt(2, 4)
                val t1 = start
                val t2 = t1 * r
                val t3 = t2 * r
                val t4 = t3 * r
                val ans = t4 * r
                val dist = generateDistractors(ans)
                Triple("Next term in GP: $t1, $t2, $t3, $t4, ?", "$ans", dist)
            }
        }
    }


    private fun generateDistractors(correct: Int): List<String> {
        val set = mutableSetOf(correct)
        val offsets = listOf(-1, 1, -2, 2, -10, 10, -5, 5, -3, 3)
        for (offset in offsets.shuffled()) {
            val candidate = correct + offset
            if (candidate > 0) set.add(candidate)
            if (set.size == 4) break
        }
        while (set.size < 4) {
            val candidate = correct + Random.nextInt(-15, 16)
            if (candidate > 0) set.add(candidate)
        }
        return set.map { "$it" }
    }
}
