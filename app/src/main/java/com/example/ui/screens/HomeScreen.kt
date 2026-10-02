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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.remote.GameSessionDto
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    user: UserEntity?,
    recentSessions: List<GameSessionDto> = emptyList(),
    onStartGame: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (user == null) return

    val balance = user.walletBalance
    val canPlay = balance >= 10.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // ── 1. Profile Card ─────────────────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteAvatar(
                        photoUrl = user.photoUrl,
                        displayName = user.displayName,
                        size = 56.dp,
                        fontSize = 24
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        UserNameWithBadges(
                            displayName = user.displayName,
                            isVerified = user.isVerified,
                            hasGoldCrown = user.hasGoldCrown,
                            vipTier = user.vipTier,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RankBadge(rank = user.rank)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "MMR ${fmtNum(user.mmr)} • XP ${fmtNum(user.xp)}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Wallet Card ──────────────────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WALLET BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = if (canPlay) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (canPlay) GreenSuccess.copy(alpha = 0.4f) else RedError.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (canPlay) "READY TO PLAY" else "LOW BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canPlay) GreenSuccess else RedError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "₹${"%.2f".format(balance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate("deposit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = BgDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add Money", color = BgDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onNavigate("withdraw") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Withdraw", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── 3. Big "START GAME" Button (CRITICAL) ───────────────────────────
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF36D399),
                        disabledContainerColor = Color(0xFF36D399).copy(alpha = 0.35f)
                    ),
                    enabled = canPlay,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "▶",
                            fontSize = 22.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "START GAME",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Text(
                                text = "Entry Fee: ₹10 • Progressive Quiz • Win up to 10×",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                if (!canPlay) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ Minimum ₹10 balance required to play.",
                            color = RedError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Deposit Now",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onNavigate("practice") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
                ) {
                    Text("🎯 Practice Mode (Free Play)", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 14.sp)
                }
            }
        }

        // ── VIP Pass Promotion Banner ────────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigate("vip") }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    GoldAccent.copy(alpha = 0.15f),
                                    PurpleAccent.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, GoldAccent),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👑", fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("VIP Pass & Perks", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                    Surface(color = GoldAccent, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = if (user.vipTier != "none") user.vipTier.uppercase() else "HOT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BgDark,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (user.vipTier != "none") "Active member • Tap to view plans" else "Gold crown, multipliers & exclusive perks",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open VIP",
                            tint = GoldAccent
                        )
                    }
                }
            }
        }

        // ── Quick Actions ───────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("referral") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎁", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Refer & Earn", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Invite friends", fontSize = 11.sp, color = GreenSuccess)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("leaderboard") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Leaderboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Global Rankings", fontSize = 11.sp, color = CyanPrimary)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("tournaments") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚔️", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Tournaments", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Live Arenas", fontSize = 11.sp, color = PurpleAccent)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("knockout") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🥊", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Knockouts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Bracket Arenas", fontSize = 11.sp, color = CyanPrimary)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("game_history") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📜", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Game Match History", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("View your past matches, accuracy, and wallet rewards", fontSize = 11.sp, color = GoldAccent)
                            }
                        }
                    }
                }
            }
        }


        // ── 5. Recent Game Sessions ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT GAMES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                if (recentSessions.isNotEmpty()) {
                    Text(
                        text = "${recentSessions.size} Sessions",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        if (recentSessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎮", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No games played yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap START GAME above to start your progressive streak!",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(recentSessions.take(10)) { session ->
                SessionHistoryCard(session = session)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: GameSessionDto) {
    val isWin = session.status.equals("COMPLETED", ignoreCase = true) || session.totalScore >= 100
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isWin) GreenSuccess.copy(alpha = 0.2f) else RedError.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isWin) "🏆" else "💔", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reached Q${maxOf(1, session.highestQuestion)} • ${session.correctAnswers} Correct",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Score: ${session.totalScore} pts",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val prize = session.totalScore / 100.0
                if (prize > 0) {
                    Text(
                        text = "+₹${"%.2f".format(prize)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = GreenSuccess
                    )
                } else {
                    Text(
                        text = "₹0.00",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                Text(
                    text = session.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWin) GreenSuccess else RedError
                )
            }
        }
    }
}
