package com.aicounseling.app.domain.counselor

data class CounselorListResponse(
    val id: Long,
    val name: String,
    val title: String,
    val description: String,
    val totalSessions: Int,
    val averageRating: Double,
)

data class CounselorDetailResponse(
    val id: Long,
    val name: String,
    val title: String,
    val description: String,
    val personalityMatrix: Map<String, Int>,
    val specialties: List<String>,
    val totalSessions: Int,
    val averageRating: Double,
)

data class CounselorRecommendationResponse(
    val id: Long,
    val name: String,
    val title: String,
    val description: String,
    val averageRating: Double,
    val totalSessions: Int,
    val matchReason: String,
)
