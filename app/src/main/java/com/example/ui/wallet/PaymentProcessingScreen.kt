package com.example.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.DepositStatus
import com.example.ui.components.ArenaCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PaymentProcessingScreen(
    depositId: String,
    viewModel: PaymentProcessingViewModel = viewModel(),
    onSuccess: (amount: Double) -> Unit,
    onFailure: (String) -> Unit
) {
    val status by viewModel.statusState.collectAsStateWithLifecycle()

    LaunchedEffect(depositId) {
        viewModel.watch(
            depositId = depositId,
            onTimeout = {
                onFailure("Verification taking longer than expected. Check transaction history.")
            }
        )
    }

    LaunchedEffect(status) {
        when (val currentStatus = status) {
            is DepositStatus.Success -> {
                delay(2000)
                onSuccess(currentStatus.amount)
            }
            is DepositStatus.Failed -> {
                onFailure("Payment failed")
            }
            is DepositStatus.NotFound -> {
                onFailure("Deposit record not found")
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ArenaCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (val currentStatus = status) {
                    is DepositStatus.Pending -> {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Verifying payment…",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait while we confirm your deposit.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }

                    is DepositStatus.Success -> {
                        Surface(
                            color = GreenSuccess.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Payment Successful!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenSuccess
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${currentStatus.amount.toInt()} credited to your wallet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    is DepositStatus.Failed, is DepositStatus.NotFound -> {
                        Text(
                            text = "Payment Failed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedError
                        )
                    }
                }
            }
        }
    }
}
