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
import com.remotemenu.model.OrderItem
import com.remotemenu.bluetooth.BluetoothPrinter

/**
 * OrderScreen
 * 메뉴 주문 및 테이블 관리를 담당하는 화면 컴포저블.
 */
@Composable
fun OrderScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel
) {
    /** -----------------------------
     * UI 상태 관리
     * ----------------------------- */
    var selectedTable by remember { mutableIntStateOf(1) }
    var selectedMenu by remember { mutableStateOf<MenuItem?>(null) }
    var qty by remember { mutableStateOf("1") }
    val selectedOptions = remember { mutableStateListOf<CustomOption>() }

    val context = LocalContext.current

    /** -----------------------------
     * 화면 레이아웃
     * ----------------------------- */
    Column(modifier.padding(16.dp)) {

        /** -----------------------------
         * 테이블 선택 섹션 (터치 칩 방식)
         * ----------------------------- */
        Text(stringResource(R.string.select_table), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
         * 메뉴 선택 섹션 (리스트)
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
                        Text("£${item.price}")
                    }
                }
            }
        }

        /** -----------------------------
         * 상세 주문 설정 (수량 및 옵션)
         * ----------------------------- */
        selectedMenu?.let { menu ->

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.order_settings), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.quantity)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

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

            Button(
                onClick = {
                    val q = qty.toIntOrNull() ?: 1
                    vm.addOrder(selectedTable, menu, q, selectedOptions.toList())
                    selectedOptions.clear()
                    qty = "1"
                    selectedMenu = null
                }
            ) {
                Text(stringResource(R.string.add))
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 현재 주문 목록 확인 섹션
         * ----------------------------- */
        Text(stringResource(R.string.order_list), style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(vm.currentOrders) { order: OrderItem ->
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
                        Button(onClick = { vm.removeOrder(order) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 최종 주문 확정 및 다중 프린트 실행
         * ----------------------------- */
        Button(
            onClick = {
                if (vm.selectedPrinters.isNotEmpty()) {
                    vm.confirmOrders(context) { text ->
                        val bp = BluetoothPrinter(context)
                        // suspend lambda 내에서는 for-loop을 사용하여 순차적으로 suspend 함수 호출 가능
                        for (printer in vm.selectedPrinters) {
                            bp.printToDevice(printer, text)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = vm.selectedPrinters.isNotEmpty()
        ) {
            Text(stringResource(R.string.confirm_order_and_print))
        }
    }
}
