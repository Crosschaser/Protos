package com.example.protosApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.utils.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeActivitySimple : AppCompatActivity() {
    
    private lateinit var api: PushNotificationApi
    
    companion object {
        private const val TAG = "HomeActivitySimple"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "Starting HomeActivity onCreate")
            setContentView(R.layout.activity_home)
            Log.d(TAG, "Layout set successfully")
            
            // Test Retrofit initialization
            Log.d(TAG, "Initializing Retrofit...")
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = retrofit.create(PushNotificationApi::class.java)
            Log.d(TAG, "Retrofit initialized successfully")
            
            // Just initialize basic views without API calls
            val noBirthdaysTextView = findViewById<TextView>(R.id.noBirthdaysTextView)
            val noSpeedUpdatesTextView = findViewById<TextView>(R.id.noSpeedUpdatesTextView)
            
            Log.d(TAG, "Basic views found successfully")
            
            // Test RecyclerView setup
            Log.d(TAG, "Setting up RecyclerViews...")
            val birthdaysRecyclerView = findViewById<RecyclerView>(R.id.birthdaysRecyclerView)
            val speedUpdatesRecyclerView = findViewById<RecyclerView>(R.id.speedUpdatesRecyclerView)
            
            birthdaysRecyclerView.layoutManager = LinearLayoutManager(this)
            speedUpdatesRecyclerView.layoutManager = LinearLayoutManager(this)
            
            Log.d(TAG, "RecyclerViews setup successfully")
            
            // Test adapter initialization with empty data
            Log.d(TAG, "Testing adapter initialization...")
            try {
                val birthdayAdapter = BirthdayAdapter(emptyList())
                val speedUpdateAdapter = SpeedUpdateAdapter(emptyList()) { speedUpdate ->
                    Log.d(TAG, "Speed update clicked: ${speedUpdate.company}")
                }
                
                birthdaysRecyclerView.adapter = birthdayAdapter
                speedUpdatesRecyclerView.adapter = speedUpdateAdapter
                
                Log.d(TAG, "Adapters initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing adapters", e)
                noBirthdaysTextView.text = "Error with adapters: ${e.message}"
                noSpeedUpdatesTextView.text = "Error with adapters: ${e.message}"
                return
            }
            
            // Set simple text
            noBirthdaysTextView.text = "Adapters working - testing basic functionality"
            noSpeedUpdatesTextView.text = "Adapters working - testing basic functionality"
            
            // Simple placeholder action (no buttons in the new layout)
            noBirthdaysTextView.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }
            
            Log.d(TAG, "HomeActivity initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            e.printStackTrace()
        }
    }
}
