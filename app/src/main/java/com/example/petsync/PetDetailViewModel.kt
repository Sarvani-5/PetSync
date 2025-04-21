package com.example.petsync.ui.pets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petsync.models.Pet
import com.google.firebase.firestore.FirebaseFirestore

class PetDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _pet = MutableLiveData<Pet>()
    val pet: LiveData<Pet> = _pet

    fun loadPet(petId: String) {
        db.collection("pets").document(petId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val pet = snapshot.toObject(Pet::class.java)
                    pet?.let {
                        _pet.value = it
                    }
                }
            }
    }

    fun updatePetStatus(petId: String, status: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pets").document(petId)
            .update("status", status)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}