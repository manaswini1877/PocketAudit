package com.pocketaudit.app.detection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RulesRiskAnalyzer(
    private val engine: DetectionEngine = DetectionEngine()
) : RiskAnalyzer {

    override suspend fun analyze(input: RiskInput): RiskResult = withContext(Dispatchers.Default) {
        val pkg = input.packageName.orEmpty()
        val title = input.displayTitle.ifBlank { input.text.lineSequence().firstOrNull().orEmpty().take(120) }
        val body = if (input.text.isNotBlank()) {
            input.text
        } else if (input.displayBody.isNotBlank()) {
            input.displayBody
        } else {
            ""
        }
        val history = if (input.contextHistory.isNotEmpty()) {
            input.contextHistory
        } else {
            emptyList()
        }
        engine.analyze(pkg, title, body, history).toRiskResult(decidedBy = DecidedBy.RULES)
    }
}
