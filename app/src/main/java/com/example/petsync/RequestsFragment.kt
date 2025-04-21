package com.example.petsync.ui.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petsync.adapters.AdoptionRequestAdapter
import com.example.petsync.databinding.FragmentRequestsBinding
import com.example.petsync.models.AdoptionRequest
import com.example.petsync.models.RequestStatus
import com.example.petsync.models.UserType
import com.example.petsync.ui.requests.RequestDetailFragment
import com.example.petsync.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RequestsViewModel
    private lateinit var requestAdapter: AdoptionRequestAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userType: UserType = UserType.USER

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[RequestsViewModel::class.java]
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        setupRecyclerView()

        // Check user type and load appropriate requests
        checkUserType()

        // Observe requests
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            requestAdapter.submitList(requests)
            binding.progressBar.visibility = View.GONE

            if (requests.isEmpty()) {
                binding.tvNoRequests.visibility = View.VISIBLE
            } else {
                binding.tvNoRequests.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        requestAdapter = AdoptionRequestAdapter(
            onApprove = { request ->
                approveRequest(request)
            },
            onReject = { request ->
                rejectRequest(request)
            },
            onViewDetails = { request ->
                showRequestDetails(request)
            }
        )

        binding.recyclerViewRequests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = requestAdapter
        }
    }

    private fun checkUserType() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTypeString = document.getString("userType") ?: UserType.USER.name
                    userType = UserType.valueOf(userTypeString)

                    // Load requests based on user type
                    loadRequests()
                }
            }
    }

    private fun loadRequests() {
        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: return

        if (userType == UserType.ORGANIZATION) {
            // For organizations, load requests sent to them
            viewModel.loadOrganizationRequests(userId)
        } else {
            // For users, load their own requests
            viewModel.loadUserRequests(userId)
        }
    }

    private fun approveRequest(request: AdoptionRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve Request")
            .setMessage("Are you sure you want to approve the adoption request for ${request.petName}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.updateRequestStatus(
                    requestId = request.id,
                    status = RequestStatus.APPROVED,
                    onSuccess = {
                        Toast.makeText(context, "Request approved", Toast.LENGTH_SHORT).show()

                        // Update pet status
                        viewModel.updatePetStatus(
                            petId = request.petId,
                            status = "PENDING",
                            onSuccess = {},
                            onFailure = {}
                        )

                        // Notify user
                        notifyUser(request, "approved")
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            "Failed to approve request: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectRequest(request: AdoptionRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Request")
            .setMessage("Are you sure you want to reject the adoption request for ${request.petName}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.updateRequestStatus(
                    requestId = request.id,
                    status = RequestStatus.REJECTED,
                    onSuccess = {
                        Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()

                        // Notify user
                        notifyUser(request, "rejected")
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            "Failed to reject request: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun notifyUser(request: AdoptionRequest, action: String) {
        // Get user details
        db.collection("users").document(request.userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val phone = document.getString("phone") ?: return@addOnSuccessListener

                    // Send SMS notification
                    context?.let { ctx ->
                        NotificationUtils.sendSMS(
                            ctx,
                            phone,
                            "Your adoption request for ${request.petName} has been $action. " +
                                    if (action == "approved") {
                                        "Your visit is confirmed for ${dateFormatter.format(Date(request.visitDate))} at ${request.visitTime}."
                                    } else {
                                        "Please check the app for more details."
                                    }
                        )

                        // Send WhatsApp message (optional)
                        try {
                            NotificationUtils.sendWhatsAppMessage(
                                ctx,
                                phone,
                                "Your adoption request for ${request.petName} has been $action. " +
                                        if (action == "approved") {
                                            "Your visit is confirmed for ${dateFormatter.format(Date(request.visitDate))} at ${request.visitTime}."
                                        } else {
                                            "Please check the app for more details."
                                        }
                            )
                        } catch (e: Exception) {
                            // WhatsApp sending is optional, so just log errors
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    private fun showRequestDetails(request: AdoptionRequest) {
        val detailFragment = RequestDetailFragment.newInstance(request.id)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}