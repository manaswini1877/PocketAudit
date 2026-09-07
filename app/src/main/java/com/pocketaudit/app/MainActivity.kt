package com.pocketaudit.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketaudit.app.data.repository.AppLanguage
import com.pocketaudit.app.ui.MainViewModel
import com.pocketaudit.app.ui.MainViewModelFactory
import com.pocketaudit.app.ui.dashboard.DashboardScreen
import com.pocketaudit.app.ui.demo.DemoModeScreen
import com.pocketaudit.app.ui.detail.AlertDetailScreen
import com.pocketaudit.app.ui.navigation.Screen
import com.pocketaudit.app.ui.permission.PermissionHelper
import com.pocketaudit.app.ui.permission.PermissionScreen
import com.pocketaudit.app.ui.settings.SettingsScreen
import com.pocketaudit.app.ui.theme.PocketAuditTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as PocketAuditApp
        MainViewModelFactory(app.alertRepository, app.userPreferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isPermissionGranted = PermissionHelper.isNotificationListenerGranted(this)
        val startDestination = if (isPermissionGranted) Screen.Dashboard.route else Screen.Permission.route

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val language by viewModel.language.collectAsState()

            updateLocale(language)

            PocketAuditTheme(appTheme = themeMode) {
                val navController = rememberNavController()
                val alerts by viewModel.alerts.collectAsState()

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
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onClearAll = {
                                viewModel.clearAll()
                            }
                        )
                    }

                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("alertId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        // Parse alertId robustly — NavType.LongType stores as Long in Bundle
                        val rawLong = try { backStackEntry.arguments?.getLong("alertId") } catch (e: Exception) { null }
                        val rawStr = try { backStackEntry.arguments?.getString("alertId")?.toLongOrNull() } catch (e: Exception) { null }
                        val alertId = rawLong?.takeIf { it > 0 } ?: rawStr ?: -1L

                        android.util.Log.d("PocketAuditNav", "📱 Detail route opened — alertId=$alertId (rawLong=$rawLong rawStr=$rawStr)")

                        // Load directly from DB flow — never relies on stale in-memory StateFlow list
                        val alert by viewModel.getAlertById(alertId).collectAsState(initial = null)

                        android.util.Log.d("PocketAuditNav", "📱 Detail alert resolved: id=${alert?.id} title='${alert?.title}'")

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
