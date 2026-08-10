package com.remotemenu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.remotemenu.MainViewModel
import com.remotemenu.R
import com.remotemenu.model.CustomOption
import com.remotemenu.model.MenuItem

/**
 * OrderScreen
 * 매장에서 메뉴를 선택하고 테이블별로 주문을 구성하는 메인 화면 컴포저블입니다.
 */
@Composable
fun OrderScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel
) {
    /** -----------------------------
     * UI 로컬 상태 관리
     * ----------------------------- */
    var selectedTable by remember { mutableIntStateOf(1) }
    var selectedMenu by remember { mutableStateOf<MenuItem?>(null) }
    var qty by remember { mutableStateOf("1") }
    val selectedOptions = remember { mutableStateListOf<CustomOption>() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    /** -----------------------------
     * 화면 레이아웃 메인 구조
     * ----------------------------- */
    Column(modifier.padding(16.dp)) {

        /** -----------------------------
         * 테이블 선택 섹션 (수평 스크롤 칩 방식)
         * ----------------------------- */
        Text(stringResource(R.string.select_table), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 설정된 테이블 개수만큼 루프를 돌며 버튼 생성
            for (i in 1..vm.tableCount.value) {
                AssistChip(
                    onClick = { selectedTable = i },
                    label = { Text(i.toString()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedTable == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 메뉴 선택 섹션 (리스트 뷰)
         * ----------------------------- */
        Text(stringResource(R.string.select_menu), style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(vm.menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable { selectedMenu = item },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMenu == item) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(item.name)
                        Text(stringResource(R.string.price_format, item.price))
                    }
                }
            }
        }

        /** -----------------------------
         * 주문 상세 설정 (메뉴 선택 시 노출)
         * ----------------------------- */
        selectedMenu?.let { menu ->

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.order_settings), style = MaterialTheme.typography.titleMedium)

            // 수량 입력 필드
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.quantity)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // 커스텀 옵션(예: 알러지 특이사항 등) 선택 체크박스 목록
            if (menu.customOptions.isNotEmpty()) {
                Text(stringResource(R.string.custom_options))
                menu.customOptions.forEach { opt ->
                    Row {
                        Checkbox(
                            checked = selectedOptions.contains(opt),
                            onCheckedChange = {
                                if (it) selectedOptions.add(opt)
                                else selectedOptions.remove(opt)
                            }
                        )
                        Text(opt.label)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            /**
             * 주문 추가 버튼
             * 현재 구성을 장바구니(vm.currentOrders)에 담습니다.
             */
            Button(
                onClick = {
                    try {
                        val q = qty.toIntOrNull() ?: 1
                        vm.addOrder(selectedTable, menu, q, selectedOptions.toList())
                        // 추가 후 필드 초기화
                        selectedOptions.clear()
                        qty = "1"
                        selectedMenu = null
                    } catch (e: Exception) {
                        vm.showError("ADD_ORDER", e)
                    }
                }
            ) {
                Text(stringResource(R.string.add))
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 현재 주문 목록 (장바구니) 확인 섹션
         * ----------------------------- */
        Text(stringResource(R.string.order_list), style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(vm.currentOrders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.table_format, order.tableNumber))
                            Text("${order.menuItem.name} x${order.quantity}")
                        }
                        // 항목 삭제 버튼
                        Button(onClick = { vm.removeOrder(order) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 최종 주문 확정 및 다중 인쇄 버튼
         * ----------------------------- */
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = vm.selectedPrinters.isNotEmpty() && vm.currentOrders.isNotEmpty()
        ) {
            Text(stringResource(R.string.confirm_order_and_print))
        }

        // 주문 확정 재확인 다이얼로그
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(stringResource(R.string.confirm_order_and_print)) },
                text = { Text(stringResource(R.string.confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.confirmOrders(context)
                        showConfirmDialog = false
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
