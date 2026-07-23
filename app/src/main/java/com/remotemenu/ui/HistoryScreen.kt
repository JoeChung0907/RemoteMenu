package com.remotemenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.remotemenu.MainViewModel
import com.remotemenu.R
import com.remotemenu.model.OrderHistoryItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryScreen
 * 과거 주문 내역을 리스트 형태로 보여주는 화면 컴포저블.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /** -----------------------------
     * 화면 레이아웃
     * ----------------------------- */
    Column(modifier.padding(16.dp)) {

        Text(
            text = stringResource(R.string.order_history),
            style = MaterialTheme.typography.titleMedium
        )

        /** -----------------------------
         * 빈 내역 처리
         * ----------------------------- */
        if (vm.orderHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(stringResource(R.string.history_empty))
            }
        }

        /** -----------------------------
         * 주문 기록 리스트 (LazyColumn)
         * ----------------------------- */
        LazyColumn {
            items(vm.orderHistory) { item: OrderHistoryItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        // 주문 시간 표시
                        Text(formatter.format(Date(item.timestamp)))
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // 주문 상세 텍스트 표시
                        Text(item.printedText)
                    }
                }
            }
        }
    }
}
