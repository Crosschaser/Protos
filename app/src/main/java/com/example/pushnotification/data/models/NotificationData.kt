package com.example.pushnotification.data.models

import com.google.gson.annotations.SerializedName
import java.util.*

data class RegisterDeviceRequest(
    val token: String,
    val deviceId: String,
    val userId: String? = null,
    @SerializedName("platform")
    val devicePlatform: DevicePlatform = DevicePlatform.ANDROID,
    val preferredCity: String? = null,
    val subscribeToSpeedUpdates: Boolean = true,
    val subscribeToEventReminders: Boolean = true,
    val subscribeToSoldOutAlerts: Boolean = true,
    val subscribeToFavouriteUpdates: Boolean = true
)

data class NotificationData(
    val title: String,
    val body: String,
    val data: Any? = null,
    val timestamp: String = Date().toString(),
    val type: String,
    val speedUpdateId: Int? = null,
    val city: String? = null,
    val company: String? = null
)

data class PendingNotificationsResponse(
    val notifications: List<NotificationData>,
    val count: Int,
    val timestamp: String
)

enum class DevicePlatform {
    @SerializedName("0")
    ANDROID,
    @SerializedName("1")
    IOS,
    @SerializedName("2")
    WEB
}
