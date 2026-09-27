package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun PlayScreen(
    user: UserEntity?,
    onStartMatchmaking: (String, Double) -> Unit,
    onStartPractice: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedModeForDialog by remember { mutableStateOf<GameModeItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Play", subtitle = "Choose your battlefield")

            if (user?.paidGameplayRestricted == true) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Paid gameplay restricted", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Free practice modes remain available.", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        items(defaultModes) { mode ->
            ArenaCard(
                onClick = { selectedModeForDialog = mode },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.2f), BlueSecondary.copy(alpha = 0.2f))))
                            .border(1.dp, BorderStrong, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.icon, fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(mode.desc, fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    "${mode.players} players",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    "${mode.questions} Qs",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    "from ₹${mode.fees.minOrNull()?.toInt() ?: 10}",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("More Ways to Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("knockout") }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Knockout Tournaments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Bracket-style elimination tournaments", fontSize = 11.sp, color = TextMuted)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("tournaments") }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Multiplayer Tournaments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Large scale mass competitive events", fontSize = 11.sp, color = TextMuted)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Practice Arena", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            ArenaCard(modifier = Modifier.padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎯 Solo Practice", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("No entry fee, no rank impact. Infinite math drills.", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ArenaGhostButton(
                        text = "Start Practice",
                        onClick = onStartPractice
                    )
                }
            }
        }
    }

    // Entry Fee Selection Modal
    if (selectedModeForDialog != null) {
        val mode = selectedModeForDialog!!
        var selectedFee by remember { mutableStateOf(mode.fees.firstOrNull() ?: 10.0) }
        val platformCommission = 0.10
        val estPrize = (selectedFee * mode.players * (1.0 - platformCommission)).toInt()

        AlertDialog(
            onDismissRequest = { selectedModeForDialog = null },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(mode.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(mode.desc, fontSize = 12.sp, color = TextMuted)
                    }
                }
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Players", fontSize = 12.sp, color = TextMuted)
                        Text("${mode.players}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Questions", fontSize = 12.sp, color = TextMuted)
                        Text("${mode.questions}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Platform Fee", fontSize = 12.sp, color = TextMuted)
                        Text("10%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("SELECT ENTRY FEE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mode.fees.forEach { fee ->
                            val isSelected = fee == selectedFee
                            Surface(
                                color = if (isSelected) CyanPrimary else SurfaceDark,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanPrimary else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFee = fee }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (fee == 0.0) "FREE" else "₹${fee.toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BgDark else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Balance", fontSize = 12.sp, color = TextMuted)
                                Text(fmtMoney(user?.walletBalance ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. Winner Prize Pool", fontSize = 12.sp, color = TextMuted)
                                Text(fmtMoney(estPrize.toDouble()), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                ArenaButton(
                    text = "Confirm & Search",
                    onClick = {
                        val fee = selectedFee
                        selectedModeForDialog = null
                        onStartMatchmaking(mode.id, fee)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedModeForDialog = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
