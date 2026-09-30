package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PlayConfirmationDialog(
    entryFee: Double,
    title: String,
    messageTemplate: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = onCancel
) {
    val feeStr = if (entryFee % 1.0 == 0.0) "₹${entryFee.toInt()}" else "₹${"%.2f".format(entryFee)}"
    val formattedMessage = messageTemplate.replace("{entry_fee}", feeStr)

    AlertDialog(
        onDismissRequest = {
            onCancel()
            onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SurfaceCard,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = formattedMessage,
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = CyanPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Entry Fee", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(feeStr, color = CyanPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF36D399))
            ) {
                Text("Start Game", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text("Cancel", color = TextPrimary)
            }
        }
    )
}
