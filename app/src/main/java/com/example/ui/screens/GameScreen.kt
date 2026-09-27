package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameState

@Composable
fun GameScreen(
    state: GameState,
    onOptionSelected: (String) -> Unit,
    onQuit: () -> Unit,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    if (state.isFinished) {
        GameResultScreen(
            state = state,
            onPlayAgain = onPlayAgain,
            onGoHome = onGoHome
        )
        return
    }

    val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
    val totalQ = state.questions.size
    val timerPct = (state.remainingSeconds / state.totalTimePerQuestion).coerceIn(0f, 1f)
    val timerColor = when {
        timerPct > 0.5f -> GreenSuccess
        timerPct > 0.25f -> GoldAccent
        else -> RedError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SurfaceDark,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Text(
                    text = "Q ${state.currentIndex + 1} / $totalQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = SurfaceDark,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Text(
                    text = "Score ${fmtNum(state.score)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(status = if (state.isPractice) "PRACTICE" else "LIVE")

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onQuit,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedError.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Quit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Countdown Timer Bar
        LinearProgressIndicator(
            progress = { timerPct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = timerColor,
            trackColor = SurfaceDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question Card
        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentQ.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${currentQ.category} · ${currentQ.difficulty}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options List
        val options = listOf(
            Triple("A", currentQ.optionA, currentQ.correctAnswer == "A"),
            Triple("B", currentQ.optionB, currentQ.correctAnswer == "B"),
            Triple("C", currentQ.optionC, currentQ.correctAnswer == "C"),
            Triple("D", currentQ.optionD, currentQ.correctAnswer == "D")
        )

        options.forEach { (key, text, isCorrect) ->
            val isSelected = state.selectedOption == key
            val optionBg = when {
                state.isLocked && isCorrect -> GreenSuccess.copy(alpha = 0.2f)
                state.isLocked && isSelected && !isCorrect -> RedError.copy(alpha = 0.2f)
                else -> SurfaceCard
            }
            val optionBorder = when {
                state.isLocked && isCorrect -> GreenSuccess
                state.isLocked && isSelected && !isCorrect -> RedError
                else -> BorderSubtle
            }

            Surface(
                color = optionBg,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, optionBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable(enabled = !state.isLocked) {
                        onOptionSelected(key)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    state.isLocked && isCorrect -> GreenSuccess
                                    state.isLocked && isSelected && !isCorrect -> RedError
                                    else -> SurfaceDark
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (state.isLocked && (isCorrect || isSelected)) BgDark else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Feedback Banner
        if (state.isLocked && state.lastIsCorrect != null) {
            Surface(
                color = if (state.lastIsCorrect) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (state.lastIsCorrect) GreenSuccess else RedError),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.lastIsCorrect) "✅ Correct · +${state.lastGainedPoints} pts" else "❌ Incorrect · Correct: ${currentQ.correctAnswer}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (state.lastIsCorrect) GreenSuccess else RedError
                    )
                }
            }
        }
    }
}

@Composable
fun GameResultScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val accuracy = if (state.answeredCount > 0) (state.correctCount.toDouble() / state.answeredCount.toDouble()) * 100 else 0.0
    val avgTime = if (state.times.isNotEmpty()) state.times.average() else 0.0
    val won = accuracy >= 60.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (won) GoldAccent.copy(alpha = 0.5f) else RedError.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            if (won) listOf(GoldAccent.copy(alpha = 0.2f), GreenSuccess.copy(alpha = 0.15f))
                            else listOf(RedError.copy(alpha = 0.18f), BlueSecondary.copy(alpha = 0.1f))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (won) "🏆" else "💪", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (won) "GREAT RUN!" else "KEEP TRAINING",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.isPractice) "Practice Session Complete" else "Match Complete",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = fmtNum(state.score),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text("total points", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.correctCount}/${state.answeredCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("CORRECT", fontSize = 10.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmtPct(accuracy), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("ACCURACY", fontSize = 10.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.2fs", avgTime), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("AVG TIME", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArenaGhostButton(
                text = "Home",
                onClick = onGoHome,
                modifier = Modifier.weight(1f)
            )
            ArenaButton(
                text = "Play Again",
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
