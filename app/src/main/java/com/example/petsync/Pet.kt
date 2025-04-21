package com.example.petsync.models

import java.util.Date

enum class PetStatus {
    AVAILABLE,
    PENDING,
    ADOPTED
}

data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val age: Int = 0,
    val description: String = "",
    val price: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    val organizationId: String = "",
    val status: PetStatus = PetStatus.AVAILABLE,
    val dateAdded: Date = Date()
)