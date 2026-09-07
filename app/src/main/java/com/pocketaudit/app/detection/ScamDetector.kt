package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType

data class TransactionContext(
    val amount: Double?,
    val merchant: String?,
    val timestamp: Long
)

data class DetectionResult(
    val riskScore: Int, // 0 to 100
    val riskLevel: RiskLevel,
    val scamType: ScamType,
    val matchedPatterns: List<String>,
    val explanation: String
) {
    companion object {
        fun safe(): DetectionResult = DetectionResult(
            riskScore = 0,
            riskLevel = RiskLevel.SAFE,
            scamType = ScamType.LEGITIMATE,
            matchedPatterns = emptyList(),
            explanation = "No suspicious scam patterns detected in this notification."
        )
    }
}

interface ScamDetector {
    val detectorName: String
    fun detect(
        packageName: String,
        title: String,
        body: String,
        contextHistory: List<TransactionContext> = emptyList()
    ): DetectionResult
}
