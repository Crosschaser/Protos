package com.example.protosApp.data.repository

import android.content.Context
import android.util.Log
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.data.models.RegisterDeviceRequest
import com.example.protosApp.data.models.RegisterDeviceResponse
import com.example.protosApp.data.models.NotificationData
import com.example.protosApp.data.models.PendingNotificationsResponse
import com.example.protosApp.data.models.DevicePlatform
import com.example.protosApp.utils.AppConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*

class PushNotificationRepository(context: Context) {
    private val appContext: Context = context.applicationContext
    private val api: PushNotificationApi
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()
    private var pollingTimer: Timer? = null

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

    fun registerDevice(
        deviceId: String,
        preferredCity: String? = null,
        subscribeToSpeedUpdates: Boolean = true,
        subscribeToEventReminders: Boolean = true,
        subscribeToSoldOutAlerts: Boolean = true,
        subscribeToFavouriteUpdates: Boolean = true
    ): String {
        val deviceToken = generateUniqueToken()
        
        val request = RegisterDeviceRequest(
            token = deviceToken,
            deviceId = deviceId,
            platform = 0, // 0 = Android
            preferredCity = preferredCity,
            subscribeToSpeedUpdates = subscribeToSpeedUpdates,
            subscribeToEventReminders = subscribeToEventReminders,
            subscribeToSoldOutAlerts = subscribeToSoldOutAlerts,
            subscribeToFavouriteUpdates = subscribeToFavouriteUpdates
        )

        Log.d("PushNotificationRepository", "Making API call to: ${AppConfig.BASE_URL}PushNotification/register")
        Log.d("PushNotificationRepository", "Request payload: ${gson.toJson(request)}")
        Log.d("PushNotificationRepository", "Full URL will be: ${AppConfig.BASE_URL}PushNotification/register")

        api.registerDevice(request).enqueue(object : retrofit2.Callback<RegisterDeviceResponse> {
            override fun onResponse(call: retrofit2.Call<RegisterDeviceResponse>, response: retrofit2.Response<RegisterDeviceResponse>) {
                if (response.isSuccessful) {
                    // Store device token locally
                    saveDeviceToken(deviceToken)
                    Log.d("PushNotificationRepository", "Device registered successfully: ${response.body()?.message}")
                } else {
                    Log.e("PushNotificationRepository", "Registration failed with code: ${response.code()}, message: ${response.message()}")
                    response.errorBody()?.let { errorBody ->
                        Log.e("PushNotificationRepository", "Error body: ${errorBody.string()}")
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<RegisterDeviceResponse>, t: Throwable) {
                // Handle registration failure
                Log.e("PushNotificationRepository", "Registration API call failed: ${t.message}", t)
            }
        })

        return deviceToken
    }

    private fun generateUniqueToken(): String {
        return UUID.randomUUID().toString()
    }

    private fun saveDeviceToken(token: String) {
        // Save token to SharedPreferences using Application Context to avoid leaking an Activity
        appContext.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppConfig.DEVICE_TOKEN_KEY, token)
            .apply()
    }

    private fun connectWebSocket(deviceToken: String) {
        val request = Request.Builder()
            .url("${AppConfig.WEBSOCKET_URL}${AppConfig.WEBSOCKET_ENDPOINT}/$deviceToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleNotification(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                // Fallback to HTTP polling
                startHttpPolling(deviceToken)
            }
        })
    }

    private fun handleNotification(jsonMessage: String) {
        val notification = gson.fromJson(jsonMessage, NotificationData::class.java)
        // Handle the notification (show to user, etc.)
    }

    private fun startHttpPolling(deviceToken: String) {
        // Cancel any existing polling before starting a new one
        pollingTimer?.cancel()
        pollingTimer = Timer()
        pollingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkPendingNotifications(deviceToken)
            }
        }, 0, 30000) // Poll every 30 seconds
    }

    /**
     * Call this to release resources and avoid leaks when the repository is no longer needed.
     */
    fun dispose() {
        // Cancel periodic polling
        pollingTimer?.cancel()
        pollingTimer = null

        // Close websocket if open
        webSocket?.close(1000, "dispose")
        webSocket = null
    }

    private fun checkPendingNotifications(deviceToken: String) {
        api.getPendingNotifications(deviceToken).enqueue(object : retrofit2.Callback<PendingNotificationsResponse> {
            override fun onResponse(
                call: retrofit2.Call<PendingNotificationsResponse>,
                response: retrofit2.Response<PendingNotificationsResponse>
            ) {
                response.body()?.notifications?.forEach { notification ->
                    handleNotification(gson.toJson(notification))
                }
            }

            override fun onFailure(call: retrofit2.Call<PendingNotificationsResponse>, t: Throwable) {
                // Handle error
                t.printStackTrace()
            }
        })
    }
}
