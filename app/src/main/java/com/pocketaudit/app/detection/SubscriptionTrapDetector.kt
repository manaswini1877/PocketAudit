package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import java.util.regex.Pattern
import kotlin.math.abs

class SubscriptionTrapDetector : ScamDetector {

    override val detectorName: String = "Subscription & Duplicate Charge Detector"

    private val amountRegex = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)

    private val subscriptionKeywords = listOf(
        "autopay executed", "recurring mandate debited", "mandate debited",
        "auto-renewed", "auto renew", "subscription debited",
        "standing instruction debited", "automatic payment debited"
    )

    private val suspiciousMerchantKeywords = listOf(
        "unknown merchant", "global services", "digital charge", "online media inc",
        "sub_charge", "trial_pay", "app_service_fee", "international merchant"
    )

    private val verifiedMerchants = listOf(
        "netflix", "spotify", "amazon prime", "youtube", "google", "apple",
        "hotstar", "jio", "airtel", "electricity", "water bill", "gas bill"
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

        // If transaction is from verified well-known subscription/utility service, return safe
        if (verifiedMerchants.any { content.contains(it) }) {
            return DetectionResult.safe()
        }

        // Extract amount if present
        val currentAmount = extractAmount(content)

        // Check for explicit autopay debit keywords
        val isSubscription = subscriptionKeywords.any { content.contains(it) }
        if (isSubscription) {
            score += 35
            matchedPatterns.add("Autopay / recurring mandate debit triggered")
        }

        // Check for duplicate charge in recent history (within 5 minutes / 300,000 ms)
        val now = System.currentTimeMillis()
        if (currentAmount != null && currentAmount > 0) {
            val duplicate = contextHistory.find { context ->
                context.amount != null &&
                abs(context.amount - currentAmount) < 0.01 &&
                (now - context.timestamp) <= 300_000 // 5 minutes window
            }

            if (duplicate != null) {
                val secondsAgo = (now - duplicate.timestamp) / 1000
                score += 55
                matchedPatterns.add("Duplicate charge detected: ₹${currentAmount} charged again within ${secondsAgo}s")
            }
        }

        // Check for unfamiliar / suspicious merchant names
        val matchedSuspiciousMerchant = suspiciousMerchantKeywords.find { content.contains(it) }
        if (matchedSuspiciousMerchant != null) {
            score += 30
            matchedPatterns.add("Unfamiliar / vague merchant descriptor ('$matchedSuspiciousMerchant')")
        }

        if (score < 50) {
            return DetectionResult.safe()
        }

        val riskLevel = if (score >= 70) RiskLevel.HIGH else RiskLevel.MEDIUM

        val explanation = buildString {
            append("⚠️ Silent Subscription / Duplicate Charge Warning: ")
            append("This notification indicates an automatic mandate debit or potential duplicate charge. ")
            if (matchedPatterns.any { it.contains("Duplicate charge") }) {
                append("Your bank account was debited the exact same amount twice within 5 minutes. Contact your bank to reverse duplicate charges. ")
            }
            if (isSubscription) {
                append("Silent subscription mandates often continue auto-billing users after hidden free trials. Review your active UPI mandates in your payment app.")
            }
        }

        return DetectionResult(
            riskScore = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            scamType = ScamType.SUBSCRIPTION_TRAP,
            matchedPatterns = matchedPatterns,
            explanation = explanation.trim()
        )
    }

    private fun extractAmount(text: String): Double? {
        val matcher = amountRegex.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }
}
