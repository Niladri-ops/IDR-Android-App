package com.voltedge.mitra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class NavigationService : Service() {

    private val binder = LocalBinder()
    var coordinator: NavigationCoordinator? = null
        private set

    inner class LocalBinder : Binder() {
        fun getService(): NavigationService = this@NavigationService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NavigationService onCreate")
        // Call startForeground FIRST to ensure Android OS requirements are immediately satisfied
        startForegroundServiceWithNotification()

        try {
            coordinator = NavigationCoordinator(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NavigationCoordinator", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NavigationService onStartCommand called")
        startForegroundServiceWithNotification()
        coordinator?.startNavigation()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "NavigationService onBind called")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "NavigationService onUnbind called")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "NavigationService onDestroy called")
        coordinator?.stopNavigation()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "NavigationChannel"
        val channel = NotificationChannel(
            channelId,
            "Dead Reckoning Navigation",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mitra Smart Navigation")
            .setContentText("AI Dead Reckoning Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundServiceWithNotification() {
        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(101, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(101, notification)
                }
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback error starting foreground service", fallbackException)
            }
        }
    }

    companion object {
        private const val TAG = "MitraService"
    }
}
