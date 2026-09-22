package com.pocketaudit.app.ui.navigation

sealed class Screen(val route: String) {
    object Permission : Screen("permission")
    object Dashboard : Screen("dashboard")
    object Detail : Screen("detail/{alertId}") {
        fun createRoute(alertId: Long) = "detail/$alertId"
    }
    object RiskResult : Screen("risk_result/{alertId}") {
        fun createRoute(alertId: Long) = "risk_result/$alertId"
    }
    object CheckMessage : Screen("check_message")
    object QrScan : Screen("qr_scan")
    object DemoMode : Screen("demo_mode")
    object Settings : Screen("settings")
}
