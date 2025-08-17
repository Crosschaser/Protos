package com.example.pushnotification.data.api

import com.example.pushnotification.data.models.*
import retrofit2.Call
import retrofit2.http.*

interface PushNotificationApi {
    @POST("/api/PushNotification/register")
    fun registerDevice(@Body request: RegisterDeviceRequest): Call<Void>

    @GET("/api/WebSocket/pending/{deviceToken}")
    fun getPendingNotifications(@Path("deviceToken") deviceToken: String): Call<PendingNotificationsResponse>

    @GET("/api/WebSocket/status")
    fun getConnectionStatus(): Call<String>
}
