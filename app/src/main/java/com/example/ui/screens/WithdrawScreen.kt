package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WithdrawScreen(
    user: UserEntity?,
    withdrawals: List<TransactionEntity> = emptyList(),
    submitting: Boolean = false,
    onWithdraw: (Double, String, String, String, (Boolean) -> Unit) -> Unit,
    onRefreshWithdrawals: () -> Unit = {},
    onBack: () -> Unit
) {
    if (user == null) return

    val minW = 50.0
    val maxW = 25000.0

    val method = "UPI"
    var upiId by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf(user.displayName) }
    var amountText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val winnings = user.winningsBalance
    val depositBalance = user.depositBalance
    val walletBalance = user.walletBalance
    val pendingSum = withdrawals.filter { it.status.equals("PENDING", ignoreCase = true) }.sumOf { it.amount }
    val availableToWithdraw = (winnings - pendingSum).coerceAtLeast(0.0)

    val amt = amountText.toDoubleOrNull() ?: 0.0
    val isOverAvailable = amt > availableToWithdraw && amt > 0
    val isUnderMin = amt > 0 && amt < minW

    val canSubmit = !submitting &&
            amt >= minW &&
            amt <= availableToWithdraw &&
            upiId.isNotBlank() &&
            upiId.contains("@") &&
            accountHolder.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        PageHeader(title = "Withdraw Money", subtitle = "Transfer winnings directly to your UPI ID", onBack = onBack)

        // ── Available Balance Card ──────────────────────────────────────────
        ArenaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆 Winnings (withdrawable)", fontSize = 12.sp, color = TextMuted)
                Text(
                    text = "₹${"%.2f".format(winnings)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSuccess
                )
            }
            if (depositBalance > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💳 Deposit (locked)", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = "₹${"%.2f".format(depositBalance)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B7FFF)
                    )
                }
            }
            if (pendingSum > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Withdrawals", fontSize = 12.sp, color = GoldAccent)
                    Text(
                        text = "- ₹${"%.2f".format(pendingSum)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }
            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available to Withdraw", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "₹${"%.2f".format(availableToWithdraw)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GreenSuccess
                )
            }
        }

        // ── Withdrawal Method Card ──────────────────────────────────────────
        ArenaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Payout Method: ⚡ UPI Transfer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amount Input ────────────────────────────────────────────────
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }
                    errorMsg = null
                },
                label = { Text("Amount (min ₹50, max ₹25,000)", color = TextMuted) },
                prefix = { Text("₹", fontWeight = FontWeight.Bold, color = CyanPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isOverAvailable || isUnderMin,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    errorBorderColor = RedError
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (isOverAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "❌ Max ₹${"%.2f".format(availableToWithdraw)} (winnings only)",
                    color = RedError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isUnderMin) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "❌ Minimum withdrawal is ₹50",
                    color = RedError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick percentages
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 50, 100).forEach { pct ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calc = (availableToWithdraw * pct / 100).toInt()
                                amountText = calc.toString()
                                errorMsg = null
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (pct == 100) "MAX" else "$pct%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UPI ID input
            OutlinedTextField(
                value = upiId,
                onValueChange = {
                    upiId = it.trim()
                    errorMsg = null
                },
                label = { Text("UPI ID (e.g. user@paytm / user@okaxis)", color = TextMuted) },
                placeholder = { Text("username@upi", color = TextMuted.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Account Holder Name
            OutlinedTextField(
                value = accountHolder,
                onValueChange = {
                    accountHolder = it
                    errorMsg = null
                },
                label = { Text("Account Holder Name", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMsg != null) {
            Surface(
                color = RedError.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RedError.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMsg!!,
                    color = RedError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Withdrawal will be processed within 24 hours. Your balance is deducted only after admin approval.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Button(
            onClick = {
                val accountRef = upiId.trim()

                if (upiId.isBlank() || !upiId.contains("@")) {
                    errorMsg = "Please enter a valid UPI ID (e.g. user@paytm)."
                } else if (accountHolder.isBlank()) {
                    errorMsg = "Please enter the Account Holder Name."
                } else if (amt < minW) {
                    errorMsg = "Minimum withdrawal amount is ₹50."
                } else if (amt > maxW) {
                    errorMsg = "Maximum withdrawal amount is ₹25,000."
                } else if (amt > availableToWithdraw) {
                    errorMsg = "Amount exceeds available balance (₹${"%.2f".format(availableToWithdraw)})."
                } else {
                    onWithdraw(amt, method, accountRef, accountHolder.trim()) { success ->
                        if (success) {
                            onBack()
                        }
                    }
                }
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                disabledContainerColor = SurfaceDark,
                disabledContentColor = TextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BgDark, strokeWidth = 2.5.dp)
            } else {
                Text("SUBMIT WITHDRAWAL REQUEST", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (withdrawals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WITHDRAWAL HISTORY & STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Refresh ↻",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    modifier = Modifier.clickable { onRefreshWithdrawals() }
                )
            }

            withdrawals.take(10).forEach { item ->
                val isApproved = item.status.equals("APPROVED", ignoreCase = true) ||
                        item.status.equals("COMPLETED", ignoreCase = true) ||
                        item.status.equals("SUCCESS", ignoreCase = true)
                val isRejected = item.status.equals("REJECTED", ignoreCase = true) ||
                        item.status.equals("FAILED", ignoreCase = true)

                val cardBg = when {
                    isApproved -> GreenSuccess.copy(alpha = 0.08f)
                    isRejected -> RedError.copy(alpha = 0.08f)
                    else -> SurfaceDark
                }
                val borderClr = when {
                    isApproved -> GreenSuccess.copy(alpha = 0.35f)
                    isRejected -> RedError.copy(alpha = 0.35f)
                    else -> BorderSubtle
                }

                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderClr),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${"%.2f".format(item.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            StatusPill(status = item.status)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.gatewayOrAccount,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
