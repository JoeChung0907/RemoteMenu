package com.remotemenu

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.remotemenu.model.*

class MainViewModel : ViewModel() {

    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()

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

    fun confirmOrders(onPrint: (String) -> Unit) {
        if (currentOrders.isEmpty()) return

        val sb = StringBuilder()
        val time = System.currentTimeMillis()

        currentOrders.groupBy { it.tableNumber }.forEach { (table, list) ->
            sb.append("[테이블 $table]\n")
            list.forEach { o ->
                sb.append("- ${o.menuItem.name} x${o.quantity}\n")
                if (o.selectedOptions.isNotEmpty()) {
                    sb.append("  옵션: ${o.selectedOptions.joinToString { it.label }}\n")
                }
                sb.append("  합계: £${o.menuItem.price * o.quantity}\n\n")
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
