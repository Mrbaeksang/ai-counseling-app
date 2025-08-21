package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.Message

data class SendMessageResponse(
    val userMessage: MessageResponse,
    val aiMessage: MessageResponse,
    val sessionTitle: String? = null,
    // AI가 판단한 현재 상담 단계 (한글명)
    val currentPhase: String? = null,
) {
    companion object {
        fun from(
            userMessage: Message,
            aiMessage: Message,
            session: ChatSession? = null,
        ) = SendMessageResponse(
            userMessage = MessageResponse.from(userMessage),
            aiMessage = MessageResponse.from(aiMessage),
            sessionTitle = session?.title,
            // AI 메시지의 phase를 사용 (AI가 최종 판단)
            currentPhase = aiMessage.phase.koreanName,
        )
    }
}
