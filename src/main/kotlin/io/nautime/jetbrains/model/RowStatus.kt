package io.nautime.jetbrains.model

data class RowStatus(
    val lineCount: Int,
    val lineNumber: Int,
    val cursorPosition: Int,
)
