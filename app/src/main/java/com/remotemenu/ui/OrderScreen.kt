package com.remotemenu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var qty by remember { mutableIntStateOf(1) } // String에서 Int로 변경 (렉 방지 및 입력 간소화)
    val selectedOptions = remember { mutableStateListOf<CustomOption>() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    /** -----------------------------
     * 화면 레이아웃 메인 구조
     * ----------------------------- */
    Column(modifier.padding(16.dp)) {

        /** -----------------------------
         * 테이블 선택 섹션
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
         * 메뉴 선택 섹션
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
         * 주문 상세 설정 (수량 조절 - 버튼 방식으로 변경)
         * ----------------------------- */
        selectedMenu?.let { menu ->

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.order_settings), style = MaterialTheme.typography.titleMedium)

            /**
             * 수량 조절 레이아웃 (키보드 없이 터치로만 조작)
             */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.quantity), modifier = Modifier.weight(1f))
                
                // 마이너스 버튼
                FilledIconButton(
                    onClick = { if (qty > 1) qty-- },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }

                // 현재 수량 표시
                Text(
                    text = qty.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // 플러스 버튼
                FilledIconButton(
                    onClick = { if (qty < 99) qty++ }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 옵션 선택
            if (menu.customOptions.isNotEmpty()) {
                Text(stringResource(R.string.custom_options))
                menu.customOptions.forEach { opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    try {
                        vm.addOrder(selectedTable, menu, qty, selectedOptions.toList())
                        selectedOptions.clear()
                        qty = 1 // 초기화
                        selectedMenu = null
                    } catch (e: Exception) {
                        vm.showError("ADD_ORDER", e)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add))
            }
        }

        Spacer(Modifier.height(16.dp))

        /** -----------------------------
         * 장바구니 및 확정 버튼 (기존 동일)
         * ----------------------------- */
        Text(stringResource(R.string.order_list), style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(vm.currentOrders) { order ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.table_format, order.tableNumber))
                            Text("${order.menuItem.name} x${order.quantity}")
                        }
                        Button(onClick = { vm.removeOrder(order) }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = vm.selectedPrinters.isNotEmpty() && vm.currentOrders.isNotEmpty()
        ) {
            Text(stringResource(R.string.confirm_order_and_print))
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(stringResource(R.string.confirm_order_and_print)) },
                text = { Text(stringResource(R.string.confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.confirmOrders(context)
                        showConfirmDialog = false
                    }) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}
