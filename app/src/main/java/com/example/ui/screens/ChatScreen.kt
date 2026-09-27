package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.UserEntity
import com.example.ui.components.ArenaButton
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ChatRoom(val slug: String, val name: String, val minVip: String = "none")

val defaultRooms = listOf(
    ChatRoom("general", "General"),
    ChatRoom("vip-lounge", "VIP Lounge 💎", "weekly"),
    ChatRoom("high-rollers", "High Rollers 💰"),
    ChatRoom("strategy", "Math Strategy 🧠")
)

@Composable
fun ChatScreen(
    user: UserEntity?,
    messages: List<ChatMessageEntity>,
    selectedRoom: String,
    onSelectRoom: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
            .imePadding()
    ) {
        PageHeader(title = "Global Chat", subtitle = "Connect with the speed math community")

        // Room Selector
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                defaultRooms.forEach { room ->
                    val isSelected = selectedRoom == room.slug
                    val isLocked = room.minVip != "none" && user?.vipTier == "none"

                    Button(
                        onClick = {
                            if (!isLocked) onSelectRoom(room.slug)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CyanPrimary else SurfaceDark,
                            contentColor = if (isSelected) BgDark else TextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text(
                            text = if (isLocked) "🔒 ${room.name.take(5)}" else room.name.take(7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Messages Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(12.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages in this room yet. Say hi! 👋", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        val isMine = msg.userId == user?.id || msg.isMine
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.createdAt))

                        Surface(
                            color = if (isMine) CyanPrimary.copy(alpha = 0.15f) else SurfaceCard,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isMine) CyanPrimary.copy(alpha = 0.3f) else BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg.userName + if (isMine) " (you)" else "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMine) CyanPrimary else TextPrimary
                                    )
                                    Text(timeStr, fontSize = 10.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(msg.message, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Type a message…", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDark),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Send ▸", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
