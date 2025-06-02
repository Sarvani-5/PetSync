package com.example.petsync.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.petsync.R
import com.example.petsync.models.User
import com.example.petsync.models.UserType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var profileImage: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var userTypeTextView: TextView

    private lateinit var editProfileButton: Button
    private lateinit var changePhotoButton: FloatingActionButton

    private var currentUser: User? = null
    private var profileImageFile: File? = null

    private val CAMERA_PERMISSION_CODE = 100

    // Using Storage Access Framework for gallery access (no permission needed)
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImage.setImageBitmap(bitmap)

                // Save the selected image to local storage
                saveImageToFile(bitmap)
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity result launcher for camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadProfileImageFromFile()
            saveProfileImageToLocalStorage()
        }
    }

    // Permission request launcher for camera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initViews(root)

        // Load user data
        loadUserData()

        // Load profile image from local storage
        loadProfileImageFromFile()

        return root
    }

    private fun initViews(view: View) {
        profileImage = view.findViewById(R.id.profile_image)
        nameTextView = view.findViewById(R.id.profile_name)
        emailTextView = view.findViewById(R.id.profile_email)
        phoneTextView = view.findViewById(R.id.profile_phone)
        addressTextView = view.findViewById(R.id.profile_address)
        userTypeTextView = view.findViewById(R.id.profile_user_type)

        editProfileButton = view.findViewById(R.id.btn_edit_profile)
        changePhotoButton = view.findViewById(R.id.btn_change_photo)

        // Setup listeners
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        changePhotoButton.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        updateUI(currentUser)
                    } else {
                        // Create default user if not exists
                        val email = auth.currentUser?.email ?: ""
                        currentUser = User(
                            id = userId,
                            email = email,
                            name = "New User",
                            userType = UserType.USER
                        )
                        updateUI(currentUser)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUI(user: User?) {
        user?.let {
            nameTextView.text = it.name
            emailTextView.text = it.email
            phoneTextView.text = if (it.phone.isNotEmpty()) it.phone else "No phone number"
            addressTextView.text = if (it.address.isNotEmpty()) it.address else "No address"
            userTypeTextView.text = it.userType.toString()
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null)

        // Initialize dialog views
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_name)
        val phoneEditText = dialogView.findViewById<EditText>(R.id.edit_phone)
        val addressEditText = dialogView.findViewById<EditText>(R.id.edit_address)

        // Prefill with current values
        currentUser?.let {
            nameEditText.setText(it.name)
            phoneEditText.setText(it.phone)
            addressEditText.setText(it.address)
        }

        // Show dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Get updated values
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()
                val address = addressEditText.text.toString().trim()

                // Update user object
                currentUser = currentUser?.copy(
                    name = name,
                    phone = phone,
                    address = address
                )

                // Update UI
                updateUI(currentUser)

                // Save to Firestore
                saveUserData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid

        if (userId != null && currentUser != null) {
            firestore.collection("users").document(userId)
                .set(currentUser!!)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkAndRequestCameraPermission()
                    1 -> openGallery() // No permission needed with Storage Access Framework
                }
            }
            .show()
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Use the permission launcher instead of requestPermissions
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Create file for the image
        try {
            createImageFile()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
            return
        }

        // Continue only if the file was successfully created
        profileImageFile?.let {
            // Use the correctly configured path that matches the file_paths.xml
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "com.example.petsync.fileprovider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureLauncher.launch(intent)
        }
    }

    private fun openGallery() {
        // Use the Storage Access Framework - no permission needed
        pickImageLauncher.launch("image/*")
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
        val imageFileName = "profile_$userId"

        // Use a location that matches what's defined in file_paths.xml
        val storageDir = File(requireContext().filesDir, "profile_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val image = File(storageDir, "$imageFileName.jpg")

        if (image.exists()) {
            image.delete()
        }
        image.createNewFile()

        profileImageFile = image
        return image
    }

    private fun saveImageToFile(bitmap: Bitmap) {
        try {
            // Create a file to save the image
            val file = createImageFile()
            val stream = FileOutputStream(file)

            // Compress the bitmap and save
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImageFromFile() {
        val userId = auth.currentUser?.uid ?: return
        val imageFileName = "profile_$userId.jpg"

        // Use the same directory path as in createImageFile()
        val storageDir = File(requireContext().filesDir, "profile_images")
        val file = File(storageDir, imageFileName)

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            profileImage.setImageBitmap(bitmap)
        } else {
            // Set default profile image
            profileImage.setImageResource(R.drawable.default_profile)
        }
    }

    private fun saveProfileImageToLocalStorage() {
        // The image is already saved to file when taking a picture
        // We just need to update the UI
        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
    }
}