package com.example.protosApp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.navigation.NavigationView
import com.example.protosApp.data.repository.PushNotificationRepository
import com.example.protosApp.service.WebSocketService
import com.example.protosApp.manager.NotificationPollingManager
import com.example.protosApp.utils.AppConfig

class MainActivity : AppCompatActivity() {
    private lateinit var pushNotificationRepository: PushNotificationRepository
    private lateinit var cityEditText: EditText
    private lateinit var speedUpdatesSwitch: Switch
    private lateinit var eventRemindersSwitch: Switch
    private lateinit var soldOutAlertsSwitch: Switch
    private lateinit var favouriteUpdatesSwitch: Switch
    private lateinit var registerButton: Button
    private lateinit var refreshDataButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    
    private var webSocketService: WebSocketService? = null
    private var isServiceBound = false
    private lateinit var pollingManager: NotificationPollingManager
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webSocketService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRepository()
        setupPollingManager()
        requestNotificationPermission()
        setupClickListeners()
        setupDrawer()
        startAndBindWebSocketService()
        
        // Try to establish WebSocket connection on startup if device token exists
        connectWebSocketOnStartup()
    }

    private fun initializeViews() {
        cityEditText = findViewById(R.id.cityEditText)
        speedUpdatesSwitch = findViewById(R.id.speedUpdatesSwitch)
        eventRemindersSwitch = findViewById(R.id.eventRemindersSwitch)
        soldOutAlertsSwitch = findViewById(R.id.soldOutAlertsSwitch)
        favouriteUpdatesSwitch = findViewById(R.id.favouriteUpdatesSwitch)
        registerButton = findViewById(R.id.registerButton)
        refreshDataButton = findViewById(R.id.refreshDataButton)
        statusTextView = findViewById(R.id.statusTextView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
    }

    private fun setupRepository() {
        pushNotificationRepository = PushNotificationRepository(this)
    }

    private fun setupPollingManager() {
        pollingManager = NotificationPollingManager(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            registerDevice()
        }
        refreshDataButton.setOnClickListener {
            // Navigate to Home and trigger a data refresh
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("refresh", true)
            startActivity(intent)
        }
    }

    private fun setupDrawer() {
        // Use existing ActionBar from theme
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_birthdays -> {
                    startActivity(Intent(this, BirthdayManagementActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    // Already here; just close the drawer
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (::toggle.isInitialized && toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun registerDevice() {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val preferredCity = cityEditText.text.toString().takeIf { it.isNotBlank() }

        Log.d("MainActivity", "Starting device registration with deviceId: $deviceId")
        Log.d("MainActivity", "Preferred city: $preferredCity")
        Log.d("MainActivity", "Speed updates: ${speedUpdatesSwitch.isChecked}")

        try {
            val deviceToken = pushNotificationRepository.registerDevice(
                deviceId = deviceId,
                preferredCity = preferredCity,
                subscribeToSpeedUpdates = speedUpdatesSwitch.isChecked,
                subscribeToEventReminders = eventRemindersSwitch.isChecked,
                subscribeToSoldOutAlerts = soldOutAlertsSwitch.isChecked,
                subscribeToFavouriteUpdates = favouriteUpdatesSwitch.isChecked
            )

            Log.d("MainActivity", "Generated device token: $deviceToken")
            statusTextView.text = "Device registered successfully!\nToken: ${deviceToken.take(8)}...\nConnecting to WebSocket..."
            Toast.makeText(this, "Device registered successfully!", Toast.LENGTH_SHORT).show()
            
            // Connect to WebSocket after successful registration
            connectWebSocketAfterRegistration(deviceToken)
        } catch (e: Exception) {
            Log.e("MainActivity", "Registration failed", e)
            statusTextView.text = "Registration failed: ${e.message}"
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startAndBindWebSocketService() {
        val intent = Intent(this, WebSocketService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun connectWebSocketOnStartup() {
        // Check if device token exists in SharedPreferences
        val sharedPrefs = getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val deviceToken = sharedPrefs.getString(AppConfig.DEVICE_TOKEN_KEY, null)
        
        if (deviceToken != null) {
            Log.d("MainActivity", "Found existing device token on startup: ${deviceToken.take(8)}...")
            statusTextView.text = "Device already registered!\nToken: ${deviceToken.take(8)}...\nConnecting to WebSocket..."
            
            // Wait for service to be bound before connecting
            android.os.Handler(mainLooper).postDelayed({
                if (isServiceBound) {
                    webSocketService?.connectWebSocket(deviceToken)
                    Log.d("MainActivity", "WebSocket connection initiated on startup")
                } else {
                    // Fallback to polling if WebSocket service not available
                    Log.d("MainActivity", "WebSocket service not bound, starting polling fallback")
                    pollingManager.startPeriodicPolling()
                }
            }, 1000) // Wait 1 second for service binding
        } else {
            Log.d("MainActivity", "No device token found on startup")
            statusTextView.text = "Device not registered. Please register to receive notifications."
        }
    }

    private fun connectWebSocketAfterRegistration(deviceToken: String) {
        Log.d("MainActivity", "Connecting WebSocket after registration with token: ${deviceToken.take(8)}...")
        
        // Ensure service is bound before connecting
        if (isServiceBound) {
            webSocketService?.connectWebSocket(deviceToken)
            statusTextView.text = "Device registered successfully!\nToken: ${deviceToken.take(8)}...\nWebSocket connected!"
        } else {
            // Wait for service to be bound
            android.os.Handler(mainLooper).postDelayed({
                if (isServiceBound) {
                    webSocketService?.connectWebSocket(deviceToken)
                    statusTextView.text = "Device registered successfully!\nToken: ${deviceToken.take(8)}...\nWebSocket connected!"
                    Log.d("MainActivity", "WebSocket connection established after registration")
                }
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconnect WebSocket when app resumes
        val sharedPrefs = getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val deviceToken = sharedPrefs.getString(AppConfig.DEVICE_TOKEN_KEY, null)
        
        if (deviceToken != null && isServiceBound) {
            Log.d("MainActivity", "App resumed, reconnecting WebSocket")
            webSocketService?.connectWebSocket(deviceToken)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't unbind service to keep it running in background
        // if (isServiceBound) {
        //     unbindService(serviceConnection)
        //     isServiceBound = false
        // }
        
        // Release resources held by the repository (Timer/WebSocket)
        if (::pushNotificationRepository.isInitialized) {
            pushNotificationRepository.dispose()
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
