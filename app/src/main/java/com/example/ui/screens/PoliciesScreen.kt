package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PageHeader
import com.example.ui.theme.*

@Composable
fun PoliciesScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("terms") }

    val policies = remember {
        mapOf(
            "terms" to """
                📜 TERMS & CONDITIONS
                
                1. Eligibility & Age Restriction
                • Math Baazi is intended exclusively for users who are 18 years of age or older.
                • Play is strictly prohibited in restricted states (Assam, Odisha, Nagaland, Sikkim, Andhra Pradesh, Telangana).
                
                2. Game of Skill
                • Math Baazi is a pure contest of mental mathematics and speed calculations.
                • Success depends on your speed, accuracy, and skill.
                
                3. Fair Play & Security
                • Multiple accounts per user or automated bot scripts are strictly forbidden.
                • Account credentials must not be shared.
            """.trimIndent(),
            "refund" to """
                💰 REFUND & WITHDRAWAL POLICY
                
                1. Entry Fees
                • Tournament & Game entry fees are non-refundable once a session has commenced.
                
                2. Technical Errors & Server Refunds
                • In the rare event of a server disconnect or verified system glitch, entry fees are automatically refunded to your wallet.
                
                3. Withdrawals
                • Minimum withdrawal limit is ₹50.
                • Winnings can be transferred directly to your UPI account within 24 hours.
            """.trimIndent(),
            "privacy" to """
                🔒 PRIVACY POLICY
                
                1. Data Protection
                • Your personal information, UPI references, and transaction records are encrypted using industry-standard SSL encryption.
                
                2. Use of Information
                • Data is used strictly for identity verification, fraud prevention, and leaderboard ranking.
                • We never sell or share your personal data with third-party advertisers.
            """.trimIndent(),
            "anti_cheat" to """
                ⚠️ ANTI-CHEAT & ZERO TOLERANCE
                
                1. Anti-Bot Monitoring
                • Advanced telemetry monitors answer speed and input patterns to detect automation or assistance tools.
                
                2. Account Termination
                • Users caught cheating or abusing system bugs will face immediate account suspension and forfeiture of wallet balance.
            """.trimIndent()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(
            title = "Policies & Terms",
            subtitle = "Legal guidelines, fair play & privacy commitments",
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "terms" to "Terms",
                "refund" to "Refund",
                "privacy" to "Privacy",
                "anti_cheat" to "Anti-Cheat"
            ).forEach { (key, label) ->
                val isSelected = selectedTab == key
                Surface(
                    color = if (isSelected) CyanPrimary else SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = key }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) BgDark else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Text(
                    text = policies[selectedTab] ?: "",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
