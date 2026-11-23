package com.example.telecom

import android.Manifest
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
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Android 14+ / targetSdk 36 Compliant Foreground Service (camera|microphone)
 * Dynamically matches foregroundServiceType to granted runtime permissions (CAMERA / RECORD_AUDIO)
 * so startForeground never throws SecurityException when a permission is pending or denied.
 */
class CallForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val participantsLabel = intent?.getStringExtra(EXTRA_LABEL)
            ?: "Mithun, Mafitha & Zayn Adam"

        ensureChannel()
        val tapIntent = PendingIntent.getActivity(
            this,
            202,
            Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ACTIVE_CALL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("KinNest Live Call Active")
            .setContentText("Connected with $participantsLabel • LiveKit SFU")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(tapIntent)
            .build()

        val started = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val allowedTypes = resolveAllowedServiceType(this)
                if (allowedTypes == 0) {
                    return@runCatching false
                }
                startForeground(NOTIFICATION_ID, notification, allowedTypes)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        }.getOrDefault(false)

        if (!started) {
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ACTIVE_CALL,
                getString(R.string.call_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ACTIVE_CALL = "kinnest_active_call_fgs"
        private const val NOTIFICATION_ID = 4002
        private const val ACTION_START = "action_start_call_fgs"
        private const val ACTION_STOP = "action_stop_call_fgs"
        private const val EXTRA_LABEL = "extra_call_label"

        @Volatile
        private var isRunning: Boolean = false

        fun resolveAllowedServiceType(context: Context): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
            var types = 0
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            return types
        }

        fun start(context: Context, label: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && resolveAllowedServiceType(context) == 0) {
                // Do not start FGS until at least CAMERA or RECORD_AUDIO permission is granted
                return false
            }
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LABEL, label)
            }
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            if (!isRunning) {
                runCatching { context.stopService(Intent(context, CallForegroundService::class.java)) }
                return
            }
            runCatching {
                context.stopService(Intent(context, CallForegroundService::class.java))
            }
        }
    }
}
