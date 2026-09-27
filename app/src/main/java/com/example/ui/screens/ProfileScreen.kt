package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    user: UserEntity?,
    onUpdateProfile: (String, String?) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (user == null) return

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(user.displayName) }
    var editPhoto by remember { mutableStateOf(user.photoUrl ?: "") }

    val xpPerLevel = 500
    val level = (user.xp / xpPerLevel) + 1
    val winRate = if (user.matchesPlayed > 0) (user.wins.toDouble() / user.matchesPlayed.toDouble()) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Profile", subtitle = "Your arena identity and battle record")

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarCircle(displayName = user.displayName, size = 72.dp, fontSize = 28)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(user.displayName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        UserBadges(isVerified = user.isVerified, hasGoldCrown = user.hasGoldCrown)
                    }

                    Text(user.email, fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RankBadge(rank = user.rank)
                        if (user.vipTier != "none") {
                            StatusPill(status = "💎 VIP ${user.vipTier.uppercase()}")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ArenaGhostButton(
                        text = "✏️ Edit Profile",
                        onClick = {
                            editName = user.displayName
                            editPhoto = user.photoUrl ?: ""
                            showEditDialog = true
                        }
                    )
                }
            }

            // Stats Card
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Progression", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtNum(user.mmr), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("MMR", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtNum(user.xp), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("XP", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.matchesPlayed}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("MATCHES", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtPct(winRate), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                        Text("WIN RATE", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            // Battle Record Card
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Battle Record", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.wins}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                        Text("WINS", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.losses}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RedError)
                        Text("LOSSES", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtMoney(user.totalWinnings), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                        Text("WINNINGS", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            // Quick Menu Links
            ArenaCard(modifier = Modifier.padding(bottom = 20.dp)) {
                val menu = listOf(
                    Triple("transactions", "Match & Wallet History", "📜"),
                    Triple("badges", "My Badges", "🏅"),
                    Triple("referral", "Refer & Earn", "🎁"),
                    Triple("settings", "Settings & Account", "⚙️")
                )

                menu.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.first) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.third, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(item.second, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                    }
                    if (idx < menu.size - 1) {
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPhoto,
                        onValueChange = { editPhoto = it },
                        label = { Text("Photo URL (optional)", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                ArenaButton(
                    text = "Save",
                    onClick = {
                        onUpdateProfile(editName, editPhoto)
                        showEditDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
