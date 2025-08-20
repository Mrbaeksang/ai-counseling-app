package com.aicounseling.app.domain.counselor.dto

data class CounselorListResponse(
    val id: Long,
    val name: String,
    val title: String,
    val description: String,
    val totalSessions: Int,
    val averageRating: Double,
)
