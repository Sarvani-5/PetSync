package com.example.petsync.models

enum class UserType {
    USER,
    ORGANIZATION
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val userType: UserType = UserType.USER
)
