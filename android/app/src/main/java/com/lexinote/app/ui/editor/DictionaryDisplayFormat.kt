package com.lexinote.app.ui.editor

internal fun formatDictionaryPhonetic(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return "音标暂无"
    var inner = value
    repeat(2) {
        if (inner.startsWith("/") && inner.endsWith("/") && inner.length >= 2) inner = inner.substring(1, inner.length - 1).trim()
        if (inner.startsWith("[") && inner.endsWith("]") && inner.length >= 2) inner = inner.substring(1, inner.length - 1).trim()
    }
    return if (inner.isEmpty()) "音标暂无" else "/$inner/"
}

private val posLabels = mapOf(
    "n" to "n.", "v" to "v.", "vt" to "vt.", "vi" to "vi.", "adj" to "adj.", "a" to "adj.",
    "adv" to "adv.", "pron" to "pron.", "prep" to "prep.", "conj" to "conj.", "det" to "det.",
    "num" to "num.", "int" to "int.", "interj" to "interj.", "art" to "art.", "aux" to "aux.",
    "abbr" to "abbr.", "unknown" to "unknown"
)

internal fun formatDictionaryPartOfSpeech(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return value
    val key = value.removeSuffix(".").lowercase()
    return posLabels[key] ?: if (value.endsWith(".")) value else "$value."
}
