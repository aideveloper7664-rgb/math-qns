package com.example.ui.wallet

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BgDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CheckoutScreen(
    checkoutUrl: String,
    depositId: String,
    onPaymentDetected: () -> Unit,
    onFailure: (String) -> Unit,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel payment?", fontWeight = FontWeight.Bold) },
            text = { Text("Your deposit will remain PENDING until it expires.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    }
                ) {
                    Text("Yes, Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Continue Payment", color = CyanPrimary)
                }
            },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Checkout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return handlePaymentUrl(url, onPaymentDetected, onFailure)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return false
                                return handlePaymentUrl(url, onPaymentDetected, onFailure)
                            }
                        }
                        loadUrl(checkoutUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun handlePaymentUrl(
    url: String,
    onPaymentDetected: () -> Unit,
    onFailure: (String) -> Unit
): Boolean {
    val lower = url.lowercase()
    if (lower.contains("status=success") || lower.contains("/payment/success") || lower.contains("success")) {
        onPaymentDetected()
        return true
    }
    if (lower.contains("status=failed") || lower.contains("status=timeout") || lower.contains("/payment/failed") || lower.contains("/payment/timeout")) {
        onFailure("Payment failed or timed out")
        return true
    }
    return false
}
