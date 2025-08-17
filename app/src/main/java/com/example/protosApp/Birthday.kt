package com.example.protosApp

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Birthday(
    val id: Int? = null,
    val name: String,
    @SerializedName(value = "date", alternate = ["birthDate"]) val date: Date?,
    val age: Int,
    val daysUntil: Int
)
