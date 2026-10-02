package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LeaderboardRow
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LeaderboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderboardScreen(
    currentUserId: String?,
    state: LeaderboardUiState,
    onMetricChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onStartAutoRefresh: () -> Unit,
    onStopAutoRefresh: () -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> onStartAutoRefresh()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> onStopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            onStartAutoRefresh()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopAutoRefresh()
        }
    }

    val top1 = state.rows.getOrNull(0)
    val top2 = state.rows.getOrNull(1)
    val top3 = state.rows.getOrNull(2)
    val remaining = if (state.rows.size > 3) state.rows.drop(3) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        // ── Header & Refresh Button ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PageHeader(title = "Leaderboard", subtitle = "Top ranked solvers in Math Baazi")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CyanPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                }
            }
        }

        // Updated time or offline status
        if (state.errorMessage != null && state.rows.isNotEmpty()) {
            Text(
                text = "Offline, showing last update",
                fontSize = 11.sp,
                color = RedError,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else if (state.updatedAtMs != null) {
            val formattedTime = remember(state.updatedAtMs) {
                SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(state.updatedAtMs))
            }
            Text(
                text = "Updated $formattedTime",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // ── Tab Selector ────────────────────────────────────────────────────
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("🏆 Best Score", "best_score"),
                    Pair("💰 Winnings", "winnings"),
                    Pair("📅 Today", "today")
                ).forEach { (label, metricKey) ->
                    val isSelected = state.metric == metricKey
                    Surface(
                        color = if (isSelected) CyanPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onMetricChange(metricKey) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BgDark else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ── My Rank Card ────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.me != null) {
                    Surface(
                        color = CyanPrimary.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${state.me.rank}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your rank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "Keep playing to climb higher!",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = formatValue(state.metric, state.me.value),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanPrimary
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Play a game to get on the leaderboard",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // ── Main Content Area ───────────────────────────────────────────────
        if (state.errorMessage != null && state.rows.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("❌", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Could not load leaderboard", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RedError)
                    Spacer(Modifier.height(4.dp))
                    Text(state.errorMessage, fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)) {
                        Text("Retry", color = BgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (state.isLoading && state.rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (state.rows.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No scores yet. Be the first!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Play matches to climb the global leaderboard!", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Top 3 Podium Card ───────────────────────────────────────
                if (top1 != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, BorderStrong),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                GoldAccent.copy(alpha = 0.08f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(vertical = 16.dp, horizontal = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 🥈 2nd Place
                                    if (top2 != null) {
                                        PodiumPillarRow(
                                            row = top2,
                                            medal = "🥈",
                                            podiumColor = Color(0xFFA9B4C4),
                                            pillarHeight = 90.dp,
                                            metric = state.metric,
                                            isMe = top2.userId == currentUserId,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    // 🥇 1st Place (Champion)
                                    PodiumPillarRow(
                                        row = top1,
                                        medal = "🥇",
                                        podiumColor = GoldAccent,
                                        pillarHeight = 115.dp,
                                        metric = state.metric,
                                        isMe = top1.userId == currentUserId,
                                        modifier = Modifier.weight(1.15f)
                                    )

                                    // 🥉 3rd Place
                                    if (top3 != null) {
                                        PodiumPillarRow(
                                            row = top3,
                                            medal = "🥉",
                                            podiumColor = Color(0xFFCD7F32),
                                            pillarHeight = 75.dp,
                                            metric = state.metric,
                                            isMe = top3.userId == currentUserId,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Ranks 4 to 50 ───────────────────────────────────────────
                if (remaining.isNotEmpty()) {
                    item {
                        Text(
                            text = "CONTENDERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(remaining, key = { it.userId }) { row ->
                        val isMe = row.userId == currentUserId

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isMe) CyanPrimary.copy(alpha = 0.08f) else SurfaceDark,
                            border = if (isMe) BorderStroke(1.5.dp, CyanPrimary) else BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Number
                                Text(
                                    text = "#${row.rank}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) CyanPrimary else TextMuted,
                                    modifier = Modifier.width(34.dp)
                                )

                                RemoteAvatar(url = row.photoUrl, displayName = row.displayName ?: "Player", size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        UserNameWithBadges(
                                            displayName = row.displayName ?: "Player",
                                            isVerified = row.isVerified,
                                            hasGoldCrown = row.hasGoldCrown,
                                            vipTier = row.vipTier,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isMe) {
                                            Text("(YOU)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyanPrimary)
                                        }
                                    }
                                }

                                Text(
                                    text = formatValue(state.metric, row.value),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumPillarRow(
    row: LeaderboardRow,
    medal: String,
    podiumColor: Color,
    pillarHeight: androidx.compose.ui.unit.Dp,
    metric: String,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(medal, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))

        RemoteAvatar(
            url = row.photoUrl,
            displayName = row.displayName ?: "Player",
            size = if (row.rank == 1) 48.dp else 40.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        UserNameWithBadges(
            displayName = row.displayName ?: "Player",
            isVerified = row.isVerified,
            hasGoldCrown = row.hasGoldCrown,
            vipTier = row.vipTier,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = formatValue(metric, row.value),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = podiumColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = podiumColor.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, if (isMe) CyanPrimary else podiumColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "#${row.rank}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = podiumColor
                )
            }
        }
    }
}

private fun formatValue(metric: String, value: Double): String =
    if (metric == "winnings") "₹" + fmtNum(value.toInt()) else fmtNum(value.toInt())
