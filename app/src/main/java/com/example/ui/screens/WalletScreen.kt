package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletScreen(
    user: UserEntity?,
    transactions: List<TransactionEntity>,
    onNavigate: (String) -> Unit,
    onRefreshWithdrawals: () -> Unit = {}
) {
    if (user == null) return

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onRefreshWithdrawals()
    }

    val pendingWithdrawalsSum = transactions.filter { it.type.equals("withdrawal", ignoreCase = true) && it.status.equals("PENDING", ignoreCase = true) }.sumOf { it.amount }
    val recentWithdrawals = transactions.filter { it.type.equals("withdrawal", ignoreCase = true) }
    val latestWithdrawal = recentWithdrawals.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Wallet", subtitle = "Manage your balance and payout status")

            // ── Available Balance Card ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    CyanPrimary.copy(alpha = 0.15f),
                                    BlueSecondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Available Balance", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fmtMoney(user.walletBalance),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        if (pendingWithdrawalsSum > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "⏳ Pending Withdrawal: ${fmtMoney(pendingWithdrawalsSum)}",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (user.lockedBalance > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "🔒 ${fmtMoney(user.lockedBalance)} locked in play",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Primary Actions ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArenaButton(
                    text = "＋ Add Money",
                    onClick = { onNavigate("deposit") },
                    modifier = Modifier.weight(1f)
                )
                ArenaGhostButton(
                    text = "Withdraw",
                    onClick = { onNavigate("withdraw") },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Real-time Withdrawal Status Banner ──────────────────────────
            if (latestWithdrawal != null) {
                val isApproved = latestWithdrawal.status.equals("APPROVED", ignoreCase = true) ||
                        latestWithdrawal.status.equals("COMPLETED", ignoreCase = true) ||
                        latestWithdrawal.status.equals("SUCCESS", ignoreCase = true)

                val cardBg = if (isApproved) GreenSuccess.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.12f)
                val cardBorder = if (isApproved) GreenSuccess.copy(alpha = 0.45f) else GoldAccent.copy(alpha = 0.45f)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (isApproved) GreenSuccess else GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Latest Withdrawal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            StatusPill(status = if (isApproved) "APPROVED" else latestWithdrawal.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "₹${latestWithdrawal.amount.toInt()} payout to ${latestWithdrawal.gatewayOrAccount}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isApproved)
                                        "Payment verified and successfully released to account."
                                    else
                                        "Verification in progress. Usually takes under 24 hours.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRefreshWithdrawals,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Lifetime Summary Card ───────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Lifetime Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Deposited", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalDeposited), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Winnings", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalWinnings), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Withdrawn", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalWithdrawn), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // ── Recent Activity Header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "View all →",
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate("transactions") }
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                ArenaCard {
                    Text("No transactions yet. Add money to get started.", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            items(transactions.take(8)) { tx ->
                TransactionRowItem(tx = tx)
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity) {
    val isPositive = tx.type in listOf("deposit", "match_win", "referral_bonus")
    val isApproved = tx.status.equals("APPROVED", ignoreCase = true) ||
            tx.status.equals("COMPLETED", ignoreCase = true) ||
            tx.status.equals("SUCCESS", ignoreCase = true)

    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.createdAt))

    ArenaCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPositive) GreenSuccess.copy(alpha = 0.15f) else (if (isApproved) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tx.type) {
                        "withdrawal" -> if (isApproved) "✓" else "⚡"
                        "deposit" -> "⬆️"
                        "match_win" -> "🏆"
                        "referral_bonus" -> "🎁"
                        else -> "⬇️"
                    },
                    fontSize = 16.sp,
                    color = if (isApproved && tx.type == "withdrawal") GreenSuccess else TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tx.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    StatusPill(status = if (isApproved && tx.type == "withdrawal") "APPROVED" else tx.status)
                }
                Text(
                    text = if (tx.gatewayOrAccount.isNotBlank() && tx.gatewayOrAccount != "Instant Gateway")
                        "$dateStr · ${tx.gatewayOrAccount}"
                    else
                        dateStr,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
            Text(
                text = "${if (isPositive) "+" else "−"}${fmtMoney(tx.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) GreenSuccess else (if (isApproved && tx.type == "withdrawal") GreenSuccess else RedError)
            )
        }
    }
}
