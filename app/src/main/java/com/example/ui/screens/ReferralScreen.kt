package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ReferralScreen(
    user: UserEntity?,
    onBack: () -> Unit
) {
    if (user == null) return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Refer & Earn", subtitle = "Invite friends to win rewards", onBack = onBack)

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Your Referral Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = CyanPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = user.referralCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanPrimary,
                            letterSpacing = 2.sp
                        )
                        Text("📋 Tap to Copy", fontSize = 12.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Your Earnings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.referralCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("REFERRALS", fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtMoney(user.referralEarnings), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                        Text("EARNED", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Text("How it works", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            ArenaCard {
                Text("1. Share your code with friends", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("2. Friend enters your code on sign up", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("3. You both receive ₹50 bonus credits!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
            }
        }
    }
}
