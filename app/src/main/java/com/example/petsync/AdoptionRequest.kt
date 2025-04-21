package com.example.petsync.models

import java.util.Date

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}

data class AdoptionRequest(
    var id: String = "",
    val petId: String = "",
    val petName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val organizationId: String = "",
    val visitDate: Long = 0,
    val visitTime: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val message: String = "",
    val timestamp: Long = 0
)