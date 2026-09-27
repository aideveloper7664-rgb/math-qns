package com.example.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ArenaButton
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*

@Composable
fun AddMoneyScreen(
    viewModel: AddMoneyViewModel = viewModel(),
    onNavigateToCheckout: (checkoutUrl: String, depositId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets = listOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2000.0)

    var selectedAmount by remember { mutableDoubleStateOf(100.0) }
    var customAmountText by remember { mutableStateOf("100") }

    LaunchedEffect(uiState.checkoutUrl) {
        val url = uiState.checkoutUrl
        val depId = uiState.depositId
        if (!url.isNullOrBlank() && !depId.isNullOrBlank()) {
            onNavigateToCheckout(url, depId)
            viewModel.clearCheckoutUrl()
        }
    }

    val isValidAmount = selectedAmount >= 10.0 && selectedAmount <= 200000.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        PageHeader(title = "Add Money", subtitle = "Deposit funds to play matches & tournaments", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Text("Select Deposit Amount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.take(3).forEach { amount ->
                        val selected = selectedAmount == amount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) CyanPrimary else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedAmount = amount
                                    customAmountText = amount.toInt().toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹${amount.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CyanPrimary else TextPrimary
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.drop(3).forEach { amount ->
                        val selected = selectedAmount == amount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) CyanPrimary else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedAmount = amount
                                    customAmountText = amount.toInt().toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹${amount.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CyanPrimary else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customAmountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    customAmountText = filtered
                    val v = filtered.toDoubleOrNull()
                    if (v != null) selectedAmount = v else selectedAmount = 0.0
                },
                label = { Text("Custom Amount", color = TextMuted) },
                prefix = { Text("₹ ", color = CyanPrimary, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Min ₹10 • Max ₹2,00,000", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gateway", fontSize = 12.sp, color = TextMuted)
                Text("ZapUPI Instant Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Deposit Amount", fontSize = 12.sp, color = TextMuted)
                Text("₹${selectedAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isValidAmount && !uiState.isLoading) {
                    viewModel.createDeposit(selectedAmount)
                }
            },
            enabled = isValidAmount && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                disabledContainerColor = CyanPrimary.copy(alpha = 0.3f)
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BgDark,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing...", color = BgDark, fontWeight = FontWeight.Bold)
            } else {
                Text("Proceed to Payment", color = BgDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error!!,
                color = RedError,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
