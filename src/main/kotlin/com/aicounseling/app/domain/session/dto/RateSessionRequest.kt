package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.global.constants.AppConstants
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 세션 평가 요청 DTO
 *
 * @property rating 1~10 정수 (별 0.5개 = 1, 별 5개 = 10)
 * @property feedback 선택적 피드백 메시지
 */
data class RateSessionRequest(
    @field:NotNull(message = "평점을 입력해주세요")
    @field:Min(
        value = AppConstants.Rating.MIN_RATING.toLong(),
        message = "평점은 ${AppConstants.Rating.MIN_RATING}점 이상이어야 합니다",
    )
    @field:Max(
        value = AppConstants.Rating.MAX_RATING.toLong(),
        message = "평점은 ${AppConstants.Rating.MAX_RATING}점 이하여야 합니다",
    )
    val rating: Int,
    @field:Size(
        max = AppConstants.Validation.MAX_FEEDBACK_LENGTH,
        message = "피드백은 ${AppConstants.Validation.MAX_FEEDBACK_LENGTH}자 이내로 입력해주세요",
    )
    val feedback: String? = null,
)
