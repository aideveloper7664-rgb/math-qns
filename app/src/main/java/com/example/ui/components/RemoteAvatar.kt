package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseClient
import com.example.ui.theme.BlueSecondary
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private val bitmapCache: LruCache<String, Bitmap> by lazy {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 8).toInt()
    object : LruCache<String, Bitmap>(maxMemory) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }
}

@Composable
fun RemoteAvatar(
    photoUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fontSize: Int = 16
) {
    if (photoUrl.isNullOrBlank()) {
        AvatarCircle(
            displayName = displayName,
            modifier = modifier,
            size = size,
            fontSize = fontSize
        )
        return
    }

    val trimmed = photoUrl.trim()

    // Emoji preset (length <= 3)
    if (trimmed.length <= 3) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CyanPrimary, BlueSecondary)))
                .border(1.5.dp, Color(0x22FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = trimmed,
                fontSize = (fontSize * 1.3f).sp
            )
        }
        return
    }

    // Remote URL
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        val bitmapState = produceState<Bitmap?>(initialValue = bitmapCache.get(trimmed), key1 = trimmed) {
            if (value != null) return@produceState
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(trimmed).build()
                    val response = SupabaseClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful && response.body != null) {
                        val bytes = response.body!!.bytes()
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        
                        // Calculate sample size
                        val targetSizePx = (size.value * 3).toInt().coerceAtLeast(64)
                        var sampleSize = 1
                        while (options.outWidth / (sampleSize * 2) >= targetSizePx &&
                            options.outHeight / (sampleSize * 2) >= targetSizePx
                        ) {
                            sampleSize *= 2
                        }
                        
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                        if (decoded != null) {
                            bitmapCache.put(trimmed, decoded)
                        }
                        decoded
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            value = bmp
        }

        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = displayName,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0x22FFFFFF), CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            AvatarCircle(
                displayName = displayName,
                modifier = modifier,
                size = size,
                fontSize = fontSize
            )
        }
        return
    }

    // Fallback
    AvatarCircle(
        displayName = displayName,
        modifier = modifier,
        size = size,
        fontSize = fontSize
    )
}
