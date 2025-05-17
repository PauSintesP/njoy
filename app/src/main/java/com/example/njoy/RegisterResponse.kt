package com.example.njoy

data class RegisterResponse(
    val success: Boolean,
    val message: String? = null,
    val user_id: Int? = null
)