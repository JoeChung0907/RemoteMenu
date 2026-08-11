package com.remotemenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.remotemenu.MainViewModel
import com.remotemenu.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryScreen
 * 과거 주문 내역을 확인하고 관리(삭제)하는 화면입니다.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel
) {
    val context = LocalContext.current
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier.padding(16.dp)) {
        
        /** -----------------------------
         * 상단 헤더: 제목 및 삭제 버튼
         * ----------------------------- */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.order_history),
                style = MaterialTheme.typography.titleMedium
            )
            
            // 기록이 있을 때만 삭제 버튼 노출
            if (vm.orderHistory.isNotEmpty()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        /** -----------------------------
         * 빈 내역 처리
         * ----------------------------- */
        if (vm.orderHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty))
            }
        }

        /** -----------------------------
         * 주문 기록 리스트
         * ----------------------------- */
        LazyColumn {
            items(vm.orderHistory) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = formatter.format(Date(item.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.printedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        /** -----------------------------
         * 기록 삭제 확인 다이얼로그
         * ----------------------------- */
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.clear_history_dialog_title)) },
                text = { Text(stringResource(R.string.clear_history_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.clearHistory(context)
                        showDeleteDialog = false
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
