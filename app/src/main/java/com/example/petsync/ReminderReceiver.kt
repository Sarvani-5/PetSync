package com.example.petsync.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.petsync.managers.RemindersManager
import com.example.petsync.utils.NotificationUtils
import com.google.firebase.firestore.FirebaseFirestore

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("title") ?: "Pet Reminder"
        val message = intent.getStringExtra("message") ?: "It's time for your pet's scheduled activity."
        val type = intent.getStringExtra("type") ?: "OTHER"

        Log.d("ReminderReceiver", "Received reminder: $title - $message")

        // Show notification
        NotificationUtils.showNotification(context, title, message)

        // Update reminder in Firestore as triggered
        val db = FirebaseFirestore.getInstance()
        db.collection("pet_reminders")
            .document(reminderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Add a "triggered" field, but don't mark as completed yet
                    db.collection("pet_reminders")
                        .document(reminderId)
                        .update("triggered", true)
                        .addOnFailureListener { e ->
                            Log.e("ReminderReceiver", "Error updating reminder", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ReminderReceiver", "Error getting reminder", e)
            }
    }
}