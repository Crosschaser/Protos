package com.example.protosApp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.data.models.PendingNotificationsResponse
import com.example.protosApp.data.repository.NotificationManager
import com.example.protosApp.utils.AppConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class HttpPollingService : Service() {
    private lateinit var api: PushNotificationApi
    private lateinit var notificationManager: NotificationManager
    private var pollingTimer: Timer? = null
    private var deviceToken: String? = null

    override fun onCreate() {
        super.onCreate()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(PushNotificationApi::class.java)
        notificationManager = NotificationManager(this)
        
        Log.d(TAG, "HttpPollingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        deviceToken = intent?.getStringExtra("deviceToken")
        
        if (deviceToken != null) {
            startPolling()
        } else {
            Log.e(TAG, "No device token provided")
            stopSelf()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        stopPolling() // Stop any existing polling
        
        pollingTimer = Timer()
        pollingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                deviceToken?.let { token ->
                    checkPendingNotifications(token)
                }
            }
        }, 0, AppConfig.POLLING_INTERVAL_MS)
        
        Log.d(TAG, "Started HTTP polling every ${AppConfig.POLLING_INTERVAL_MS / 1000} seconds")
    }

    private fun stopPolling() {
        pollingTimer?.cancel()
        pollingTimer = null
    }

    private fun checkPendingNotifications(deviceToken: String) {
        api.getPendingNotifications(deviceToken).enqueue(object : Callback<PendingNotificationsResponse> {
            override fun onResponse(
                call: Call<PendingNotificationsResponse>,
                response: Response<PendingNotificationsResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.notifications?.forEach { notification ->
                        notificationManager.showNotification(notification)
                    }
                    Log.d(TAG, "Checked pending notifications: ${response.body()?.count ?: 0} found")
                } else {
                    Log.e(TAG, "Failed to check pending notifications: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PendingNotificationsResponse>, t: Throwable) {
                Log.e(TAG, "Error checking pending notifications", t)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        Log.d(TAG, "HttpPollingService destroyed")
    }

    companion object {
        private const val TAG = "HttpPollingService"
    }
}
