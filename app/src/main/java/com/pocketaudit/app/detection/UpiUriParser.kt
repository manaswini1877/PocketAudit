package com.pocketaudit.app.detection

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class UpiPayUri(
    val raw: String,
    val isUpiPay: Boolean,
    val pa: String? = null,
    val pn: String? = null,
    val am: String? = null,
    val cu: String? = null,
    val tn: String? = null,
    val mc: String? = null,
    val tr: String? = null,
    val url: String? = null,
    val params: Map<String, String> = emptyMap()
) {
    fun amountOrNull(): Double? {
        val value = am?.replace(",", "")?.trim().orEmpty()
        if (value.isEmpty()) return null
        return value.toDoubleOrNull()
    }
}

object UpiUriParser {

    fun parse(raw: String): UpiPayUri {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return UpiPayUri(raw = raw, isUpiPay = false)
        }

        val schemeSeparator = trimmed.indexOf("://")
        val scheme = if (schemeSeparator > 0) {
            trimmed.substring(0, schemeSeparator)
        } else {
            trimmed.substringBefore(':', missingDelimiterValue = "")
        }

        if (!scheme.equals("upi", ignoreCase = true)) {
            return UpiPayUri(raw = trimmed, isUpiPay = false)
        }

        val afterScheme = if (schemeSeparator >= 0) {
            trimmed.substring(schemeSeparator + 3)
        } else {
            trimmed.substringAfter(':', "")
        }
        val pathAndQuery = afterScheme
        val queryIndex = pathAndQuery.indexOf('?')
        val path = if (queryIndex >= 0) pathAndQuery.substring(0, queryIndex) else pathAndQuery
        val query = if (queryIndex >= 0) pathAndQuery.substring(queryIndex + 1) else ""
        val isPay = path.equals("pay", ignoreCase = true)

        val params = parseQuery(query).toMutableMap()
        params["pn"]?.let {
            params["pn"] = it.replace('+', ' ').trim()
        }
        return UpiPayUri(
            raw = trimmed,
            isUpiPay = isPay,
            pa = params["pa"],
            pn = params["pn"],
            am = params["am"],
            cu = params["cu"],
            tn = params["tn"],
            mc = params["mc"],
            tr = params["tr"],
            url = params["url"],
            params = params
        )
    }

    internal fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val result = linkedMapOf<String, String>()
        query.split('&').forEach { pair ->
            if (pair.isBlank()) return@forEach
            val eq = pair.indexOf('=')
            val rawKey = if (eq >= 0) pair.substring(0, eq) else pair
            val rawValue = if (eq >= 0) pair.substring(eq + 1) else ""
            val key = decode(rawKey).trim().lowercase()
            if (key.isEmpty()) return@forEach
            val value = decode(rawValue).trim()
            if (value.isNotEmpty() && key !in result) {
                result[key] = value
            }
        }
        return result
    }

    private fun decode(value: String): String {
        if (value.isEmpty()) return value
        return try {
            URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            value
        }
    }
}
