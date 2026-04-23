package com.secure.codereviewer.data.api

data class AnalyzeRequest(
    val code: String
)

data class AnalyzeResponse(
    val input_code: String,
    val commented_code: String,
    val explanation: String
)
