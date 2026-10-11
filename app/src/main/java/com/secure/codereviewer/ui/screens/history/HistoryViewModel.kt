package com.secure.codereviewer.ui.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.codereviewer.data.api.HistoryEntry
import com.secure.codereviewer.data.repository.CodeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime


data class HistoryUiItem(
    val id: Int,
    val title: String,
    val createdAtEpochMs: Long,
    val source: String?,
    val inputCode: String,
    val commentedCode: String,
    val explanation: String
)

class HistoryViewModel : ViewModel() {
    private val repository = CodeRepository()

    var items by mutableStateOf<List<HistoryUiItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set

    private var offset = 0
    private val limit = 20
    private var fetchJob: Job? = null

    fun loadInitial() {
        if (items.isNotEmpty() || isLoading) return
        isLoading = true
        viewModelScope.launch {
            // Show cache instantly (file read + parse happen on IO / Default).
            repository.getLocalHistory()?.let { cached ->
                items = cached.items.toUiItems()
                hasMore = false
            }
            fetch(reset = true)
        }
    }

    fun refresh() {
        if (isLoading) return
        fetch(reset = true)
    }

    fun loadMore() {
        if (isLoading || !hasMore) return
        fetch(reset = false)
    }

    private fun fetch(reset: Boolean) {
        if (reset) fetchJob?.cancel()
        isLoading = true
        errorMessage = null
        if (reset) {
            offset = 0
            hasMore = true
        }

        fetchJob = viewModelScope.launch {
            try {
                val response = repository.getHistory(limit = limit, offset = offset)
                val newItems = response.items.toUiItems()
                val merged = if (reset) newItems else items + newItems
                // Server-side inserts can shift pages; keep ids unique (required by LazyColumn keys).
                items = merged.distinctBy { it.id }
                offset += response.items.size
                hasMore = response.items.isNotEmpty() && offset < response.total
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (items.isEmpty()) {
                    val localResponse = repository.getLocalHistory()
                    if (localResponse != null) {
                        items = localResponse.items.toUiItems()
                        hasMore = false
                        errorMessage = "Showing cached history (offline)"
                    } else {
                        errorMessage = e.message ?: "Failed to load history"
                    }
                } else {
                    errorMessage = e.message
                }
            } finally {
                isLoading = false
            }
        }
    }

    /** Regex + date parsing for each entry is CPU work: keep it off the main thread. */
    private suspend fun List<HistoryEntry>.toUiItems(): List<HistoryUiItem> =
        withContext(Dispatchers.Default) { map(::toUiItem) }

    private fun toUiItem(entry: HistoryEntry): HistoryUiItem {
        return HistoryUiItem(
            id = entry.id,
            title = deriveTitle(entry),
            createdAtEpochMs = parseEpochMs(entry.created_at),
            source = entry.source,
            inputCode = entry.input_code,
            commentedCode = entry.commented_code,
            explanation = entry.explanation
        )
    }

    private fun deriveTitle(entry: HistoryEntry): String {
        val firstLine = entry.input_code
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.replace(WHITESPACE, " ")

        if (firstLine.isNullOrBlank()) {
            return "Analysis #${entry.id}"
        }

        return if (firstLine.length > 48) {
            firstLine.take(48).trimEnd() + "..."
        } else {
            firstLine
        }
    }

    private fun parseEpochMs(createdAt: String): Long {
        return try {
            OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private companion object {
        // Compiled once instead of once per history item.
        val WHITESPACE = Regex("\\s+")
    }
}
