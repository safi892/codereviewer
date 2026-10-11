package com.secure.codereviewer.data.api

import com.google.gson.annotations.SerializedName

/**
 * Request sent to POST /analyze.
 * - [code]: C++ code to analyze.
 * - [output_language]: "english" or "roman_urdu".
 * - [source]: Client tag, e.g. "android".
 */
data class AnalyzeRequest(
    @SerializedName("code")
    val code: String,
    @SerializedName("output_language")
    val output_language: String = "english",
    @SerializedName("source")
    val source: String = "android"
)

/**
 * Response returned from POST /analyze.
 */
data class AnalyzeResponse(
    @SerializedName("input_code")
    val input_code: String,
    @SerializedName("commented_code")
    val commented_code: String,
    @SerializedName("explanation")
    val explanation: String,
    @SerializedName("needs_review")
    val needs_review: Boolean = false
)

/**
 * Request sent to POST /validate-code.
 * Lightweight syntax check without calling the AI model or database.
 */
data class CodeValidationRequest(
    @SerializedName("code")
    val code: String
)

/**
 * Single line edit proposed inside [SuggestedFix].
 */
data class CodeFixEdit(
    @SerializedName("line")
    val line: Int,
    @SerializedName("before")
    val before: String,
    @SerializedName("after")
    val after: String
)

/**
 * Candidate punctuation/syntax auto-fix (e.g. missing semicolon or closing bracket).
 */
data class SuggestedFix(
    @SerializedName("code")
    val code: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("edits")
    val edits: List<CodeFixEdit> = emptyList()
)

/**
 * Response returned from POST /validate-code.
 */
data class CodeValidationResponse(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("line")
    val line: Int? = null,
    @SerializedName("suggested_fix")
    val suggested_fix: SuggestedFix? = null
)

/**
 * Request sent to POST /optimize.
 * Asks the backend to convert recursion into an iterative loop.
 */
data class OptimizeRequest(
    @SerializedName("code")
    val code: String,
    @SerializedName("source")
    val source: String = "android",
    @SerializedName("mode")
    val mode: String = "loop"
)

/**
 * Response returned from POST /optimize.
 * Contains the rewritten loop code and verification status.
 */
data class OptimizeResponse(
    @SerializedName("input_code")
    val input_code: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("changed")
    val changed: Boolean,
    @SerializedName("verified")
    val verified: Boolean,
    @SerializedName("speedup")
    val speedup: Double = 0.0,
    @SerializedName("note")
    val note: String = ""
)

/**
 * Individual analysis history entry from the database.
 */
data class HistoryEntry(
    @SerializedName("id")
    val id: Int,
    @SerializedName("input_code")
    val input_code: String,
    @SerializedName("commented_code")
    val commented_code: String,
    @SerializedName("explanation")
    val explanation: String,
    @SerializedName("source")
    val source: String?,
    @SerializedName("created_at")
    val created_at: String
)

/**
 * Paginated list response from GET /analyze/history.
 */
data class HistoryListResponse(
    @SerializedName("items")
    val items: List<HistoryEntry>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("offset")
    val offset: Int
)
