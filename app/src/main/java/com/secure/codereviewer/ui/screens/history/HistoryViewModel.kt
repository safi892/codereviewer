package com.secure.codereviewer.ui.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.codereviewer.data.api.HistoryEntry
import com.secure.codereviewer.data.repository.CodeRepository
import kotlinx.coroutines.launch
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

    fun loadInitial() {
        if (items.isNotEmpty() || isLoading) return
        fetch(reset = true)
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
        isLoading = true
        errorMessage = null
        if (reset) {
            offset = 0
            hasMore = true
        }

        viewModelScope.launch {
            try {
                val response = repository.getHistory(limit = limit, offset = offset)
                val newItems = response.items.map(::toUiItem)
                items = if (reset) newItems else items + newItems
                offset += newItems.size
                hasMore = items.size < response.total
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load history"
            } finally {
                isLoading = false
            }
        }
    }

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
            ?.replace("\t", " ")
            ?.replace(Regex("\\s+"), " ")

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
}
