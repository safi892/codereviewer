package com.secure.codereviewer.data.api

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
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

    fun saveAuthData(token: String, user: AuthUser) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }

    fun getAuthHeader(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
}







