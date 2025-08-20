package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.ChatSession
import java.time.LocalDateTime

data class SessionResponse(
    val id: Long,
    val title: String,
    val counselorId: Long,
    val phaseKoreanName: String,
    val isBookmarked: Boolean,
    val createdAt: LocalDateTime,
    val lastMessageAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
) {
    companion object {
        fun from(session: ChatSession) =
            SessionResponse(
                id = session.id,
                title = session.title ?: "새 상담",
                counselorId = session.counselorId,
                phaseKoreanName = session.phase.koreanName,
                isBookmarked = session.isBookmarked,
                createdAt = session.createdAt,
                lastMessageAt = session.lastMessageAt,
                closedAt = session.closedAt,
            )
    }
}
