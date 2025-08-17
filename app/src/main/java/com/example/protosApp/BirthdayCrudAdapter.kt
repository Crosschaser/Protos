package com.example.protosApp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.protosApp.data.models.BirthdayCrud
import java.text.SimpleDateFormat
import java.util.Locale

class BirthdayCrudAdapter(
    private var items: MutableList<BirthdayCrud>,
    private val onClick: (BirthdayCrud) -> Unit,
    private val onDelete: (BirthdayCrud) -> Unit,
    private val showDelete: Boolean = true
) : RecyclerView.Adapter<BirthdayCrudAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.nameText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val idText: TextView = view.findViewById(R.id.idText)
        val deleteButton: TextView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_birthday_crud, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameText.text = item.name
        holder.dateText.text = formatDate(item.birthDate)
        holder.idText.text = item.id?.toString() ?: "-"
        holder.itemView.setOnClickListener { onClick(item) }

        // Show delete button only if showDelete is true
        if (showDelete) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener { onDelete(item) }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): BirthdayCrud = items[position]

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeById(id: Int) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) removeAt(idx)
    }

    fun setData(newItems: List<BirthdayCrud>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    private fun formatDate(serverDate: String): String {
        return try {
            val base = serverDate.substringBefore('T').let { s -> if (s.length >= 10) s.substring(0, 10) else s }
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = inFmt.parse(base)
            val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            outFmt.format(parsed!!)
        } catch (e: Exception) {
            serverDate
        }
    }
}
