package com.example.petsync.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Utility class to handle runtime permissions
 */
class PermissionHandler {
    companion object {
        const val SMS_PERMISSION_REQUEST_CODE = 101

        /**
         * Check if SMS permission is granted
         */
        fun hasSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Request SMS permission from Activity
         */
        fun requestSmsPermission(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }

        /**
         * Request SMS permission from Fragment
         */
        fun requestSmsPermission(fragment: Fragment) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }
}