package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onNavigate: (String) -> Unit
) {
    if (user == null) return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Wallet", subtitle = "Manage your balance and earnings")

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.15f), BlueSecondary.copy(alpha = 0.15f))))
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
                        if (user.lockedBalance > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
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

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Lifetime Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Deposited", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalDeposited), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Winnings", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalWinnings), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Withdrawn", fontSize = 12.sp, color = TextMuted)
                    Text(fmtMoney(user.totalWithdrawn), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

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
            items(transactions.take(5)) { tx ->
                TransactionRowItem(tx = tx)
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity) {
    val isPositive = tx.type in listOf("deposit", "match_win", "referral_bonus")
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.createdAt))

    ArenaCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPositive) GreenSuccess.copy(alpha = 0.15f) else RedError.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isPositive) "⬆️" else "⬇️", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = tx.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    StatusPill(status = tx.status)
                }
                Text(dateStr, fontSize = 11.sp, color = TextMuted)
            }
            Text(
                text = "${if (isPositive) "+" else "−"}${fmtMoney(tx.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) GreenSuccess else RedError
            )
        }
    }
}
