package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArenaCard
import com.example.ui.components.ArenaGhostButton
import com.example.ui.components.fmtMoney
import com.example.ui.theme.*

private val Gold = Color(0xFFFECA57)
private val BgTop = Color(0xFF0F0C29)
private val BgMid = Color(0xFF302B63)
private val BgBot = Color(0xFF24243E)

@Composable
fun MatchmakingScreen(
    mode: String,
    entryFee: Double,
    elapsedSeconds: Int = 0,
    matchFound: Boolean = false,
    matchId: String? = null,
    onCancel: () -> Unit,
    onPlayPractice: () -> Unit,
    onMatchNavigate: (String) -> Unit = {}
) {
    // Navigate immediately when match is found
    LaunchedEffect(matchFound, matchId) {
        if (matchFound && !matchId.isNullOrBlank()) {
            onMatchNavigate(matchId)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRot"
    )

    val modeMeta = defaultModes.find { it.id == mode } ?: defaultModes.first()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgTop, BgMid, BgBot))
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            if (matchFound) {
                // ── MATCH FOUND ──────────────────────────────────
                Text(
                    text = "⚡",
                    fontSize = 56.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "MATCH FOUND!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Starting game…",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            } else {
                // ── SEARCHING ────────────────────────────────────

                // Spinner ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(4.dp, CyanPrimary.copy(alpha = 0.15f), CircleShape)
                        .rotate(rotation)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, CyanPrimary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(modeMeta.icon, fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Finding Opponent…",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Searching for players with similar skill",
                    fontSize = 13.sp,
                    color = TextMuted.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                // Timer
                Text(
                    text = "${elapsedSeconds}s",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold
                )

                Spacer(modifier = Modifier.height(20.dp))

                ArenaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mode", fontSize = 12.sp, color = TextMuted)
                        Text(modeMeta.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Players Needed", fontSize = 12.sp, color = TextMuted)
                        Text("${modeMeta.players}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Entry Fee", fontSize = 12.sp, color = TextMuted)
                        Text(
                            if (entryFee == 0.0) "FREE" else fmtMoney(entryFee),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status", fontSize = 12.sp, color = TextMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CyanPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Searching", fontSize = 12.sp, color = CyanPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ArenaGhostButton(
                    text = "Cancel Search",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
