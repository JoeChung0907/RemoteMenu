package com.remotemenu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remotemenu.MainViewModel
import com.remotemenu.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, vm: MainViewModel) {
    val context = LocalContext.current
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showDeleteOverlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.order_history), style = MaterialTheme.typography.titleMedium)
                if (vm.orderHistory.isNotEmpty()) {
                    IconButton(onClick = { showDeleteOverlay = true }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (vm.orderHistory.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty))
                }
            }

            LazyColumn {
                items(vm.orderHistory) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = formatter.format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(text = item.printedText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        /** -----------------------------
         * 안드로이드 10 보안 에러 방지용 커스텀 오버레이
         * ----------------------------- */
        if (showDeleteOverlay) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { /* block */ }, contentAlignment = Alignment.Center) {
                Card(Modifier.fillMaxWidth(0.8f).padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.clear_history_dialog_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.clear_history_dialog_message))
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showDeleteOverlay = false }) { Text(stringResource(R.string.cancel)) }
                            Button(
                                onClick = { vm.clearHistory(context); showDeleteOverlay = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.padding(start = 8.dp)
                            ) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }
}
