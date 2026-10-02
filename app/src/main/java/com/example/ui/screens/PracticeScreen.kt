package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PracticeScreen(
    onBack: () -> Unit
) {
    var questionIndex by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var timeLeftSec by remember { mutableIntStateOf(10) }
    var isAnswered by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isGameOver by remember { mutableStateOf(false) }

    // Generate math practice questions locally on the fly
    val currentQuestion = remember(questionIndex) {
        val op = listOf("+", "-", "×").random()
        val num1: Int
        val num2: Int
        val correctVal: Int
        when (op) {
            "+" -> {
                num1 = (5..50).random()
                num2 = (5..50).random()
                correctVal = num1 + num2
            }
            "-" -> {
                num1 = (20..99).random()
                num2 = (5..num1).random()
                correctVal = num1 - num2
            }
            else -> {
                num1 = (2..12).random()
                num2 = (2..12).random()
                correctVal = num1 * num2
            }
        }
        val options = mutableListOf(correctVal)
        while (options.size < 4) {
            val wrong = correctVal + (-10..10).random()
            if (wrong != correctVal && !options.contains(wrong)) {
                options.add(wrong)
            }
        }
        options.shuffle()
        val correctLetter = when (options.indexOf(correctVal)) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            else -> "D"
        }
        PracticeQuestionData(
            text = "$num1 $op $num2 = ?",
            optionA = options[0].toString(),
            optionB = options[1].toString(),
            optionC = options[2].toString(),
            optionD = options[3].toString(),
            correctOption = correctLetter,
            correctValue = correctVal.toString()
        )
    }

    LaunchedEffect(questionIndex, isAnswered, isGameOver) {
        if (!isAnswered && !isGameOver) {
            timeLeftSec = 10
            while (timeLeftSec > 0 && !isAnswered) {
                delay(1000L)
                timeLeftSec--
            }
            if (timeLeftSec <= 0 && !isAnswered) {
                isAnswered = true
                selectedOption = null
                delay(1200L)
                if (questionIndex >= 10) {
                    isGameOver = true
                } else {
                    questionIndex++
                    isAnswered = false
                }
            }
        }
    }

    fun submitPracticeAnswer(option: String) {
        if (isAnswered || isGameOver) return
        selectedOption = option
        isAnswered = true
        if (option == currentQuestion.correctOption) {
            score += 10
            correctCount++
        }
    }

    fun nextQuestion() {
        if (questionIndex >= 10) {
            isGameOver = true
        } else {
            questionIndex++
            isAnswered = false
            selectedOption = null
        }
    }

    if (isGameOver) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎯", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Practice Complete!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Practice mode - No entry fee, pure skill training",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text("$score pts", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CyanPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACCURACY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text("$correctCount / 10", fontSize = 18.sp, fontWeight = FontWeight.Black, color = GreenSuccess)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            questionIndex = 1
                            score = 0
                            correctCount = 0
                            isAnswered = false
                            selectedOption = null
                            isGameOver = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("🔄 Play Practice Again", fontWeight = FontWeight.Bold, color = BgDark)
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("🏠 Return to Home", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Surface(
                    color = GreenSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🎯 PRACTICE MODE (FREE)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text("Score: $score", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyanPrimary)
            }

            Spacer(Modifier.height(16.dp))

            // Progress & Timer Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question $questionIndex of 10", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Text("⏳ ${timeLeftSec}s", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (timeLeftSec <= 3) RedError else GoldAccent)
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { questionIndex / 10f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = CyanPrimary,
                trackColor = SurfaceDark
            )

            Spacer(Modifier.height(24.dp))

            // Question Box
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentQuestion.text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Options
            listOf(
                "A" to currentQuestion.optionA,
                "B" to currentQuestion.optionB,
                "C" to currentQuestion.optionC,
                "D" to currentQuestion.optionD
            ).forEach { (optKey, optVal) ->
                val isSelected = selectedOption == optKey
                val isCorrect = isAnswered && currentQuestion.correctOption == optKey
                val isWrong = isSelected && !isCorrect

                Surface(
                    color = when {
                        isCorrect -> GreenSuccess.copy(alpha = 0.25f)
                        isWrong -> RedError.copy(alpha = 0.25f)
                        isSelected -> CyanPrimary.copy(alpha = 0.25f)
                        else -> SurfaceCard
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCorrect -> GreenSuccess
                            isWrong -> RedError
                            isSelected -> CyanPrimary
                            else -> BorderSubtle
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Button(
                        onClick = { submitPracticeAnswer(optKey) },
                        enabled = !isAnswered,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(optKey, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = optVal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (isAnswered) {
                Button(
                    onClick = { nextQuestion() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (questionIndex >= 10) "Finish Practice" else "Next Question →", fontWeight = FontWeight.Bold, color = BgDark)
                }
            }
        }
    }
}

private data class PracticeQuestionData(
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String,
    val correctValue: String
)
