package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GameHistoryItem
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.HistoryUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GameHistoryScreen(
    state: HistoryUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PageHeader(
                    title = "Game History",
                    subtitle = "Your battle record and wallet earnings",
                    onBack = onBack
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CyanPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimary
                    )
                }
            }
        }

        if (state.error != null && state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        color = RedError,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Retry", color = BgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (state.isLoading && state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No games played yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Play matches to see your score and reward history here!",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.sessionId }) { item ->
                    GameHistoryCard(item = item)
                }
            }
        }
    }
}

@Composable
fun GameHistoryCard(item: GameHistoryItem) {
    val isWin = item.prizeEarned > 0
    val formattedDate = remember(item.startedAt) {
        formatHistoryDate(item.startedAt)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: status icon + date + Net Impact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isWin) GreenSuccess.copy(alpha = 0.18f) else RedError.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isWin) "✅" else "❌", fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isWin) "Victory" else "Game Over",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWin) GreenSuccess else TextPrimary
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Net wallet impact
                val isPositive = item.walletImpact >= 0
                val netStr = if (isPositive) {
                    "+₹${"%.2f".format(item.walletImpact)}"
                } else {
                    "-₹${"%.2f".format(kotlin.math.abs(item.walletImpact))}"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositive) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isPositive) GreenSuccess.copy(alpha = 0.4f) else RedError.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Net: $netStr",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositive) GreenSuccess else RedError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                color = BorderSubtle,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Middle row: score & answers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: ${item.totalScore} pts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Text(
                    text = "${item.correctAnswers} / ${item.questionsAttempted} Correct",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Breakdown: Entry, Prize, Refund
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Entry −₹${item.entryFee.toInt()}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (item.prizeEarned > 0) {
                    Surface(
                        color = GreenSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Prize +₹${"%.2f".format(item.prizeEarned)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (item.refundAmount != null && item.refundAmount > 0) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Refund +₹${"%.2f".format(item.refundAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun formatHistoryDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Recent"
    return try {
        val cleanIso = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(cleanIso) ?: return isoString
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        formatter.format(date)
    } catch (e: Exception) {
        isoString
    }
}
