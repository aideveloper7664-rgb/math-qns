package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun fmtMoney(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount).replace("INR", "₹").trim()
}

fun fmtNum(num: Int): String {
    return NumberFormat.getNumberInstance(Locale("en", "IN")).format(num)
}

fun fmtPct(num: Double): String {
    return String.format(Locale.getDefault(), "%.1f%%", if (num.isNaN()) 0.0 else num)
}

fun rankColor(rank: String): Color {
    return when (rank.lowercase()) {
        "legend" -> GoldAccent
        "grandmaster" -> RedError
        "master" -> PurpleAccent
        "diamond" -> BlueSecondary
        "platinum" -> CyanPrimary
        "gold" -> GoldAccent
        "silver" -> Color(0xFFA9B4C4)
        else -> Color(0xFFC07A4A)
    }
}

@Composable
fun RankBadge(rank: String, modifier: Modifier = Modifier) {
    val color = rankColor(rank)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = rank.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun UserBadges(isVerified: Boolean, hasGoldCrown: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (isVerified) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CyanPrimary, BlueSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = BgDark,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        if (hasGoldCrown) {
            Text(
                text = "👑",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AvatarCircle(
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fontSize: Int = 16
) {
    val initial = displayName.trim().take(1).uppercase().ifBlank { "?" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CyanPrimary, BlueSecondary)))
            .border(1.5.dp, Color(0x22FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = BgDark,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp
        )
    }
}

@Composable
fun ArenaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = BorderStroke(1.dp, BorderSubtle),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = border,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val upper = status.uppercase()
    val (label, bgColor, textColor) = when (upper) {
        "APPROVED" -> Triple("✓ APPROVED", GreenSuccess.copy(alpha = 0.2f), GreenSuccess)
        "COMPLETED", "SUCCESS", "PAID", "ACTIVE", "WIN", "LIVE", "VERIFIED" -> Triple(upper, GreenSuccess.copy(alpha = 0.15f), GreenSuccess)
        "PENDING", "PROCESSING", "UPCOMING", "IN_PROGRESS", "UNDER_REVIEW" -> Triple(if (upper == "PENDING") "⏳ PENDING" else upper, GoldAccent.copy(alpha = 0.18f), GoldAccent)
        "FAILED", "REJECTED", "CANCELLED", "LOSS" -> Triple(if (upper == "REJECTED") "✕ REJECTED" else upper, RedError.copy(alpha = 0.18f), RedError)
        else -> Triple(upper, SurfaceDark, TextMuted)
    }

    Surface(
        color = bgColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ArenaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CyanPrimary,
            contentColor = BgDark,
            disabledContainerColor = SurfaceDark,
            disabledContentColor = TextMuted
        ),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun ArenaGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldAccent,
            contentColor = Color(0xFF2B1A00)
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun ArenaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        modifier = modifier.height(48.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaTopBar(
    user: UserEntity?,
    unreadNotifsCount: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgDark.copy(alpha = 0.95f),
            titleContentColor = TextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Math Baazi",
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Math Baazi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                    if (unreadNotifsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RedError)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            if (user != null) {
                Surface(
                    color = SurfaceDark,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .clickable { onProfileClick() }
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AvatarCircle(
                            displayName = user.displayName,
                            size = 28.dp,
                            fontSize = 12
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        UserBadges(
                            isVerified = user.isVerified,
                            hasGoldCrown = user.hasGoldCrown,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ArenaBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = BgDark.copy(alpha = 0.96f),
        contentColor = TextMuted,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("home", "Home", Icons.Default.Home),
            Triple("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
            Triple("leaderboard", "Ranks", Icons.Default.Leaderboard),
            Triple("chat", "Chat", Icons.Default.ChatBubble),
            Triple("profile", "Profile", Icons.Default.Person)
        )

        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) CyanPrimary else TextMuted
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) CyanPrimary else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
