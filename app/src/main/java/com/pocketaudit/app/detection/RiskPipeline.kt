package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType

enum class RiskSource {
    NOTIFICATION,
    QR_SCAN,
    SHARED_TEXT,
    SHARED_IMAGE,
    CLIPBOARD,
    SIMULATOR,
    MANUAL
}

enum class DecidedBy {
    RULES,
    ON_DEVICE_AI
}

data class RiskInput(
    val text: String,
    val source: RiskSource,
    val packageName: String? = null,
    val displayTitle: String = "",
    val displayBody: String = "",
    val contextHistory: List<TransactionContext> = emptyList(),
    val extraReasons: List<String> = emptyList(),
    val extraScore: Int = 0
)

data class RiskResult(
    val level: RiskLevel,
    val score: Int,
    val reasons: List<String>,
    val decidedBy: DecidedBy,
    val scamType: ScamType,
    val summary: String,
    val aiUnavailable: Boolean = false
)

interface RiskAnalyzer {
    suspend fun analyze(input: RiskInput): RiskResult
}

fun DetectionResult.toRiskResult(decidedBy: DecidedBy = DecidedBy.RULES): RiskResult = RiskResult(
    level = riskLevel,
    score = riskScore,
    reasons = matchedPatterns,
    decidedBy = decidedBy,
    scamType = scamType,
    summary = explanation,
    aiUnavailable = false
)
