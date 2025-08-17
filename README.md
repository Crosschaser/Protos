# Custom Push Notification System - Android App

A Kotlin Android application that implements a custom push notification system without relying on third-party services like Firebase. It uses WebSockets for real-time communication and HTTP polling as a fallback.

## Features

- **Real-time WebSocket connections** for instant notifications
- **HTTP polling fallback** when WebSocket is unavailable
- **Device registration and management**
- **Topic-based subscriptions** (city preferences, notification types)
- **Notification history and tracking**
- **Multiple notification types**: SpeedUpdate, Sold-out alerts, Favourite updates

## Architecture

### Core Components

1. **Data Models** (`data/models/`)
   - `DevicePlatform.kt` - Platform enumeration
   - `RegisterDeviceRequest.kt` - Device registration request model
   - `NotificationData.kt` - Notification data structure
   - `PendingNotificationsResponse.kt` - API response model

2. **API Layer** (`data/api/`)
   - `PushNotificationApi.kt` - Retrofit API interface

3. **Repository Layer** (`data/repository/`)
   - `PushNotificationRepository.kt` - Main repository for device registration
   - `NotificationManager.kt` - Handles notification display

4. **Services** (`service/`)
   - `WebSocketService.kt` - Real-time WebSocket connection management
   - `HttpPollingService.kt` - HTTP polling fallback service

5. **UI Components**
   - `MainActivity.kt` - Main registration and settings screen
   - `SpeedUpdateActivity.kt` - Speed update notification details
   - `SoldOutActivity.kt` - Sold-out alert details

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 (API level 24) or higher
- Kotlin 1.8.0+

### Backend Setup

1. Ensure your backend API is running on `http://localhost:7077`
2. The backend should implement the following endpoints:
   - `POST /api/PushNotification/register` - Device registration
   - `POST /api/PushNotification/send` - Send notifications
   - `GET /api/WebSocket/pending/{deviceToken}` - Get pending notifications
   - `ws://localhost:7077/api/WebSocket/connect/{deviceToken}` - WebSocket connection

### Android App Setup

1. Clone or download this project
2. Open in Android Studio
3. Update the base URL in the following files if your backend is not on localhost:
   - `PushNotificationRepository.kt` (line 21)
   - `HttpPollingService.kt` (line 32)
   - `WebSocketService.kt` (line 45)

4. Build and run the app

## Usage

### Device Registration

1. Open the app
2. Enter your preferred city (optional)
3. Configure notification preferences using the switches
4. Tap "Register Device"
5. The app will automatically connect to WebSocket after successful registration

### Testing the System

#### 1. Test Device Registration

```bash
curl -X POST "http://localhost:7077/api/PushNotification/register" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "test-device-token-123",
    "deviceId": "android-test-device",
    "platform": 0,
    "preferredCity": "Amsterdam",
    "subscribeToSpeedUpdates": true,
    "subscribeToEventReminders": true,
    "subscribeToSoldOutAlerts": true,
    "subscribeToFavouriteUpdates": true
  }'
```

#### 2. Test Sending Notification

```bash
curl -X POST "http://localhost:7077/api/PushNotification/send" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Speed Update",
    "body": "Check out the latest update in Amsterdam",
    "type": "speed_update",
    "priority": 2,
    "targetDeviceToken": "your-device-token-here",
    "data": "{\"speedUpdateId\": 123, \"city\": \"Amsterdam\", \"company\": \"TestCompany\"}"
  }'
```

#### 3. Test WebSocket Connection

Use a WebSocket client to connect to:
```
ws://localhost:7077/api/WebSocket/connect/your-device-token-here
```

#### 4. Test HTTP Polling

```bash
curl "http://localhost:7077/api/WebSocket/pending/your-device-token-here"
```

## Configuration

### Notification Types

The app supports the following notification types:

- **speed_update** - Opens `SpeedUpdateActivity`
- **sold_out_alert** - Opens `SoldOutActivity`
- **default** - Opens `MainActivity`

### Polling Configuration

- **WebSocket reconnection delay**: 5 seconds
- **HTTP polling interval**: 30 seconds
- **WebSocket timeout**: 30 seconds

## Permissions

The app requires the following permissions:

- `INTERNET` - For network communication
- `ACCESS_NETWORK_STATE` - To check network connectivity
- `POST_NOTIFICATIONS` - To display notifications (Android 13+)

## Error Handling

- **WebSocket connection failure**: Automatically falls back to HTTP polling
- **Network errors**: Logged and handled gracefully
- **Registration failures**: Displayed to user with error messages
- **Notification parsing errors**: Logged without crashing the app

## Logging

All services include comprehensive logging with the following tags:
- `WebSocketService` - WebSocket connection events
- `HttpPollingService` - HTTP polling events
- `PushNotificationRepository` - Registration and API calls

## Security Considerations

- Device tokens are generated using UUID for uniqueness
- All network communication should use HTTPS in production
- Consider implementing authentication for sensitive operations
- Store sensitive data securely using Android Keystore

## Troubleshooting

### Common Issues

1. **WebSocket connection fails**
   - Check if backend WebSocket endpoint is running
   - Verify the URL format in `WebSocketService.kt`
   - Check network connectivity

2. **Notifications not appearing**
   - Ensure notification permissions are granted
   - Check if the notification channel is created properly
   - Verify the backend is sending notifications to the correct device token

3. **Registration fails**
   - Check backend API endpoint availability
   - Verify request format matches backend expectations
   - Check network connectivity

### Debug Steps

1. Enable verbose logging in Android Studio
2. Check Logcat for service-specific logs
3. Test backend endpoints independently using curl
4. Verify WebSocket connection using a WebSocket client

## Future Enhancements

- Add notification history storage
- Implement notification categories and priorities
- Add user authentication
- Support for rich notifications with images
- Background sync for offline scenarios
- Push notification analytics and tracking
