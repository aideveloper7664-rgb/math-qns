package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/** Reads admin-set prizes: keys can be "1","2","3" or "first","second","third". */
fun prizeFor(dist: Map<String, Any?>?, rank: Int): Double? {
    if (dist == null) return null
    val keys = when (rank) {
        1 -> listOf("1", "first", "rank1", "1st")
        2 -> listOf("2", "second", "rank2", "2nd")
        else -> listOf("3", "third", "rank3", "3rd")
    }
    for (k in keys) {
        val v = dist[k]
        val d = (v as? Number)?.toDouble() ?: (v as? String)?.toDoubleOrNull()
        if (d != null && d > 0) return d
    }
    return null
}

@Composable
fun PrizeRow(dist: Map<String, Any?>?, modifier: Modifier = Modifier) {
    val p1 = prizeFor(dist, 1)
    val p2 = prizeFor(dist, 2)
    val p3 = prizeFor(dist, 3)
    if (p1 == null && p2 == null && p3 == null) return
    fun fmt(d: Double) = if (d % 1.0 == 0.0) "₹${d.toInt()}" else "₹${"%.2f".format(d)}"
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        p1?.let { Text("🥇 ${fmt(it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent) }
        p2?.let { Text("🥈 ${fmt(it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        p3?.let { Text("🥉 ${fmt(it)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
    }
}
