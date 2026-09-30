package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.remote.SupabaseVipPlanDto
import com.example.ui.components.*
import com.example.ui.theme.*

data class VipPlanDisplay(
    val tier: String,
    val name: String,
    val price: Double,
    val days: Long,
    val perks: List<String>,
    val tag: String? = null
)

val defaultVipPlans = listOf(
    VipPlanDisplay("weekly", "Weekly Pass", 49.0, 7, listOf("Gold Crown Badge 👑", "VIP Chat Room Access", "2x XP Multiplier"), null),
    VipPlanDisplay("monthly", "Monthly Pass", 149.0, 30, listOf("Gold Crown Badge 👑", "VIP Lounge & High Rollers Access", "3x XP Multiplier", "Exclusive Diamond Tournaments"), "POPULAR"),
    VipPlanDisplay("yearly", "Yearly Pass", 999.0, 365, listOf("Gold Crown Badge 👑", "All VIP Rooms Unlocked", "5x XP Multiplier", "Zero Platform Fees on Duels"), "BEST VALUE")
)

@Composable
fun VipScreen(
    user: UserEntity?,
    plans: List<SupabaseVipPlanDto> = emptyList(),
    isLoading: Boolean = false,
    onPurchaseVip: (String) -> Unit,
    onBack: () -> Unit
) {
    if (user == null) return

    val isVipActive = user.vipTier != "none" && (user.vipExpiresAt == null || user.vipExpiresAt > System.currentTimeMillis())

    val displayPlans: List<VipPlanDisplay> = if (plans.isNotEmpty()) {
        plans.map { p ->
            val days = (p.durationDays ?: (if (p.tier == "weekly") 7 else if (p.tier == "monthly") 30 else 365)).toLong()
            val perks = p.benefitLines()
            val tag = if (p.tier == "monthly") "POPULAR" else if (p.tier == "yearly") "BEST VALUE" else null
            VipPlanDisplay(
                tier = p.tier,
                name = p.name,
                price = p.price,
                days = days,
                perks = if (perks.isNotEmpty()) perks else listOf("Gold Crown Badge 👑", "VIP Access"),
                tag = tag
            )
        }
    } else {
        defaultVipPlans
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "VIP Pass", subtitle = "Unlock gold status and elite perks", onBack = onBack)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(GoldAccent.copy(alpha = 0.2f), PurpleAccent.copy(alpha = 0.15f))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Current VIP Status", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVipActive) "💎 ${user.vipTier.uppercase()} ACTIVE" else "No Active Pass",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isVipActive) GoldAccent else TextPrimary
                        )
                    }
                }
            }
        }

        items(displayPlans) { plan ->
            val isActiveTier = isVipActive && user.vipTier.equals(plan.tier, ignoreCase = true)

            ArenaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                border = if (plan.tag != null) BorderStroke(1.5.dp, GoldAccent) else BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(plan.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            if (plan.tag != null) {
                                Surface(
                                    color = GoldAccent,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        plan.tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BgDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text("${plan.days} Days", fontSize = 11.sp, color = TextMuted)
                    }
                    Text("₹${plan.price.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                }

                Spacer(modifier = Modifier.height(10.dp))

                plan.perks.forEach { perk ->
                    Text("✨ $perk", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(vertical = 2.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                ArenaButton(
                    text = if (isActiveTier) "✓ Currently Active" else "Buy ${plan.name} (₹${plan.price.toInt()})",
                    onClick = { onPurchaseVip(plan.tier) },
                    enabled = !isActiveTier && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
