package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KnockoutTournamentEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun KnockoutScreen(
    tournaments: List<KnockoutTournamentEntity>,
    onBack: () -> Unit
) {
    var selectedBracketDialog by remember { mutableStateOf<KnockoutTournamentEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "Knockout Tournaments", subtitle = "Bracket elimination showdowns", onBack = onBack)

        if (tournaments.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Text("No active knockout tournaments right now.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tournaments) { kt ->
                    ArenaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(kt.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Entry ${fmtMoney(kt.entryFee)} · ${kt.currentPlayers}/${kt.maxPlayers} players", fontSize = 11.sp, color = TextMuted)
                            }
                            StatusPill(status = kt.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Prize Pool", fontSize = 12.sp, color = TextMuted)
                            Text(fmtMoney(kt.prizePool), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArenaButton(
                                text = "View Bracket",
                                onClick = { selectedBracketDialog = kt },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedBracketDialog != null) {
        val kt = selectedBracketDialog!!
        AlertDialog(
            onDismissRequest = { selectedBracketDialog = null },
            containerColor = SurfaceCard,
            title = {
                Text("${kt.name} Bracket", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("ROUND 1 (Quarter-Finals)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ProSolver vs CyberSigma", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("180 - 120 (WIN)", fontSize = 11.sp, color = GreenSuccess)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ApexMath vs VectorX", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("210 - 190 (WIN)", fontSize = 11.sp, color = GreenSuccess)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("FINALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ProSolver vs ApexMath", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("TBD", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBracketDialog = null }) {
                    Text("Close", color = CyanPrimary)
                }
            }
        )
    }
}
