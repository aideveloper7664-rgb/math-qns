package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    currentUserId: String?,
    topUsersByMmr: List<UserEntity>,
    topUsersByXp: List<UserEntity>,
    topUsersByWins: List<UserEntity>
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: MMR, 1: XP, 2: Wins

    val currentList = when (selectedTab) {
        1 -> topUsersByXp
        2 -> topUsersByWins
        else -> topUsersByMmr
    }

    // Find current user rank
    val myIndex = currentList.indexOfFirst { it.id == currentUserId }
    val myUser = if (myIndex >= 0) currentList[myIndex] else null

    val top1 = currentList.getOrNull(0)
    val top2 = currentList.getOrNull(1)
    val top3 = currentList.getOrNull(2)

    val remaining = if (currentList.size > 3) currentList.subList(3, currentList.size) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "Leaderboard", subtitle = "Top ranked solvers in SpeedMath Arena")

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
                    Pair("🏆 MMR", 0),
                    Pair("⚡ XP", 1),
                    Pair("👑 Wins", 2)
                ).forEach { (label, idx) ->
                    val isSelected = selectedTab == idx
                    Surface(
                        color = if (isSelected) CyanPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = idx }
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

        // ── User's Own Standing Card (If logged in) ──────────────────────────
        if (myUser != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CyanPrimary.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${myIndex + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserNameWithBadges(
                                displayName = myUser.displayName,
                                isVerified = myUser.isVerified,
                                hasGoldCrown = myUser.hasGoldCrown,
                                vipTier = myUser.vipTier,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "YOU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanPrimary
                            )
                        }
                        Text(
                            text = "Tier: ${myUser.rank} · ${myUser.wins} Wins",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    val myScore = when (selectedTab) {
                        1 -> "${fmtNum(myUser.xp)} XP"
                        2 -> "${myUser.wins} Wins"
                        else -> "${fmtNum(myUser.mmr)} MMR"
                    }

                    Text(
                        text = myScore,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanPrimary
                    )
                }
            }
        }

        if (currentList.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No rankings available yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                    // 🥈 2nd Place (Left)
                                    if (top2 != null) {
                                        PodiumPillar(
                                            user = top2,
                                            rank = 2,
                                            medal = "🥈",
                                            podiumColor = Color(0xFFA9B4C4),
                                            pillarHeight = 90.dp,
                                            selectedTab = selectedTab,
                                            isMe = top2.id == currentUserId,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    // 🥇 1st Place (Center - Champion)
                                    PodiumPillar(
                                        user = top1,
                                        rank = 1,
                                        medal = "🥇",
                                        podiumColor = GoldAccent,
                                        pillarHeight = 115.dp,
                                        selectedTab = selectedTab,
                                        isMe = top1.id == currentUserId,
                                        modifier = Modifier.weight(1.15f)
                                    )

                                    // 🥉 3rd Place (Right)
                                    if (top3 != null) {
                                        PodiumPillar(
                                            user = top3,
                                            rank = 3,
                                            medal = "🥉",
                                            podiumColor = Color(0xFFCD7F32),
                                            pillarHeight = 75.dp,
                                            selectedTab = selectedTab,
                                            isMe = top3.id == currentUserId,
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

                // ── Ranks 4 and Below ───────────────────────────────────────
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

                    itemsIndexed(remaining) { index, user ->
                        val rank = index + 4
                        val isMe = user.id == currentUserId

                        val metricText = when (selectedTab) {
                            1 -> "${fmtNum(user.xp)} XP"
                            2 -> "${user.wins} Wins"
                            else -> "${fmtNum(user.mmr)} MMR"
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceDark,
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
                                    text = "#$rank",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) CyanPrimary else TextMuted,
                                    modifier = Modifier.width(34.dp)
                                )

                                AvatarCircle(displayName = user.displayName, size = 36.dp, fontSize = 13)
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        UserNameWithBadges(
                                            displayName = user.displayName,
                                            isVerified = user.isVerified,
                                            hasGoldCrown = user.hasGoldCrown,
                                            vipTier = user.vipTier,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isMe) {
                                            Text("(YOU)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CyanPrimary)
                                        }
                                    }
                                    Text(
                                        text = "${user.rank} · ${user.matchesPlayed} matches",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                RankBadge(rank = user.rank)
                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = metricText,
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
private fun PodiumPillar(
    user: UserEntity,
    rank: Int,
    medal: String,
    podiumColor: Color,
    pillarHeight: androidx.compose.ui.unit.Dp,
    selectedTab: Int,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val metric = when (selectedTab) {
        1 -> "${fmtNum(user.xp)} XP"
        2 -> "${user.wins}W"
        else -> "${fmtNum(user.mmr)} MMR"
    }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crown for Rank 1
        if (rank == 1) {
            Text("👑", fontSize = 20.sp)
        } else {
            Spacer(Modifier.height(18.dp))
        }

        // Avatar
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 56.dp else 46.dp)
                    .clip(CircleShape)
                    .border(2.dp, podiumColor, CircleShape)
            ) {
                AvatarCircle(
                    displayName = user.displayName,
                    size = if (rank == 1) 56.dp else 46.dp,
                    fontSize = if (rank == 1) 22 else 16
                )
            }

            Surface(
                color = podiumColor,
                shape = CircleShape,
                modifier = Modifier.offset(y = 8.dp)
            ) {
                Text(
                    text = medal,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Username
        Text(
            text = user.displayName,
            fontSize = if (rank == 1) 13.sp else 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isMe) CyanPrimary else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Metric
        Text(
            text = metric,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = podiumColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pillar pedestal
        Surface(
            color = podiumColor.copy(alpha = if (rank == 1) 0.22f else 0.12f),
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            border = BorderStroke(1.dp, podiumColor.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    fontSize = if (rank == 1) 24.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color = podiumColor
                )
            }
        }
    }
}
