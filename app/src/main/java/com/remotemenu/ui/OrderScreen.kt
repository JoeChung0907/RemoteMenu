package com.remotemenu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.remotemenu.MainViewModel
import com.remotemenu.R
import com.remotemenu.model.CustomOption
import com.remotemenu.model.MenuItem

@Composable
fun OrderScreen(modifier: Modifier = Modifier, vm: MainViewModel) {
    var selectedTable by remember { mutableIntStateOf(1) }
    var selectedMenu by remember { mutableStateOf<MenuItem?>(null) }
    var qty by remember { mutableIntStateOf(1) }
    val selectedOptions = remember { mutableStateListOf<CustomOption>() }
    var showConfirmOverlay by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            
            /** -----------------------------
             * 1. 테이블 선택 (상단 가로 칩)
             * ----------------------------- */
            Text(stringResource(R.string.select_table), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..vm.tableCount.value) {
                    FilterChip(
                        selected = selectedTable == i,
                        onClick = { selectedTable = i },
                        label = { Text(i.toString(), style = MaterialTheme.typography.bodyLarge) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            /** -----------------------------
             * 2. 메뉴 선택 (더블 그리드 유지)
             * ----------------------------- */
            Text(stringResource(R.string.select_menu), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.menuItems) { item ->
                    val isSelected = selectedMenu == item
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable {
                                selectedMenu = item
                                qty = 1
                                selectedOptions.clear()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.allergy.isNotEmpty()) {
                                    Text(
                                        text = item.allergy,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.price_format, item.price),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            /** -----------------------------
             * 3. 상세 설정 (메뉴 선택 시 노출)
             * ----------------------------- */
            selectedMenu?.let { menu ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(menu.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(stringResource(R.string.quantity), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            FilledIconButton(onClick = { if (qty > 1) qty-- }) { Icon(Icons.Default.Remove, null) }
                            Text(qty.toString(), modifier = Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineMedium)
                            FilledIconButton(onClick = { if (qty < 99) qty++ }) { Icon(Icons.Default.Add, null) }
                        }

                        if (menu.customOptions.isNotEmpty()) {
                            menu.customOptions.forEach { opt ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = selectedOptions.contains(opt), onCheckedChange = {
                                        if (it) selectedOptions.add(opt) else selectedOptions.remove(opt)
                                    })
                                    Text(opt.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                vm.addOrder(selectedTable, menu, qty, selectedOptions.toList())
                                selectedOptions.clear()
                                qty = 1
                                selectedMenu = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text(stringResource(R.string.add), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            /** -----------------------------
             * 4. 현재 주문 목록 및 확정 버튼
             * ----------------------------- */
            Text(stringResource(R.string.order_list), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(0.5f)) {
                items(vm.currentOrders) { order ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("[T${order.tableNumber}] ${order.menuItem.name}", fontWeight = FontWeight.Bold)
                                if (order.selectedOptions.isNotEmpty()) {
                                    Text(order.selectedOptions.joinToString { it.label }, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("x${order.quantity}", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { vm.removeOrder(order) }) { Icon(Icons.Default.Remove, null, tint = Color.Red) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showConfirmOverlay = true },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = vm.selectedPrinters.isNotEmpty() && vm.currentOrders.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm_order_and_print), style = MaterialTheme.typography.titleLarge)
            }
        }

        /** -----------------------------
         * 안드로이드 10 보안 에러 방지용 커스텀 오버레이 확인창
         * ----------------------------- */
        if (showConfirmOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = true) { /* 차단용 */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.8f).padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.confirm_order_and_print), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.confirm), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showConfirmOverlay = false }) { Text(stringResource(R.string.cancel)) }
                            Button(onClick = {
                                vm.confirmOrders(context)
                                showConfirmOverlay = false
                            }, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.confirm)) }
                        }
                    }
                }
            }
        }
    }
}
