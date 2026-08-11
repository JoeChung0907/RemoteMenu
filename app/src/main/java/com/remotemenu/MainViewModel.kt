package com.remotemenu

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.remotemenu.model.*
import com.remotemenu.bluetooth.BluetoothPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

class MainViewModel : ViewModel() {

    private val storage = StorageManager()
    private val gson = Gson()

    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()
    val bluetoothPrinters = mutableStateListOf<BluetoothDevice>()
    val selectedPrinters = mutableStateListOf<BluetoothDevice>()
    val currentLanguage = mutableStateOf("ko")
    val globalErrorMessage = mutableStateOf<String?>(null)

    private var menuId = 1
    private var optionId = 1
    private var orderId = 1
    private var historyId = 1

    fun showError(tag: String, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val errorCode = "[ERR-${tag.uppercase()}]\n${e.message}\n\n${sw.toString().take(200)}..."
        globalErrorMessage.value = errorCode
    }

    fun initialize(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = storage.loadAll(context)
                withContext(Dispatchers.Main) {
                    if (data.isEmpty) {
                        resetInternalState()
                    } else {
                        menuItems.clear()
                        menuItems.addAll(data.menus)
                        tableCount.value = data.tableCount
                        orderHistory.clear()
                        orderHistory.addAll(data.history)
                        currentLanguage.value = data.language
                        updateInternalIds()
                        loadBluetoothPrinters(context)
                        selectedPrinters.clear()
                        data.printerNames.forEach { savedAddr ->
                            bluetoothPrinters.firstOrNull { it.address == savedAddr }?.let { selectedPrinters.add(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("INIT", e) }
            }
        }
    }

    private fun updateInternalIds() {
        menuId = (menuItems.maxOfOrNull { it.id } ?: 0) + 1
        optionId = (menuItems.flatMap { it.customOptions }.maxOfOrNull { it.id } ?: 0) + 1
        historyId = (orderHistory.maxOfOrNull { it.id } ?: 0) + 1
    }

    private fun resetInternalState() {
        tableCount.value = 1
        menuItems.clear()
        currentOrders.clear()
        orderHistory.clear()
        selectedPrinters.clear()
        currentLanguage.value = "ko"
        menuId = 1; optionId = 1; orderId = 1; historyId = 1
    }

    fun forceSave(context: Context) {
        val data = StorageManager.LoadedData(
            menus = menuItems.toList(),
            tableCount = tableCount.value,
            history = orderHistory.toList(),
            printerNames = selectedPrinters.map { it.address },
            language = currentLanguage.value
        )
        viewModelScope.launch(Dispatchers.IO) {
            try { storage.saveAll(context, data) } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("SAVE", e) }
            }
        }
    }

    fun addMenu(context: Context, name: String, price: Double, allergy: String, options: List<String>) {
        val optionObjs = options.map { CustomOption(optionId++, it) }
        menuItems.add(MenuItem(menuId++, name, price, allergy, optionObjs))
        forceSave(context)
    }

    fun importMenus(context: Context, json: String): Int {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val data: List<Map<String, Any>> = gson.fromJson(json, type)
            var count = 0
            data.forEach { item ->
                val name = item["name"] as? String ?: ""
                val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                val allergy = item["allergy"] as? String ?: ""
                val options = (item["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (name.isNotEmpty()) {
                    val optionObjs = options.map { CustomOption(optionId++, it) }
                    menuItems.add(MenuItem(menuId++, name, price, allergy, optionObjs))
                    count++
                }
            }
            forceSave(context)
            count
        } catch (e: Exception) {
            showError("IMPORT", e)
            -1
        }
    }

    fun removeMenu(context: Context, item: MenuItem) {
        menuItems.remove(item)
        forceSave(context)
    }

    fun updateTableCount(context: Context, count: Int) {
        tableCount.value = count
        forceSave(context)
    }

    fun addOrder(table: Int, menu: MenuItem, qty: Int, opts: List<CustomOption>) {
        currentOrders.add(OrderItem(orderId++, table, menu, qty, opts, opts.isNotEmpty()))
    }

    fun removeOrder(order: OrderItem) {
        currentOrders.remove(order)
    }

    fun clearHistory(context: Context) {
        orderHistory.clear()
        forceSave(context)
    }

    fun resetAllData(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storage.clearAll(context)
                withContext(Dispatchers.Main) {
                    resetInternalState()
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("RESET", e) }
            }
        }
    }

    fun generateOrderText(context: Context): String {
        if (currentOrders.isEmpty()) return ""
        val sb = StringBuilder()
        currentOrders.groupBy { it.tableNumber }.toSortedMap().forEach { (table, list) ->
            sb.append(context.getString(R.string.receipt_table_prefix, table))
            sb.append("\n")
            list.forEach { o ->
                sb.append(context.getString(R.string.receipt_menu, o.menuItem.name))
                sb.append(context.getString(R.string.receipt_quantity, o.quantity))
                if (o.selectedOptions.isNotEmpty()) {
                    sb.append(context.getString(R.string.receipt_options))
                    o.selectedOptions.forEach { opt -> sb.append(" - ${opt.label}\n") }
                }
                val totalPrice = o.menuItem.price * o.quantity
                val priceText = String.format(Locale.US, "%.2f", totalPrice)
                sb.append("Total: £$priceText\n")
                sb.append(context.getString(R.string.receipt_divider))
            }
        }
        return sb.toString()
    }

    fun confirmOrders(context: Context) {
        val text = generateOrderText(context)
        if (text.isEmpty()) return

        if (selectedPrinters.isEmpty()) {
            showError("PRINT", RuntimeException("선택된 프린터가 없습니다. [설정] 탭에서 프린터를 먼저 체크해 주세요."))
            return
        }
        
        viewModelScope.launch {
            try {
                val bp = BluetoothPrinter(context)
                for (printer in selectedPrinters) {
                    try {
                        bp.printToDevice(printer, text)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { showError("PRINT_DEVICE", e) }
                    }
                }
                orderHistory.add(0, OrderHistoryItem(historyId++, System.currentTimeMillis(), text))
                currentOrders.clear()
                forceSave(context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("CONFIRM_ORDERS", e) }
            }
        }
    }

    fun printTest(context: Context) {
        viewModelScope.launch {
            try {
                val orderText = generateOrderText(context)
                val testText = context.getString(R.string.test_print_text)
                val finalText = if (orderText.isEmpty()) testText else "$orderText\n$testText"
                
                if (selectedPrinters.isEmpty()) {
                    showError("PRINT", RuntimeException("선택된 프린터가 없습니다."))
                    return@launch
                }

                val bp = BluetoothPrinter(context)
                for (printer in selectedPrinters) {
                    try {
                        bp.printToDevice(printer, finalText)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { showError("TEST_PRINT_DEVICE", e) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("PRINT_TEST", e) }
            }
        }
    }

    fun loadBluetoothPrinters(context: Context) {
        try {
            val manager = context.getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter ?: return
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
            if (!hasPermission) return
            val paired = adapter.bondedDevices
            bluetoothPrinters.clear()
            bluetoothPrinters.addAll(paired)
        } catch (e: Exception) {
            showError("BT_LOAD", e)
        }
    }

    fun togglePrinter(context: Context, device: BluetoothDevice) {
        val existing = selectedPrinters.firstOrNull { it.address == device.address }
        if (existing != null) {
            selectedPrinters.remove(existing)
        } else {
            selectedPrinters.add(device)
        }
        forceSave(context)
    }

    fun setLanguage(context: Context, lang: String) {
        currentLanguage.value = lang
        forceSave(context)
    }
}
