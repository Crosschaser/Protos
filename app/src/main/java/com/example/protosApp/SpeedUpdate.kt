package com.example.protosApp

import java.util.Date

data class SpeedUpdate(
    val id: Int,
    val city: String,
    val company: String,
    val message: String,
    val timestamp: Date,
    val website: String? = null,
    val isRead: Boolean = false
)
