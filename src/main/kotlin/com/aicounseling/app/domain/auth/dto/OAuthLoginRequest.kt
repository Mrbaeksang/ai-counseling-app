package com.aicounseling.app.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank(message = "토큰은 필수입니다")
    val token: String,
)
