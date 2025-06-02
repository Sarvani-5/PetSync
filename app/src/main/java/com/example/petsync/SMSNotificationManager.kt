package com.example.petsync.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.example.petsync.models.User

/**
 * Enhanced SMS notification manager with delivery confirmation
 */
class SMSNotificationManager {
    companion object {
        private const val TAG = "SMSNotificationManager"
        private const val SMS_SENT_ACTION = "com.example.petsync.SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "com.example.petsync.SMS_DELIVERED"

        /**
         * Send SMS notification for product added to cart with delivery tracking
         */
        fun sendProductAddedNotification(context: Context, user: User, productName: String) {
            if (!PermissionHandler.hasSmsPermission(context)) {
                Log.e(TAG, "SMS permission not granted")
                Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val message = "PetSync: Hi ${user.name}, you've added '$productName' to your cart!"
                sendSmsWithDeliveryTracking(context, user.phone, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send product notification SMS", e)
                Toast.makeText(
                    context,
                    "Failed to send SMS notification: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        /**
         * Send SMS notification for order confirmation with delivery tracking
         */
        fun sendOrderConfirmationNotification(context: Context, user: User, orderTotal: Double) {
            if (!PermissionHandler.hasSmsPermission(context)) {
                Log.e(TAG, "SMS permission not granted")
                Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val message = "PetSync: Hi ${user.name}, your order for ₹${String.format("%.2f", orderTotal)} " +
                        "has been confirmed! It will be delivered to ${user.address}."
                sendSmsWithDeliveryTracking(context, user.phone, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send order confirmation SMS", e)
                Toast.makeText(
                    context,
                    "Failed to send confirmation SMS: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        /**
         * Send SMS with delivery tracking
         */
        private fun sendSmsWithDeliveryTracking(context: Context, phoneNumber: String, message: String) {
            // Check if phone number is valid (basic validation)
            if (phoneNumber.isBlank() || phoneNumber.length < 10) {
                Log.e(TAG, "Invalid phone number: $phoneNumber")
                Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                return
            }

            // Register for SMS sent status
            val sentPI = PendingIntent.getBroadcast(
                context,
                0,
                Intent(SMS_SENT_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )

            // Register for SMS delivered status
            val deliveredPI = PendingIntent.getBroadcast(
                context,
                0,
                Intent(SMS_DELIVERED_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )

            // Register receivers for tracking SMS status
            registerSmsStatusReceivers(context)

            // Send the SMS
            try {
                val smsManager = SmsManager.getDefault()

                // Check if message needs to be divided into parts
                if (message.length > 160) {
                    val messageParts = smsManager.divideMessage(message)
                    val sentIntents = ArrayList<PendingIntent>(messageParts.size)
                    val deliveredIntents = ArrayList<PendingIntent>(messageParts.size)

                    for (i in 0 until messageParts.size) {
                        sentIntents.add(sentPI)
                        deliveredIntents.add(deliveredPI)
                    }

                    smsManager.sendMultipartTextMessage(
                        phoneNumber,
                        null,
                        messageParts,
                        sentIntents,
                        deliveredIntents
                    )
                } else {
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        sentPI,
                        deliveredPI
                    )
                }

                Log.d(TAG, "SMS sent to $phoneNumber: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS", e)
                Toast.makeText(
                    context,
                    "SMS sending failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Unregister receivers if sending fails
                try {
                    context.unregisterReceiver(smsSentReceiver)
                    context.unregisterReceiver(smsDeliveredReceiver)
                } catch (ignored: Exception) {}
            }
        }

        // Receiver for SMS sent status
        private val smsSentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "SMS sent successfully")
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.e(TAG, "Generic failure")
                        Toast.makeText(context, "SMS not sent: Generic failure", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.e(TAG, "No service")
                        Toast.makeText(context, "SMS not sent: No service", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.e(TAG, "Null PDU")
                        Toast.makeText(context, "SMS not sent: Null PDU", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.e(TAG, "Radio off")
                        Toast.makeText(context, "SMS not sent: Radio off", Toast.LENGTH_SHORT).show()
                    }
                }

                // Unregister after receiving
                try {
                    context.unregisterReceiver(this)
                } catch (ignored: Exception) {}
            }
        }

        // Receiver for SMS delivered status
        private val smsDeliveredReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "SMS delivered successfully")
                    }
                    Activity.RESULT_CANCELED -> {
                        Log.e(TAG, "SMS not delivered")
                        Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show()
                    }
                }

                // Unregister after receiving
                try {
                    context.unregisterReceiver(this)
                } catch (ignored: Exception) {}
            }
        }

        /**
         * Register receivers for SMS status tracking
         */
        private fun registerSmsStatusReceivers(context: Context) {
            try {
                context.registerReceiver(
                    smsSentReceiver,
                    IntentFilter(SMS_SENT_ACTION),
                    Context.RECEIVER_EXPORTED
                )

                context.registerReceiver(
                    smsDeliveredReceiver,
                    IntentFilter(SMS_DELIVERED_ACTION),
                    Context.RECEIVER_EXPORTED
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register SMS receivers", e)
            }
        }
    }
}