package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InterceptedNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object JarvisServiceBus {
    private val _isForegroundActive = MutableStateFlow(false)
    val isForegroundActive: StateFlow<Boolean> = _isForegroundActive.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<InterceptedNotification>>(emptyList())
    val recentNotifications: StateFlow<List<InterceptedNotification>> = _recentNotifications.asStateFlow()

    private val _incomingReminderAlerts = MutableStateFlow<String?>(null)
    val incomingReminderAlerts: StateFlow<String?> = _incomingReminderAlerts.asStateFlow()

    fun setForegroundActive(active: Boolean) {
        _isForegroundActive.value = active
    }

    fun onNotificationPosted(item: InterceptedNotification) {
        val current = _recentNotifications.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        _recentNotifications.value = current.take(15)
    }

    fun emitReminderAlert(alertText: String) {
        _incomingReminderAlerts.value = alertText
    }

    fun consumeReminderAlert() {
        _incomingReminderAlerts.value = null
    }
}

class JarvisForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_foreground_channel"
        const val NOTIFICATION_ID = 7001
        const val ACTION_STOP = "com.example.service.ACTION_STOP_JARVIS"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            JarvisServiceBus.setForegroundActive(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("J.A.R.V.I.S. Mark VII Active")
            .setContentText("Standing by for voice directives and scheduled briefings")
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_pause, "Disengage", pendingStop)
            .build()

        runCatching {
            startForeground(NOTIFICATION_ID, notification)
            JarvisServiceBus.setForegroundActive(true)
        }.onFailure {
            JarvisServiceBus.setForegroundActive(false)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        JarvisServiceBus.setForegroundActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Core Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent status for JARVIS voice and reminder monitoring"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }
}

class JarvisNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        if (title.isNotBlank() || text.isNotBlank()) {
            JarvisServiceBus.onNotificationPosted(
                InterceptedNotification(
                    id = "${sbn.packageName}_${sbn.id}",
                    packageName = sbn.packageName,
                    title = title.ifBlank { sbn.packageName },
                    text = text
                )
            )
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "JARVIS Reminder"
        val message = intent.getStringExtra("REMINDER_MESSAGE") ?: "Scheduled directive triggered."
        val reminderId = intent.getIntExtra("REMINDER_ID", 8001)

        val channelId = "jarvis_reminders_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JARVIS Tactical Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm?.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            context,
            reminderId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("J.A.R.V.I.S. Directive: $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .build()

        runCatching {
            nm?.notify(reminderId, notification)
        }

        JarvisServiceBus.emitReminderAlert("Reminder alert: $title. $message")
    }
}
