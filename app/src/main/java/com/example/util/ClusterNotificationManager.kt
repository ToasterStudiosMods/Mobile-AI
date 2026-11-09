package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

object ClusterNotificationManager {
    private const val CHANNEL_ID = "edgeloom_cluster_test_alerts"
    private const val CHANNEL_NAME = "Edgeloom Cluster Alerts"
    private var notificationIdCounter = 2000

    fun createAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate alerts and connectivity test notifications between cluster devices"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showClusterNotification(
        context: Context,
        senderName: String,
        senderIp: String,
        message: String
    ) {
        createAlertChannel(context)
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⚡ Cluster Ping from $senderName")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nVerified connection with $senderIp"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            notificationManager.notify(notificationIdCounter++, notification)
            Log.i("ClusterNotification", "Posted cluster test notification from $senderName ($senderIp)")
        } catch (e: Exception) {
            Log.w("ClusterNotification", "Could not display notification: ${e.message}")
        }
    }
}
