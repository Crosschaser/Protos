package com.example.protosApp.data.models

import com.google.gson.annotations.SerializedName

data class RegisterDeviceWrapper(
    @SerializedName("request") val request: RegisterDeviceRequest
)
