package com.pocketaudit.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pocketaudit.app.MainActivity
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.detection.DetectionResult

object VerdictNotifier {

    const val VERDICT_CHANNEL_ID = "pocketaudit_verdict_channel"
    private const val VERDICT_CHANNEL_NAME = "PocketAudit Verdict Notifications"
    private const val VERDICT_CHANNEL_DESC = "Real-time on-device scam verdict confirmation for every incoming banking & payment message"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VERDICT_CHANNEL_ID,
                VERDICT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = VERDICT_CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postVerdictNotification(
        context: Context,
        appName: String,
        title: String,
        body: String,
        result: DetectionResult
    ) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (verdictTitle, verdictBody, iconRes) = when (result.riskLevel) {
            RiskLevel.HIGH -> Triple(
                "🚨 SCAM ALERT (${result.riskScore}% Risk)",
                "$appName: ${result.matchedPatterns.firstOrNull() ?: result.explanation}",
                android.R.drawable.ic_dialog_alert
            )
            RiskLevel.MEDIUM -> Triple(
                "⚠️ SUSPICIOUS (${result.riskScore}% Risk)",
                "$appName: ${result.matchedPatterns.firstOrNull() ?: result.explanation}",
                android.R.drawable.ic_dialog_info
            )
            RiskLevel.LOW, RiskLevel.SAFE -> Triple(
                "✅ SAFE (0% Risk)",
                "$appName: Verified legitimate transaction (${title.ifBlank { body }})",
                android.R.drawable.ic_menu_compass
            )
        }

        val builder = NotificationCompat.Builder(context, VERDICT_CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(verdictTitle)
            .setContentText(verdictBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$verdictBody\n\nOriginal: $title - $body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
