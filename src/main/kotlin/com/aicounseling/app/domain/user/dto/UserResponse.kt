package com.aicounseling.app.domain.user.dto

import com.aicounseling.app.domain.user.entity.User

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
