package com.aicounseling.app.domain.session.dto

import java.time.LocalDateTime

data class RatingResponse(
    val id: Long,
    val sessionId: Long,
    val rating: Double,
    val review: String?,
    val createdAt: LocalDateTime,
)
