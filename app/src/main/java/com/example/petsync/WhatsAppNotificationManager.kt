package com.example.petsync.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.petsync.models.User

/**
 * Utility class for sending WhatsApp notifications
 */
object WhatsAppNotificationManager {
    private const val TAG = "WhatsAppNotificationManager"

    /**
     * Send product added notification via WhatsApp
     */
    fun sendProductAddedNotification(context: Context, user: User, productName: String) {
        val phone = formatPhoneNumber(user.phone)
        val message = "Hi ${user.name}, you've added $productName to your cart in PetSync!"
        sendWhatsAppMessage(context, phone, message)
    }

    /**
     * Send order confirmation notification via WhatsApp
     */
    fun sendOrderConfirmationNotification(context: Context, user: User, orderTotal: Double, paymentMethod: String, orderItems: String) {
        val phone = formatPhoneNumber(user.phone)
        val message = """
            🛍️ *Order Confirmation* 🛍️
            
            Hi ${user.name}, thank you for your order!
            
            *Order Details:*
            $orderItems
            
            *Total: ₹${String.format("%.2f", orderTotal)}*
            *Payment Method: $paymentMethod*
            
            Your order will be delivered to:
            ${user.address}
            
            Thank you for shopping with PetSync!
        """.trimIndent()

        sendWhatsAppMessage(context, phone, message)
    }

    /**
     * Format phone number to ensure it has the country code
     */
    private fun formatPhoneNumber(phone: String): String {
        // If phone number doesn't start with +, assume it's an Indian number and add +91
        return if (phone.startsWith("+")) {
            phone
        } else if (phone.length == 10) {
            "+91$phone"
        } else {
            phone // Return as is if it doesn't match expected format
        }
    }

    /**
     * Send WhatsApp message using Intent
     */
    private fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.setPackage("com.whatsapp")

            // Add FLAG_ACTIVITY_NEW_TASK when starting from a non-activity context
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e(TAG, "WhatsApp not installed")
                // Can't show Toast from here if context is not an Activity
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp", e)
        }
    }
}