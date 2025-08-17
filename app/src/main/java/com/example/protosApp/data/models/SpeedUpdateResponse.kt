package com.example.protosApp.data.models

import com.example.protosApp.SpeedUpdate

data class SpeedUpdateResponse(
    val success: Boolean,
    val data: List<SpeedUpdate>
)
