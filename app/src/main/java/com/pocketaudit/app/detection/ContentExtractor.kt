package com.pocketaudit.app.detection

import java.util.regex.Pattern

object ContentExtractor {

    private val urlPattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>"']+)"""
    )

    private val vpaPattern = Pattern.compile(
        """(?i)\b([a-z0-9._-]{2,}@[a-z0-9.-]{2,})\b"""
    )

    private val upiIdPattern = Pattern.compile(
        """(?i)\b(upi id|vpa|payee vpa)[\s:=-]+([a-z0-9._-]+@[a-z0-9.-]+)\b"""
    )

    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val matcher = urlPattern.matcher(text)
        val urls = linkedSetOf<String>()
        while (matcher.find()) {
            var url = matcher.group(1)?.trimEnd('.', ',', ';', ')') ?: continue
            if (url.startsWith("www.", ignoreCase = true)) {
                url = "https://$url"
            }
            urls.add(url)
        }
        return urls.toList()
    }

    fun extractVpas(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val vpas = linkedSetOf<String>()

        val vpaMatcher = vpaPattern.matcher(text)
        while (vpaMatcher.find()) {
            val candidate = vpaMatcher.group(1) ?: continue
            if (isLikelyVpa(candidate)) {
                vpas.add(candidate.lowercase())
            }
        }

        val upiMatcher = upiIdPattern.matcher(text)
        while (upiMatcher.find()) {
            val candidate = upiMatcher.group(2) ?: continue
            if (isLikelyVpa(candidate)) {
                vpas.add(candidate.lowercase())
            }
        }

        return vpas.toList()
    }

    /**
     * Enriches analysis text with extracted URLs/VPAs when they appear outside the raw string
     * (e.g. share sheet trimming). Does not mutate user-visible display text.
     */
    fun textForAnalysis(rawText: String): String {
        if (rawText.isBlank()) return rawText
        val extras = buildList {
            extractUrls(rawText).forEach { add("URL: $it") }
            extractVpas(rawText).forEach { add("VPA: $it") }
        }
        if (extras.isEmpty()) return rawText
        return rawText + "\n" + extras.joinToString("\n")
    }

    private fun isLikelyVpa(candidate: String): Boolean {
        if (!candidate.contains('@')) return false
        val handle = candidate.substringAfter('@')
        if (handle.length < 2) return false
        // Filter obvious email domains that are not UPI handles
        val nonUpiDomains = setOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com")
        if (nonUpiDomains.any { handle.equals(it, ignoreCase = true) }) return false
        return true
    }
}
