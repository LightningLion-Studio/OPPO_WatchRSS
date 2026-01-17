package com.lightningstudio.watchrss.ui.util

import org.jsoup.Jsoup

private const val DEFAULT_MAX_CHARS = 60

fun formatRssSummary(raw: String?, maxChars: Int = DEFAULT_MAX_CHARS): String? {
    if (raw.isNullOrBlank()) return null
    val doc = Jsoup.parseBodyFragment(raw)
    doc.outputSettings().prettyPrint(false)
    doc.select("script,style").remove()
    val cleaned = doc.text().replace(Regex("\\s+"), " ").trim()
    if (cleaned.isEmpty()) return null
    if (cleaned.length <= maxChars) return cleaned
    val trimmed = cleaned.take(maxChars).trimEnd()
    return "$trimmed..."
}
