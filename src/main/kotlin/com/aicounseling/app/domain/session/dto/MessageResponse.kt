package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.Message
import java.time.LocalDateTime

data class MessageResponse(
    val id: Long,
    val senderType: String,
    val content: String,
    // 상담 단계 (한글명)
    val phase: String,
    // 상담 단계 ENUM 코드
    val phaseCode: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(message: Message) =
            MessageResponse(
                id = message.id,
                senderType = message.senderType.name,
                content = message.content,
                // AI가 판단한 현재 상담 단계의 한글명
                phase = message.phase.koreanName,
                // ENUM 코드 (API 클라이언트에서 활용 가능)
                phaseCode = message.phase.name,
                createdAt = message.createdAt,
            )
    }
}
