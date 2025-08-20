package com.aicounseling.app.domain.auth.dto

data class OAuthUserInfo(
    val providerId: String,
    val email: String,
    val name: String?,
    val provider: String,
)
