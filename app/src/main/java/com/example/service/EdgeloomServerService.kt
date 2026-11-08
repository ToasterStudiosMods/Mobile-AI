package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class EdgeloomServerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
        val ip = intent?.getStringExtra(EXTRA_IP) ?: "127.0.0.1"

        val notification = buildNotification(ip, port)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Edgeloom OpenAI Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the local OpenAI HTTP API server active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(ip: String, port: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edgeloom Server Active")
            .setContentText("OpenAI API endpoint running at http://$ip:$port/v1")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "edgeloom_server_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "com.example.service.START_SERVER"
        const val ACTION_STOP = "com.example.service.STOP_SERVER"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_IP = "extra_ip"

        fun startService(context: Context, ip: String, port: Int) {
            val intent = Intent(context, EdgeloomServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EdgeloomServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
