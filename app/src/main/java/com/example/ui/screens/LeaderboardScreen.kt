package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun LeaderboardScreen(
    currentUserId: String?,
    topUsersByMmr: List<UserEntity>,
    topUsersByXp: List<UserEntity>,
    topUsersByWins: List<UserEntity>
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Global, 1: Weekly, 2: Monthly

    val currentList = when (selectedTab) {
        1 -> topUsersByXp
        2 -> topUsersByWins
        else -> topUsersByMmr
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "Leaderboard", subtitle = "The best solvers in SpeedMath Arena")

        // Tab Selector
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf("Global (MMR)", "Weekly (XP)", "Monthly (Wins)").forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Button(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CyanPrimary else SurfaceDark,
                            contentColor = if (isSelected) BgDark else TextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(title.split(" ").first(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (currentList.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Text("No leaderboard entries found.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(currentList) { idx, user ->
                    val isMe = user.id == currentUserId
                    val medal = when (idx) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${idx + 1}"
                    }
                    val posColor = when (idx) {
                        0 -> GoldAccent
                        1 -> Color(0xFFA9B4C4)
                        2 -> Color(0xFFC07A4A)
                        else -> TextMuted
                    }
                    val metricText = when (selectedTab) {
                        1 -> "${fmtNum(user.xp)} XP"
                        2 -> "${user.wins} Wins"
                        else -> "${fmtNum(user.mmr)} MMR"
                    }

                    ArenaCard(
                        modifier = Modifier.fillMaxWidth(),
                        border = if (isMe) androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary) else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = medal,
                                fontSize = if (idx < 3) 20.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = posColor,
                                modifier = Modifier.width(32.dp)
                            )
                            AvatarCircle(displayName = user.displayName, size = 36.dp, fontSize = 13)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    UserBadges(isVerified = user.isVerified, hasGoldCrown = user.hasGoldCrown)
                                    if (isMe) {
                                        Text("(YOU)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CyanPrimary)
                                    }
                                }
                                Text("${user.rank} · ${user.matchesPlayed} matches", fontSize = 11.sp, color = TextMuted)
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
