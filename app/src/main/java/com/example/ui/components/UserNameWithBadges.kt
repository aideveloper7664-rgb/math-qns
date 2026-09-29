package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary

@Composable
fun UserNameWithBadges(
    displayName: String,
    isVerified: Boolean,
    hasGoldCrown: Boolean,
    vipTier: String = "none",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textColor: Color = TextPrimary,
    maxLines: Int = 1
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = displayName,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        // Blue Tick
        if (isVerified) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = CyanPrimary,
                modifier = Modifier.size(fontSize.value.dp + 2.dp)
            )
        }
        // Gold Crown
        if (hasGoldCrown) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = "👑",
                fontSize = (fontSize.value + 2).sp
            )
        }
        // VIP Badge
        if (vipTier != "none" && vipTier.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Surface(
                color = when (vipTier.lowercase()) {
                    "weekly" -> CyanPrimary
                    "monthly" -> PurpleAccent
                    "yearly" -> GoldAccent
                    else -> PurpleAccent
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = vipTier.uppercase(),
                    fontSize = (fontSize.value - 4).coerceAtLeast(8f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
