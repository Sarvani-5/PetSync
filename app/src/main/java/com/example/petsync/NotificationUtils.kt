package com.example.petsync.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.petsync.MainActivity
import com.example.petsync.R
import com.example.petsync.ViewRemindersActivity
import com.example.petsync.managers.RemindersManager

object NotificationUtils {

    private const val CHANNEL_ID = "petsync_notification_channel"
    private const val REMINDER_CHANNEL_ID = "petsync_reminder_channel"
    private const val NOTIFICATION_ID = 1001

    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PetSync Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from PetSync app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for notification click
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showReminderNotification(context: Context, reminderId: String, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create reminder notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Pet Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important pet care reminders"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for viewing reminders
        val intent = Intent(context, ViewRemindersActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create action for marking as completed
        val markCompletedIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "MARK_COMPLETED"
            putExtra("reminder_id", reminderId)
        }

        val markCompletedPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 100,
            markCompletedIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification with action
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification) // Create a specific icon for reminders
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "Mark as Done", markCompletedPendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification with unique ID based on reminder ID
        notificationManager.notify(reminderId.hashCode(), notification)
    }

    fun sendSMS(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )

            Log.d("NotificationUtils", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Failed to send SMS: ${e.message}")
            e.printStackTrace()
        }
    }

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        try {
            // Format phone number (remove spaces, add country code if needed)
            val formattedNumber = if (!phoneNumber.startsWith("+")) {
                "+1$phoneNumber" // Default to US code, adjust as needed
            } else {
                phoneNumber
            }

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.setPackage("com.whatsapp")

            // WhatsApp API format: https://api.whatsapp.com/send?phone=PHONE_NUMBER&text=MESSAGE
            intent.putExtra(Intent.EXTRA_TEXT, message)
            intent.putExtra("jid", "$formattedNumber@s.whatsapp.net")

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Failed to send WhatsApp message: ${e.message}")
            e.printStackTrace()

            // Fallback to opening WhatsApp
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${android.net.Uri.encode(message)}"))
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                Log.e("NotificationUtils", "Failed to open WhatsApp: ${e2.message}")
            }
        }
    }
}

// Create this class to handle the notification action
class ReminderActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "MARK_COMPLETED") {
            val reminderId = intent.getStringExtra("reminder_id") ?: return

            // Get RemindersManager and mark as completed
            val remindersManager = RemindersManager(context)
            remindersManager.markReminderAsCompleted(reminderId) { success ->
                if (success) {
                    Log.d("ReminderActionReceiver", "Reminder marked as completed: $reminderId")

                    // Cancel the notification
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(reminderId.hashCode())
                } else {
                    Log.e("ReminderActionReceiver", "Failed to mark reminder as completed")
                }
            }
        }
    }
}