package com.example.protosApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.data.models.BirthdayResponse
import com.example.protosApp.data.models.SpeedUpdateResponse
import com.example.protosApp.data.models.BirthdayCrud
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.protosApp.utils.AppConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class HomeActivity : AppCompatActivity() {
    
    private lateinit var birthdaysRecyclerView: RecyclerView
    private lateinit var speedUpdatesRecyclerView: RecyclerView
    private lateinit var noBirthdaysTextView: TextView
    private lateinit var noSpeedUpdatesTextView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    
    private lateinit var api: PushNotificationApi
    private var birthdayCrudAdapter: BirthdayCrudAdapter? = null
    private lateinit var speedUpdateAdapter: SpeedUpdateAdapter
    
    companion object {
        private const val TAG = "HomeActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "Starting HomeActivity onCreate")
            setContentView(R.layout.activity_home)
            Log.d(TAG, "Layout set successfully")
            
            // Initialize Retrofit API client
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = retrofit.create(PushNotificationApi::class.java)
            Log.d(TAG, "Retrofit initialized successfully")
            
            initializeViews()
            setupRecyclerViews()
            setupClickListeners()
            setupSwipeRefresh()
            
            // Auto-load data on startup
            loadData()
            
            Log.d(TAG, "HomeActivity initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeViews() {
        birthdaysRecyclerView = findViewById(R.id.birthdaysRecyclerView)
        speedUpdatesRecyclerView = findViewById(R.id.speedUpdatesRecyclerView)
        noBirthdaysTextView = findViewById(R.id.noBirthdaysTextView)
        noSpeedUpdatesTextView = findViewById(R.id.noSpeedUpdatesTextView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
    }
    
    private fun setupRecyclerViews() {
        birthdaysRecyclerView.layoutManager = LinearLayoutManager(this)
        speedUpdatesRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }
    
    private fun showInitialState() {
        // Show initial empty state
        noBirthdaysTextView.text = "Ready to load birthdays - click refresh to test API"
        noBirthdaysTextView.visibility = View.VISIBLE
        birthdaysRecyclerView.visibility = View.GONE
        
        noSpeedUpdatesTextView.text = "Ready to load speed updates - click refresh to test API"
        noSpeedUpdatesTextView.visibility = View.VISIBLE
        speedUpdatesRecyclerView.visibility = View.GONE
    }
    
    private fun loadData() {
        Log.d(TAG, "Loading data from API")
        swipeRefreshLayout.isRefreshing = true
        try {
            loadBirthdays()
            loadSpeedUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadData", e)
            showBirthdaysError("Error loading data: ${e.message}")
            showSpeedUpdatesError("Error loading data: ${e.message}")
        }
    }
    
    private fun loadBirthdays() {
        // Show loading state
        noBirthdaysTextView.text = "Loading birthdays..."
        noBirthdaysTextView.visibility = View.VISIBLE
        birthdaysRecyclerView.visibility = View.GONE
        
        try {
            api.getUpcomingBirthdays(7).enqueue(object : Callback<BirthdayResponse> {
                override fun onResponse(call: Call<BirthdayResponse>, response: Response<BirthdayResponse>) {
                    try {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val birthdays = response.body()?.data ?: emptyList()
                            updateBirthdaysUI(birthdays)
                        } else {
                            showBirthdaysError("Failed to load birthdays (${response.code()})")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing birthday response", e)
                        showBirthdaysError("Error processing birthday data")
                    }
                }

                override fun onFailure(call: Call<BirthdayResponse>, t: Throwable) {
                    showBirthdaysError("Network error: ${t.message}")
                    Log.e(TAG, "Error loading birthdays", t)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadBirthdays", e)
            showBirthdaysError("Error loading birthdays")
        }
    }
    
    private fun loadSpeedUpdates() {
        // Show loading state
        noSpeedUpdatesTextView.text = "Loading speed updates..."
        noSpeedUpdatesTextView.visibility = View.VISIBLE
        speedUpdatesRecyclerView.visibility = View.GONE
        
        try {
            api.getRecentSpeedUpdates(limit = 10, hours = 24).enqueue(object : Callback<SpeedUpdateResponse> {
                override fun onResponse(call: Call<SpeedUpdateResponse>, response: Response<SpeedUpdateResponse>) {
                    try {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val speedUpdates = response.body()?.data ?: emptyList()
                            updateSpeedUpdatesUI(speedUpdates)
                        } else {
                            showSpeedUpdatesError("Failed to load speed updates (${response.code()})")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing speed update response", e)
                        showSpeedUpdatesError("Error processing speed update data")
                    }
                }

                override fun onFailure(call: Call<SpeedUpdateResponse>, t: Throwable) {
                    showSpeedUpdatesError("Network error: ${t.message}")
                    Log.e(TAG, "Error loading speed updates", t)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadSpeedUpdates", e)
            showSpeedUpdatesError("Error loading speed updates")
        }
    }
    

    
    private fun setupClickListeners() {
        // Setup drawer toggle (hamburger) with existing ActionBar from theme
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Navigation item clicks
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_birthdays -> {
                    startActivity(Intent(this, BirthdayManagementActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    Log.d(TAG, "Drawer: Settings clicked")
                    startActivity(Intent(this, MainActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
    
    private fun openSpeedUpdateDetails(speedUpdate: SpeedUpdate) {
        val intent = Intent(this, SpeedUpdateActivity::class.java).apply {
            putExtra("speedUpdateId", speedUpdate.id)
            putExtra("city", speedUpdate.city)
            putExtra("company", speedUpdate.company)
        }
        startActivity(intent)
    }
    
    private fun updateBirthdaysUI(birthdays: List<Birthday>) {
        runOnUiThread {
            try {
                Log.d(TAG, "Updating birthdays UI with ${birthdays.size} items")
                if (birthdays.isEmpty()) {
                    noBirthdaysTextView.text = "No upcoming birthdays in the next 7 days"
                    noBirthdaysTextView.visibility = View.VISIBLE
                    birthdaysRecyclerView.visibility = View.GONE
                } else {
                    noBirthdaysTextView.visibility = View.GONE
                    birthdaysRecyclerView.visibility = View.VISIBLE
                    
                    // Convert Birthday items to BirthdayCrud format for consistent UI
                    val crudItems = birthdays.map { b ->
                        val dateStr = if (b.date != null) {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(b.date)
                        } else ""
                        BirthdayCrud(
                            id = b.id,
                            name = b.name,
                            birthDate = dateStr,
                            userId = null
                        )
                    }
                    
                    birthdayCrudAdapter = BirthdayCrudAdapter(crudItems.toMutableList(), onClick = { item ->
                        // On home screen, clicking opens the management screen for editing
                        val intent = Intent(this@HomeActivity, BirthdayManagementActivity::class.java)
                        startActivity(intent)
                    }, onDelete = { item ->
                        // Delete not allowed on home screen - this shouldn't be called
                    }, showDelete = false)
                    
                    birthdaysRecyclerView.adapter = birthdayCrudAdapter
                    Log.d(TAG, "Birthday CRUD adapter set successfully")
                }
                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e(TAG, "Error updating birthdays UI", e)
                noBirthdaysTextView.text = "Error displaying birthdays: ${e.message}"
                noBirthdaysTextView.visibility = View.VISIBLE
                birthdaysRecyclerView.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateSpeedUpdatesUI(speedUpdates: List<SpeedUpdate>) {
        runOnUiThread {
            try {
                Log.d(TAG, "Updating speed updates UI with ${speedUpdates.size} items")
                if (speedUpdates.isEmpty()) {
                    noSpeedUpdatesTextView.text = "No recent speed updates"
                    noSpeedUpdatesTextView.visibility = View.VISIBLE
                    speedUpdatesRecyclerView.visibility = View.GONE
                } else {
                    noSpeedUpdatesTextView.visibility = View.GONE
                    speedUpdatesRecyclerView.visibility = View.VISIBLE
                    speedUpdateAdapter = SpeedUpdateAdapter(speedUpdates) { speedUpdate ->
                        openSpeedUpdateDetails(speedUpdate)
                    }
                    speedUpdatesRecyclerView.adapter = speedUpdateAdapter
                    Log.d(TAG, "Speed update adapter set successfully")
                }
                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e(TAG, "Error updating speed updates UI", e)
                noSpeedUpdatesTextView.text = "Error displaying speed updates: ${e.message}"
                noSpeedUpdatesTextView.visibility = View.VISIBLE
                speedUpdatesRecyclerView.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun showBirthdaysError(message: String) {
        runOnUiThread {
            noBirthdaysTextView.text = message
            noBirthdaysTextView.visibility = View.VISIBLE
            birthdaysRecyclerView.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpeedUpdatesError(message: String) {
        runOnUiThread {
            noSpeedUpdatesTextView.text = message
            noSpeedUpdatesTextView.visibility = View.VISIBLE
            speedUpdatesRecyclerView.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getDateInDays(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.time
    }
    
    private fun getDateHoursAgo(hours: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -hours)
        return calendar.time
    }
}
