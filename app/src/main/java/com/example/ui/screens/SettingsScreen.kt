package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun SettingsScreen(
    user: UserEntity?,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    if (user == null) return

    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            PageHeader(title = "Settings", subtitle = "Preferences and responsible play limits", onBack = onBack)

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Account Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Email", fontSize = 12.sp, color = TextMuted)
                    Text(user.email, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Verification", fontSize = 12.sp, color = TextMuted)
                    StatusPill(status = user.verificationStatus)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Account Status", fontSize = 12.sp, color = TextMuted)
                    StatusPill(status = user.status)
                }
            }

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("App Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Effects", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Vibration", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Push Notifications", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }

            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Responsible Gaming", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min Withdrawal Limit", fontSize = 12.sp, color = TextMuted)
                    Text("₹100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max Withdrawal Limit", fontSize = 12.sp, color = TextMuted)
                    Text("₹10,000", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Gameplay Limit", fontSize = 12.sp, color = TextMuted)
                    Text("₹5,000", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Play responsibly. Math Baazi encourages mental math training and fair competitive gaming.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RedError.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
