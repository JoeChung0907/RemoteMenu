package com.remotemenu

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.remotemenu.model.*
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager


class MainViewModel : ViewModel() {

    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()
    val bluetoothPrinters = mutableStateListOf<BluetoothDevice>()

    val selectedPrinter = mutableStateOf<BluetoothDevice?>(null)

    fun loadBluetoothPrinters(context: Context) {
        // BluetoothManager 에서 Adapter 가져오기
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter ?: return

        // Android 12+ 권한 체크
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        // 페어링된 기기 목록 가져오기
        val paired = adapter.bondedDevices

        // 상태 업데이트
        bluetoothPrinters.clear()
        bluetoothPrinters.addAll(paired)
    }
    fun selectPrinter(device: BluetoothDevice) {
        selectedPrinter.value = device
    }

    private var menuId = 1
    private var optionId = 1
    private var orderId = 1
    private var historyId = 1

    fun addMenu(name: String, price: Int, allergy: String, options: List<String>) {
        val optionObjs = options.map { CustomOption(optionId++, it) }
        menuItems.add(

            MenuItem(menuId++, name, price, allergy, optionObjs)
        )
    }

    fun removeMenu(item: MenuItem) {
        menuItems.remove(item)
    }

    fun addOrder(table: Int, menu: MenuItem, qty: Int, opts: List<CustomOption>) {
        currentOrders.add(
            OrderItem(orderId++, table, menu, qty, opts, opts.isNotEmpty())
        )
    }

    fun removeOrder(order: OrderItem) {
        currentOrders.remove(order)
    }

    fun updateQty(order: OrderItem, newQty: Int) {
        if (order.isCustom) return
        val idx = currentOrders.indexOf(order)
        if (idx != -1) currentOrders[idx] = order.copy(quantity = newQty)
    }

    fun clearOrders() {
        currentOrders.clear()
    }

    // 주문 내역 데이터 ( 프린트에 사용할 것)
    fun confirmOrders(onPrint: (String) -> Unit) {
        if (currentOrders.isEmpty()) return

        val sb = StringBuilder()
        val time = System.currentTimeMillis()

        currentOrders.groupBy { it.tableNumber }.forEach { (table, list) ->
            sb.append("[테이블 $table]\n")
            list.forEach { o ->
                // 리스트 포맷
                sb.append("메뉴: ${o.menuItem.name}\n")
                sb.append("수량: ${o.quantity}\n")

                if (o.selectedOptions.isNotEmpty()) {
                    sb.append("옵션:\n")
                    o.selectedOptions.forEach { opt ->
                        sb.append(" - ${opt.label}\n")
                    }
                }

                val itemTotal = o.menuItem.price * o.quantity
                sb.append("합계: £$itemTotal\n")
                sb.append("----------------------\n")
            }
        }

        val text = sb.toString()
        onPrint(text)

        orderHistory.add(
            0,
            OrderHistoryItem(historyId++, time, text)
        )

        currentOrders.clear()
    }
}
