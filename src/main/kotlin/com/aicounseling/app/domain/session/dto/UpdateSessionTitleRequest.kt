package com.aicounseling.app.domain.session.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateSessionTitleRequest(
    @field:NotBlank(message = "제목을 입력해주세요")
    @field:Size(min = 1, max = 15, message = "제목은 1자 이상 15자 이내로 입력해주세요")
    val title: String,
)
