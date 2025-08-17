package com.example.protosApp.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.protosApp.MainActivity
import com.example.protosApp.SpeedUpdateActivity
import com.example.protosApp.SoldOutActivity
import com.example.protosApp.data.models.NotificationData
import com.google.gson.Gson

class NotificationManager(private val context: Context) {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val gson = Gson()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Push Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for push notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(notification: NotificationData) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)

        notification.data?.let { data ->
            val intent = createIntentFromData(data)
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createIntentFromData(data: Any): android.content.Intent {
        val notificationData = gson.fromJson(gson.toJson(data), NotificationData::class.java)
        return when (notificationData.type) {
            "speed_update" -> android.content.Intent(context, SpeedUpdateActivity::class.java).apply {
                putExtra("speedUpdateId", notificationData.speedUpdateId)
            }
            "sold_out_alert" -> android.content.Intent(context, SoldOutActivity::class.java)
            else -> android.content.Intent(context, MainActivity::class.java)
        }
    }

    companion object {
        private const val CHANNEL_ID = "push_notification_channel"
    }
}
