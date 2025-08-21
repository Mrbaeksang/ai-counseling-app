package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.global.constants.AppConstants
import java.time.LocalDateTime

data class SessionResponse(
    val id: Long,
    val title: String,
    val counselorId: Long,
    val counselorName: String? = null,
    // 현재 상담 단계 ENUM 코드
    val phase: String,
    // 현재 상담 단계 한글명
    val phaseKoreanName: String,
    val isBookmarked: Boolean,
    val createdAt: LocalDateTime,
    val lastMessageAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val messageCount: Int? = null,
) {
    companion object {
        fun from(
            session: ChatSession,
            lastMessage: Message? = null,
        ) = SessionResponse(
            id = session.id,
            title = session.title ?: AppConstants.Session.DEFAULT_SESSION_TITLE,
            counselorId = session.counselorId,
            phase = (lastMessage?.phase ?: CounselingPhase.ENGAGEMENT).name,
            phaseKoreanName = (lastMessage?.phase ?: CounselingPhase.ENGAGEMENT).koreanName,
            isBookmarked = session.isBookmarked,
            createdAt = session.createdAt,
            lastMessageAt = session.lastMessageAt,
            closedAt = session.closedAt,
        )
    }
}
