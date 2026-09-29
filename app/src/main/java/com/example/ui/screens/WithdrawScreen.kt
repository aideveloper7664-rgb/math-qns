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
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WithdrawScreen(
    user: UserEntity?,
    withdrawals: List<com.example.data.model.TransactionEntity> = emptyList(),
    onWithdraw: (Double, String, String, String) -> Unit,
    onRefreshWithdrawals: () -> Unit = {},
    onApproveWithdrawal: () -> Unit = {},
    onBack: () -> Unit
) {
    if (user == null) return

    val minW = 50.0
    val maxW = 25000.0

    var method by remember { mutableStateOf("UPI") } // "UPI" or "Bank"
    var upiId by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf(user.displayName) }
    var amountText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        PageHeader(title = "Withdraw Money", subtitle = "Transfer winnings directly to UPI / Bank", onBack = onBack)

        // ── Available Balance Card ──────────────────────────────────────────
        ArenaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available Balance", fontSize = 13.sp, color = TextMuted)
                Text(
                    text = "₹${"%.2f".format(user.walletBalance)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GreenSuccess
                )
            }
            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Withdrawal Range", fontSize = 12.sp, color = TextMuted)
                Text("₹50 - ₹25,000", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // ── Withdrawal Method Selector ──────────────────────────────────────
        ArenaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Select Payout Method", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("UPI", "Bank").forEach { m ->
                    val isSelected = method == m
                    Surface(
                        color = if (isSelected) CyanPrimary else SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) CyanPrimary else BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                method = m
                                errorMsg = null
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (m == "UPI") "⚡ UPI Transfer" else "🏦 Bank Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BgDark else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amount Input ────────────────────────────────────────────────
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }
                    errorMsg = null
                },
                label = { Text("Amount (min ₹50, max ₹25,000)", color = TextMuted) },
                prefix = { Text("₹", fontWeight = FontWeight.Bold, color = CyanPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                                val calc = (user.walletBalance * pct / 100).toInt()
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

            if (method == "UPI") {
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
            } else {
                // Bank inputs
                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = {
                        bankAccount = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Bank Account Number", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = {
                        ifscCode = it.trim().uppercase()
                        errorMsg = null
                    },
                    label = { Text("IFSC Code", color = TextMuted) },
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
                    text = "Withdrawal will be processed within 24 hours. UPI ID will be verified before release.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Button(
            onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val accountRef = if (method == "UPI") upiId.trim() else "${bankAccount.trim()} / ${ifscCode.trim()}"

                if (method == "UPI" && (upiId.isBlank() || !upiId.contains("@"))) {
                    errorMsg = "Please enter a valid UPI ID (e.g. user@paytm)."
                } else if (method == "Bank" && (bankAccount.isBlank() || ifscCode.isBlank())) {
                    errorMsg = "Please enter both Account Number and IFSC Code."
                } else if (accountHolder.isBlank()) {
                    errorMsg = "Please enter the Account Holder Name."
                } else if (amt < minW) {
                    errorMsg = "Minimum withdrawal amount is ₹50."
                } else if (amt > maxW) {
                    errorMsg = "Maximum withdrawal amount is ₹25,000."
                } else if (amt > user.walletBalance) {
                    errorMsg = "Amount exceeds available balance (₹${"%.2f".format(user.walletBalance)})."
                } else {
                    onWithdraw(amt, method, accountRef, accountHolder.trim())
                    onBack()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("SUBMIT WITHDRAWAL REQUEST", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BgDark)
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

            withdrawals.take(5).forEach { item ->
                val isApproved = item.status.equals("APPROVED", ignoreCase = true) ||
                        item.status.equals("COMPLETED", ignoreCase = true) ||
                        item.status.equals("SUCCESS", ignoreCase = true)

                Surface(
                    color = if (isApproved) GreenSuccess.copy(alpha = 0.08f) else SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isApproved) GreenSuccess.copy(alpha = 0.35f) else BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${item.amount.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isApproved) GreenSuccess else TextPrimary
                            )
                            StatusPill(status = if (isApproved) "APPROVED" else item.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Account: ${item.gatewayOrAccount}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        if (!isApproved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onApproveWithdrawal,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Text("Simulate Approve (Demo)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BgDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
