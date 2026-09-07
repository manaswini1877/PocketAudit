package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import java.util.regex.Pattern

class PhishingLinkDetector : ScamDetector {

    override val detectorName: String = "Banking Phishing Link Detector"

    // Regex to match URLs
    private val urlPattern = Pattern.compile(
        "(https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?|https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?:/[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    // Known URL shortener domains
    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "is.gd", "cutt.ly", "t.co", "rb.gy",
        "shorturl.at", "d.link", "goo.gl", "ow.ly", "buff.ly", "t.ly"
    )

    // High risk suspicious TLDs frequently abused for phishing
    private val suspiciousTlds = setOf(
        "xyz", "top", "info", "site", "link", "online", "tech",
        "club", "work", "click", "website", "shop", "cfd", "rest", "cc", "tk", "ml", "ga"
    )

    // Banking brand keywords paired with phishing intent
    private val bankBrands = listOf(
        "sbi", "hdfc", "icici", "axis", "kotak", "pnb", "bob", "bank", "rbi", "income.tax", "pan"
    )

    // Urgency and panic triggers
    private val urgencyKeywords = listOf(
        "account blocked", "account suspended", "deactivated", "kyc pending", "kyc update",
        "verify now", "24 hours", "24 hrs", "pan card expired", "pan linked",
        "immediate action", "avoid penalty", "security breach", "unauthorized transaction"
    )

    override fun detect(
        packageName: String,
        title: String,
        body: String,
        contextHistory: List<TransactionContext>
    ): DetectionResult {
        val content = "$title $body".lowercase()
        val matchedPatterns = mutableListOf<String>()
        var score = 0

        val matcher = urlPattern.matcher(body)
        var foundUrl = false
        var extractedUrl = ""

        while (matcher.find()) {
            val url = matcher.group(1) ?: continue
            foundUrl = true
            extractedUrl = url
            val lowerUrl = url.lowercase()

            // Check IP address based URL
            if (url.matches(Regex("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*"))) {
                score += 45
                matchedPatterns.add("Raw IP address URL detected ($url)")
            }

            // Check URL Shortener
            val domain = extractDomain(lowerUrl)
            if (urlShorteners.contains(domain)) {
                score += 40
                matchedPatterns.add("Shortened URL detected ('$domain') hiding final destination")
            }

            // Check Suspicious TLD
            val tld = domain.substringAfterLast('.', "")
            if (suspiciousTlds.contains(tld)) {
                score += 35
                matchedPatterns.add("High-risk phishing domain TLD ('.$tld') in '$domain'")
            }

            // Check spoofed banking domains (e.g., sbi-verify-kyc.xyz)
            if (bankBrands.any { brand -> domain.contains(brand) } && !isOfficialDomain(domain)) {
                score += 40
                matchedPatterns.add("Spoofed banking domain name ('$domain') pretending to be an official bank")
            }
        }

        // Urgency check
        val matchedUrgency = urgencyKeywords.filter { content.contains(it) }
        if (matchedUrgency.isNotEmpty()) {
            if (foundUrl) {
                score += 35
                matchedPatterns.add("Urgency/Panic trigger words paired with unverified link: ${matchedUrgency.joinToString(", ") { "'$it'" }}")
            } else {
                score += 15
            }
        }

        // If no link was found or score is under 50, it is safe
        if (!foundUrl || score < 50) {
            return DetectionResult.safe()
        }

        val riskLevel = if (score >= 70) RiskLevel.HIGH else RiskLevel.MEDIUM

        val explanation = buildString {
            append("⚠️ Phishing Security Risk: ")
            append("This notification contains suspicious link patterns disguised as bank communication. ")
            if (foundUrl) {
                append("The embedded web link ($extractedUrl) does NOT lead to an official bank domain. ")
            }
            if (matchedUrgency.isNotEmpty()) {
                append("Scammers use urgent threats like 'account blocked' or 'KYC update required within 24 hours' to cause panic. Official banks NEVER demand urgent KYC credential updates via SMS links.")
            }
        }

        return DetectionResult(
            riskScore = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            scamType = ScamType.PHISHING_LINK,
            matchedPatterns = matchedPatterns,
            explanation = explanation.trim()
        )
    }

    private fun extractDomain(url: String): String {
        val clean = url.removePrefix("http://").removePrefix("https://").substringBefore('/')
        return clean.split(':').first()
    }

    private fun isOfficialDomain(domain: String): Boolean {
        val official = setOf(
            "onlinesbi.sbi", "sbi.co.in", "hdfcbank.com", "icicibank.com",
            "axisbank.com", "kotak.com", "pnbindia.in", "bankofbaroda.in",
            "npci.org.in", "rbi.org.in", "incometax.gov.in"
        )
        return official.any { domain == it || domain.endsWith(".$it") }
    }
}
