package com.example.petsync.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object ImageUtils {

    private const val TAG = "ImageUtils"
    private const val APP_FOLDER = "PetSync"
    private const val IMAGE_FOLDER = "pet_images"

    /**
     * Saves an image from URI to app's local storage
     */
    fun saveImageToLocal(context: Context, imageUri: Uri): String? {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            val directory = File(context.filesDir, "$APP_FOLDER/$IMAGE_FOLDER")

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "pet_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)

            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                return file.absolutePath
            } catch (e: IOException) {
                Log.e(TAG, "Error saving image: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
        }

        return null
    }

    /**
     * Gets a content URI for a file using FileProvider
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Deletes an image file
     */
    fun deleteImage(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}