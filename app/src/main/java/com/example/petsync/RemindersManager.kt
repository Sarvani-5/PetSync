package com.example.petsync.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.petsync.models.PetReminder
import com.example.petsync.receivers.ReminderReceiver
import com.example.petsync.utils.NotificationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class RemindersManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun saveReminder(reminder: PetReminder, callback: (Boolean) -> Unit) {
        // Save to Firestore
        db.collection("pet_reminders")
            .document(reminder.id)
            .set(reminder.toMap())
            .addOnSuccessListener {
                // Schedule notification
                scheduleReminder(reminder)
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error saving reminder", e)
                callback(false)
            }
    }

    fun updateReminder(reminder: PetReminder, callback: (Boolean) -> Unit) {
        // Update in Firestore
        db.collection("pet_reminders")
            .document(reminder.id)
            .update(reminder.toMap())
            .addOnSuccessListener {
                // Reschedule notification
                cancelReminder(reminder.id)
                if (!reminder.isCompleted) {
                    scheduleReminder(reminder)
                }
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error updating reminder", e)
                callback(false)
            }
    }

    fun deleteReminder(reminderId: String, callback: (Boolean) -> Unit) {
        // Delete from Firestore
        db.collection("pet_reminders")
            .document(reminderId)
            .delete()
            .addOnSuccessListener {
                // Cancel scheduled notification
                cancelReminder(reminderId)
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error deleting reminder", e)
                callback(false)
            }
    }

    fun getUserReminders(userId: String, callback: (List<PetReminder>) -> Unit) {
        db.collection("pet_reminders")
            .whereEqualTo("userId", userId)
            .orderBy("reminderTime")
            .get()
            .addOnSuccessListener { documents ->
                val reminders = documents.mapNotNull { doc ->
                    try {
                        PetReminder.fromMap(doc.data)
                    } catch (e: Exception) {
                        Log.e("RemindersManager", "Error parsing reminder", e)
                        null
                    }
                }
                callback(reminders)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error getting reminders", e)
                callback(emptyList())
            }
    }

    fun getPetReminders(petId: String, callback: (List<PetReminder>) -> Unit) {
        db.collection("pet_reminders")
            .whereEqualTo("petId", petId)
            .orderBy("reminderTime")
            .get()
            .addOnSuccessListener { documents ->
                val reminders = documents.mapNotNull { doc ->
                    try {
                        PetReminder.fromMap(doc.data)
                    } catch (e: Exception) {
                        Log.e("RemindersManager", "Error parsing reminder", e)
                        null
                    }
                }
                callback(reminders)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error getting reminders", e)
                callback(emptyList())
            }
    }

    // New method to get general reminders (ones with null petId)
    fun getGeneralReminders(userId: String, callback: (List<PetReminder>) -> Unit) {
        db.collection("pet_reminders")
            .whereEqualTo("userId", userId)
            .whereEqualTo("petId", null)  // Get reminders with null petId (general reminders)
            .orderBy("reminderTime")
            .get()
            .addOnSuccessListener { documents ->
                val reminders = documents.mapNotNull { doc ->
                    try {
                        PetReminder.fromMap(doc.data)
                    } catch (e: Exception) {
                        Log.e("RemindersManager", "Error parsing reminder", e)
                        null
                    }
                }
                callback(reminders)
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error getting general reminders", e)
                callback(emptyList())
            }
    }

    private fun scheduleReminder(reminder: PetReminder) {
        // Skip if reminder time has already passed
        if (reminder.reminderTime <= System.currentTimeMillis()) {
            Log.w("RemindersManager", "Skipping past reminder: ${reminder.title}")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("message", reminder.message)
            putExtra("type", reminder.reminderType.name)
            putExtra("pet_id", reminder.petId)  // Can be null for general reminders
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            flags
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.reminderTime,
                pendingIntent
            )
        }

        Log.d("RemindersManager", "Scheduled reminder: ${reminder.title} for ${Date(reminder.reminderTime)}")
    }

    private fun cancelReminder(reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            flags
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.d("RemindersManager", "Cancelled reminder: $reminderId")
    }

    fun rescheduleAllReminders(userId: String) {
        getUserReminders(userId) { reminders ->
            for (reminder in reminders) {
                if (!reminder.isCompleted && reminder.reminderTime > System.currentTimeMillis()) {
                    scheduleReminder(reminder)
                }
            }
        }
    }

    fun markReminderAsCompleted(reminderId: String, callback: (Boolean) -> Unit) {
        db.collection("pet_reminders")
            .document(reminderId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    try {
                        val reminder = PetReminder.fromMap(doc.data!!)
                        val updatedReminder = reminder.copy(isCompleted = true)
                        updateReminder(updatedReminder, callback)
                    } catch (e: Exception) {
                        Log.e("RemindersManager", "Error parsing reminder", e)
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RemindersManager", "Error marking reminder as completed", e)
                callback(false)
            }
    }
}