package com.example.petsync.ui.pets

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider  // Added import
import com.bumptech.glide.Glide
import com.example.petsync.R
import com.example.petsync.databinding.DialogAdoptionRequestBinding
import com.example.petsync.databinding.FragmentPetDetailBinding
import com.example.petsync.models.AdoptionRequest
import com.example.petsync.models.Pet
import com.example.petsync.models.RequestStatus
import com.example.petsync.models.User
import com.example.petsync.models.UserType
import com.example.petsync.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PetDetailFragment : Fragment() {

    private var _binding: FragmentPetDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PetDetailViewModel
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var petId: String = ""
    private var userType: UserType = UserType.USER
    private var currentPet: Pet? = null
    private var currentUser: User? = null
    private var selectedDate: Long = 0
    private var selectedTime: String = ""

    // Date and time formatters
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val ARG_PET_ID = "pet_id"

        fun newInstance(petId: String): PetDetailFragment {
            val fragment = PetDetailFragment()
            val args = Bundle()
            args.putString(ARG_PET_ID, petId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            petId = it.getString(ARG_PET_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetDetailBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[PetDetailViewModel::class.java]  // Fixed order

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get current user details
        fetchCurrentUser()

        // Check user type
        checkUserType()

        // Observe pet data
        viewModel.pet.observe(viewLifecycleOwner) { pet ->
            currentPet = pet
            updateUI(pet)
        }

        // Load pet details
        if (petId.isNotEmpty()) {
            viewModel.loadPet(petId)
        }

        // Set up adopt button
        binding.btnAdopt.setOnClickListener {
            showAdoptionConfirmationDialog()
        }

        // Set up back button
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun fetchCurrentUser() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                }
            }
    }

    private fun checkUserType() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTypeString = document.getString("userType") ?: UserType.USER.name
                    userType = UserType.valueOf(userTypeString)

                    // Show/hide adopt button based on user type
                    binding.btnAdopt.visibility = if (userType == UserType.USER) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
    }

    private fun updateUI(pet: Pet) {
        binding.tvPetName.text = pet.name
        binding.tvPetType.text = "Type: ${pet.type}"
        binding.tvPetBreed.text = "Breed: ${pet.breed}"
        binding.tvPetAge.text = "Age: ${pet.age} years"
        binding.tvPetPrice.text = "Price: $${pet.price}"
        binding.tvPetDescription.text = pet.description

        // Load first image if available
        if (pet.imageUrls.isNotEmpty()) {
            val imagePath = pet.imageUrls[0]

            // Check if this is a local file path or URL
            if (imagePath.startsWith("file://") || File(imagePath).exists()) {
                // Local file
                Glide.with(this)
                    .load(File(imagePath))
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(binding.ivPetImage)
            } else {
                // URL
                Glide.with(this)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(binding.ivPetImage)
            }
        } else {
            binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
        }
    }

    private fun showAdoptionConfirmationDialog() {
        val pet = currentPet ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Request Adoption")
            .setMessage("Are you sure you want to request adoption for ${pet.name}?")
            .setPositiveButton("Yes") { _, _ ->
                showVisitSchedulingDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVisitSchedulingDialog() {
        val pet = currentPet ?: return
        val user = currentUser ?: return

        // Fixed binding initialization
        val dialogBinding = DialogAdoptionRequestBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Schedule a Visit")
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Set initial values
        dialogBinding.tvPetName.text = "Pet: ${pet.name}"

        // Set up date picker - ensure they're using local variables not properties
        dialogBinding.etDate.setOnClickListener {
            showDatePicker(dialogBinding)
        }

        // Set up time picker
        dialogBinding.etTime.setOnClickListener {
            showTimePicker(dialogBinding)
        }

        // Set up buttons
        dialogBinding.btnSubmit.setOnClickListener {
            if (selectedDate == 0L || selectedTime.isEmpty()) {
                Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = dialogBinding.etMessage.text.toString().trim()

            // Create adoption request
            submitAdoptionRequest(pet, user, message)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(dialogBinding: DialogAdoptionRequestBinding) {
        val calendar = Calendar.getInstance()

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            selectedDate = calendar.timeInMillis
            dialogBinding.etDate.setText(dateFormatter.format(calendar.time))
        }

        DatePickerDialog(
            requireContext(),
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set min date to today
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showTimePicker(dialogBinding: DialogAdoptionRequestBinding) {
        val calendar = Calendar.getInstance()

        val timeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            selectedTime = timeFormatter.format(calendar.time)
            dialogBinding.etTime.setText(selectedTime)
        }

        TimePickerDialog(
            requireContext(),
            timeListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun submitAdoptionRequest(pet: Pet, user: User, message: String) {
        val requestId = db.collection("adoption_requests").document().id

        val adoptionRequest = AdoptionRequest(
            id = requestId,
            petId = pet.id,
            petName = pet.name,
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            organizationId = pet.organizationId,
            visitDate = selectedDate,
            visitTime = selectedTime,
            status = RequestStatus.PENDING,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        db.collection("adoption_requests")
            .document(requestId)
            .set(adoptionRequest)
            .addOnSuccessListener {
                Toast.makeText(context, "Adoption request sent!", Toast.LENGTH_SHORT).show()

                // Send notification to organization
                sendNotificationToOrganization(pet.organizationId, adoptionRequest)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to send request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendNotificationToOrganization(organizationId: String, request: AdoptionRequest) {
        // Fetch organization details to get their phone number
        db.collection("users").document(organizationId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val organization = document.toObject(User::class.java)
                    organization?.let {
                        // Send SMS notification
                        context?.let { ctx ->
                            NotificationUtils.sendSMS(
                                ctx,
                                it.phone,
                                "New adoption request for ${request.petName} from ${request.userName}. " +
                                        "Visit scheduled for ${dateFormatter.format(Date(request.visitDate))} at ${request.visitTime}."
                            )

                            // Send app notification
                            NotificationUtils.showNotification(
                                ctx,
                                "New Adoption Request",
                                "Request for ${request.petName} from ${request.userName}"
                            )
                        }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}