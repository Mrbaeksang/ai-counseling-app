package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.session.entity.ChatSession
import java.time.LocalDateTime

data class SessionListResponse(
    val id: Long,
    val title: String,
    val counselorName: String,
    val isBookmarked: Boolean,
    val isActive: Boolean,
    val lastMessageAt: LocalDateTime?,
) {
    companion object {
        fun from(
            session: ChatSession,
            counselor: Counselor,
        ) = SessionListResponse(
            id = session.id,
            title = session.title ?: "새 상담",
            counselorName = counselor.name,
            isBookmarked = session.isBookmarked,
            isActive = session.closedAt == null,
            lastMessageAt = session.lastMessageAt ?: session.createdAt,
        )
    }
}
