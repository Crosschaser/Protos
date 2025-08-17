package com.example.protosApp.utils

object AppConfig {
    // Backend Configuration
    // Use 10.0.2.2 for Android Emulator (maps to host machine's localhost)
    // Use your actual IP address for physical device (e.g., "http://192.168.1.100:7077/")
    const val BASE_URL = "http://10.0.2.2:7077/api/"  // Update with your API URL
    const val WEBSOCKET_URL = "ws://10.0.2.2:7077"    // Update with your API URL
    const val WEBSOCKET_ENDPOINT = "/api/WebSocket/connect"
    
    // API Endpoints
    const val REGISTER_ENDPOINT = "/api/PushNotification/register"
    const val SEND_ENDPOINT = "/api/PushNotification/send"
    const val PENDING_ENDPOINT = "/api/WebSocket/pending"
    
    // Timing Configuration
    const val POLLING_INTERVAL_MS = 30000L // 30 seconds
    const val RECONNECT_DELAY_MS = 5000L   // 5 seconds
    const val WEBSOCKET_TIMEOUT_MS = 30000L // 30 seconds
    
    // Notification Configuration
    const val NOTIFICATION_CHANNEL_ID = "push_notification_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Push Notifications"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Channel for push notifications"
    
    // SharedPreferences Keys
    const val PREFS_NAME = "push_notifications_prefs"
    const val DEVICE_TOKEN_KEY = "device_token"
    const val PREFERRED_CITY_KEY = "preferredCity"
    const val SPEED_UPDATES_KEY = "subscribeToSpeedUpdates"
    const val EVENT_REMINDERS_KEY = "subscribeToEventReminders"
    const val SOLD_OUT_ALERTS_KEY = "subscribeToSoldOutAlerts"
    const val FAVOURITE_UPDATES_KEY = "subscribeToFavouriteUpdates"
    
    // Notification Types
    const val NOTIFICATION_TYPE_SPEED_UPDATE = "speed_update"
    const val NOTIFICATION_TYPE_SOLD_OUT_ALERT = "sold_out_alert"
    const val NOTIFICATION_TYPE_FAVOURITE_UPDATE = "favourite_update"
    const val NOTIFICATION_TYPE_EVENT_REMINDER = "event_reminder"
}
