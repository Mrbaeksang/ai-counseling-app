package com.aicounseling.app.domain.auth.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String,
    val nickname: String,
)
