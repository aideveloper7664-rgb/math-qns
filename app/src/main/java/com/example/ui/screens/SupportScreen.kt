package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*

@Composable
fun SupportScreen(config: Map<String, String>, onBack: () -> Unit) {
    val context = LocalContext.current

    fun open(uri: String, action: String = Intent.ACTION_VIEW) {
        try {
            context.startActivity(Intent(action, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = "Help & Support", subtitle = "Hum yahan hain aapki madad ke liye", onBack = onBack)

        val rows = buildList {
            config["email"]?.let { add(Triple("📧", "Email Support", it) to { open("mailto:$it", Intent.ACTION_SENDTO) }) }
            config["whatsapp"]?.let { wa ->
                add(Triple("💬", "WhatsApp", wa) to { open("https://wa.me/" + wa.filter { c -> c.isDigit() }) })
            }
            listOf(
                "telegram" to ("📱" to "Telegram"),
                "instagram" to ("📷" to "Instagram"),
                "facebook" to ("📘" to "Facebook"),
                "youtube" to ("▶️" to "YouTube"),
                "twitter" to ("🐦" to "Twitter / X")
            ).forEach { (key, meta) ->
                config[key]?.let { url -> add(Triple(meta.first, meta.second, url) to { open(url) }) }
            }
        }

        if (rows.isEmpty()) {
            Text("Support details abhi available nahi hain.", color = TextMuted, fontSize = 13.sp)
        }

        rows.forEach { (info, action) ->
            ArenaCard(modifier = Modifier.padding(vertical = 6.dp), onClick = { action() }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(info.first, fontSize = 24.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(info.second, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text(info.third, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
