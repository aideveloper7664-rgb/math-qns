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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.TournamentLiveDto
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TournamentsScreen(
    tournaments: List<TournamentLiveDto>,
    isLoading: Boolean,
    joinedTournamentIds: Set<String> = emptySet(),
    joiningTournamentId: String? = null,
    onJoinTournament: (String) -> Unit = {},
    onRefresh: () -> Unit,
    onStartAutoRefresh: () -> Unit,
    onStopAutoRefresh: () -> Unit,
    onBack: () -> Unit
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

    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
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
                    title = "Tournaments",
                    subtitle = "Compete in live math arenas for mega prize pools",
                    onBack = onBack
                )
            }
            if (isLoading) {
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

        if (tournaments.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No active tournaments right now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Check back soon for upcoming speed arena battles!",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(tournaments, key = { it.id }) { t ->
                    TournamentLiveCard(
                        t = t,
                        currentTimeMs = currentTimeMs,
                        alreadyJoined = joinedTournamentIds.contains(t.id),
                        joining = joiningTournamentId == t.id,
                        onJoin = { onJoinTournament(t.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentLiveCard(
    t: TournamentLiveDto,
    currentTimeMs: Long,
    alreadyJoined: Boolean = false,
    joining: Boolean = false,
    onJoin: () -> Unit = {}
) {
    val startMs = remember(t.startTime) { parseIsoToMillis(t.startTime) }
    val endMs = remember(t.endTime) { parseIsoToMillis(t.endTime) }

    val countdownText = remember(currentTimeMs, startMs, endMs) {
        when {
            startMs != null && currentTimeMs < startMs -> {
                val diff = startMs - currentTimeMs
                "⏳ Starts in " + formatDuration(diff)
            }
            endMs != null && currentTimeMs < endMs -> {
                val diff = endMs - currentTimeMs
                "🔥 Ends in " + formatDuration(diff)
            }
            endMs != null && currentTimeMs >= endMs -> "Ended"
            else -> "LIVE"
        }
    }

    val isLive = startMs != null && currentTimeMs >= startMs && (endMs == null || currentTimeMs < endMs)
    val statusClean = (t.status ?: "").uppercase()
    val canJoin = (statusClean == "UPCOMING" || statusClean == "LIVE" || isLive) && (endMs == null || currentTimeMs < endMs) && !alreadyJoined
    val entryFeeVal = t.entryFee?.toInt() ?: 0

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, if (isLive) CyanPrimary.copy(alpha = 0.6f) else BorderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t.name ?: "Math Tournament",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusPill(status = t.status ?: if (isLive) "LIVE" else "UPCOMING")
            }

            if (!t.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = t.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Entry & Prize info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    if (entryFeeVal == 0) {
                        Text("🎁 FREE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GreenSuccess)
                    } else {
                        Text("₹$entryFeeVal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("${t.playersJoined ?: 0} / ${t.maxPlayers ?: 0}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PRIZE POOL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("₹${t.prizePool?.toInt() ?: 0}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Date & countdown
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${formatTournamentDates(t.startTime, t.endTime)}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = countdownText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) CyanPrimary else GoldAccent
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action Button
            if (canJoin) {
                Button(
                    onClick = onJoin,
                    enabled = !joining,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF36D399)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (joining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BgDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (entryFeeVal == 0) "🎯 JOIN FREE" else "🎯 JOIN (₹$entryFeeVal)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = BgDark
                        )
                    }
                }
            } else if (alreadyJoined) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = GreenSuccess.copy(alpha = 0.20f),
                        disabledContentColor = GreenSuccess
                    ),
                    border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "✅ JOINED",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess
                    )
                }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = SurfaceDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = if (endMs != null && currentTimeMs >= endMs) "Tournament Ended" else "Registration Closed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun parseIsoToMillis(isoString: String?): Long? {
    if (isoString.isNullOrBlank()) return null
    return try {
        val clean = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        parser.parse(clean)?.time
    } catch (e: Exception) {
        null
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0s"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatTournamentDates(startIso: String?, endIso: String?): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    val startStr = startIso?.let {
        parseIsoToMillis(it)?.let { ms -> formatter.format(Date(ms)) }
    } ?: "Now"
    val endStr = endIso?.let {
        parseIsoToMillis(it)?.let { ms -> formatter.format(Date(ms)) }
    } ?: "Open"
    return "$startStr - $endStr"
}
