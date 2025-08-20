package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.Message
import java.time.LocalDateTime

data class MessageResponse(
    val id: Long,
    val senderType: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(message: Message) =
            MessageResponse(
                id = message.id,
                senderType = message.senderType.name,
                content = message.content,
                createdAt = message.createdAt,
            )
    }
}
