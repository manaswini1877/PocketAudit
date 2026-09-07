package com.pocketaudit.app.data.repository

import com.pocketaudit.app.data.local.AlertDao
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.detection.DetectionEngine
import com.pocketaudit.app.detection.DetectionResult
import com.pocketaudit.app.detection.TransactionContext
import com.pocketaudit.app.service.NotificationFilter
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

class AlertRepository(
    private val alertDao: AlertDao,
    private val detectionEngine: DetectionEngine = DetectionEngine()
) {

    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlerts()

    fun getAlertById(id: Long): Flow<AlertEntity?> = alertDao.getAlertById(id)

    suspend fun analyzeAndStoreNotification(
        packageName: String,
        title: String,
        body: String
    ): Pair<DetectionResult, AlertEntity?> {
        val appName = NotificationFilter.getAppNameForPackage(packageName)
        val recentAlerts = alertDao.getRecentAlertsDirect()

        val contextHistory = recentAlerts.mapNotNull { alert ->
            val amt = extractAmount(alert.body)
            if (amt != null) {
                TransactionContext(amount = amt, merchant = alert.appName, timestamp = alert.timestamp)
            } else null
        }

        val result = detectionEngine.analyze(packageName, title, body, contextHistory)
        android.util.Log.i("AlertRepository", "🔎 Detection result for pkg='$packageName': score=${result.riskScore}% level=${result.riskLevel} explanation='${result.explanation.take(80)}'")

        // Store EVERY analyzed notification regardless of risk level (HIGH, MEDIUM, LOW/SAFE)
        val isSafe = (result.riskLevel == RiskLevel.SAFE || result.riskLevel == RiskLevel.LOW || result.riskScore < 50)
        val alert = AlertEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            body = body,
            riskLevel = result.riskLevel,
            riskScore = result.riskScore,
            scamType = result.scamType,
            matchedPatterns = result.matchedPatterns,
            explanation = result.explanation,
            isSafe = isSafe
        )
        android.util.Log.i("AlertRepository", "💾 [INSERT_START] Inserting notification alert to Room DB: pkg='$packageName' level=${result.riskLevel} score=${result.riskScore}%")
        val insertedId = alertDao.insertAlert(alert)
        val savedAlert = alert.copy(id = insertedId)
        android.util.Log.i("AlertRepository", "✅ [INSERT_DONE] Persisted alert with id=$insertedId for pkg='$packageName'")

        return Pair(result, savedAlert)
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

    private fun extractAmount(text: String): Double? {
        val pattern = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }
}
