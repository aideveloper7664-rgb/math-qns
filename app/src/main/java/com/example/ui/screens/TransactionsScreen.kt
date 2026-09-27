package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.ArenaCard
import com.example.ui.components.PageHeader
import com.example.ui.theme.BgDark
import com.example.ui.theme.TextMuted

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        PageHeader(title = "Transactions", subtitle = "Complete wallet history", onBack = onBack)

        if (transactions.isEmpty()) {
            ArenaCard(modifier = Modifier.fillMaxWidth()) {
                Text("No transactions found.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { tx ->
                    TransactionRowItem(tx = tx)
                }
            }
        }
    }
}
