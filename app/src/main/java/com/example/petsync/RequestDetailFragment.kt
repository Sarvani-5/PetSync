package com.example.petsync.ui.requests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.petsync.databinding.FragmentRequestDetailBinding
import com.example.petsync.models.AdoptionRequest
import com.example.petsync.models.Pet
import com.example.petsync.models.RequestStatus
import com.example.petsync.models.User
import com.example.petsync.models.UserType
import com.example.petsync.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PetStatus {
    AVAILABLE,
    PENDING,
    ADOPTED
}

class RequestDetailFragment : Fragment() {

    private var _binding: FragmentRequestDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var requestId: String = ""
    private var currentRequest: AdoptionRequest? = null
    private var userType: UserType = UserType.USER

    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    companion object {
        private const val ARG_REQUEST_ID = "request_id"

        fun newInstance(requestId: String): RequestDetailFragment {
            val fragment = RequestDetailFragment()
            val args = Bundle()
            args.putString(ARG_REQUEST_ID, requestId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            requestId = it.getString(ARG_REQUEST_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check user type
        checkUserType()

        // Load request details
        if (requestId.isNotEmpty()) {
            loadRequestDetails()
        }

        // Set up buttons
        binding.btnCall.setOnClickListener {
            currentRequest?.let { request ->
                val phone = when (userType) {
                    UserType.ORGANIZATION -> request.userPhone
                    UserType.USER -> getOrganizationPhone(request.organizationId)
                    else -> ""  // Add else branch for exhaustive when
                }

                if (phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                    }
                    startActivity(intent)
                }
            }
        }

        binding.btnSms.setOnClickListener {
            currentRequest?.let { request ->
                val phone = when (userType) {
                    UserType.ORGANIZATION -> request.userPhone
                    UserType.USER -> getOrganizationPhone(request.organizationId)
                    else -> ""  // Add else branch for exhaustive when
                }

                if (phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:$phone")
                    }
                    startActivity(intent)
                }
            }
        }

        binding.btnWhatsapp.setOnClickListener {
            currentRequest?.let { request ->
                val phone = when (userType) {
                    UserType.ORGANIZATION -> request.userPhone
                    UserType.USER -> getOrganizationPhone(request.organizationId)
                    else -> ""  // Add else branch for exhaustive when
                }

                if (phone.isNotEmpty()) {
                    context?.let { ctx ->
                        try {
                            NotificationUtils.sendWhatsAppMessage(
                                ctx,
                                phone,
                                "Hello, I'm contacting you regarding the adoption request for ${request.petName}."
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Organization-specific buttons
        binding.btnApprove.setOnClickListener {
            approveRequest()
        }

        binding.btnReject.setOnClickListener {
            rejectRequest()
        }
    }

    private fun checkUserType() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTypeString = document.getString("userType") ?: UserType.USER.name
                    userType = UserType.valueOf(userTypeString)

                    // Show/hide organization-specific buttons
                    val isOrg = userType == UserType.ORGANIZATION
                    binding.btnApprove.visibility = if (isOrg) View.VISIBLE else View.GONE
                    binding.btnReject.visibility = if (isOrg) View.VISIBLE else View.GONE
                }
            }
    }

    private fun loadRequestDetails() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("adoption_requests").document(requestId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document != null && document.exists()) {
                    val request = document.toObject(AdoptionRequest::class.java)
                    request?.let {
                        it.id = document.id  // Ensure ID is set
                        currentRequest = it
                        updateUI(it)

                        // Load associated pet details
                        loadPetDetails(it.petId)
                    }
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load request details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPetDetails(petId: String) {
        db.collection("pets").document(petId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pet = document.toObject(Pet::class.java)
                    pet?.let {
                        binding.tvPetDetails.text = "Pet: ${it.name}, ${it.breed}, ${it.age} years old"
                    }
                }
            }
    }

    private fun updateUI(request: AdoptionRequest) {
        // Update request details
        binding.tvRequestId.text = "Request #${request.id.takeLast(8)}"
        binding.tvStatus.text = "Status: ${request.status}"
        binding.tvDate.text = "Visit date: ${dateFormatter.format(Date(request.visitDate))} at ${request.visitTime}"

        if (userType == UserType.ORGANIZATION) {
            binding.tvUserName.text = "From: ${request.userName}"
            binding.tvUserPhone.text = "Phone: ${request.userPhone}"

            binding.tvOrganizationName.visibility = View.GONE
            binding.tvOrganizationPhone.visibility = View.GONE
            binding.tvOrganizationAddress.visibility = View.GONE
        } else {
            binding.tvUserName.visibility = View.GONE
            binding.tvUserPhone.visibility = View.GONE

            // Load organization details instead
            loadOrganizationDetails(request.organizationId)
        }

        binding.tvMessage.text = "Message: ${request.message}"

        // Disable action buttons if the request is not pending
        val isPending = request.status == RequestStatus.PENDING
        binding.btnApprove.isEnabled = isPending
        binding.btnReject.isEnabled = isPending
    }

    private fun loadOrganizationDetails(organizationId: String) {
        db.collection("users").document(organizationId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val organization = document.toObject(User::class.java)
                    organization?.let {
                        binding.tvOrganizationName.text = "Organization: ${it.name}"
                        binding.tvOrganizationPhone.text = "Phone: ${it.phone}"
                        binding.tvOrganizationAddress.text = "Address: ${it.address}"

                        binding.tvOrganizationName.visibility = View.VISIBLE
                        binding.tvOrganizationPhone.visibility = View.VISIBLE
                        binding.tvOrganizationAddress.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun getOrganizationPhone(organizationId: String): String {
        // This is a blocking operation, better done with a callback or coroutine
        var phone = ""
        try {
            val task = db.collection("users").document(organizationId).get()
            task.addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    phone = document.getString("phone") ?: ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return phone
    }

    private fun approveRequest() {
        currentRequest?.let { request ->
            AlertDialog.Builder(requireContext())
                .setTitle("Approve Request")
                .setMessage("Are you sure you want to approve this adoption request?")
                .setPositiveButton("Yes") { _, _ ->
                    binding.progressBar.visibility = View.VISIBLE

                    // Update request status in Firestore
                    db.collection("adoption_requests").document(request.id)
                        .update("status", RequestStatus.APPROVED.name)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Request approved", Toast.LENGTH_SHORT).show()

                            // Update pet status
                            updatePetStatus(request.petId, PetStatus.PENDING)

                            // Send notification to user
                            notifyUser(request, "approved")

                            // Refresh UI
                            loadRequestDetails()
                        }
                        .addOnFailureListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to approve request", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun rejectRequest() {
        currentRequest?.let { request ->
            AlertDialog.Builder(requireContext())
                .setTitle("Reject Request")
                .setMessage("Are you sure you want to reject this adoption request?")
                .setPositiveButton("Yes") { _, _ ->
                    binding.progressBar.visibility = View.VISIBLE

                    // Update request status in Firestore
                    db.collection("adoption_requests").document(request.id)
                        .update("status", RequestStatus.REJECTED.name)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()

                            // Send notification to user
                            notifyUser(request, "rejected")

                            // Refresh UI
                            loadRequestDetails()
                        }
                        .addOnFailureListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to reject request", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updatePetStatus(petId: String, status: PetStatus) {
        db.collection("pets").document(petId)
            .update("status", status.name)
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update pet status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun notifyUser(request: AdoptionRequest, action: String) {
        // Get user details
        db.collection("users").document(request.userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        context?.let { ctx ->
                            // Send SMS notification
                            NotificationUtils.sendSMS(
                                ctx,
                                it.phone,
                                "Your adoption request for ${request.petName} has been $action."
                            )

                            // Send app notification
                            NotificationUtils.showNotification(
                                ctx,
                                "Adoption Request $action",
                                "Your request for ${request.petName} has been $action."
                            )

                            // Try to send WhatsApp message
                            try {
                                NotificationUtils.sendWhatsAppMessage(
                                    ctx,
                                    it.phone,
                                    "Your adoption request for ${request.petName} has been $action. " +
                                            "Please check the app for more details."
                                )
                            } catch (e: Exception) {
                                // WhatsApp error is not critical
                                e.printStackTrace()
                            }
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