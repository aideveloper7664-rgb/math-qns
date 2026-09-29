package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.remote.SupabaseReferralDto
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ReferralScreen(
    user: UserEntity?,
    referrals: List<SupabaseReferralDto> = emptyList(),
    onBack: () -> Unit
) {
    if (user == null) return
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Refer & Earn", subtitle = "Invite friends to win rewards", onBack = onBack)

            // ── Referral Code Card ──────────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "YOUR REFERRAL CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = CyanPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.referralCode,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanPrimary,
                            letterSpacing = 3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Referral Code", user.referralCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BgDark, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Code", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🎮 Play SpeedMath Arena and win real prizes! Use my referral code: ${user.referralCode} to get ₹50 bonus credits! Download app: https://ais-dev-cfcha536thi2azarfnw6uq-840513166105.asia-southeast1.run.app"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // ── Stats Summary ───────────────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Referral Stats", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${user.referralCount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanPrimary
                        )
                        Text("TOTAL INVITED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(BorderSubtle))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = fmtMoney(user.referralEarnings),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenSuccess
                        )
                        Text("TOTAL EARNED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                }
            }

            // ── How It Works ────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 How Referral Works", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    RuleBullet(num = "1", text = "Share your referral code with your friends or classmates.")
                    RuleBullet(num = "2", text = "Friend enters your code when signing up for SpeedMath.")
                    RuleBullet(num = "3", text = "You get ₹50 bonus credits credited straight to your wallet!")
                }
            }

            // ── Referrals List Header ───────────────────────────────────────
            Text(
                text = "YOUR REFERRALS (${referrals.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (referrals.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎁", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No referrals yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Share your code above to earn ₹50 per friend!",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(referrals) { ref ->
                ReferralItemCard(referral = ref)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReferralItemCard(referral: SupabaseReferralDto) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = referral.referredName ?: "Invited Player",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Code: ${referral.referralCode}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${referral.rewardAmount.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (referral.rewardPaid) GreenSuccess else GoldAccent
                )
                Text(
                    text = if (referral.rewardPaid) "CREDITED" else "PENDING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (referral.rewardPaid) GreenSuccess else GoldAccent
                )
            }
        }
    }
}
