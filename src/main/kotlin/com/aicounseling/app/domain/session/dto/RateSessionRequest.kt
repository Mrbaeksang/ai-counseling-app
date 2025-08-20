package com.aicounseling.app.domain.session.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RateSessionRequest(
    @field:NotNull(message = "평점을 입력해주세요")
    @field:Min(1, message = "평점은 1점 이상이어야 합니다")
    @Suppress("MagicNumber")
    @field:Max(5, message = "평점은 5점 이하여야 합니다")
    val rating: Int,
    @field:Size(max = 500, message = "피드백은 500자 이내로 입력해주세요")
    val feedback: String? = null,
)
