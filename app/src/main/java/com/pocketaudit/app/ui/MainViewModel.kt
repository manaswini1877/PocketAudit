package com.pocketaudit.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketaudit.app.PocketAuditApp
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.repository.AlertRepository
import com.pocketaudit.app.data.repository.AppLanguage
import com.pocketaudit.app.data.repository.AppTheme
import com.pocketaudit.app.data.repository.UserPreferencesRepository
import com.pocketaudit.app.service.NotificationFilter
import com.pocketaudit.app.service.VerdictNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AlertRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Never stop collecting — prevents stale emptyList() on recompose
            initialValue = emptyList()
        )

    val themeMode: StateFlow<AppTheme> = userPrefsRepository.themeMode
    val language: StateFlow<AppLanguage> = userPrefsRepository.language

    fun setTheme(theme: AppTheme) {
        userPrefsRepository.setTheme(theme)
    }

    fun setLanguage(language: AppLanguage) {
        userPrefsRepository.setLanguage(language)
    }

    fun markAsSafe(id: Long) {
        viewModelScope.launch {
            repository.markAsSafe(id)
        }
    }

    fun deleteAlert(alert: AlertEntity) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    fun simulateNotification(packageName: String, title: String, body: String) {
        viewModelScope.launch {
            val (result, _) = repository.analyzeAndStoreNotification(packageName, title, body)
            val appName = NotificationFilter.getAppNameForPackage(packageName)
            VerdictNotifier.postVerdictNotification(
                context = PocketAuditApp.instance,
                appName = appName,
                title = title,
                body = body,
                result = result
            )
        }
    }

    /** Load a single alert by ID directly from the DB — used by detail screen to avoid stale StateFlow lookups */
    fun getAlertById(id: Long): kotlinx.coroutines.flow.Flow<AlertEntity?> = repository.getAlertById(id)
}

class MainViewModelFactory(
    private val repository: AlertRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, userPrefsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
