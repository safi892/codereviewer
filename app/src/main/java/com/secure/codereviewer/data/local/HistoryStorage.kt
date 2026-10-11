package com.secure.codereviewer.data.local

import android.content.Context
import android.content.SharedPreferences
import com.secure.codereviewer.data.api.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

object HistoryStorage {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_HISTORY = "history_cache"
    private const val KEY_HISTORY_TOTAL = "history_total"
    private const val KEY_HISTORY_TIMESTAMP = "history_timestamp"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveHistoryCache(history: List<HistoryEntry>, total: Int) {
        val jsonArray = JSONArray()
        history.forEach { entry ->
            val json = JSONObject().apply {
                put("id", entry.id)
                put("input_code", entry.input_code)
                put("commented_code", entry.commented_code)
                put("explanation", entry.explanation)
                put("source", entry.source)
                put("created_at", entry.created_at)
            }
            jsonArray.put(json)
        }
        prefs.edit().apply {
            putString(KEY_HISTORY, jsonArray.toString())
            putInt(KEY_HISTORY_TOTAL, total)
            putLong(KEY_HISTORY_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun getHistoryCache(): Pair<List<HistoryEntry>, Int>? {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return null
        val total = prefs.getInt(KEY_HISTORY_TOTAL, 0)
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<HistoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        id = json.getInt("id"),
                        input_code = json.getString("input_code"),
                        commented_code = json.getString("commented_code"),
                        explanation = json.getString("explanation"),
                        source = json.optString("source").takeIf { it.isNotEmpty() },
                        created_at = json.getString("created_at")
                    )
                )
            }
            Pair(list, total)
        } catch (e: Exception) {
            null
        }
    }

    fun clearHistoryCache() {
        prefs.edit().apply {
            remove(KEY_HISTORY)
            remove(KEY_HISTORY_TOTAL)
            remove(KEY_HISTORY_TIMESTAMP)
            apply()
        }
    }
}