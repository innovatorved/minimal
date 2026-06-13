package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.LauncherDatabase
import com.example.usage.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 5)
        val startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())

        createChannel()
        startForeground(
            NOTIFICATION_ID_FOREGROUND,
            buildForegroundNotification(appName, durationMinutes)
        )

        monitorJob?.cancel()
        monitorJob = scope.launch {
            val usageRepo = UsageStatsRepository(applicationContext, LauncherDatabase.getDatabase(applicationContext).launcherDao())
            val endTime = startTime + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
            while (System.currentTimeMillis() < endTime) {
                delay(5000)
                val foreground = usageRepo.getForegroundPackage()
                if (foreground != null && foreground != packageName) {
                    // User left the app; keep timer running in background
                }
            }
            postTimeUpNotification(appName)
            val dao = LauncherDatabase.getDatabase(applicationContext).launcherDao()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.addAppMinutes(packageName, appName, date, durationMinutes)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun postTimeUpNotification(appName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SHOW_BREATHING, true)
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("time's up")
            .setContentText("your $appName session has ended")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("your $appName session has ended"))
            .build()
        nm.notify(NOTIFICATION_ID_TIME_UP, notification)
    }

    private fun buildForegroundNotification(appName: String, minutes: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("session active")
            .setContentText("$appName — $minutes min limit")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus sessions",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Mindful launch time reminders"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_START_TIME = "extra_start_time"
        const val ACTION_SESSION_EXPIRED = "com.minimalist.launcher.SESSION_EXPIRED"
        private const val CHANNEL_ID = "focus_sessions"
        private const val NOTIFICATION_ID_FOREGROUND = 1001
        private const val NOTIFICATION_ID_TIME_UP = 1002

        fun startSession(
            context: Context,
            packageName: String,
            appName: String,
            durationMinutes: Int
        ) {
            val intent = Intent(context, SessionMonitorService::class.java).apply {
                putExtra(EXTRA_PACKAGE, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
                putExtra(EXTRA_START_TIME, System.currentTimeMillis())
            }
            context.startForegroundService(intent)
        }
    }
}
