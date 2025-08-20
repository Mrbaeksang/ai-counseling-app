package com.aicounseling.app.domain.session.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class StartSessionRequest(
    @field:NotNull(message = "상담사를 선택해주세요")
    @field:Positive(message = "유효한 상담사 ID를 입력해주세요")
    val counselorId: Long,
)
