package com.example.protosApp.data.models

data class PendingNotificationsResponse(
    val notifications: List<NotificationData>,
    val count: Int,
    val timestamp: String
)
