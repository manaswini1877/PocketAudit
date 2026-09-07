package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType

class DetectionEngine(
    private val detectors: List<ScamDetector> = listOf(
        CollectRequestDetector(),
        PhishingLinkDetector(),
        SubscriptionTrapDetector()
    )
) {

    fun analyze(
        packageName: String,
        title: String,
        body: String,
        contextHistory: List<TransactionContext> = emptyList()
    ): DetectionResult {
        if (title.isBlank() && body.isBlank()) {
            logDebug("DetectionEngine", "⚠️ Skipping analysis: empty title and body for pkg='$packageName'")
            return DetectionResult.safe()
        }

        logDebug("DetectionEngine", "🔬 Analyzing payload with ${detectors.size} detectors for pkg='$packageName'")

        val results = detectors.map { detector ->
            logDebug("DetectionEngine", "  -> Running '${detector.detectorName}'...")
            val result = detector.detect(packageName, title, body, contextHistory)
            logDebug("DetectionEngine", "  <- '${detector.detectorName}' returned score=${result.riskScore}%")
            result
        }

        // Find detector result with maximum risk score
        val highestRiskResult = results.maxByOrNull { it.riskScore } ?: return DetectionResult.safe()
        logDebug("DetectionEngine", "🏁 Detection finished for pkg='$packageName': maxRiskScore=${highestRiskResult.riskScore}%")

        // Threshold tuning: scores under 50 are safe / unflagged
        if (highestRiskResult.riskScore < 50) {
            return DetectionResult.safe()
        }

        // Aggregate patterns across all triggered detectors
        val allPatterns = results.flatMap { it.matchedPatterns }.distinct()

        // Clamp combined risk score strictly to 0-100 range
        val clampedScore = highestRiskResult.riskScore.coerceIn(0, 100)

        // Risk Level classification tuning
        val riskLevel = if (clampedScore >= 75) RiskLevel.HIGH else RiskLevel.MEDIUM

        return DetectionResult(
            riskScore = clampedScore,
            riskLevel = riskLevel,
            scamType = highestRiskResult.scamType,
            matchedPatterns = allPatterns,
            explanation = highestRiskResult.explanation
        )
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (t: Throwable) {
            println("[$tag] $msg")
        }
    }
}
