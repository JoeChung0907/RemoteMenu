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

/**
 * MainViewModel
 * 앱의 전체적인 상태(메뉴, 주문, 프린터, 설정 등)를 관리하고 비즈니스 로직을 처리합니다.
 */
class MainViewModel : ViewModel() {

    private val storage = StorageManager()
    private val gson = Gson()

    /** -----------------------------
     * UI 상태 및 에러 관리 변수
     * ----------------------------- */
    val tableCount = mutableStateOf(1)
    val menuItems = mutableStateListOf<MenuItem>()
    val currentOrders = mutableStateListOf<OrderItem>()
    val orderHistory = mutableStateListOf<OrderHistoryItem>()
    val bluetoothPrinters = mutableStateListOf<BluetoothDevice>()
    val selectedPrinters = mutableStateListOf<BluetoothDevice>()
    val currentLanguage = mutableStateOf("ko")

    // 에러 발생 시 UI에 띄울 진단 메시지
    val globalErrorMessage = mutableStateOf<String?>(null)

    private var menuId = 1
    private var optionId = 1
    private var orderId = 1
    private var historyId = 1

    /**
     * showError
     * 예외 발생 시 에러 코드를 생성하여 진단 다이얼로그를 활성화합니다.
     * @param tag 에러 발생 위치 태그
     * @param e 발생한 Throwable 객체
     */
    fun showError(tag: String, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        android.util.Log.e("RemoteMenuError", "[$tag] ${e.message}", e)
        
        val errorCode = "[ERR-${tag.uppercase()}]\n${e.message}\n\n${stackTrace.take(200)}..."
        globalErrorMessage.value = errorCode
    }

    /**
     * initialize
     * 앱 시작 시 저장된 데이터를 불러오고 내부 상태를 복원합니다.
     * @param context 애플리케이션 컨텍스트
     */
    fun initialize(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = storage.loadAll(context)
                withContext(Dispatchers.Main) {
                    if (data.isEmpty) {
                        resetInternalState()
                    } else {
                        // 데이터 복구 로직
                        menuItems.clear()
                        menuItems.addAll(data.menus)
                        tableCount.value = data.tableCount
                        orderHistory.clear()
                        orderHistory.addAll(data.history)
                        currentLanguage.value = data.language

                        updateInternalIds()
                        loadBluetoothPrinters(context)
                        
                        // 선택된 프린터 복원
                        selectedPrinters.clear()
                        data.printerNames.forEach { name ->
                            bluetoothPrinters.firstOrNull { 
                                try { it.name == name } catch (_: SecurityException) { false }
                            }?.let { selectedPrinters.add(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("INIT", e) }
            }
        }
    }

    /**
     * updateInternalIds
     * 기존 데이터 중 최대 ID를 찾아 다음 ID 생성 기준을 설정합니다.
     */
    private fun updateInternalIds() {
        menuId = (menuItems.maxOfOrNull { it.id } ?: 0) + 1
        optionId = (menuItems.flatMap { it.customOptions }.maxOfOrNull { it.id } ?: 0) + 1
        historyId = (orderHistory.maxOfOrNull { it.id } ?: 0) + 1
    }

    /**
     * resetInternalState
     * 메모리상의 모든 상태와 ID를 초기값으로 리셋합니다.
     */
    private fun resetInternalState() {
        tableCount.value = 1
        menuItems.clear()
        currentOrders.clear()
        orderHistory.clear()
        selectedPrinters.clear()
        currentLanguage.value = "ko"
        menuId = 1; optionId = 1; orderId = 1; historyId = 1
    }

    /**
     * forceSave
     * 현재 상태를 영구 저장소(DataStore)에 기록합니다.
     * @param context 애플리케이션 컨텍스트
     */
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
            try {
                storage.saveAll(context, data)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("SAVE", e) }
            }
        }
    }

    /** -----------------------------
     * 메뉴 관리 관련 함수
     * ----------------------------- */

    fun addMenu(context: Context, name: String, price: Int, allergy: String, options: List<String>) {
        val optionObjs = options.map { CustomOption(optionId++, it) }
        menuItems.add(MenuItem(menuId++, name, price, allergy, optionObjs))
        forceSave(context)
    }

    /**
     * importMenus
     * JSON 텍스트를 파싱하여 메뉴 목록을 일괄 추가합니다.
     * @param context 애플리케이션 컨텍스트
     * @param json 입력받은 JSON 텍스트
     * @return 추가된 메뉴 개수 (실패 시 -1)
     */
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
        } catch (e: Exception) {
            showError("IMPORT", e)
            -1
        }
    }

    fun removeMenu(context: Context, item: MenuItem) {
        menuItems.remove(item)
        forceSave(context)
    }

    /** -----------------------------
     * 주문 및 설정 관리 관련 함수
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

    /**
     * generateOrderText
     * 현재 장바구니에 담긴 주문 목록을 인쇄용 텍스트 포맷으로 생성합니다.
     */
    fun generateOrderText(context: Context): String {
        if (currentOrders.isEmpty()) return ""
        val sb = StringBuilder()
        currentOrders.groupBy { it.tableNumber }.forEach { (table, list) ->
            sb.append(context.getString(R.string.receipt_table_prefix, table))
            sb.append("\n") // 가독성을 위한 공백 추가
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

    /**
     * confirmOrders
     * 주문을 확정하고, 선택된 모든 프린터로 인쇄 후 기록을 저장합니다.
     */
    fun confirmOrders(context: Context) {
        val text = generateOrderText(context)
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val bp = BluetoothPrinter(context)
                for (printer in selectedPrinters) {
                    bp.printToDevice(printer, text)
                }
                orderHistory.add(0, OrderHistoryItem(historyId++, System.currentTimeMillis(), text))
                currentOrders.clear()
                forceSave(context)
            } catch (e: Exception) {
                showError("PRINT", e)
            }
        }
    }

    /**
     * printTest
     * 현재 장바구니 내역을 바탕으로 테스트 인쇄를 수행합니다.
     */
    fun printTest(context: Context) {
        viewModelScope.launch {
            try {
                val orderText = generateOrderText(context)
                val testText = context.getString(R.string.test_print_text)
                val finalText = if (orderText.isEmpty()) testText else "$orderText\n$testText"
                
                val bp = BluetoothPrinter(context)
                for (printer in selectedPrinters) {
                    bp.printToDevice(printer, finalText)
                }
            } catch (e: Exception) {
                showError("TEST_PRINT", e)
            }
        }
    }

    /** -----------------------------
     * 블루투스 관리 관련 함수
     * ----------------------------- */

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
        if (selectedPrinters.contains(device)) selectedPrinters.remove(device)
        else selectedPrinters.add(device)
        forceSave(context)
    }

    fun setLanguage(context: Context, lang: String) {
        currentLanguage.value = lang
        forceSave(context)
    }
}
