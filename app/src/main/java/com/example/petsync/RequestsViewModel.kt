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
}