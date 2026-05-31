package com.hermes.mobile.feature.chat

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hermes.mobile.MainActivity
import com.hermes.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun startStreamingService() {
        ChatStreamForegroundService.start(context)
    }

    fun stopStreamingService() {
        ChatStreamForegroundService.stop(context)
    }

    fun notifyReply(sessionId: String, content: String) {
        if (!canPostNotifications()) return
        ensureChatNotificationChannels(context)
        val preview = content.firstReadableNotificationLine() ?: "Hermes finished replying."
        try {
            NotificationManagerCompat.from(context).notify(
                sessionId.notificationId(),
                chatReplyNotification(context, sessionId, preview),
            )
        } catch (_: SecurityException) {
        }
    }

    fun notifyFailure(sessionId: String, readable: String) {
        if (!canPostNotifications()) return
        ensureChatNotificationChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(
                sessionId.notificationId(),
                chatFailureNotification(context, sessionId, readable),
            )
        } catch (_: SecurityException) {
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

class ChatStreamForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChatNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = streamingNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.hermes.mobile.action.CHAT_STREAM_START"
        private const val ACTION_STOP = "com.hermes.mobile.action.CHAT_STREAM_STOP"

        fun start(context: Context) {
            val intent = Intent(context, ChatStreamForegroundService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChatStreamForegroundService::class.java).setAction(ACTION_STOP)
            context.stopService(intent)
        }
    }
}

private fun streamingNotification(context: Context): Notification {
    return NotificationCompat.Builder(context, CHAT_STREAM_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Hermes is replying")
        .setContentText("The chat stream will keep running in the background.")
        .setContentIntent(openAppIntent(context, REQUEST_OPEN_CHAT))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}

private fun chatReplyNotification(context: Context, sessionId: String, preview: String): Notification {
    return NotificationCompat.Builder(context, CHAT_REPLIES_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Hermes replied")
        .setContentText(preview)
        .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
        .setContentIntent(openAppIntent(context, sessionId.notificationId()))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}

private fun chatFailureNotification(context: Context, sessionId: String, readable: String): Notification {
    return NotificationCompat.Builder(context, CHAT_REPLIES_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Hermes reply stopped")
        .setContentText(readable)
        .setStyle(NotificationCompat.BigTextStyle().bigText(readable))
        .setContentIntent(openAppIntent(context, sessionId.notificationId()))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}

private fun ensureChatNotificationChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            CHAT_STREAM_CHANNEL_ID,
            "Chat streaming",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps Hermes replies running while the app is in the background."
        },
    )
    manager.createNotificationChannel(
        NotificationChannel(
            CHAT_REPLIES_CHANNEL_ID,
            "Chat replies",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifies when Hermes finishes a reply or a stream fails."
        },
    )
}

private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun String.firstReadableNotificationLine(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(MAX_NOTIFICATION_PREVIEW_LENGTH)
}

private fun String.notificationId(): Int {
    val value = hashCode()
    return if (value == Int.MIN_VALUE) 0 else abs(value)
}

private const val CHAT_STREAM_CHANNEL_ID = "chat_streaming"
private const val CHAT_REPLIES_CHANNEL_ID = "chat_replies"
private const val FOREGROUND_NOTIFICATION_ID = 41_020
private const val REQUEST_OPEN_CHAT = 41_021
private const val MAX_NOTIFICATION_PREVIEW_LENGTH = 180
