package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArenaCard
import com.example.ui.components.ArenaGhostButton
import com.example.ui.components.fmtMoney
import com.example.ui.theme.*

@Composable
fun MatchmakingScreen(
    mode: String,
    entryFee: Double,
    onCancel: () -> Unit,
    onPlayPractice: () -> Unit
) {
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
            .background(BgDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            // Animated Ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(4.dp, CyanPrimary.copy(alpha = 0.2f), CircleShape)
                    .rotate(rotation)
                    .border(4.dp, CyanPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(modeMeta.icon, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Finding Opponents…",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Text(
                text = "${modeMeta.name} · Entry ${if (entryFee == 0.0) "FREE" else fmtMoney(entryFee)} · ${modeMeta.questions} Questions",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

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
                    Text(if (entryFee == 0.0) "FREE" else fmtMoney(entryFee), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status", fontSize = 12.sp, color = TextMuted)
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyanPrimary,
                        strokeWidth = 2.dp
                    )
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
