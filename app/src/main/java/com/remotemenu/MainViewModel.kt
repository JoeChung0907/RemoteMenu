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
import com.remotemenu.model.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * MainViewModel
 * 앱의 핵심 상태 관리 및 데이터 영속성을 담당하는 클래스.
 */
class MainViewModel : ViewModel() {

    private val storage = StorageManager()

    /** -----------------------------
     * UI 상태
     * ----------------------------- */
    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()
    val bluetoothPrinters = mutableStateListOf<BluetoothDevice>()
    val selectedPrinter = mutableStateOf<BluetoothDevice?>(null)
    val currentLanguage = mutableStateOf("ko")

    /** -----------------------------
     * 내부 ID 관리
     * ----------------------------- */
    private var menuId = 1
    private var optionId = 1
    private var orderId = 1
    private var historyId = 1

    /** -----------------------------
     * 앱 초기화
     * ----------------------------- */
    fun initialize(context: Context) {
        viewModelScope.launch {
            val data = storage.loadAll(context)

            if (data.isEmpty) {
                resetInternalState()
                return@launch
            }

            // 데이터 복원
            menuItems.clear()
            menuItems.addAll(data.menus)
            tableCount.value = data.tableCount
            orderHistory.clear()
            orderHistory.addAll(data.history)
            currentLanguage.value = data.language

            // ID 동기화
            menuId = (menuItems.maxOfOrNull { it.id } ?: 0) + 1
            optionId = (menuItems.flatMap { it.customOptions }.maxOfOrNull { it.id } ?: 0) + 1
            historyId = (orderHistory.maxOfOrNull { it.id } ?: 0) + 1
            orderId = 1

            // 저장된 프린터 복원
            val savedName = data.printerName
            if (savedName != null) {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else true

                if (hasPermission) {
                    val device = bluetoothPrinters.firstOrNull { it.name == savedName }
                    selectedPrinter.value = device
                }
            }
        }
    }

    private fun resetInternalState() {
        tableCount.value = 1
        menuItems.clear()
        currentOrders.clear()
        orderHistory.clear()
        selectedPrinter.value = null
        currentLanguage.value = "ko"
        menuId = 1
        optionId = 1
        orderId = 1
        historyId = 1
    }

    /** -----------------------------
     * 데이터 저장
     * ----------------------------- */
    fun forceSave(context: Context) {
        val data = StorageManager.LoadedData(
            menus = menuItems.toList(),
            tableCount = tableCount.value,
            history = orderHistory.toList(),
            printerName = selectedPrinter.value?.name,
            language = currentLanguage.value
        )
        viewModelScope.launch {
            storage.saveAll(context, data)
        }
    }

    /** -----------------------------
     * 메뉴 관리
     * ----------------------------- */
    fun addMenu(context: Context, name: String, price: Int, allergy: String, options: List<String>) {
        val optionObjs = options.map { CustomOption(optionId++, it) }
        menuItems.add(MenuItem(menuId++, name, price, allergy, optionObjs))
        forceSave(context)
    }

    fun removeMenu(context: Context, item: MenuItem) {
        menuItems.remove(item)
        forceSave(context)
    }

    /** -----------------------------
     * 테이블 설정 관리
     * ----------------------------- */
    fun updateTableCount(context: Context, count: Int) {
        tableCount.value = count
        forceSave(context)
    }

    /** -----------------------------
     * 주문 관리
     * ----------------------------- */
    fun addOrder(table: Int, menu: MenuItem, qty: Int, opts: List<CustomOption>) {
        currentOrders.add(
            OrderItem(orderId++, table, menu, qty, opts, opts.isNotEmpty())
        )
    }

    fun removeOrder(order: OrderItem) {
        currentOrders.remove(order)
    }

    /** -----------------------------
     * 전체 데이터 초기화
     * ----------------------------- */
    fun resetAllData(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            storage.clearAll(context)
            resetInternalState()
            onComplete()
        }
    }

    /** -----------------------------
     * 주문 확정 (다국어 영수증 생성)
     * ----------------------------- */
    fun confirmOrders(context: Context, onPrint: (String) -> Unit) {
        if (currentOrders.isEmpty()) return

        val sb = StringBuilder()
        val time = System.currentTimeMillis()

        currentOrders.groupBy { it.tableNumber }.forEach { (table, list) ->
            sb.append(context.getString(R.string.receipt_table_prefix, table))
            list.forEach { o ->
                sb.append(context.getString(R.string.receipt_menu, o.menuItem.name))
                sb.append(context.getString(R.string.receipt_quantity, o.quantity))

                if (o.selectedOptions.isNotEmpty()) {
                    sb.append(context.getString(R.string.receipt_options))
                    o.selectedOptions.forEach { opt ->
                        sb.append(" - ${opt.label}\n")
                    }
                }

                val itemTotal = o.menuItem.price * o.quantity
                sb.append(context.getString(R.string.receipt_total, itemTotal))
                sb.append(context.getString(R.string.receipt_divider))
            }
        }

        val text = sb.toString()
        onPrint(text)

        orderHistory.add(0, OrderHistoryItem(historyId++, time, text))
        currentOrders.clear()
        forceSave(context)
    }

    /** -----------------------------
     * Bluetooth 프린터 목록 로드
     * ----------------------------- */
    fun loadBluetoothPrinters(context: Context) {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter ?: return

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) return

        val paired = try {
            adapter.bondedDevices
        } catch (_: SecurityException) {
            emptySet()
        }

        bluetoothPrinters.clear()
        bluetoothPrinters.addAll(paired)
    }

    /** -----------------------------
     * 프린터 선택
     * ----------------------------- */
    fun selectPrinter(context: Context, device: BluetoothDevice) {
        selectedPrinter.value = device
        forceSave(context)
    }

    /** -----------------------------
     * 언어 설정 변경
     * ----------------------------- */
    fun setLanguage(context: Context, lang: String) {
        currentLanguage.value = lang
        forceSave(context)
    }
}
