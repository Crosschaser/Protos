package com.example.protosApp.data.api

import com.example.protosApp.data.models.RegisterDeviceRequest
import com.example.protosApp.data.models.RegisterDeviceResponse
import com.example.protosApp.data.models.NotificationData
import com.example.protosApp.data.models.PendingNotificationsResponse
import com.example.protosApp.data.models.BirthdayResponse
import com.example.protosApp.data.models.SpeedUpdateResponse
import com.example.protosApp.data.models.BirthdayCrud
import com.example.protosApp.data.models.UpdateIsSoldOutRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface PushNotificationApi {
    @POST("PushNotification/register")
    fun registerDevice(@Body request: RegisterDeviceRequest): Call<RegisterDeviceResponse>

    @POST("PushNotification/send")
    fun sendNotification(@Body notification: NotificationData): Call<Void>

    @GET("WebSocket/pending/{deviceToken}")
    fun getPendingNotifications(@Path("deviceToken") deviceToken: String): Call<PendingNotificationsResponse>

    @GET("birthdays/upcoming")
    fun getUpcomingBirthdays(
        @Query("days") days: Int = 7,
        @Query("userId") userId: Int? = null
    ): Call<BirthdayResponse>

    @GET("speed-updates/recent")
    fun getRecentSpeedUpdates(
        @Query("limit") limit: Int = 10,
        @Query("city") city: String? = null,
        @Query("hours") hours: Int = 24
    ): Call<SpeedUpdateResponse>

    // Birthdays CRUD
    @POST("birthdays")
    fun createBirthday(@Body birthday: BirthdayCrud): Call<BirthdayCrud>

    @GET("birthdays/{id}")
    fun getBirthdayById(@Path("id") id: Int): Call<BirthdayCrud>

    @PUT("birthdays/{id}")
    fun updateBirthday(
        @Path("id") id: Int,
        @Body birthday: BirthdayCrud
    ): Call<BirthdayCrud>

    @DELETE("birthdays/{id}")
    fun deleteBirthday(@Path("id") id: Int): Call<Void>

    // Optional: list all birthdays (needed for swipe-to-delete/update by ID)
    @GET("birthdays")
    fun listBirthdays(): Call<List<BirthdayCrud>>

    // SpeedUpdate endpoints
    @POST("SpeedUpdate/UpdateEventIsSoldOut")
    fun updateEventIsSoldOut(@Body request: UpdateIsSoldOutRequest): Call<Void>
}
