package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationEntity
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "Notifications", subtitle = "Announcements and updates", onBack = onBack)

        if (notifications.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Text("No notifications. You're all caught up!", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.createdAt))

                    ArenaCard(
                        modifier = Modifier.fillMaxWidth(),
                        border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary) else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Surface(
                                color = CyanPrimary.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (notif.category == "Wallet") "💳" else "📢", fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(notif.message, fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(dateStr, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
