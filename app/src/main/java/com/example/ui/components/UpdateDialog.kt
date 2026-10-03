package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

@Composable
fun UpdateDialog(
    latestVersion: String,
    downloadUrl: String,
    forceUpdate: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (!forceUpdate) onDismiss()
        },
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        title = {
            Text("🚀 App Update Available (v$latestVersion)", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                if (forceUpdate)
                    "A mandatory update is required to continue playing. Please update to the latest version."
                else
                    "A new version of Math Baazi is available with bug fixes and new features."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("UPDATE NOW", color = BgDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!forceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("LATER", color = TextMuted)
                }
            } else null
        }
    )
}
