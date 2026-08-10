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

/**
 * MainViewModel
 * 앱의 핵심 상태 관리 및 데이터 영속성을 담당하는 클래스.
 */
class MainViewModel : ViewModel() {

    private val storage = StorageManager()
    private val gson = Gson()

    /** -----------------------------
     * UI 상태
     * ----------------------------- */
    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()
    val bluetoothPrinters = mutableStateListOf<BluetoothDevice>()
    val selectedPrinters = mutableStateListOf<BluetoothDevice>()
    val currentLanguage = mutableStateOf("ko")

    private var menuId = 1
    private var optionId = 1
    private var orderId = 1
    private var historyId = 1

    /** -----------------------------
     * 앱 초기화
     * ----------------------------- */
    fun initialize(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
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
                    data.printerNames.forEach { name ->
                        bluetoothPrinters.firstOrNull { 
                            try { it.name == name } catch (_: SecurityException) { false }
                        }?.let { selectedPrinters.add(it) }
                    }
                }
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

    /** -----------------------------
     * 데이터 저장
     * ----------------------------- */
    fun forceSave(context: Context) {
        val data = StorageManager.LoadedData(
            menus = menuItems.toList(),
            tableCount = tableCount.value,
            history = orderHistory.toList(),
            printerNames = selectedPrinters.mapNotNull { 
                try { it.name } catch (_: SecurityException) { it.address }
            },
            language = currentLanguage.value
        )
        viewModelScope.launch(Dispatchers.IO) {
            storage.saveAll(context, data)
        }
    }

    /** -----------------------------
     * 메뉴 관리 및 일괄 가져오기
     * ----------------------------- */
    fun addMenu(context: Context, name: String, price: Int, allergy: String, options: List<String>) {
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
                val price = (item["price"] as? Double)?.toInt() ?: 0
                val allergy = item["allergy"] as? String ?: ""
                val options = (item["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                if (name.isNotEmpty()) {
                    val optionObjs = options.map { CustomOption(optionId++, it) }
                    menuItems.add(MenuItem(menuId++, name, price, allergy, optionObjs))
                    count++
                }
            }
            forceSave(context)
            count
        } catch (_: Exception) {
            -1
        }
    }

    fun removeMenu(context: Context, item: MenuItem) {
        menuItems.remove(item)
        forceSave(context)
    }

    /** -----------------------------
     * 기타 관리 기능
     * ----------------------------- */
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

    fun resetAllData(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.clearAll(context)
            withContext(Dispatchers.Main) {
                resetInternalState()
                onComplete()
            }
        }
    }

    /** -----------------------------
     * 주문서 텍스트 생성
     * ----------------------------- */
    fun generateOrderText(context: Context): String {
        if (currentOrders.isEmpty()) return ""
        val sb = StringBuilder()
        currentOrders.groupBy { it.tableNumber }.forEach { (table, list) ->
            sb.append(context.getString(R.string.receipt_table_prefix, table))
            sb.append("\n")
            list.forEach { o ->
                sb.append(context.getString(R.string.receipt_menu, o.menuItem.name))
                sb.append(context.getString(R.string.receipt_quantity, o.quantity))
                if (o.selectedOptions.isNotEmpty()) {
                    sb.append(context.getString(R.string.receipt_options))
                    o.selectedOptions.forEach { opt -> sb.append(" - ${opt.label}\n") }
                }
                sb.append(context.getString(R.string.receipt_total, o.menuItem.price * o.quantity))
                sb.append(context.getString(R.string.receipt_divider))
            }
        }
        return sb.toString()
    }

    /** -----------------------------
     * 주문 확정 및 다중 프린트
     * ----------------------------- */
    fun confirmOrders(context: Context) {
        val text = generateOrderText(context)
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            // 1. 인쇄 수행 (IO 쓰레드에서 병렬 처리됨)
            val bp = BluetoothPrinter(context)
            for (printer in selectedPrinters) {
                bp.printToDevice(printer, text)
            }
            
            // 2. 기록 저장 및 상태 초기화
            orderHistory.add(0, OrderHistoryItem(historyId++, System.currentTimeMillis(), text))
            currentOrders.clear()
            forceSave(context)
        }
    }

    /** -----------------------------
     * 테스트 인쇄 전용
     * ----------------------------- */
    fun printTest(context: Context) {
        viewModelScope.launch {
            val orderText = generateOrderText(context)
            val testText = context.getString(R.string.test_print_text)
            val finalText = if (orderText.isEmpty()) testText else "$orderText\n$testText"
            
            val bp = BluetoothPrinter(context)
            for (printer in selectedPrinters) {
                bp.printToDevice(printer, finalText)
            }
        }
    }

    /** -----------------------------
     * Bluetooth 관리
     * ----------------------------- */
    fun loadBluetoothPrinters(context: Context) {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter ?: return
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!hasPermission) return
        val paired = try { adapter.bondedDevices } catch (_: SecurityException) { emptySet() }
        bluetoothPrinters.clear()
        bluetoothPrinters.addAll(paired)
    }

    fun togglePrinter(context: Context, device: BluetoothDevice) {
        if (selectedPrinters.contains(device)) selectedPrinters.remove(device)
        else selectedPrinters.add(device)
        forceSave(context)
    }

    fun setLanguage(context: Context, lang: String) {
        currentLanguage.value = lang
        forceSave(context)
    }
}
