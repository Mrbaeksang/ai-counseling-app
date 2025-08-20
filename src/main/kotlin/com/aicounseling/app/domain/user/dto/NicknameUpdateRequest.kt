package com.aicounseling.app.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameUpdateRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요")
    val nickname: String,
)
