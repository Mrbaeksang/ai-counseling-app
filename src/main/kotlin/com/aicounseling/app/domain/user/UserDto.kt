package com.aicounseling.app.domain.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// 요청 DTO
data class NicknameUpdateRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요")
    val nickname: String,
)

// 응답 DTO
data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val authProvider: String,
    val isActive: Boolean,
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                authProvider = user.authProvider.name,
                isActive = user.isActive,
            )
        }
    }
}
