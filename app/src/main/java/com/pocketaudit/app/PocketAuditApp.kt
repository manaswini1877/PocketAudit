package com.pocketaudit.app

import android.app.Application
import com.pocketaudit.app.data.local.AppDatabase
import com.pocketaudit.app.data.repository.AlertRepository
import com.pocketaudit.app.data.repository.UserPreferencesRepository
import com.pocketaudit.app.service.VerdictNotifier

class PocketAuditApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val alertRepository by lazy { AlertRepository(database.alertDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }

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
