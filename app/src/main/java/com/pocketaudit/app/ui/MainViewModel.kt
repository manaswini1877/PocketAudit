package com.pocketaudit.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketaudit.app.PocketAuditApp
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.repository.AlertRepository
import com.pocketaudit.app.data.repository.AppLanguage
import com.pocketaudit.app.data.repository.AppTheme
import com.pocketaudit.app.data.repository.UserPreferencesRepository
import com.pocketaudit.app.detection.QrRiskRules
import com.pocketaudit.app.detection.RiskInput
import com.pocketaudit.app.detection.RiskSource
import com.pocketaudit.app.detection.UpiUriParser
import com.pocketaudit.app.ocr.ImageTextExtractor
import com.pocketaudit.app.service.NotificationFilter
import com.pocketaudit.app.service.VerdictNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class MainUiEvent {
    data class OpenRiskResult(val alertId: Long) : MainUiEvent()
    data class ShowTransientMessage(val message: String) : MainUiEvent()
}

class MainViewModel(
    private val repository: AlertRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val imageTextExtractor: ImageTextExtractor
) : ViewModel() {

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val themeMode: StateFlow<AppTheme> = userPrefsRepository.themeMode
    val language: StateFlow<AppLanguage> = userPrefsRepository.language

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _checkStatusMessage = MutableStateFlow<String?>(null)
    val checkStatusMessage: StateFlow<String?> = _checkStatusMessage.asStateFlow()

    private val uiEventsChannel = Channel<MainUiEvent>(Channel.BUFFERED)
    val uiEvents = uiEventsChannel.receiveAsFlow()

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
            val combined = buildString {
                if (title.isNotBlank()) append(title)
                if (body.isNotBlank()) {
                    if (isNotEmpty()) append(" | ")
                    append(body)
                }
            }
            val input = RiskInput(
                text = combined.ifBlank { body.ifBlank { title } },
                source = RiskSource.SIMULATOR,
                packageName = packageName,
                displayTitle = title,
                displayBody = body
            )
            val (result, _) = repository.analyzeAndPersist(input)
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

    fun analyzeManualText(text: String) {
        analyzeFromCheck(text, RiskSource.MANUAL)
    }

    fun analyzeClipboardText(text: String) {
        analyzeFromCheck(text, RiskSource.CLIPBOARD)
    }

    fun analyzeSharedText(text: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                runCheckAnalysis(text, RiskSource.SHARED_TEXT)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeSharedImage(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _checkStatusMessage.value = null
            try {
                runImageAnalysis(uri, RiskSource.SHARED_IMAGE)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeQrPayload(raw: String) {
        viewModelScope.launch {
            val payload = raw.trim()
            if (payload.isEmpty()) return@launch
            _isAnalyzing.value = true
            _checkStatusMessage.value = null
            try {
                val parsed = UpiUriParser.parse(payload)
                val qr = QrRiskRules.evaluate(payload, parsed)
                val title = when {
                    parsed.isUpiPay && !parsed.pn.isNullOrBlank() -> "UPI: ${parsed.pn}"
                    parsed.isUpiPay || !parsed.pa.isNullOrBlank() -> "UPI QR"
                    else -> "QR code"
                }
                val input = RiskInput(
                    text = payload,
                    source = RiskSource.QR_SCAN,
                    displayTitle = title,
                    displayBody = payload,
                    extraReasons = qr.reasons,
                    extraScore = qr.score
                )
                val (_, alert) = repository.analyzeAndPersist(input)
                uiEventsChannel.send(MainUiEvent.OpenRiskResult(alert.id))
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "QR analysis failed", t)
                uiEventsChannel.send(
                    MainUiEvent.ShowTransientMessage("Could not analyse this QR. Try Rescan.")
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzePickedImage(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _checkStatusMessage.value = null
            try {
                runImageAnalysis(uri, RiskSource.SHARED_IMAGE)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearCheckStatus() {
        _checkStatusMessage.value = null
    }

    private fun analyzeFromCheck(text: String, source: RiskSource) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _checkStatusMessage.value = null
            try {
                runCheckAnalysis(text, source)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private suspend fun runCheckAnalysis(text: String, source: RiskSource) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        val parsed = UpiUriParser.parse(trimmed)
        val qr = QrRiskRules.evaluate(trimmed, parsed)
        val title = when {
            parsed.isUpiPay && !parsed.pn.isNullOrBlank() -> "UPI: ${parsed.pn}"
            parsed.isUpiPay || !parsed.pa.isNullOrBlank() -> "UPI QR"
            qr.signals.isNotEmpty() -> "Checked link / QR"
            else -> "Checked content"
        }
        val input = RiskInput(
            text = trimmed,
            source = source,
            displayTitle = title,
            displayBody = trimmed,
            extraReasons = qr.reasons,
            extraScore = qr.score
        )
        val (_, alert) = repository.analyzeAndPersist(input)
        uiEventsChannel.send(MainUiEvent.OpenRiskResult(alert.id))
    }

    private suspend fun runImageAnalysis(uri: Uri, source: RiskSource) {
        val extracted = try {
            imageTextExtractor.extractTextFromUri(uri)
        } catch (t: Throwable) {
            android.util.Log.e("MainViewModel", "OCR failed", t)
            null
        }
        if (extracted.isNullOrBlank()) {
            val message = "No text found in this image. Try a clearer screenshot with visible message text."
            _checkStatusMessage.value = message
            uiEventsChannel.send(MainUiEvent.ShowTransientMessage(message))
            return
        }
        val input = RiskInput(
            text = extracted,
            source = source,
            displayTitle = "Image text (OCR)",
            displayBody = extracted
        )
        val (_, alert) = repository.analyzeAndPersist(input)
        uiEventsChannel.send(MainUiEvent.OpenRiskResult(alert.id))
    }

    fun getAlertById(id: Long): kotlinx.coroutines.flow.Flow<AlertEntity?> = repository.getAlertById(id)
}

class MainViewModelFactory(
    private val repository: AlertRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val imageTextExtractor: ImageTextExtractor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, userPrefsRepository, imageTextExtractor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
