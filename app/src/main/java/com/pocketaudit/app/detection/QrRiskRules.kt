package com.pocketaudit.app.detection

import java.net.IDN
import java.util.regex.Pattern
import kotlin.math.max

data class QrRiskSignal(
    val id: String,
    val reason: String,
    val score: Int
)

data class QrRiskAssessment(
    val signals: List<QrRiskSignal>,
    val score: Int
) {
    val reasons: List<String> get() = signals.map { it.reason }
}

object QrRiskRules {

    const val HIGH_AMOUNT_THRESHOLD = 20_000.0

    private val urlPattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>"']+)"""
    )

    private val ipv4Pattern = Pattern.compile(
        """^(?:\d{1,3}\.){3}\d{1,3}$"""
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "is.gd", "cutt.ly", "t.co", "rb.gy",
        "shorturl.at", "goo.gl", "ow.ly", "buff.ly", "t.ly", "tiny.cc"
    )

    private val suspiciousTlds = setOf(
        "xyz", "top", "click", "gq", "tk", "ml", "cf", "ga", "rest",
        "country", "zip", "mov", "cfd", "link", "work", "icu"
    )

    private val urgencyLotteryWords = listOf(
        "urgent", "immediately", "hurry", "last chance", "24 hours", "24 hrs",
        "lottery", "jackpot", "prize", "winner", "refund", "cashback", "reward",
        "claim now", "kyc"
    )

    fun evaluate(rawQr: String, parsed: UpiPayUri = UpiUriParser.parse(rawQr)): QrRiskAssessment {
        val signals = mutableListOf<QrRiskSignal>()

        if (parsed.isUpiPay || !parsed.pa.isNullOrBlank()) {
            evaluateUpi(parsed, signals)
        } else {
            evaluateNonUpi(rawQr, signals)
        }

        val score = signals.sumOf { it.score }.coerceIn(0, 100)
        return QrRiskAssessment(signals = signals, score = score)
    }

    private fun evaluateUpi(parsed: UpiPayUri, signals: MutableList<QrRiskSignal>) {
        val amount = parsed.amountOrNull()
        if (amount != null && amount >= HIGH_AMOUNT_THRESHOLD) {
            signals += QrRiskSignal(
                id = "high_amount",
                reason = "Pre-filled amount ₹${formatAmount(amount)} is unusually high. Confirm with the merchant before paying.",
                score = 20
            )
        }

        val note = parsed.tn.orEmpty().lowercase()
        if (note.isNotBlank() && urgencyLotteryWords.any { note.contains(it) }) {
            signals += QrRiskSignal(
                id = "suspicious_note",
                reason = "Payment note contains urgency, lottery, refund, or reward wording.",
                score = 30
            )
        }

        val vpa = parsed.pa.orEmpty()
        if (vpa.contains('@') && looksRandomVpa(vpa)) {
            signals += QrRiskSignal(
                id = "random_vpa",
                reason = "Payee VPA '$vpa' looks randomly generated.",
                score = 20
            )
        }

        val name = parsed.pn.orEmpty()
        if (name.isNotBlank() && vpa.contains('@') && !nameResemblesVpa(name, vpa)) {
            signals += QrRiskSignal(
                id = "name_vpa_mismatch",
                reason = "Payee name does not resemble the VPA handle.",
                score = 20
            )
        }

        val embeddedUrl = parsed.url.orEmpty()
        if (embeddedUrl.isNotBlank()) {
            signals += QrRiskSignal(
                id = "upi_url_param",
                reason = "UPI QR includes a url parameter ($embeddedUrl). Treat this as a risk signal, not proof of fraud.",
                score = 25
            )
            evaluateUrl(embeddedUrl, signals, prefix = "upi_url_")
        }
    }

    private fun evaluateNonUpi(rawQr: String, signals: MutableList<QrRiskSignal>) {
        val urls = extractUrls(rawQr)
        if (urls.isEmpty()) return
        urls.forEach { evaluateUrl(it, signals, prefix = "") }
    }

    private fun evaluateUrl(url: String, signals: MutableList<QrRiskSignal>, prefix: String) {
        val normalized = if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
        val host = extractHost(normalized) ?: return

        if (normalized.startsWith("http://", ignoreCase = true)) {
            addOnce(signals, prefix + "insecure_http") {
                QrRiskSignal(
                    id = prefix + "insecure_http",
                    reason = "QR contains an http (not https) link: $normalized",
                    score = 25
                )
            }
        }

        val hostLower = host.lowercase()
        if (urlShorteners.any { hostLower == it || hostLower.endsWith(".$it") }) {
            addOnce(signals, prefix + "shortener") {
                QrRiskSignal(
                    id = prefix + "shortener",
                    reason = "QR link uses a URL shortener ($host).",
                    score = 35
                )
            }
        }

        if (ipv4Pattern.matcher(host).matches()) {
            addOnce(signals, prefix + "ip_host") {
                QrRiskSignal(
                    id = prefix + "ip_host",
                    reason = "QR link host is an IP address ($host).",
                    score = 40
                )
            }
        }

        if (hostLower.contains("xn--") || containsLookalikeCharacters(host)) {
            addOnce(signals, prefix + "lookalike") {
                QrRiskSignal(
                    id = prefix + "lookalike",
                    reason = "QR link host uses punycode or lookalike characters ($host).",
                    score = 30
                )
            }
        }

        val tld = hostLower.substringAfterLast('.', missingDelimiterValue = "")
        if (tld in suspiciousTlds) {
            addOnce(signals, prefix + "suspicious_tld") {
                QrRiskSignal(
                    id = prefix + "suspicious_tld",
                    reason = "QR link uses a frequently abused TLD (.$tld).",
                    score = 30
                )
            }
        }

        val encodedCount = Regex("%[0-9a-fA-F]{2}").findAll(normalized).count()
        if (normalized.length >= 120 || encodedCount >= 8) {
            addOnce(signals, prefix + "obfuscated") {
                QrRiskSignal(
                    id = prefix + "obfuscated",
                    reason = "QR link is unusually long or obfuscated.",
                    score = 20
                )
            }
        }
    }

    internal fun looksRandomVpa(vpa: String): Boolean {
        val handle = vpa.substringBefore('@').lowercase()
        if (handle.length < 10) return false
        if (handle.any { it == '.' || it == '-' || it == '_' }) return false
        val letters = handle.filter { it.isLetter() }
        if (letters.length < 8) return false
        val vowels = letters.count { it in "aeiou" }
        val vowelRatio = vowels.toDouble() / letters.length
        val uniqueRatio = letters.toSet().size.toDouble() / letters.length
        return vowelRatio < 0.22 && uniqueRatio > 0.55
    }

    internal fun nameResemblesVpa(name: String, vpa: String): Boolean {
        val handle = vpa.substringBefore('@').lowercase()
        val nameTokens = tokenize(name)
        val handleTokens = tokenize(handle.replace('.', ' ').replace('-', ' ').replace('_', ' '))
        if (nameTokens.isEmpty() || handleTokens.isEmpty()) return false
        val compactName = nameTokens.joinToString("")
        val compactHandle = handle.filter { it.isLetterOrDigit() }
        if (compactHandle.length >= 4 && compactName.contains(compactHandle)) return true
        if (compactName.length >= 4 && compactHandle.contains(compactName)) return true
        return nameTokens.any { token ->
            token.length >= 3 && handleTokens.any { handleToken ->
                handleToken.contains(token) || token.contains(handleToken)
            }
        }
    }

    private fun tokenize(value: String): List<String> {
        return value.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in setOf("the", "and", "pvt", "ltd", "inc") }
    }

    private fun extractUrls(text: String): List<String> {
        val matcher = urlPattern.matcher(text)
        val urls = linkedSetOf<String>()
        while (matcher.find()) {
            val url = matcher.group(1)?.trimEnd('.', ',', ';', ')') ?: continue
            urls.add(url)
        }
        return urls.toList()
    }

    private fun extractHost(url: String): String? {
        val withoutScheme = url.substringAfter("://", url)
        val hostPort = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = hostPort.substringBefore(':').trim().lowercase()
        if (host.isBlank()) return null
        return try {
            IDN.toASCII(host)
        } catch (_: Exception) {
            host
        }
    }

    private fun containsLookalikeCharacters(host: String): Boolean {
        val letters = host.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        val scripts = letters.map { Character.UnicodeScript.of(it.code) }.toSet()
        if (scripts.size > 1) return true
        return host.any { ch ->
            ch.code > 127 && Character.UnicodeScript.of(ch.code) != Character.UnicodeScript.LATIN
        }
    }

    private fun addOnce(
        signals: MutableList<QrRiskSignal>,
        id: String,
        builder: () -> QrRiskSignal
    ) {
        if (signals.none { it.id == id }) {
            signals += builder()
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
    }

    fun mergeWithEngine(engineScore: Int, engineLevel: com.pocketaudit.app.data.model.RiskLevel, qrScore: Int): Pair<Int, com.pocketaudit.app.data.model.RiskLevel> {
        val score = max(engineScore, qrScore).coerceIn(0, 100)
        val level = when {
            score >= 70 -> com.pocketaudit.app.data.model.RiskLevel.HIGH
            score >= 30 -> com.pocketaudit.app.data.model.RiskLevel.MEDIUM
            score > 0 -> com.pocketaudit.app.data.model.RiskLevel.LOW
            else -> engineLevel
        }
        return score to level
    }
}
