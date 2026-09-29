package com.example.ui.wallet

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.example.SpeedMathApp

class PaymentActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkoutUrl = intent.getStringExtra("checkout_url") ?: run {
            finish()
            return
        }
        val depositId = intent.getStringExtra("deposit_id") ?: ""

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            userAgentString = "$userAgentString MathBaaziApp/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return handleUrl(url, depositId)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(url, depositId)
            }
        }

        webView.loadUrl(checkoutUrl)
    }

    private fun handleUrl(url: String, depositId: String): Boolean {
        return when {
            url.contains("status=success") -> {
                finishPayment(true, depositId, "Payment successful")
                true
            }
            url.contains("status=failed") -> {
                finishPayment(false, depositId, "Payment failed")
                true
            }
            url.contains("status=timeout") -> {
                finishPayment(false, depositId, "Payment timed out")
                true
            }
            url.startsWith("upi://") || url.startsWith("intent://") -> {
                try {
                    val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@PaymentActivity, "No UPI app found", android.widget.Toast.LENGTH_SHORT).show()
                }
                true
            }
            isAllowedPaymentUrl(url) -> {

                false // Load inside WebView
            }
            else -> {
                false
            }
        }
    }

    private fun isAllowedPaymentUrl(url: String): Boolean {
        val allowedHosts = listOf(
            "pay.zapupi.com",
            "zapupi.com",
            "api.zapupi.com",
            "checkout.razorpay.com",
            "razorpay.com",
            "secure.phonepe.com",
            "paytm.com",
            "onlinesbi.com"
        )
        return allowedHosts.any { url.contains(it, ignoreCase = true) }
    }

    private fun finishPayment(success: Boolean, depositId: String, message: String) {
        val app = application as? SpeedMathApp
        app?.paymentListener?.invoke(success, depositId, message)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishPayment(false, intent.getStringExtra("deposit_id") ?: "", "Cancelled by user")
    }
}
