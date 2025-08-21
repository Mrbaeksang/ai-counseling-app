package com.aicounseling.app.domain.session.dto

import java.time.LocalDateTime

/**
 * 세션 목록 조회 응답 DTO
 *
 * 목록 화면에서 필요한 최소 정보만 포함
 * - 세션 식별 정보
 * - 표시용 정보 (제목, 상담사명, 시간)
 * - UI 상태 정보 (북마크 여부)
 */
data class SessionListResponse(
    val sessionId: Long,
    val title: String,
    val counselorName: String,
    val lastMessageAt: LocalDateTime,
    val isBookmarked: Boolean,
)
