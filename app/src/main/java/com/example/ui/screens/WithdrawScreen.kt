package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WithdrawScreen(
    user: UserEntity?,
    onWithdraw: (Double, String, String) -> Unit,
    onBack: () -> Unit
) {
    if (user == null) return

    val minW = 100.0
    val maxW = 10000.0

    var method by remember { mutableStateOf("UPI") }
    var accountDetails by remember { mutableStateOf("") }
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
        PageHeader(title = "Withdraw", subtitle = "Cash out your winnings", onBack = onBack)

        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available Balance", fontSize = 12.sp, color = TextMuted)
                Text(fmtMoney(user.walletBalance), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Min Withdrawal", fontSize = 12.sp, color = TextMuted)
                Text(fmtMoney(minW), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max Withdrawal", fontSize = 12.sp, color = TextMuted)
                Text(fmtMoney(maxW), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Text("Withdrawal Method", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("UPI", "BANK").forEach { m ->
                    val isSelected = method == m
                    Surface(
                        color = if (isSelected) CyanPrimary else SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { method = m }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (m == "UPI") "UPI" else "Bank Transfer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) BgDark else TextPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = accountDetails,
                onValueChange = { accountDetails = it },
                label = { Text(if (method == "UPI") "UPI ID (e.g. user@upi)" else "Bank Account & IFSC", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹)", color = TextMuted) },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 50, 100).forEach { pct ->
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calc = (user.walletBalance * pct / 100).toInt()
                                amountText = calc.toString()
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(if (pct == 100) "MAX" else "$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        }
                    }
                }
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMsg!!, color = RedError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(1f))

        ArenaButton(
            text = "Request Withdrawal",
            onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (accountDetails.isBlank()) {
                    errorMsg = "Please enter your UPI ID or account details."
                } else if (amt < minW) {
                    errorMsg = "Minimum withdrawal is ${fmtMoney(minW)}."
                } else if (amt > maxW) {
                    errorMsg = "Maximum withdrawal is ${fmtMoney(maxW)}."
                } else if (amt > user.walletBalance) {
                    errorMsg = "Amount exceeds available wallet balance."
                } else {
                    onWithdraw(amt, method, accountDetails)
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
