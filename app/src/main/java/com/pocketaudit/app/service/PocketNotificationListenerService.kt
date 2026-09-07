package com.pocketaudit.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pocketaudit.app.MainActivity
import com.pocketaudit.app.data.local.AppDatabase
import com.pocketaudit.app.data.repository.AlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PocketNotificationListenerService : NotificationListenerService() {

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AlertRepository

    private fun ensureActiveScope(): CoroutineScope {
        if (serviceJob.isCancelled || serviceJob.isCompleted) {
            Log.w(TAG, "⚠️ Service coroutine job was cancelled/completed. Recreating SupervisorJob...")
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
        }
        return serviceScope
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = AlertRepository(database.alertDao())
        VerdictNotifier.createNotificationChannel(applicationContext)
        createForegroundNotificationChannel()
        startForegroundServiceNotification()
        Log.i(TAG, "🟢 PocketNotificationListenerService CREATED. Started Foreground Protection Service.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "🟢 Listener CONNECTED to Android NotificationManager service.")
        startForegroundServiceNotification()
        // Catch notifications active when service connects / rebinds
        scanActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "🔴 Listener DISCONNECTED by system. Requesting rebind...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, PocketNotificationListenerService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request listener rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            sbn ?: return
            Log.d(TAG, "📥 [ON_NOTIFICATION_POSTED] Received raw notification: pkg='${sbn.packageName}' id=${sbn.id}")
            processStatusBarNotification(sbn)
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Uncaught exception in onNotificationPosted for pkg='${sbn?.packageName}'", t)
        }
    }

    private fun scanActiveNotifications() {
        ensureActiveScope().launch {
            try {
                val activeNotifs = activeNotifications ?: return@launch
                Log.d(TAG, "Scanning ${activeNotifs.size} active notifications upon listener connection.")
                for (sbn in activeNotifs) {
                    processStatusBarNotification(sbn)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error scanning active notifications", t)
            }
        }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName ?: return

            // Ignore notifications posted by PocketAudit itself (prevent infinite feedback loop)
            if (packageName == applicationContext.packageName) return

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Skip our own verdict & status notification channels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = notification.channelId
                if (channelId == VerdictNotifier.VERDICT_CHANNEL_ID || channelId == FOREGROUND_CHANNEL_ID) return
            }

            // Extract primary title & body fields safely
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
            val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty()

            val mainBody = if (bigText.isNotBlank() && bigText.length > text.length) bigText else text

            val messagingLines = mutableListOf<String>()

            // 1. Android NotificationCompat MessagingStyle extraction
            try {
                val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
                messagingStyle?.messages?.forEach { msg ->
                    val msgText = msg.text?.toString()
                    if (!msgText.isNullOrBlank() && !messagingLines.contains(msgText)) {
                        messagingLines.add(msgText)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "MessagingStyle extraction warning for pkg='$packageName'", e)
            }

            // 2. Extract messages from EXTRA_MESSAGES (Bundles or native MessagingStyle.Message objects)
            try {
                val parcelableArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (parcelableArray != null) {
                    for (p in parcelableArray) {
                        if (p is Bundle) {
                            val msgText = p.getCharSequence("text")?.toString()
                            if (!msgText.isNullOrBlank() && !messagingLines.contains(msgText)) {
                                messagingLines.add(msgText)
                            }
                        } else if (p != null) {
                            try {
                                val textMethod = p.javaClass.getMethod("getText")
                                val textResult = textMethod.invoke(p) as? CharSequence
                                val msgText = textResult?.toString()
                                if (!msgText.isNullOrBlank() && !messagingLines.contains(msgText)) {
                                    messagingLines.add(msgText)
                                }
                            } catch (ignored: Exception) {
                                // Fallback reflection ignored
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "EXTRA_MESSAGES array extraction warning for pkg='$packageName'", e)
            }

            // 3. Extract lines from InboxStyle multi-line notifications
            try {
                val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (textLines != null) {
                    for (line in textLines) {
                        val lineStr = line?.toString()
                        if (!lineStr.isNullOrBlank() && !messagingLines.contains(lineStr)) {
                            messagingLines.add(lineStr)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "EXTRA_TEXT_LINES extraction warning for pkg='$packageName'", e)
            }

            val combinedBody = buildString {
                if (mainBody.isNotBlank()) append(mainBody)
                if (subText.isNotBlank() && !contains(subText)) {
                    if (isNotEmpty()) append(" | ")
                    append(subText)
                }
                if (summaryText.isNotBlank() && !contains(summaryText)) {
                    if (isNotEmpty()) append(" | ")
                    append(summaryText)
                }
                if (messagingLines.isNotEmpty()) {
                    val subContent = messagingLines.joinToString(" ")
                    if (!contains(subContent)) {
                        if (isNotEmpty()) append(" | ")
                        append(subContent)
                    }
                }
            }.trim()

            Log.d(TAG, "📥 Raw Extracted Payload: pkg='$packageName' id=${sbn.id} title='$title' body='$combinedBody'")

            if (title.isBlank() && combinedBody.isBlank()) {
                Log.w(TAG, "⚠️ [SKIPPED_EMPTY] Extracted title and body were BOTH blank for pkg='$packageName' id=${sbn.id}")
                return
            }

            // Filter check
            val isTarget = NotificationFilter.isTargetNotification(packageName, title, combinedBody, applicationContext)
            if (!isTarget) {
                Log.d(TAG, "⛔ Filtered OUT (NotificationFilter): pkg='$packageName'")
                return
            }

            Log.i(TAG, "🎯 TARGET Notification Passed Filter: pkg='$packageName' title='$title'")

            // Pass to detection engine asynchronously in isolated coroutine
            ensureActiveScope().launch {
                try {
                    Log.d(TAG, "🔬 [START_DETECTION] Analyzing target notification: pkg='$packageName'")
                    val appName = NotificationFilter.getAppNameForPackage(packageName)
                    val (result, alert) = repository.analyzeAndStoreNotification(packageName, title, combinedBody)
                    Log.d(TAG, "🏁 [END_DETECTION] Result for pkg='$packageName': score=${result.riskScore}% level=${result.riskLevel}")

                    if (alert != null) {
                        Log.w(TAG, "🚨 SCAM ALERT FLAGGED [${alert.riskLevel}] Score:${alert.riskScore}% Type:${alert.scamType}")
                    } else {
                        Log.i(TAG, "✅ SAFE Message Verified [${result.riskLevel}] Score:${result.riskScore}%")
                    }

                    // Immediately post real-time Android verdict notification back to user
                    VerdictNotifier.postVerdictNotification(
                        context = applicationContext,
                        appName = appName,
                        title = title,
                        body = combinedBody,
                        result = result
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "❌ Error inside DetectionEngine coroutine for pkg='$packageName'", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Exception inside processStatusBarNotification for pkg='${sbn.packageName}'", t)
        }
    }

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "PocketAudit Protection Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing status for PocketAudit on-device scam protection listener"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🛡️ PocketAudit Protection Active")
            .setContentText("Monitoring UPI & Banking notifications 100% on-device")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground for listener service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "PocketAuditService"
        const val FOREGROUND_CHANNEL_ID = "pocketaudit_protection_guard_channel"
        private const val SERVICE_NOTIFICATION_ID = 9991
    }
}
