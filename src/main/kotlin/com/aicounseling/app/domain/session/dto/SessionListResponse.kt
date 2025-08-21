package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.global.constants.AppConstants
import java.time.LocalDateTime

data class SessionListResponse(
    val id: Long,
    val title: String,
    val counselorName: String,
    val counselorId: Long,
    // 현재 상담 단계 (한글명)
    val phase: String,
    val isBookmarked: Boolean,
    val isActive: Boolean,
    val lastMessageAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(
            session: ChatSession,
            counselor: Counselor,
            lastMessage: Message? = null,
        ) = SessionListResponse(
            id = session.id,
            title = session.title ?: AppConstants.Session.DEFAULT_SESSION_TITLE,
            counselorName = counselor.name,
            counselorId = counselor.id,
            phase = (lastMessage?.phase ?: CounselingPhase.ENGAGEMENT).koreanName,
            isBookmarked = session.isBookmarked,
            isActive = session.closedAt == null,
            lastMessageAt = session.lastMessageAt ?: session.createdAt,
            createdAt = session.createdAt,
        )
    }
}
