package com.example.petsync.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petsync.R
import com.example.petsync.models.PetReminder
import java.text.SimpleDateFormat
import java.util.*

class RemindersAdapter(
    private val reminders: List<PetReminder>,
    private val onReminderCompleted: (PetReminder) -> Unit,
    private val onReminderDeleted: (PetReminder) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)
    }

    override fun getItemCount(): Int = reminders.size

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvReminderTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvReminderMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvReminderTime)
        private val tvType: TextView = itemView.findViewById(R.id.tvReminderType)
        private val tvPetName: TextView = itemView.findViewById(R.id.tvPetName)
        private val btnComplete: ImageButton = itemView.findViewById(R.id.btnComplete)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(reminder: PetReminder) {
            tvTitle.text = reminder.title
            tvMessage.text = reminder.message
            tvTime.text = dateFormat.format(Date(reminder.reminderTime))
            tvType.text = reminder.reminderType.displayName

            // Set pet name or "General" if it's a general reminder
            if (reminder.petId == null) {
                tvPetName.text = "General"
            } else {
                // Ideally, we would fetch the pet's name here
                // For now, just show "Pet" as a placeholder
                tvPetName.text = "Pet" // You can replace this with actual pet name
            }

            // Handle completed reminders
            if (reminder.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvMessage.paintFlags = tvMessage.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                btnComplete.isEnabled = false
                btnComplete.alpha = 0.5f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvMessage.paintFlags = tvMessage.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                btnComplete.isEnabled = true
                btnComplete.alpha = 1.0f
            }

            // Setup click listeners
            btnComplete.setOnClickListener {
                if (!reminder.isCompleted) {
                    onReminderCompleted(reminder)
                }
            }

            btnDelete.setOnClickListener {
                onReminderDeleted(reminder)
            }
        }
    }
}