package com.example.pushnotification.services

import android.content.Context
import android.provider.Settings
import com.example.pushnotification.data.api.PushNotificationApi
import com.example.pushnotification.data.models.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushNotificationService(
    private val context: Context,
    private val baseUrl: String = "http://localhost:7077"
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()
    private var deviceToken: String? = null

    private val api: PushNotificationApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PushNotificationApi::class.java)
    }

    suspend fun registerDevice(): Result<String> = suspendCoroutine { continuation ->
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()

        val deviceToken = generateUniqueToken()
        val registrationRequest = RegisterDeviceRequest(
            token = deviceToken,
            deviceId = deviceId,
            devicePlatform = DevicePlatform.ANDROID,
            subscribeToSpeedUpdates = true,
            subscribeToEventReminders = true,
            subscribeToSoldOutAlerts = true,
            subscribeToFavouriteUpdates = true
        )

        api.registerDevice(registrationRequest).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    saveDeviceToken(deviceToken)
                    continuation.resume(Result.success(deviceToken))
                } else {
                    continuation.resume(Result.failure(Exception("Registration failed")))
                }
            }

            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                continuation.resume(Result.failure(t))
            }
        })
    }

    fun connectWebSocket() {
        deviceToken?.let { token ->
            val request = Request.Builder()
                .url("$baseUrl/api/WebSocket/connect/$token")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleNotification(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    // Fallback to HTTP polling
                    startHttpPolling()
                }
            })
        }
    }

    private fun handleNotification(jsonMessage: String) {
        val notification = gson.fromJson(jsonMessage, NotificationData::class.java)
        showNotification(notification)
    }

    private fun showNotification(notification: NotificationData) {
        // Implementation of notification display logic
    }

    private fun saveDeviceToken(token: String) {
        deviceToken = token
        // Save token to SharedPreferences or secure storage
    }

    private fun generateUniqueToken(): String {
        return UUID.randomUUID().toString()
    }

    private fun startHttpPolling() {
        // Implementation of HTTP polling fallback
    }
}
