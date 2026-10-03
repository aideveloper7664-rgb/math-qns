package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(
    user: UserEntity?,
    withdrawals: List<com.example.data.model.TransactionEntity> = emptyList(),
    photoUploading: Boolean = false,
    onUpdateProfile: (String, String?) -> Unit,
    onUploadPhoto: (String) -> Unit = {},
    onNavigate: (String) -> Unit
) {
    if (user == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(user.displayName) }
    var editPhoto by remember { mutableStateOf(user.photoUrl ?: "") }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                        var inSample = 1
                        while (maxDim / (inSample * 2) >= 512) {
                            inSample *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                        if (bitmap != null) {
                            val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
                                val scale = 512f / maxOf(bitmap.width, bitmap.height)
                                Bitmap.createScaledBitmap(
                                    bitmap,
                                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                                    true
                                )
                            } else {
                                bitmap
                            }
                            val baos = ByteArrayOutputStream()
                            var quality = 80
                            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                            var compressedBytes = baos.toByteArray()
                            if (compressedBytes.size > 4 * 1024 * 1024) {
                                baos.reset()
                                quality = 60
                                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                                compressedBytes = baos.toByteArray()
                            }
                            val b64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                            withContext(Dispatchers.Main) {
                                onUploadPhoto(b64)
                                showEditDialog = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var soundEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val xpPerLevel = 500
    val level = (user.xp / xpPerLevel) + 1
    val currentLevelXp = user.xp % xpPerLevel
    val progress = (currentLevelXp.toFloat() / xpPerLevel.toFloat()).coerceIn(0f, 1f)

    val winRate = if (user.matchesPlayed > 0) (user.wins.toDouble() / user.matchesPlayed.toDouble()) * 100 else 0.0

    val avatarPresets = listOf("⚡", "🧠", "👑", "🎯", "🚀", "🦁", "🔥", "💎", "⭐", "🏆")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            PageHeader(title = "Profile", subtitle = "Your arena identity and battle record")

            // ── Hero Profile Card ───────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    CyanPrimary.copy(alpha = 0.12f),
                                    PurpleAccent.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with ring & optional edit icon
                        Box(contentAlignment = Alignment.BottomEnd) {
                            RemoteAvatar(
                                url = user.photoUrl,
                                displayName = user.displayName,
                                size = 80.dp
                            )

                            Surface(
                                shape = CircleShape,
                                color = CyanPrimary,
                                border = BorderStroke(2.dp, BgDark),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        editName = user.displayName
                                        editPhoto = user.photoUrl ?: ""
                                        showEditDialog = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = BgDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name & Badges
                        UserNameWithBadges(
                            displayName = user.displayName,
                            isVerified = user.isVerified,
                            hasGoldCrown = user.hasGoldCrown,
                            vipTier = user.vipTier,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = user.email,
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RankBadge(rank = user.rank)
                            if (user.vipTier != "none") {
                                Surface(
                                    color = PurpleAccent.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "💎 VIP ${user.vipTier.uppercase()}",
                                        color = PurpleAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                color = SurfaceDark,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("User ID", user.id))
                                    Toast.makeText(context, "Player ID copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ID: ${user.id.take(6)}...",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Level & XP Progress Card ────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Level $level Master",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${currentLevelXp} / ${xpPerLevel} XP to Level ${level + 1}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Surface(
                        color = CyanPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${user.xp} Total XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(CyanPrimary, PurpleAccent))
                            )
                    )
                }
            }

            // ── Wallet & Earnings Summary Card ──────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💰 Wallet & Earnings",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanPrimary,
                            modifier = Modifier.clickable { onNavigate("wallet") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = fmtMoney(user.walletBalance),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = GreenSuccess
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WINNINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = fmtMoney(user.totalWinnings),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("WITHDRAWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = fmtMoney(user.totalWithdrawn),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate("deposit") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("＋ Deposit", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onNavigate("withdraw") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("⚡ Withdraw", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Latest Withdrawal Live Status ─────────────────────────────
            val latestWithdrawal = withdrawals.filter { it.type.equals("withdrawal", ignoreCase = true) }.firstOrNull()
            if (latestWithdrawal != null) {
                val isApproved = latestWithdrawal.status.equals("APPROVED", ignoreCase = true) ||
                        latestWithdrawal.status.equals("COMPLETED", ignoreCase = true) ||
                        latestWithdrawal.status.equals("SUCCESS", ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isApproved) GreenSuccess.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isApproved) GreenSuccess.copy(alpha = 0.45f) else GoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Latest Withdrawal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            StatusPill(status = if (isApproved) "APPROVED" else latestWithdrawal.status)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "₹${"%.2f".format(latestWithdrawal.amount)} payout to ${latestWithdrawal.gatewayOrAccount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isApproved) "Verification complete. Transferred to account." else "Verification in progress. Usually takes under 24 hours.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // ── Battle Statistics Grid ──────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "⚔️ Arena Performance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(label = "MMR RATING", value = fmtNum(user.mmr), color = CyanPrimary, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox(label = "MATCHES", value = "${user.matchesPlayed}", color = TextPrimary, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox(label = "WIN RATE", value = fmtPct(winRate), color = GreenSuccess, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(label = "WINS", value = "${user.wins}", color = GreenSuccess, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox(label = "LOSSES", value = "${user.losses}", color = RedError, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox(label = "WINNINGS", value = fmtMoney(user.totalWinnings), color = GoldAccent, modifier = Modifier.weight(1f))
                }
            }

            // ── Referral Quick Card ─────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigate("referral") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Refer & Earn", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Code: ${user.referralCode} · Tap to invite", fontSize = 11.sp, color = CyanPrimary)
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CyanPrimary
                    )
                }
            }

            // ── App Preferences & Settings ──────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Sound Effects", fontSize = 13.sp, color = TextPrimary)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = CyanPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        )
                    )
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Haptic Feedback", fontSize = 13.sp, color = TextPrimary)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { hapticsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = CyanPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        )
                    )
                }
            }

            // ── Menu Links ──────────────────────────────────────────────────
            ArenaCard(modifier = Modifier.padding(bottom = 20.dp)) {
                val menu = listOf(
                    Triple("vip", "VIP Pass & Membership", "👑"),
                    Triple("knockout", "Knockout Tournaments", "🥊"),
                    Triple("tournaments", "Live Tournaments", "⚔️"),
                    Triple("practice", "Practice Mode (Free Play)", "🎯"),
                    Triple("game_history", "Game Match History", "🎮"),
                    Triple("transactions", "Wallet Transactions", "📜"),
                    Triple("badges", "Badges & Achievements", "🏅"),
                    Triple("support", "Help & Support", "💬"),
                    Triple("policies", "Policies & Terms", "📋"),
                    Triple("leaderboard", "Global Leaderboard", "🏆"),
                    Triple("settings", "Account & Settings", "⚙️")
                )

                menu.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.first) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.third, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.second,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (idx < menu.size - 1) {
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // ── Edit Profile Dialog ─────────────────────────────────────────────────
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "CHOOSE AVATAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Gallery Photo Upload Button ──
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !photoUploading) {
                                photoLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (photoUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CyanPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Uploading Photo...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Upload Photo",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Choose Photo from Gallery",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "OR CHOOSE AN EMOJI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    ) {
                        items(avatarPresets) { emo ->
                            val isSelected = editPhoto == emo
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) CyanPrimary.copy(alpha = 0.25f) else SurfaceDark,
                                border = BorderStroke(1.5.dp, if (isSelected) CyanPrimary else BorderSubtle),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { editPhoto = emo }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emo, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name", color = TextMuted) },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editName.trim()
                        if (trimmed.isNotBlank()) {
                            onUpdateProfile(trimmed, editPhoto.ifBlank { null })
                        }
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Save Changes", color = BgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
    }
}
