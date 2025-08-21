package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.global.constants.AppConstants
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateSessionTitleRequest(
    @field:NotBlank(message = "제목을 입력해주세요")
    @field:Size(
        min = AppConstants.Validation.MIN_TITLE_LENGTH,
        max = AppConstants.Validation.MAX_TITLE_LENGTH,
        message =
            "제목은 ${AppConstants.Validation.MIN_TITLE_LENGTH}자 이상 " +
                "${AppConstants.Validation.MAX_TITLE_LENGTH}자 이내로 입력해주세요",
    )
    val title: String,
)
