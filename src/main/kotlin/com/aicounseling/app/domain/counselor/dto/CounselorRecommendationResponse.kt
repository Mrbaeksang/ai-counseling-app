package com.aicounseling.app.domain.counselor.dto

data class CounselorRecommendationResponse(
    val id: Long,
    val name: String,
    val title: String,
    val description: String,
    val averageRating: Double,
    val totalSessions: Int,
    val matchReason: String,
)
