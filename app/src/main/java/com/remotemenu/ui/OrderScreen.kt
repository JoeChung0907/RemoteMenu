package com.remotemenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotemenu.MainViewModel
import com.remotemenu.model.CustomOption
import com.remotemenu.model.MenuItem
import com.remotemenu.model.OrderItem
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.remotemenu.bluetooth.BluetoothPrinter

@Composable
fun OrderScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    var selectedTable by remember { mutableStateOf(1) }
    var selectedMenu by remember { mutableStateOf<MenuItem?>(null) }
    var qty by remember { mutableStateOf("1") }
    val selectedOptions = remember { mutableStateListOf<CustomOption>() }

    Column(modifier.padding(16.dp)) {

        Text("테이블 선택", style = MaterialTheme.typography.titleMedium)
        Row {
            OutlinedTextField(
                value = selectedTable.toString(),
                onValueChange = {
                    val t = it.toIntOrNull()
                    if (t != null && t > 0) selectedTable = t
                },
                label = { Text("테이블 번호") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("메뉴 선택", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(vm.menuItems) { item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable { selectedMenu = item }
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(item.name)
                        Text("£${item.price}")
                    }
                }
            }
        }

        selectedMenu?.let { menu ->
            Spacer(Modifier.height(16.dp))
            Text("주문 설정", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter(Char::isDigit) },
                label = { Text("수량") }
            )

            Spacer(Modifier.height(8.dp))

            if (menu.customOptions.isNotEmpty()) {
                Text("커스텀 옵션")
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

            Button(onClick = {
                val q = qty.toIntOrNull() ?: 1
                vm.addOrder(selectedTable, menu, q, selectedOptions.toList())
                selectedOptions.clear()
                qty = "1"
            }) { Text("추가") }
        }

        Spacer(Modifier.height(16.dp))

        Text("주문 목록", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(vm.currentOrders) { order: OrderItem ->
                Card(Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("테이블 ${order.tableNumber}")
                            Text("${order.menuItem.name} x${order.quantity}")
                        }
                        Button(onClick = { vm.removeOrder(order) }) { Text("삭제") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        val context = LocalContext.current
        Button(
            onClick = {
                vm.confirmOrders { text ->
                    // 나중에 Bluetooth 프린트 연결
                    val printer = vm.selectedPrinter.value

                    if (printer != null) {
                        vm.confirmOrders { text ->
                            val bp = BluetoothPrinter(context)
                            bp.printToDevice(printer, text)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("주문 확정 및 프린트") }
    }
}
