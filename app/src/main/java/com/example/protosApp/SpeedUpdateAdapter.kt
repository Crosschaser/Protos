package com.example.protosApp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SpeedUpdateAdapter(
    private val speedUpdates: List<SpeedUpdate>,
    private val onItemClick: (SpeedUpdate) -> Unit
) : RecyclerView.Adapter<SpeedUpdateAdapter.SpeedUpdateViewHolder>() {

    class SpeedUpdateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val companyTextView: TextView = view.findViewById(R.id.companyTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val websiteTextView: TextView = view.findViewById(R.id.websiteTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeedUpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_update, parent, false)
        return SpeedUpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeedUpdateViewHolder, position: Int) {
        val speedUpdate = speedUpdates[position]
        val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        
        // Company - city format
        holder.companyTextView.text = "${speedUpdate.company} - ${speedUpdate.city}"
        
        // Event date
        holder.timestampTextView.text = timeFormat.format(speedUpdate.timestamp)
        
        // Website link
        holder.websiteTextView.text = "website"
        holder.websiteTextView.setOnClickListener {
            val website = speedUpdate.website
            if (!website.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                holder.itemView.context.startActivity(intent)
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(speedUpdate)
        }
    }

    override fun getItemCount() = speedUpdates.size
}
