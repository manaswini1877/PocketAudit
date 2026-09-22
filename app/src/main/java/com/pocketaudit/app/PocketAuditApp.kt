package com.pocketaudit.app

import android.app.Application
import com.pocketaudit.app.data.local.AppDatabase
import com.pocketaudit.app.data.repository.AlertRepository
import com.pocketaudit.app.data.repository.UserPreferencesRepository
import com.pocketaudit.app.detection.RiskAnalyzer
import com.pocketaudit.app.detection.RulesRiskAnalyzer
import com.pocketaudit.app.ocr.ImageTextExtractor
import com.pocketaudit.app.service.VerdictNotifier

class PocketAuditApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val riskAnalyzer: RiskAnalyzer by lazy { RulesRiskAnalyzer() }
    val alertRepository by lazy { AlertRepository(database.alertDao(), riskAnalyzer) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val imageTextExtractor by lazy { ImageTextExtractor(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        VerdictNotifier.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: PocketAuditApp
            private set
    }
}
