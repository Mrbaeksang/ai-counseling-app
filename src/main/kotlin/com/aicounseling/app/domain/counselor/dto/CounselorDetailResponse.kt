package com.aicounseling.app.domain.counselor.dto

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
