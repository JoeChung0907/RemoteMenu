package com.remotemenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotemenu.MainViewModel
import com.remotemenu.model.OrderHistoryItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Column(modifier.padding(16.dp)) {
        Text("주문 기록", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(vm.orderHistory) { item: OrderHistoryItem ->
                Card(Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(formatter.format(Date(item.timestamp)))
                        Spacer(Modifier.height(4.dp))
                        Text(item.printedText)
                    }
                }
            }
        }
    }
}
