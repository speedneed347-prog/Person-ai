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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.AegisRepository
import com.example.voice.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AegisBackgroundService : Service() {

    private val tag = "AegisBackgroundService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var repository: AegisRepository

    companion object {
        const val CHANNEL_ID = "aegis_assistant_service"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.aegis.action.START"
        const val ACTION_STOP = "com.example.aegis.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, AegisBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AegisBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val dao = AppDatabase.getDatabase(applicationContext).aegisDao()
        repository = AegisRepository(dao)

        wakeWordDetector = WakeWordDetector(applicationContext, serviceScope)

        serviceScope.launch {
            val identity = repository.getAssistantIdentityOnce()
            val wakeWord = identity?.wakeWord ?: "Hey Aegis"
            wakeWordDetector.setWakeWord(wakeWord)

            wakeWordDetector.startListening { detectedWord ->
                Log.i(tag, "Wake word detected in background: $detectedWord")
                // Launch MainActivity when wake word is spoken
                val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("AUTO_START_VOICE", true)
                }
                startActivity(openIntent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aegis Assistant Background Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Aegis on-device assistant, wake word detection, and automations active."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aegis AI Assistant")
            .setContentText("Shield armed • Offline intelligence & wake word active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        wakeWordDetector.stopListening()
        serviceScope.cancel()
        super.onDestroy()
    }
}
