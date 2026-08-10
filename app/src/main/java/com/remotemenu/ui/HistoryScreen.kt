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
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryScreen
 * 과거 주문 내역을 리스트 형태로 보여주는 화면 컴포저블입니다.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel
) {
    // 날짜 및 시간 포맷 설정
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /** -----------------------------
     * 화면 레이아웃 구성
     * ----------------------------- */
    Column(modifier.padding(16.dp)) {

        Text(
            text = stringResource(R.string.order_history),
            style = MaterialTheme.typography.titleMedium
        )

        /** -----------------------------
         * 빈 내역 처리 및 리스트 렌더링
         * ----------------------------- */
        if (vm.orderHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(stringResource(R.string.history_empty))
            }
        }

        LazyColumn {
            items(vm.orderHistory) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        // 주문이 확정된 시간 표시
                        Text(
                            text = formatter.format(Date(item.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // 실제 인쇄된 주문서 내용 전문 표시
                        Text(
                            text = item.printedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
