package com.example.protosApp.data.models

import com.google.gson.annotations.SerializedName

data class RegisterDeviceRequest(
    @SerializedName("Token") val token: String,
    @SerializedName("DeviceId") val deviceId: String,
    @SerializedName("UserId") val userId: String? = null,
    @SerializedName("Platform") val platform: Int = 0, // 0 = Android, 1 = iOS, 2 = Web
    @SerializedName("PreferredCity") val preferredCity: String? = null,
    @SerializedName("SubscribeToSpeedUpdates") val subscribeToSpeedUpdates: Boolean = true,
    @SerializedName("SubscribeToEventReminders") val subscribeToEventReminders: Boolean = true,
    @SerializedName("SubscribeToSoldOutAlerts") val subscribeToSoldOutAlerts: Boolean = true,
    @SerializedName("SubscribeToFavouriteUpdates") val subscribeToFavouriteUpdates: Boolean = true
)
