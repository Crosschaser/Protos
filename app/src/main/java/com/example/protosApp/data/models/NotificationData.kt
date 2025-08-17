package com.example.protosApp.data.models

import com.google.gson.annotations.SerializedName

data class NotificationData(
    val id: String? = null,
    val title: String,
    val body: String,
    val data: Any? = null,
    val timestamp: String,
    val type: String,
    @SerializedName("speedUpdateId") val speedUpdateId: Int? = null,
    val city: String? = null,
    val company: String? = null
)
