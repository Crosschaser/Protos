package com.example.protosApp.data.models

// Data model for CRUD operations with the Birthdays API
// Matches typical ASP.NET Core model conventions: Id, Name, Date, optional UserId
// Date is represented as ISO-8601 string (e.g., "2000-12-31") for request/response clarity

data class BirthdayCrud(
    val id: Int? = null,
    val name: String,
    val birthDate: String, // ISO-8601 (yyyy-MM-dd or full ISO8601)
    val userId: Int? = null
)
