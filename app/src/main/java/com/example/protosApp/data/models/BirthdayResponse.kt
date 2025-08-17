package com.example.protosApp.data.models

import com.example.protosApp.Birthday

data class BirthdayResponse(
    val success: Boolean,
    val data: List<Birthday>
)
