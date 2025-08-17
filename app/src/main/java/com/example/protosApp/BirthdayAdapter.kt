package com.example.protosApp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BirthdayAdapter(
    private val birthdays: List<Birthday>,
    private val onClick: ((Birthday) -> Unit)? = null
) : RecyclerView.Adapter<BirthdayAdapter.BirthdayViewHolder>() {

    class BirthdayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val detailsTextView: TextView = view.findViewById(R.id.detailsTextView)
        val daysTextView: TextView = view.findViewById(R.id.daysTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_birthday, parent, false)
        return BirthdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirthdayViewHolder, position: Int) {
        val birthday = birthdays[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        holder.nameTextView.text = birthday.name
        
        // Handle null date safely
        val dateText = if (birthday.date != null) {
            dateFormat.format(birthday.date)
        } else {
            "Unknown date"
        }
        holder.detailsTextView.text = "Turning ${birthday.age} on $dateText"
        
        when (birthday.daysUntil) {
            0 -> holder.daysTextView.text = "TODAY!"
            1 -> holder.daysTextView.text = "Tomorrow"
            else -> holder.daysTextView.text = "${birthday.daysUntil} days"
        }

        holder.itemView.setOnClickListener {
            onClick?.invoke(birthday)
        }
    }

    override fun getItemCount() = birthdays.size
}
