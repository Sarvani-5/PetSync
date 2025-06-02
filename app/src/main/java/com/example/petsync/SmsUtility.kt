package com.example.petsync.ui.shops.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling SMS functionality
 * In a real app, you would implement proper SMS permission handling and actual SMS sending
 */
class SmsUtility {
    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101

        /**
         * Check if the app has SMS permission and request if not granted
         */
        fun checkSmsPermission(activity: Activity): Boolean {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    SMS_PERMISSION_REQUEST_CODE
                )
                return false
            }
            return true
        }

        /**
         * Send an SMS notification
         * This is a mock implementation for demo purposes
         */
        fun sendSmsNotification(context: Context, phoneNumber: String, message: String) {
            try {
                // In a real app, you would use this to actually send SMS
                // val smsManager = SmsManager.getDefault()
                // smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                // For now, just log and show a toast for demo purposes
                println("SMS would be sent to $phoneNumber: $message")
                Toast.makeText(
                    context,
                    "SMS notification would be sent: $message",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "SMS sending failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
}