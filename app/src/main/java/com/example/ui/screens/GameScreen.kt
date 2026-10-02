package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.remote.GameResult
import com.example.data.remote.QuestionData
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameConfigUi
import com.example.ui.viewmodel.GameState
import java.util.Locale

@Composable
fun GameScreen(
    state: GameState,
    config: GameConfigUi = GameConfigUi(),
    user: UserEntity? = null,
    onAnswer: (String?) -> Unit,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    onViewHistory: () -> Unit = {},
    onRetry: () -> Unit = {},
    onQuit: () -> Unit = {}
) {
    if (state.isGameOver && state.gameResult != null) {
        GameOverView(
            result = state.gameResult,
            config = config,
            user = user,
            onPlayAgain = onPlayAgain,
            onHome = onGoHome,
            onViewHistory = onViewHistory
        )
        return
    }

    if (state.errorMessage != null && !state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(BgDark).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Something went wrong", color = RedError, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(state.errorMessage, color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.errorRetryable) {
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    OutlinedButton(onClick = onQuit) { Text("Quit") }
                }
            }
        }
        return
    }

    if (state.isLoading || state.currentQuestion == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Loading Next Question…",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        return
    }

    val question = state.currentQuestion
    val progress = (state.timeLeftMs.toFloat() / state.timeLimitMs.toFloat()).coerceIn(0f, 1f)
    val secondsText = String.format(Locale.US, "%.1f", state.timeLeftMs / 1000f)

    val timerColor = when {
        state.timeLeftMs < 3000L -> RedError
        state.timeLeftMs < 7000L -> GoldAccent
        else -> GreenSuccess
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // ── Top Bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q ${state.questionNumber}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = CyanPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = when (question.difficulty.lowercase()) {
                            "easy" -> GreenSuccess.copy(alpha = 0.2f)
                            "medium" -> GoldAccent.copy(alpha = 0.2f)
                            else -> RedError.copy(alpha = 0.2f)
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = question.difficulty.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (question.difficulty.lowercase()) {
                                "easy" -> GreenSuccess
                                "medium" -> GoldAccent
                                else -> RedError
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.refundCredited && state.refundAmount > 0) {
                    Surface(
                        color = GreenSuccess.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "✅ Refund Credited",
                            color = GreenSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Score: ", fontSize = 12.sp, color = TextMuted)
                        Text(
                            text = "${state.score}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Timer Bar ───────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME REMAINING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${secondsText}s",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = timerColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = timerColor,
                trackColor = SurfaceDark
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Question Card ───────────────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderStrong),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = question.questionText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Options List ────────────────────────────────────────────────────
        val options = listOf(
            "A" to question.optionA,
            "B" to question.optionB,
            "C" to question.optionC,
            "D" to question.optionD
        )

        options.forEach { (key, text) ->
            val isSelected = state.selectedOption == key
            val isCorrectAnswer = state.isLocked && question.correctAnswer != null && question.correctAnswer == key

            OptionCard(
                key = key,
                text = text,
                selected = isSelected,
                isCorrect = isCorrectAnswer,
                isWrong = isSelected && state.lastIsCorrect == false,
                locked = state.isLocked,
                onClick = {
                    if (!state.isLocked) {
                        android.util.Log.d("Game", "User clicked: $key")
                        onAnswer(key)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.weight(1f))

        // ── Feedback Banner ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.lastIsCorrect != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut()
        ) {
            if (state.lastIsCorrect == true) {
                Surface(
                    color = GreenSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CORRECT! +${state.lastPointsEarned} pts (Next question incoming…)",
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Surface(
                    color = RedError.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RedError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("❌", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.selectedOption == null) "TIME'S UP! GAME OVER" else "WRONG ANSWER! GAME OVER",
                            color = RedError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionCard(
    key: String,
    text: String,
    selected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCorrect -> GreenSuccess.copy(alpha = 0.25f)
        isWrong -> RedError.copy(alpha = 0.25f)
        selected -> Color(0xFF5B7FFF)
        else -> Color(0xFF1A212D)
    }

    val borderColor = when {
        isCorrect -> GreenSuccess
        isWrong -> RedError
        selected -> Color(0xFF5B7FFF)
        else -> BorderSubtle
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCorrect -> GreenSuccess
                            isWrong -> RedError
                            selected -> Color.White
                            else -> Color(0xFF2AD9C9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    color = if (selected && !isCorrect && !isWrong) Color.Black else Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = text,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (isCorrect) {
                Text("✓", color = GreenSuccess, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            } else if (isWrong) {
                Text("✕", color = RedError, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── 6. Game Over View ────────────────────────────────────────────────────────

@Composable
fun GameOverView(
    result: GameResult,
    config: GameConfigUi,
    user: UserEntity?,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onViewHistory: () -> Unit
) {
    val isWin = result.prize > 0
    val title = if (isWin) config.winTitle else config.loseTitle
    val rawMessage = if (isWin) config.winMessage else config.loseMessage
    val currentBal = user?.walletBalance ?: 0.0

    val formattedMessage = rawMessage
        .replace("{score}", "${result.totalScore}")
        .replace("{prize}", "₹${"%.2f".format(result.prize)}")
        .replace("{correct}", "${result.correctAnswers}")
        .replace("{total}", "${config.questionsPerGame}")
        .replace("{balance}", "₹${"%.2f".format(currentBal)}")
        .replace("{refund}", "₹${"%.2f".format(result.refund)}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isWin) GreenSuccess.copy(alpha = 0.2f) else RedError.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isWin) "🎉" else "💔", fontSize = 36.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = if (isWin) GreenSuccess else RedError,
            textAlign = TextAlign.Center
        )

        if (formattedMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = formattedMessage,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderStrong),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                StatRow("Final Score", "${result.totalScore} pts")
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                StatRow("Correct Answers", "${result.correctAnswers} / ${config.questionsPerGame}")

                if (result.prize > 0) {
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prize Won", color = TextMuted, fontSize = 14.sp)
                        Text(
                            text = "₹${"%.2f".format(result.prize)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = GreenSuccess
                        )
                    }
                }

                if (result.refund > 0) {
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Entry Fee Refunded", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "✅ ₹${"%.2f".format(result.refund)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = GoldAccent
                        )
                    }
                }

                if (result.prize > 0 || result.refund > 0) {
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("New Wallet Balance", color = TextMuted, fontSize = 14.sp)
                        Text(
                            text = "₹${"%.2f".format(currentBal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CyanPrimary
                        )
                    }
                }

                if (result.reason != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = RedError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reason: ${result.reason}",
                            color = RedError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF36D399)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("▶ PLAY AGAIN", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("📜 HISTORY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
            }

            OutlinedButton(
                onClick = onHome,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("HOME", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
    }
}
