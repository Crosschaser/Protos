package com.example.protosApp.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.data.models.PendingNotificationsResponse
import com.example.protosApp.data.repository.NotificationManager
import com.example.protosApp.utils.AppConfig
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class NotificationPollingWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val notificationManager = NotificationManager(context)
    private val api: PushNotificationApi
    
    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(PushNotificationApi::class.java)
    }

    override fun doWork(): Result {
        return try {
            val deviceToken = getDeviceToken()
            if (deviceToken == null) {
                Log.w(TAG, "No device token found, skipping polling")
                return Result.success()
            }

            Log.d(TAG, "Polling for notifications with token: ${deviceToken.take(8)}...")
            
            // Make synchronous API call to get pending notifications
            val response = api.getPendingNotifications(deviceToken).execute()
            
            if (response.isSuccessful) {
                val pendingNotifications = response.body()
                val notifications = pendingNotifications?.notifications ?: emptyList()
                
                Log.d(TAG, "Found ${notifications.size} pending notifications")
                
                // Show each notification
                notifications.forEach { notification ->
                    notificationManager.showNotification(notification)
                    Log.d(TAG, "Displayed notification: ${notification.title}")
                }
                
                // Mark notifications as delivered (if your API supports this)
                if (notifications.isNotEmpty()) {
                    markNotificationsAsDelivered(deviceToken, notifications.map { it.id ?: "" })
                }
                
                Result.success()
            } else {
                Log.e(TAG, "Failed to fetch notifications: ${response.code()} - ${response.message()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during notification polling", e)
            Result.retry()
        }
    }

    private fun getDeviceToken(): String? {
        val sharedPrefs = applicationContext.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(AppConfig.DEVICE_TOKEN_KEY, null)
    }

    private fun markNotificationsAsDelivered(deviceToken: String, notificationIds: List<String>) {
        try {
            // This would be an API call to mark notifications as delivered
            // For now, just log it
            Log.d(TAG, "Would mark ${notificationIds.size} notifications as delivered for token: ${deviceToken.take(8)}")
            
            // TODO: Implement API call to mark notifications as delivered
            // api.markNotificationsAsDelivered(deviceToken, notificationIds).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notifications as delivered", e)
        }
    }

    companion object {
        private const val TAG = "NotificationPollingWorker"
        const val WORK_NAME = "notification_polling_work"
    }
}
