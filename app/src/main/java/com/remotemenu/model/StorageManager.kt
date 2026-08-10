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
 * Jetpack DataStore를 사용하여 앱의 모든 데이터를 영구적으로 저장하고 불러오는 역할을 담당합니다.
 */

// Context의 확장 프로퍼티로 DataStore 인스턴스 생성
val Context.dataStore by preferencesDataStore(name = "remotemenu_store")

class StorageManager {

    private val gson = Gson()

    /** -----------------------------
     * 내부 저장 키 정의
     * ----------------------------- */
    private object Keys {
        val MENU_ITEMS = stringPreferencesKey("menu_items")
        val TABLE_COUNT = stringPreferencesKey("table_count")
        val ORDER_HISTORY = stringPreferencesKey("order_history")
        val SELECTED_PRINTERS = stringPreferencesKey("selected_printers")
        val LANGUAGE = stringPreferencesKey("language")
    }

    /** -----------------------------
     * 데이터 묶음 클래스 (일괄 로드/저장용)
     * ----------------------------- */
    data class LoadedData(
        val menus: List<MenuItem> = emptyList(),
        val tableCount: Int = 1,
        val history: List<OrderHistoryItem> = emptyList(),
        val printerNames: List<String> = emptyList(),
        val language: String = "ko",
        val isEmpty: Boolean = false
    )

    /**
     * loadAll
     * 저장된 모든 데이터를 불러와 LoadedData 객체로 반환합니다.
     * @param context 애플리케이션 컨텍스트
     * @return 불러온 데이터 객체
     */
    suspend fun loadAll(context: Context): LoadedData {
        val prefs = context.dataStore.data.first()
        
        val menuJson = prefs[Keys.MENU_ITEMS]
        val historyJson = prefs[Keys.ORDER_HISTORY]
        val printersJson = prefs[Keys.SELECTED_PRINTERS]
        
        // JSON 역직렬화 수행
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

        // 초기 데이터 존재 여부 판단
        val isEmpty = menus.isEmpty() && history.isEmpty() && printerNames.isEmpty()

        return LoadedData(menus, tableCount, history, printerNames, language, isEmpty)
    }

    /**
     * saveAll
     * LoadedData 객체에 담긴 모든 데이터를 DataStore에 일괄 저장합니다.
     * @param context 애플리케이션 컨텍스트
     * @param data 저장할 데이터 객체
     */
    suspend fun saveAll(context: Context, data: LoadedData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MENU_ITEMS] = gson.toJson(data.menus)
            prefs[Keys.TABLE_COUNT] = data.tableCount.toString()
            prefs[Keys.ORDER_HISTORY] = gson.toJson(data.history)
            prefs[Keys.LANGUAGE] = data.language
            prefs[Keys.SELECTED_PRINTERS] = gson.toJson(data.printerNames)
        }
    }

    /**
     * clearAll
     * 저장된 모든 데이터를 삭제하고 초기화합니다.
     * @param context 애플리케이션 컨텍스트
     */
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
