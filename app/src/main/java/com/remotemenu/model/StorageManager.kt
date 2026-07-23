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
val Context.dataStore by preferencesDataStore(name = "remotemenu_store")

class StorageManager {

    private val gson = Gson()

    private object Keys {
        val MENU_ITEMS = stringPreferencesKey("menu_items")
        val TABLE_COUNT = stringPreferencesKey("table_count")
        val ORDER_HISTORY = stringPreferencesKey("order_history")
        val SELECTED_PRINTERS = stringPreferencesKey("selected_printers") // 리스트로 변경
        val LANGUAGE = stringPreferencesKey("language")
    }

    data class LoadedData(
        val menus: List<MenuItem> = emptyList(),
        val tableCount: Int = 1,
        val history: List<OrderHistoryItem> = emptyList(),
        val printerNames: List<String> = emptyList(), // 다중 프린터 이름
        val language: String = "ko",
        val isEmpty: Boolean = false
    )

    suspend fun loadAll(context: Context): LoadedData {
        val prefs = context.dataStore.data.first()
        
        val menuJson = prefs[Keys.MENU_ITEMS]
        val historyJson = prefs[Keys.ORDER_HISTORY]
        val printersJson = prefs[Keys.SELECTED_PRINTERS]
        
        val menus: List<MenuItem> = if (menuJson != null) {
            gson.fromJson(menuJson, object : TypeToken<List<MenuItem>>() {}.type)
        } else emptyList()

        val history: List<OrderHistoryItem> = if (historyJson != null) {
            gson.fromJson(historyJson, object : TypeToken<List<OrderHistoryItem>>() {}.type)
        } else emptyList()

        val printerNames: List<String> = if (printersJson != null) {
            gson.fromJson(printersJson, object : TypeToken<List<String>>() {}.type)
        } else emptyList()

        val tableCount = prefs[Keys.TABLE_COUNT]?.toIntOrNull() ?: 1
        val language = prefs[Keys.LANGUAGE] ?: "ko"

        val isEmpty = menus.isEmpty() && history.isEmpty() && printerNames.isEmpty()

        return LoadedData(menus, tableCount, history, printerNames, language, isEmpty)
    }

    suspend fun saveAll(context: Context, data: LoadedData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MENU_ITEMS] = gson.toJson(data.menus)
            prefs[Keys.TABLE_COUNT] = data.tableCount.toString()
            prefs[Keys.ORDER_HISTORY] = gson.toJson(data.history)
            prefs[Keys.LANGUAGE] = data.language
            prefs[Keys.SELECTED_PRINTERS] = gson.toJson(data.printerNames)
        }
    }

    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
