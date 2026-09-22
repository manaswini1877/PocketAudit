package com.pocketaudit.app.data.repository

import com.pocketaudit.app.data.local.AlertDao
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.detection.ContentExtractor
import com.pocketaudit.app.detection.QrRiskRules
import com.pocketaudit.app.detection.RiskAnalyzer
import com.pocketaudit.app.detection.RiskInput
import com.pocketaudit.app.detection.RiskResult
import com.pocketaudit.app.detection.RiskSource
import com.pocketaudit.app.detection.RulesRiskAnalyzer
import com.pocketaudit.app.detection.TransactionContext
import com.pocketaudit.app.service.NotificationFilter
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

class AlertRepository(
    private val alertDao: AlertDao,
    private val riskAnalyzer: RiskAnalyzer = RulesRiskAnalyzer()
) {

    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlerts()

    fun getAlertById(id: Long): Flow<AlertEntity?> = alertDao.getAlertById(id)

    suspend fun analyzeAndPersist(input: RiskInput): Pair<RiskResult, AlertEntity> {
        val recentAlerts = alertDao.getRecentAlertsDirect()
        val contextHistory = if (input.contextHistory.isNotEmpty()) {
            input.contextHistory
        } else {
            recentAlerts.mapNotNull { alert ->
                val amt = extractAmount(alert.body)
                if (amt != null) {
                    TransactionContext(amount = amt, merchant = alert.appName, timestamp = alert.timestamp)
                } else null
            }
        }

        val analysisText = ContentExtractor.textForAnalysis(input.text)
        val enrichedInput = input.copy(
            text = analysisText,
            contextHistory = contextHistory
        )

        val engineResult = riskAnalyzer.analyze(enrichedInput)
        val result = mergeOptionalQrSignals(engineResult, input)
        android.util.Log.i(
            "AlertRepository",
            "🔎 Risk result source=${input.source}: score=${result.score}% level=${result.level} decidedBy=${result.decidedBy}"
        )

        val (packageName, appName, title, body) = displayFieldsFor(input)

        val isSafe = (
            result.level == RiskLevel.SAFE ||
                result.level == RiskLevel.LOW
            )

        val alert = AlertEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            body = body,
            riskLevel = result.level,
            riskScore = result.score,
            scamType = result.scamType,
            matchedPatterns = result.reasons,
            explanation = result.summary,
            isSafe = isSafe,
            source = input.source,
            decidedBy = result.decidedBy
        )

        val insertedId = alertDao.insertAlert(alert)
        val savedAlert = alert.copy(id = insertedId)
        android.util.Log.i("AlertRepository", "✅ Persisted alert id=$insertedId source=${input.source}")

        return Pair(result, savedAlert)
    }

    suspend fun analyzeAndStoreNotification(
        packageName: String,
        title: String,
        body: String
    ): Pair<RiskResult, AlertEntity> {
        val combined = buildString {
            if (title.isNotBlank()) append(title)
            if (body.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append(body)
            }
        }
        val input = RiskInput(
            text = combined.ifBlank { body.ifBlank { title } },
            source = RiskSource.NOTIFICATION,
            packageName = packageName,
            displayTitle = title,
            displayBody = body
        )
        return analyzeAndPersist(input)
    }

    suspend fun processAndStoreNotification(
        packageName: String,
        title: String,
        body: String
    ): AlertEntity? {
        val (_, alert) = analyzeAndStoreNotification(packageName, title, body)
        return alert
    }

    suspend fun markAsSafe(id: Long) {
        alertDao.markAsSafe(id)
    }

    suspend fun deleteAlert(alert: AlertEntity) {
        alertDao.deleteAlert(alert)
    }

    suspend fun clearAllAlerts() {
        alertDao.clearAllAlerts()
    }

    private fun mergeOptionalQrSignals(engineResult: RiskResult, input: RiskInput): RiskResult {
        if (input.extraReasons.isEmpty() && input.extraScore <= 0) {
            return engineResult
        }
        val mergedReasons = (engineResult.reasons + input.extraReasons).distinct()
        val (mergedScore, mergedLevel) = QrRiskRules.mergeWithEngine(
            engineScore = engineResult.score,
            engineLevel = engineResult.level,
            qrScore = input.extraScore
        )
        val summary = when {
            input.extraReasons.isNotEmpty() && (engineResult.score < 30 || engineResult.summary.isBlank()) ->
                input.extraReasons.first()
            engineResult.summary.isNotBlank() -> engineResult.summary
            input.extraReasons.isNotEmpty() -> input.extraReasons.first()
            else -> engineResult.summary
        }
        val scamType = when {
            engineResult.scamType != com.pocketaudit.app.data.model.ScamType.LEGITIMATE -> engineResult.scamType
            input.extraReasons.any { it.contains("url", ignoreCase = true) || it.contains("link", ignoreCase = true) || it.contains("http", ignoreCase = true) } ->
                com.pocketaudit.app.data.model.ScamType.PHISHING_LINK
            input.extraReasons.isNotEmpty() ->
                com.pocketaudit.app.data.model.ScamType.FAKE_UPI_COLLECT
            else -> engineResult.scamType
        }
        return engineResult.copy(
            level = mergedLevel,
            score = mergedScore,
            reasons = mergedReasons,
            summary = summary,
            scamType = scamType
        )
    }

    private fun displayFieldsFor(input: RiskInput): DisplayFields {
        return when (input.source) {
            RiskSource.NOTIFICATION, RiskSource.SIMULATOR -> {
                val pkg = input.packageName.orEmpty()
                DisplayFields(
                    packageName = pkg,
                    appName = NotificationFilter.getAppNameForPackage(pkg),
                    title = input.displayTitle.ifBlank { "Notification" },
                    body = input.displayBody.ifBlank { input.text }
                )
            }
            RiskSource.SHARED_TEXT -> DisplayFields(
                packageName = "",
                appName = "Shared text",
                title = "Shared message",
                body = input.text
            )
            RiskSource.SHARED_IMAGE -> DisplayFields(
                packageName = "",
                appName = "Shared image",
                title = "Screenshot / image",
                body = input.text
            )
            RiskSource.CLIPBOARD -> DisplayFields(
                packageName = "",
                appName = "Clipboard",
                title = "Pasted content",
                body = input.text
            )
            RiskSource.MANUAL -> DisplayFields(
                packageName = "",
                appName = "Manual check",
                title = "Checked message",
                body = input.text
            )
            RiskSource.QR_SCAN -> DisplayFields(
                packageName = "",
                appName = "QR scan",
                title = input.displayTitle.ifBlank { "QR code" },
                body = input.displayBody.ifBlank { input.text }
            )
        }
    }

    private fun extractAmount(text: String): Double? {
        val pattern = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }

    private data class DisplayFields(
        val packageName: String,
        val appName: String,
        val title: String,
        val body: String
    )
}
