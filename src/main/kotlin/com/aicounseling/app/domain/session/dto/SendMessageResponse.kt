package com.aicounseling.app.domain.session.dto

import com.aicounseling.app.domain.session.entity.Message

data class SendMessageResponse(
    val userMessage: MessageResponse,
    val aiMessage: MessageResponse,
) {
    companion object {
        fun from(
            userMessage: Message,
            aiMessage: Message,
        ) = SendMessageResponse(
            userMessage = MessageResponse.from(userMessage),
            aiMessage = MessageResponse.from(aiMessage),
        )
    }
}
