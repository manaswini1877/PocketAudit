package com.pocketaudit.app.ui.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.pocketaudit.app.service.PocketNotificationListenerService

object PermissionHelper {

    fun isNotificationListenerGranted(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val names = flat.split(":")
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null && componentName.packageName == packageName) {
                return true
            }
        }
        return false
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }
    }

    fun requestRebindService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, PocketNotificationListenerService::class.java)
                )
            } catch (e: Exception) {
                // Ignore if not allowed
            }
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun isOEMAggressiveDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
               manufacturer.contains("redmi") ||
               manufacturer.contains("vivo") ||
               manufacturer.contains("iqoo") ||
               manufacturer.contains("oppo") ||
               manufacturer.contains("realme") ||
               manufacturer.contains("oneplus") ||
               manufacturer.contains("huawei") ||
               manufacturer.contains("honor")
    }

    fun getOEMName(): String {
        return when {
            Build.MANUFACTURER.contains("vivo", true) || Build.MANUFACTURER.contains("iqoo", true) -> "Vivo / iQOO (Funtouch OS / OriginOS)"
            Build.MANUFACTURER.contains("xiaomi", true) || Build.MANUFACTURER.contains("redmi", true) -> "Xiaomi (MIUI / HyperOS)"
            Build.MANUFACTURER.contains("oppo", true) || Build.MANUFACTURER.contains("realme", true) || Build.MANUFACTURER.contains("oneplus", true) -> "Oppo / Realme / OnePlus (ColorOS / OxygenOS)"
            else -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }
}
