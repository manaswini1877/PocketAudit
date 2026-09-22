package com.pocketaudit.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketaudit.app.data.repository.AppLanguage
import com.pocketaudit.app.share.ShareIntentHandler
import com.pocketaudit.app.share.SharePayload
import com.pocketaudit.app.ui.MainUiEvent
import com.pocketaudit.app.ui.MainViewModel
import com.pocketaudit.app.ui.MainViewModelFactory
import com.pocketaudit.app.ui.check.CheckMessageScreen
import com.pocketaudit.app.ui.dashboard.DashboardScreen
import com.pocketaudit.app.ui.demo.DemoModeScreen
import com.pocketaudit.app.ui.detail.AlertDetailScreen
import com.pocketaudit.app.ui.navigation.Screen
import com.pocketaudit.app.ui.permission.PermissionHelper
import com.pocketaudit.app.ui.permission.PermissionScreen
import com.pocketaudit.app.ui.qr.QrScanScreen
import com.pocketaudit.app.ui.result.RiskResultScreen
import com.pocketaudit.app.ui.settings.SettingsScreen
import com.pocketaudit.app.ui.theme.PocketAuditTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as PocketAuditApp
        MainViewModelFactory(app.alertRepository, app.userPreferencesRepository, app.imageTextExtractor)
    }

    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingIntent = intent

        val isPermissionGranted = PermissionHelper.isNotificationListenerGranted(this)
        val startDestination = if (isPermissionGranted) Screen.Dashboard.route else Screen.Permission.route

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val language by viewModel.language.collectAsState()
            val isAnalyzing by viewModel.isAnalyzing.collectAsState()
            val checkStatus by viewModel.checkStatusMessage.collectAsState()

            updateLocale(language)

            PocketAuditTheme(appTheme = themeMode) {
                val navController = rememberNavController()
                val alerts by viewModel.alerts.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.uiEvents.collect { event ->
                        when (event) {
                            is MainUiEvent.OpenRiskResult -> {
                                navController.navigate(Screen.RiskResult.createRoute(event.alertId)) {
                                    launchSingleTop = true
                                }
                            }
                            is MainUiEvent.ShowTransientMessage -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                LaunchedEffect(pendingIntent) {
                    val intent = pendingIntent ?: return@LaunchedEffect
                    when (val payload = ShareIntentHandler.parse(intent)) {
                        is SharePayload.Text -> viewModel.analyzeSharedText(payload.value)
                        is SharePayload.Image -> viewModel.analyzeSharedImage(payload.uri)
                        null -> Unit
                    }
                    if (ShareIntentHandler.parse(intent) != null) {
                        pendingIntent = null
                        intent.action = null
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable(Screen.Permission.route) {
                        PermissionScreen(
                            onPermissionGranted = {
                                PermissionHelper.requestRebindService(this@MainActivity)
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Permission.route) { inclusive = true }
                                }
                            },
                            onSkipToDemo = {
                                navController.navigate(Screen.DemoMode.route)
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            alerts = alerts,
                            onAlertClick = { alertId ->
                                navController.navigate(Screen.Detail.createRoute(alertId))
                            },
                            onOpenDemoMode = {
                                navController.navigate(Screen.DemoMode.route)
                            },
                            onOpenCheckMessage = {
                                viewModel.clearCheckStatus()
                                navController.navigate(Screen.CheckMessage.route)
                            },
                            onOpenScanQr = {
                                navController.navigate(Screen.QrScan.route)
                            },
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onClearAll = {
                                viewModel.clearAll()
                            }
                        )
                    }

                    composable(Screen.CheckMessage.route) {
                        CheckMessageScreen(
                            isAnalyzing = isAnalyzing,
                            statusMessage = checkStatus,
                            onAnalyzeManual = { text -> viewModel.analyzeManualText(text) },
                            onAnalyzeClipboard = { text -> viewModel.analyzeClipboardText(text) },
                            onPickImage = { uri -> viewModel.analyzePickedImage(uri) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.QrScan.route) {
                        QrScanScreen(
                            isAnalyzing = isAnalyzing,
                            onQrCaptured = { raw -> viewModel.analyzeQrPayload(raw) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.RiskResult.route,
                        arguments = listOf(navArgument("alertId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val alertId = backStackEntry.arguments?.getLong("alertId") ?: -1L
                        val alert by viewModel.getAlertById(alertId).collectAsState(initial = null)
                        RiskResultScreen(
                            alert = alert,
                            onBack = {
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            },
                            onViewHistoryDetail = { id ->
                                navController.navigate(Screen.Detail.createRoute(id))
                            }
                        )
                    }

                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("alertId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val rawLong = try { backStackEntry.arguments?.getLong("alertId") } catch (e: Exception) { null }
                        val rawStr = try { backStackEntry.arguments?.getString("alertId")?.toLongOrNull() } catch (e: Exception) { null }
                        val alertId = rawLong?.takeIf { it > 0 } ?: rawStr ?: -1L

                        val alert by viewModel.getAlertById(alertId).collectAsState(initial = null)

                        AlertDetailScreen(
                            alert = alert,
                            onBack = { navController.popBackStack() },
                            onMarkAsSafe = { id -> viewModel.markAsSafe(id) },
                            onDelete = { item -> viewModel.deleteAlert(item); navController.popBackStack() }
                        )
                    }

                    composable(Screen.DemoMode.route) {
                        DemoModeScreen(
                            onTriggerScenario = { pkg, title, body ->
                                viewModel.simulateNotification(pkg, title, body)
                            },
                            onBackToDashboard = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            currentTheme = themeMode,
                            currentLanguage = language,
                            onThemeSelected = { newTheme -> viewModel.setTheme(newTheme) },
                            onLanguageSelected = { newLang -> viewModel.setLanguage(newLang) },
                            onBack = { navController.popBackStack() },
                            onOpenPermissionSettings = {
                                PermissionHelper.openNotificationListenerSettings(this@MainActivity)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.isNotificationListenerGranted(this)) {
            PermissionHelper.requestRebindService(this)
        }
    }

    private fun updateLocale(appLanguage: AppLanguage) {
        val locale = Locale(appLanguage.code)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
