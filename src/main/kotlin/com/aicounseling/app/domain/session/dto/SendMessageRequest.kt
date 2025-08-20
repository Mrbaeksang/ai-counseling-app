package com.aicounseling.app.domain.session.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendMessageRequest(
    @field:NotBlank(message = "메시지 내용을 입력해주세요")
    @field:Size(max = 1000, message = "메시지는 1000자 이내로 입력해주세요")
    val content: String,
)
