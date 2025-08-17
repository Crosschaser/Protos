package com.example.protosApp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SoldOutActivity : AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var contentTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sold_out)

        initializeViews()
        handleIntent()
    }

    private fun initializeViews() {
        titleTextView = findViewById(R.id.titleTextView)
        contentTextView = findViewById(R.id.contentTextView)
    }

    private fun handleIntent() {
        val title = intent.getStringExtra("title") ?: "Sold Out Alert"
        val body = intent.getStringExtra("body") ?: "An item you're interested in is now sold out."
        
        titleTextView.text = title
        contentTextView.text = body
    }
}
