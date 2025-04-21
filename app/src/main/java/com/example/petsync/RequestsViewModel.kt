package com.example.petsync.ui.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petsync.models.AdoptionRequest
import com.example.petsync.models.RequestStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RequestsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _requests = MutableLiveData<List<AdoptionRequest>>()
    val requests: LiveData<List<AdoptionRequest>> = _requests

    // Add the new requests count LiveData
    private val _newRequestsCount = MutableLiveData<Int>()
    val newRequestsCount: LiveData<Int> = _newRequestsCount

    // Set to track seen request IDs
    private val seenRequestIds = mutableSetOf<String>()

    fun loadOrganizationRequests(organizationId: String) {
        db.collection("adoption_requests")
            .whereEqualTo("organizationId", organizationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requestsList = snapshot.toObjects(AdoptionRequest::class.java)
                    _requests.value = requestsList

                    // Count new requests (those not in seenRequestIds)
                    val newRequests = requestsList.filter { !seenRequestIds.contains(it.id) }
                    _newRequestsCount.value = newRequests.size
                }
            }
    }

    fun loadUserRequests(userId: String) {
        db.collection("adoption_requests")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requestsList = snapshot.toObjects(AdoptionRequest::class.java)
                    _requests.value = requestsList

                    // For regular users, we don't track new requests
                    _newRequestsCount.value = 0
                }
            }
    }

    fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("adoption_requests")
            .document(requestId)
            .update("status", status.name)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updatePetStatus(
        petId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("pets")
            .document(petId)
            .update("status", status)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Add function to mark requests as seen
    fun markRequestsAsSeen(requestIds: List<String>) {
        seenRequestIds.addAll(requestIds)

        // Recalculate new requests count
        _requests.value?.let { requests ->
            val newRequests = requests.filter { !seenRequestIds.contains(it.id) }
            _newRequestsCount.value = newRequests.size
        }
    }

    // Add function to get seen request IDs
    fun getSeenRequestIds(): Set<String> {
        return seenRequestIds
    }
}