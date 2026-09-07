package com.pocketaudit.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TELUGU("te", "తెలుగు (Telugu)")
}

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pocket_audit_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedTheme())
    val themeMode: StateFlow<AppTheme> = _themeMode

    private val _language = MutableStateFlow(getSavedLanguage())
    val language: StateFlow<AppLanguage> = _language

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _themeMode.value = theme
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("app_language", language.code).apply()
        _language.value = language
    }

    private fun getSavedTheme(): AppTheme {
        val saved = prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(saved)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun getSavedLanguage(): AppLanguage {
        val saved = prefs.getString("app_language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        return AppLanguage.entries.find { it.code == saved } ?: AppLanguage.ENGLISH
    }
}
