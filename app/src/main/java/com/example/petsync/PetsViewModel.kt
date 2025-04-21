package com.example.petsync.ui.pets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petsync.models.Pet
import com.example.petsync.models.PetStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PetsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _pets = MutableLiveData<List<Pet>>()
    val pets: LiveData<List<Pet>> = _pets

    fun loadAllAvailablePets() {
        db.collection("pets")
            .whereEqualTo("status", PetStatus.AVAILABLE.name)
            .orderBy("dateAdded", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val petsList = snapshot.toObjects(Pet::class.java)
                    _pets.value = petsList
                }
            }
    }

    fun loadOrganizationPets(organizationId: String) {
        db.collection("pets")
            .whereEqualTo("organizationId", organizationId)
            .orderBy("dateAdded", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val petsList = snapshot.toObjects(Pet::class.java)
                    _pets.value = petsList
                }
            }
    }

    fun addPet(pet: Pet, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val petRef = db.collection("pets").document()
        val petWithId = pet.copy(id = petRef.id)

        petRef.set(petWithId)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}