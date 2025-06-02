package com.example.petsync.models

import java.util.*

data class PetReminder(
    val id: String,
    val userId: String,
    val petId: String? = null,  // Now nullable for general reminders
    val title: String,
    val message: String,
    val reminderType: PetReminderType,
    val reminderTime: Long,
    var isCompleted: Boolean = false,  // Making this a var since it gets updated
    val createdAt: Long = System.currentTimeMillis()
) {
    // Empty constructor for Firebase
    constructor() : this(
        id = "",
        userId = "",
        petId = null,
        title = "",
        message = "",
        reminderType = PetReminderType.OTHER,
        reminderTime = 0L,
        isCompleted = false,
        createdAt = 0L
    )

    // Convert PetReminder to Map for Firebase
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "petId" to petId,
            "title" to title,
            "message" to message,
            "reminderType" to reminderType.name,
            "reminderTime" to reminderTime,
            "isCompleted" to isCompleted,
            "createdAt" to createdAt
        )
    }

    companion object {
        // Create PetReminder from Firebase Map
        fun fromMap(map: Map<String, Any?>): PetReminder {
            return PetReminder(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                petId = map["petId"] as? String,  // Allows null for general reminders
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                reminderType = try {
                    PetReminderType.valueOf(map["reminderType"] as? String ?: PetReminderType.OTHER.name)
                } catch (e: IllegalArgumentException) {
                    PetReminderType.OTHER
                },
                reminderTime = (map["reminderTime"] as? Long) ?: 0L,
                isCompleted = (map["isCompleted"] as? Boolean) ?: false,
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

enum class PetReminderType(val displayName: String) {
    MEDICATION("Medication"),
    FEEDING("Feeding"),
    VET_VISIT("Vet Visit"),
    GROOMING("Grooming"),
    EXERCISE("Exercise"),
    OTHER("Other")
}