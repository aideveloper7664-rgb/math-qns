package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BadgeEntity
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*

@Composable
fun BadgesScreen(
    badges: List<BadgeEntity>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "My Badges", subtitle = "Achievements you have unlocked", onBack = onBack)

        if (badges.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Text("No badges available.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(badges) { badge ->
                    val isOwned = badge.isOwned
                    Surface(
                        color = if (isOwned) SurfaceCard else SurfaceDark.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOwned) CyanPrimary.copy(alpha = 0.5f) else BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isOwned) badge.icon else "🔒",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = badge.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwned) TextPrimary else TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = badge.rarity.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (badge.rarity.lowercase()) {
                                    "legendary" -> GoldAccent
                                    "epic" -> PurpleAccent
                                    "rare" -> BlueSecondary
                                    else -> TextMuted
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
