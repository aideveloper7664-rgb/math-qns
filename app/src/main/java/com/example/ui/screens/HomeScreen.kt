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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchParticipantEntity
import com.example.data.model.TournamentEntity
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

data class GameModeItem(
    val id: String,
    val name: String,
    val icon: String,
    val players: Int,
    val questions: Int,
    val desc: String,
    val fees: List<Double>
)

val defaultModes = listOf(
    GameModeItem("1v1", "1v1 Duel", "⚔️", 2, 10, "Head-to-head speed battle", listOf(10.0, 25.0, 50.0, 100.0)),
    GameModeItem("2v2", "2v2 Squad", "🛡️", 4, 10, "Two teams of two players", listOf(20.0, 50.0, 100.0, 200.0)),
    GameModeItem("4v4", "4v4 Arena", "🏟️", 8, 10, "Two teams of four players", listOf(50.0, 100.0, 250.0, 500.0)),
    GameModeItem("mega", "Mega Tournament", "👑", 100, 15, "Massive battle royale", listOf(100.0, 250.0, 500.0, 1000.0))
)

@Composable
fun HomeScreen(
    user: UserEntity?,
    recentMatches: List<MatchParticipantEntity>,
    tournaments: List<TournamentEntity>,
    topUsers: List<UserEntity>,
    onQuickPlay: () -> Unit,
    onSelectMode: (GameModeItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (user == null) return

    val xpPerLevel = 500
    val level = (user.xp / xpPerLevel) + 1
    val xpIntoLevel = user.xp % xpPerLevel
    val xpProgress = (xpIntoLevel.toFloat() / xpPerLevel.toFloat()).coerceIn(0f, 1f)
    val winRate = if (user.matchesPlayed > 0) (user.wins.toDouble() / user.matchesPlayed.toDouble()) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Hero Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    CyanPrimary.copy(alpha = 0.15f),
                                    BlueSecondary.copy(alpha = 0.15f),
                                    PurpleAccent.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(
                                displayName = user.displayName,
                                size = 64.dp,
                                fontSize = 24
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.displayName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    UserBadges(isVerified = user.isVerified, hasGoldCrown = user.hasGoldCrown)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RankBadge(rank = user.rank)
                                    if (user.vipTier != "none") {
                                        Surface(
                                            color = GoldAccent.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "💎 ${user.vipTier.uppercase()}",
                                                color = GoldAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = SurfaceDark,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Text(
                                        text = "MMR ${fmtNum(user.mmr)}",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = SurfaceDark,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Text(
                                        text = "Level $level",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Win Rate ${fmtPct(winRate)}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // XP Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("XP · Level $level", fontSize = 11.sp, color = TextMuted)
                            Text("${fmtNum(xpIntoLevel)} / $xpPerLevel", fontSize = 11.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = CyanPrimary,
                            trackColor = SurfaceDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Wallet Balance Card
        item {
            ArenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Wallet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text(
                        text = "History →",
                        fontSize = 12.sp,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigate("transactions") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(text = "Available Balance", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = fmtMoney(user.walletBalance),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        if (user.lockedBalance > 0) {
                            Text(
                                text = "🔒 ${fmtMoney(user.lockedBalance)} locked in play",
                                fontSize = 11.sp,
                                color = GoldAccent
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigate("deposit") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDark),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("＋ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigate("withdraw") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Withdraw", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Play Banner
        item {
            ArenaGoldButton(
                text = "⚡ QUICK PLAY",
                onClick = onQuickPlay,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Game Modes Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Game Modes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "See all",
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate("play") }
                )
            }
        }

        // Modes Grid (2 columns or list)
        items(defaultModes) { mode ->
            ArenaCard(
                onClick = { onSelectMode(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.2f), BlueSecondary.copy(alpha = 0.2f))))
                            .border(1.dp, BorderStrong, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.icon, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(mode.desc, fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    "from ₹${mode.fees.minOrNull()?.toInt() ?: 10}",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    "${mode.questions} Qs",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

        // Quick Access Section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Quick Access", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                val quickLinks = listOf(
                    Triple("vip", "VIP Pass", "💎"),
                    Triple("referral", "Refer & Earn", "🎁"),
                    Triple("badges", "My Badges", "🏅"),
                    Triple("knockout", "Knockout Tournaments", "🎯")
                )

                quickLinks.forEachIndexed { index, link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(link.first) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(link.third, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(link.second, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
                    }
                    if (index < quickLinks.size - 1) {
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Live & Upcoming Tournaments Preview
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live & Upcoming Tournaments", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "All",
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate("tournaments") }
                )
            }

            if (tournaments.isEmpty()) {
                ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text("No tournaments right now. Check back soon!", fontSize = 12.sp, color = TextMuted)
                }
            } else {
                tournaments.take(2).forEach { t ->
                    ArenaCard(
                        onClick = { onNavigate("tournaments") },
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (t.status == "LIVE") "🔴" else "🏆", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(t.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                                    StatusPill(status = t.status)
                                }
                                Text("${t.playersJoined}/${t.maxPlayers} players · Entry ${fmtMoney(t.entryFee)}", fontSize = 11.sp, color = TextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Prize Pool", fontSize = 10.sp, color = TextMuted)
                                Text(fmtMoney(t.prizePool), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                            }
                        }
                    }
                }
            }
        }

        // Top Players Preview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top Leaderboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Full board",
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate("leaderboard") }
                )
            }

            ArenaCard(modifier = Modifier.padding(bottom = 24.dp)) {
                topUsers.take(4).forEachIndexed { idx, topUser ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val posColor = when (idx) {
                            0 -> GoldAccent
                            1 -> Color(0xFFA9B4C4)
                            2 -> Color(0xFFC07A4A)
                            else -> TextMuted
                        }
                        Text(
                            text = "${idx + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = posColor,
                            modifier = Modifier.width(24.dp)
                        )
                        AvatarCircle(displayName = topUser.displayName, size = 32.dp, fontSize = 12)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = topUser.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        RankBadge(rank = topUser.rank)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${topUser.mmr}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    if (idx < topUsers.take(4).size - 1) {
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
