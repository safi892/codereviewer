package com.secure.codereviewer.data.local

import android.content.Context
import com.google.gson.Gson
import com.secure.codereviewer.data.api.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline cache for the first page of history.
 *
 * Previously this was a large JSON string stored inside the *same* SharedPreferences
 * file as the auth token ("auth_prefs"). That meant:
 *  - every auth read on the main thread waited for the whole history blob to load,
 *  - every apply() rewrote the whole file, and Android waits for pending apply()
 *    writes on the main thread in onPause/onStop (jank / ANR risk),
 *  - JSON parsing ran on the main thread.
 *
 * Now it's a plain file in cacheDir, and every access is a suspend function on IO.
 */
object HistoryStorage {
    private const val FILE_NAME = "history_cache.json"
    private const val LEGACY_PREF_NAME = "auth_prefs"
    private val LEGACY_KEYS = listOf("history_cache", "history_total", "history_timestamp")

    private val gson = Gson()
    private lateinit var appContext: Context

    private data class CachePayload(
        val items: List<HistoryEntry>,
        val total: Int,
        val timestamp: Long
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val cacheFile: File get() = File(appContext.cacheDir, FILE_NAME)

    suspend fun saveHistoryCache(history: List<HistoryEntry>, total: Int) = withContext(Dispatchers.IO) {
        runCatching {
            val json = gson.toJson(CachePayload(history, total, System.currentTimeMillis()))
            // Write to a temp file and rename so a crash never leaves a half-written cache.
            val tmp = File(appContext.cacheDir, "$FILE_NAME.tmp")
            tmp.writeText(json)
            tmp.renameTo(cacheFile)
        }
        removeLegacyCache()
    }

    suspend fun getHistoryCache(): Pair<List<HistoryEntry>, Int>? = withContext(Dispatchers.IO) {
        runCatching {
            val file = cacheFile
            if (!file.exists()) return@runCatching null
            val payload = gson.fromJson(file.readText(), CachePayload::class.java)
            // Gson can produce null for non-null Kotlin fields on malformed data.
            @Suppress("SENSELESS_COMPARISON")
            if (payload?.items == null) null else payload.items to payload.total
        }.getOrNull()
    }

    suspend fun clearHistoryCache() = withContext(Dispatchers.IO) {
        runCatching { cacheFile.delete() }
        removeLegacyCache()
    }

    /** One-time cleanup of the old blob that lived in auth_prefs. */
    private fun removeLegacyCache() {
        val prefs = appContext.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
        if (LEGACY_KEYS.any(prefs::contains)) {
            prefs.edit().apply { LEGACY_KEYS.forEach(::remove) }.apply()
        }
    }
}
