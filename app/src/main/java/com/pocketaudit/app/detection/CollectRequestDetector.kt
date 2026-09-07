package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import java.util.regex.Pattern

class CollectRequestDetector : ScamDetector {

    override val detectorName: String = "Fake UPI Collect Detector"

    // VPA pattern matcher (e.g., user@bank or fakehandle.service@ybl)
    private val vpaRegex = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+)", Pattern.CASE_INSENSITIVE)

    // Explicit collect request scam triggers
    private val explicitCollectPhrases = listOf(
        "collect request", "requested money from", "requesting money",
        "upi collect", "payment request of", "request from",
        "pay to receive", "enter pin to receive", "enter pin to claim",
        "enter upi pin to receive", "enter upi pin to get"
    )

    // Keywords indicating collect requests disguised as payouts
    private val rewardDisguiseKeywords = listOf(
        "refund", "cashback", "claim", "prize", "reward", "lottery", "scratch card", "bonus"
    )

    // Suspicious VPA prefixes scammers commonly craft
    private val suspiciousVpaPrefixes = listOf(
        "cashback", "refund", "claim", "prize", "support", "helpdesk",
        "govt", "official", "reward", "customer.care", "verify"
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

        // Strict collect term check - avoids false positives on generic "request" (e.g., "Friend request")
        val hasExplicitCollectPhrase = explicitCollectPhrases.any { content.contains(it) }
        val hasCollectWordWithPayment = content.contains("collect") && (content.contains("₹") || content.contains("rs") || content.contains("inr") || content.contains("upi") || content.contains("vpa"))
        val hasRewardDisguise = rewardDisguiseKeywords.any { content.contains(it) }

        if (hasExplicitCollectPhrase && hasRewardDisguise) {
            score += 55
            matchedPatterns.add("Collect request disguised as refund/cashback/claim")
        } else if (hasExplicitCollectPhrase || hasCollectWordWithPayment) {
            score += 35
            matchedPatterns.add("Incoming payment collect request detected")
        }

        // Check for PIN trap phrase ("Enter PIN to receive")
        val hasPin = content.contains("pin")
        val hasReceive = content.contains("receive") || content.contains("get") || content.contains("claim") || content.contains("credit")
        if (hasPin && hasReceive && (hasExplicitCollectPhrase || hasCollectWordWithPayment || hasRewardDisguise)) {
            score += 35
            matchedPatterns.add("PIN Trap: Claims entering UPI PIN will receive money (Entering PIN ALWAYS debits money!)")
        }

        // VPA inspection
        val vpaMatcher = vpaRegex.matcher(body)
        var suspiciousVpaFound = false
        var extractedVpa = ""

        while (vpaMatcher.find()) {
            val vpa = vpaMatcher.group(1) ?: continue
            extractedVpa = vpa
            val handlePrefix = vpa.split("@").firstOrNull()?.lowercase() ?: ""
            if (suspiciousVpaPrefixes.any { handlePrefix.contains(it) }) {
                suspiciousVpaFound = true
                matchedPatterns.add("Suspicious VPA sender handle: '$vpa'")
                score += 25
                break
            }
        }

        if (score < 50) {
            return DetectionResult.safe()
        }

        val riskLevel = if (score >= 75) RiskLevel.HIGH else RiskLevel.MEDIUM

        val explanation = buildString {
            append("⚠️ Fake UPI Collect Scam Warning: ")
            append("An incoming UPI payment collect request was sent to your phone asking you to authorize a transaction. ")
            if (matchedPatterns.any { it.contains("PIN Trap") }) {
                append("Scammers disguise collect requests as 'cashback' or 'refunds' to trick you into entering your UPI PIN. REMEMBER: You NEVER enter your UPI PIN to RECEIVE money — entering your PIN will instantly DEBIT money from your bank account! ")
            } else {
                append("Approving this request will immediately transfer money OUT of your account to the requester. ")
            }
            if (suspiciousVpaFound) {
                append("The requester VPA ($extractedVpa) uses suspicious domain/support keywords commonly spoofed by fraudsters.")
            }
        }

        return DetectionResult(
            riskScore = score.coerceIn(0, 100),
            riskLevel = riskLevel,
            scamType = ScamType.FAKE_UPI_COLLECT,
            matchedPatterns = matchedPatterns,
            explanation = explanation.trim()
        )
    }
}
