package com.example.protosApp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SpeedUpdateActivity : AppCompatActivity() {
    private lateinit var speedUpdateIdTextView: TextView
    private lateinit var cityTextView: TextView
    private lateinit var contentTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed_update)

        initializeViews()
        handleIntent()
    }

    private fun initializeViews() {
        speedUpdateIdTextView = findViewById(R.id.speedUpdateIdTextView)
        cityTextView = findViewById(R.id.cityTextView)
        contentTextView = findViewById(R.id.contentTextView)
    }

    private fun handleIntent() {
        val speedUpdateId = intent.getIntExtra("speedUpdateId", -1)
        val city = intent.getStringExtra("city")
        val company = intent.getStringExtra("company")

        speedUpdateIdTextView.text = "Speed Update ID: $speedUpdateId"
        cityTextView.text = "City: ${city ?: "Unknown"}"
        contentTextView.text = "Speed update notification received for ${company ?: "unknown company"} in ${city ?: "unknown city"}."
    }
}
