package com.secure.codereviewer.data.api

data class AnalyzeRequest(
    val code: String
)

data class AnalyzeResponse(
    val input_code: String,
    val commented_code: String,
    val explanation: String
)

data class HistoryEntry(
    val id: Int,
    val input_code: String,
    val commented_code: String,
    val explanation: String,
    val source: String?,
    val created_at: String
)

data class HistoryListResponse(
    val items: List<HistoryEntry>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
