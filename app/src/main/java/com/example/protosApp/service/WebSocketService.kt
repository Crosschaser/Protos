package com.example.protosApp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.protosApp.data.models.NotificationData
import com.example.protosApp.data.repository.NotificationManager as AppNotificationManager
import com.example.protosApp.manager.NotificationPollingManager
import com.example.protosApp.utils.AppConfig
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketService : Service() {
    private val binder = LocalBinder()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var pollingManager: NotificationPollingManager
    private var deviceToken: String? = null
    private var isConnected = false
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var systemNotificationManager: NotificationManager

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = AppNotificationManager(this)
        pollingManager = NotificationPollingManager(this)
        systemNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        acquireWakeLock()
        Log.d(TAG, "WebSocketService created")
    }

    fun connectWebSocket(deviceToken: String, baseUrl: String = AppConfig.WEBSOCKET_URL) {
        this.deviceToken = deviceToken
        
        if (isConnected) {
            Log.d(TAG, "WebSocket already connected")
            return
        }

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        val request = Request.Builder()
            .url("$baseUrl${AppConfig.WEBSOCKET_ENDPOINT}/$deviceToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "WebSocket connection opened")
                // Stop polling since WebSocket is connected
                pollingManager.stopPeriodicPolling()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                handleNotification(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket closing: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket closed: $code $reason")
                // Attempt to reconnect after a delay
                reconnectAfterDelay()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "WebSocket failure", t)
                // Start WorkManager polling as fallback
                startPollingFallback()
            }
        })
    }

    private fun handleNotification(jsonMessage: String) {
        try {
            val notification = gson.fromJson(jsonMessage, NotificationData::class.java)
            notificationManager.showNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification", e)
        }
    }

    private fun reconnectAfterDelay() {
        // Reconnect after 5 seconds
        android.os.Handler(mainLooper).postDelayed({
            deviceToken?.let { token ->
                connectWebSocket(token)
            }
        }, 5000)
    }

    private fun startPollingFallback() {
        Log.d(TAG, "Starting WorkManager polling fallback")
        pollingManager.startPeriodicPolling()
    }

    fun disconnectWebSocket() {
        webSocket?.close(1000, "Service stopping")
        webSocket = null
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected

    fun startPolling() {
        Log.d(TAG, "Starting periodic polling")
        pollingManager.startPeriodicPolling()
    }

    fun stopPolling() {
        Log.d(TAG, "Stopping periodic polling")
        pollingManager.stopPeriodicPolling()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WebSocket Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains connection for push notifications"
                setShowBadge(false)
            }
            systemNotificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, com.example.protosApp.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Push Notifications Active")
            .setContentText("Connected to receive notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PushNotifications::WebSocketWakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WebSocketService onStartCommand")
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
        pollingManager.stopPeriodicPolling()
        releaseWakeLock()
        Log.d(TAG, "WebSocketService destroyed")
    }

    companion object {
        private const val TAG = "WebSocketService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "websocket_service_channel"
    }
}
