package com.remotemenu.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * StorageManager
 * 앱의 모든 데이터를 DataStore에 저장하고 불러오는 영속성 관리 클래스.
 */

// DataStore 확장 프로퍼티 정의
val Context.dataStore by preferencesDataStore(name = "remotemenu_store")

class StorageManager {

    private val gson = Gson()

    /** -----------------------------
     * 저장 키 정의
     * ----------------------------- */
    private object Keys {
        val MENU_ITEMS = stringPreferencesKey("menu_items")
        val TABLE_COUNT = stringPreferencesKey("table_count")
        val ORDER_HISTORY = stringPreferencesKey("order_history")
        val SELECTED_PRINTER = stringPreferencesKey("selected_printer")
    }

    /** -----------------------------
     * 데이터 모델 (일괄 로드용)
     * ----------------------------- */
    data class LoadedData(
        val menus: List<MenuItem> = emptyList(),
        val tableCount: Int = 1,
        val history: List<OrderHistoryItem> = emptyList(),
        val printerName: String? = null,
        val isEmpty: Boolean = false
    )

    /** -----------------------------
     * 모든 데이터 일괄 불러오기
     * ----------------------------- */
    suspend fun loadAll(context: Context): LoadedData {
        val prefs = context.dataStore.data.first()
        
        val menuJson = prefs[Keys.MENU_ITEMS]
        val historyJson = prefs[Keys.ORDER_HISTORY]
        
        // JSON 역직렬화
        val menus: List<MenuItem> = if (menuJson != null) {
            gson.fromJson(menuJson, object : TypeToken<List<MenuItem>>() {}.type)
        } else emptyList()

        val history: List<OrderHistoryItem> = if (historyJson != null) {
            gson.fromJson(historyJson, object : TypeToken<List<OrderHistoryItem>>() {}.type)
        } else emptyList()

        val tableCount = prefs[Keys.TABLE_COUNT]?.toIntOrNull() ?: 1
        val printerName = prefs[Keys.SELECTED_PRINTER]

        val isEmpty = menus.isEmpty() && history.isEmpty() && printerName == null

        return LoadedData(menus, tableCount, history, printerName, isEmpty)
    }

    /** -----------------------------
     * 모든 데이터 일괄 저장 (트랜잭션)
     * ----------------------------- */
    suspend fun saveAll(context: Context, data: LoadedData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MENU_ITEMS] = gson.toJson(data.menus)
            prefs[Keys.TABLE_COUNT] = data.tableCount.toString()
            prefs[Keys.ORDER_HISTORY] = gson.toJson(data.history)
            if (data.printerName != null) {
                prefs[Keys.SELECTED_PRINTER] = data.printerName
            } else {
                prefs.remove(Keys.SELECTED_PRINTER)
            }
        }
    }

    /** -----------------------------
     * 전체 데이터 삭제
     * ----------------------------- */
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
