package com.example.protosApp.data.models

import com.google.gson.annotations.SerializedName

enum class DevicePlatform {
    @SerializedName("ANDROID")
    ANDROID,
    @SerializedName("IOS") 
    IOS,
    @SerializedName("WEB")
    WEB
}
