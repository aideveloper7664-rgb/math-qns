package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.KnockoutMatchupDto
import com.example.data.remote.KnockoutParticipantDto
import com.example.data.remote.KnockoutTournamentDto
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun KnockoutScreen(
    tournaments: List<KnockoutTournamentDto>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onLoadBracket: (String) -> Unit = {},
    bracketMatchups: List<KnockoutMatchupDto> = emptyList(),
    bracketParticipants: List<KnockoutParticipantDto> = emptyList(),
    bracketLoading: Boolean = false,
    joinedTournamentIds: Set<String> = emptySet(),
    joiningTournamentId: String? = null,
    onJoinTournament: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var selectedTournament by remember { mutableStateOf<KnockoutTournamentDto?>(null) }

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
                    title = "Knockout Tournaments",
                    subtitle = "Bracket elimination showdowns",
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

        when {
            isLoading && tournaments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            }
            errorMessage != null && tournaments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = RedError, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Retry", color = BgDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            tournaments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🥊", fontSize = 48.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No knockout tournaments right now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Bracket tournaments will appear here when scheduled.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tournaments, key = { it.id }) { kt ->
                        ArenaCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = kt.name ?: "Knockout Cup",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Max ${kt.maxPlayers ?: 16} Players · ${kt.totalRounds ?: 4} Rounds",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                StatusPill(status = kt.status ?: "REGISTRATION")
                            }

                            if (!kt.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = kt.description,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Text(
                                        text = "₹${kt.entryFee?.toInt() ?: 0}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("CURRENT ROUND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Text(
                                        text = "Round ${kt.currentRound ?: 1} / ${kt.totalRounds ?: 4}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyanPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PLAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Text(
                                        text = "${kt.maxPlayers ?: 16} Slots",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val isJoined = joinedTournamentIds.contains(kt.id)
                            val isJoining = joiningTournamentId == kt.id
                            val isRegistration = (kt.status ?: "").equals("REGISTRATION", ignoreCase = true)
                            val entryFeeVal = kt.entryFee?.toInt() ?: 0

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isRegistration) {
                                    if (isJoined) {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                disabledContainerColor = GreenSuccess.copy(alpha = 0.20f),
                                                disabledContentColor = GreenSuccess
                                            ),
                                            border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "✅ JOINED",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = GreenSuccess
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { onJoinTournament(kt.id) },
                                            enabled = !isJoining,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF36D399)
                                            )
                                        ) {
                                            if (isJoining) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = BgDark,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(
                                                    text = "🎯 JOIN (₹$entryFeeVal)",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = BgDark
                                                )
                                            }
                                        }
                                    }
                                }

                                ArenaButton(
                                    text = "🏆 View Bracket",
                                    onClick = {
                                        selectedTournament = kt
                                        onLoadBracket(kt.id)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Live Bracket Breakdown Dialog ──
    if (selectedTournament != null) {
        val kt = selectedTournament!!
        val nameMap = remember(bracketParticipants) {
            bracketParticipants.associate { it.userId to (it.userName ?: it.userId.take(8)) }
        }

        AlertDialog(
            onDismissRequest = { selectedTournament = null },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "${kt.name ?: "Knockout"} Bracket",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                if (bracketLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyanPrimary)
                    }
                } else if (bracketMatchups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Bracket will be generated when registration fills.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val maxRound = bracketMatchups.maxOfOrNull { it.roundNumber } ?: 1
                    val roundsGrouped = bracketMatchups.groupBy { it.roundNumber }.toSortedMap()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        roundsGrouped.forEach { (roundNum, matchups) ->
                            item {
                                val roundTitle = when {
                                    roundNum == maxRound -> "🏆 FINALS"
                                    roundNum == maxRound - 1 -> "🥊 SEMI-FINALS"
                                    roundNum == 1 -> "ROUND 1 (Quarter-Finals)"
                                    else -> "ROUND $roundNum"
                                }

                                Text(
                                    text = roundTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roundNum == maxRound) GoldAccent else CyanPrimary,
                                    modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                                )

                                matchups.forEach { m ->
                                    val p1Name = nameMap[m.player1Id ?: ""] ?: (m.player1Id?.take(8) ?: "TBD")
                                    val p2Name = if (m.isBotMatch && !m.botDisplayName.isNullOrBlank()) {
                                        m.botDisplayName
                                    } else {
                                        nameMap[m.player2Id ?: ""] ?: (m.player2Id?.take(8) ?: "TBD")
                                    }
                                    val p1Win = m.winnerId != null && m.winnerId == m.player1Id
                                    val p2Win = m.winnerId != null && (m.winnerId == m.player2Id || (m.isBotMatch && m.winnerId != m.player1Id))

                                    Surface(
                                         color = SurfaceDark,
                                         shape = RoundedCornerShape(10.dp),
                                         border = BorderStroke(1.dp, BorderSubtle),
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .padding(vertical = 3.dp)
                                     ) {
                                         Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = p1Name,
                                                     fontSize = 12.sp,
                                                     fontWeight = if (p1Win) FontWeight.Bold else FontWeight.Normal,
                                                     color = if (p1Win) GreenSuccess else TextPrimary,
                                                     modifier = Modifier.weight(1f)
                                                 )
                                                 Text(
                                                     text = if (m.player1Score != null && (m.player2Score != null || m.botScore != null))
                                                         "${m.player1Score} - ${m.player2Score ?: m.botScore ?: 0}"
                                                     else "VS",
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = if (m.winnerId != null) GoldAccent else TextMuted,
                                                     modifier = Modifier.padding(horizontal = 6.dp)
                                                 )
                                                 Text(
                                                     text = p2Name,
                                                     fontSize = 12.sp,
                                                     fontWeight = if (p2Win) FontWeight.Bold else FontWeight.Normal,
                                                     color = if (p2Win) GreenSuccess else TextPrimary,
                                                     textAlign = TextAlign.End,
                                                     modifier = Modifier.weight(1f)
                                                 )
                                             }

                                             if (m.winnerId != null) {
                                                 val winnerName = if (p1Win) p1Name else p2Name
                                                 Spacer(modifier = Modifier.height(2.dp))
                                                 Text(
                                                     text = "🏆 $winnerName advanced",
                                                     fontSize = 10.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = Color(0xFF36D399)
                                                 )
                                             }
                                         }
                                     }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTournament = null }) {
                    Text("Close", color = CyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
