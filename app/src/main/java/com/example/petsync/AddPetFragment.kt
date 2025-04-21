package com.example.petsync.ui.pets

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.petsync.databinding.FragmentAddPetBinding
import com.example.petsync.models.Pet
import com.example.petsync.models.PetStatus
import com.example.petsync.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

class AddPetFragment : Fragment() {

    private var _binding: FragmentAddPetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PetsViewModel
    private lateinit var auth: FirebaseAuth

    private val selectedImages = mutableListOf<Uri>()
    private val localImagePaths = mutableListOf<String>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImages.add(uri)
                updateImagePreview()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[PetsViewModel::class.java]
        _binding = FragmentAddPetBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Setup image picker
        binding.btnAddImage.setOnClickListener {
            openImagePicker()
        }

        // Setup save button
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveImagesToLocal()
            }
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun updateImagePreview() {
        // Update the image preview count
        binding.tvImageCount.text = "${selectedImages.size} images selected"
        binding.tvImageCount.visibility = View.VISIBLE
    }

    private fun validateInputs(): Boolean {
        val name = binding.etPetName.text.toString().trim()
        val type = binding.etPetType.text.toString().trim()
        val breed = binding.etPetBreed.text.toString().trim()
        val ageText = binding.etPetAge.text.toString().trim()
        val priceText = binding.etPetPrice.text.toString().trim()
        val description = binding.etPetDescription.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || breed.isEmpty() ||
            ageText.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(context, "Please add at least one image", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveImagesToLocal() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        if (selectedImages.isEmpty()) {
            savePet(emptyList())
            return
        }

        localImagePaths.clear()
        context?.let { ctx ->
            for (uri in selectedImages) {
                val localPath = ImageUtils.saveImageToLocal(ctx, uri)
                localPath?.let {
                    localImagePaths.add(it)
                }
            }

            if (localImagePaths.isNotEmpty()) {
                savePet(localImagePaths)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(context, "Failed to save images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePet(imagePaths: List<String>) {
        val name = binding.etPetName.text.toString().trim()
        val type = binding.etPetType.text.toString().trim()
        val breed = binding.etPetBreed.text.toString().trim()
        val age = binding.etPetAge.text.toString().toInt()
        val price = binding.etPetPrice.text.toString().toDouble()
        val description = binding.etPetDescription.text.toString().trim()

        val organizationId = auth.currentUser?.uid ?: ""

        val pet = Pet(
            name = name,
            type = type,
            breed = breed,
            age = age,
            description = description,
            price = price,
            imageUrls = imagePaths,
            organizationId = organizationId,
            status = PetStatus.AVAILABLE,
            dateAdded = Date()
        )

        viewModel.addPet(
            pet = pet,
            onSuccess = {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Pet added successfully", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            },
            onFailure = { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(context, "Failed to add pet: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}