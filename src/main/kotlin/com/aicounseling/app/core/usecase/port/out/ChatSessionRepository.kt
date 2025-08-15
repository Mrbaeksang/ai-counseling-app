package com.aicounseling.app.core.usecase.port.out

import com.aicounseling.app.core.domain.ChatSession

/**
 * ChatSession Repository 인터페이스
 */
interface ChatSessionRepository {
    fun save(session: ChatSession): ChatSession
    fun findById(id: Long): ChatSession?
    fun findByUserId(userId: Long): List<ChatSession>
    fun findByCounselorId(counselorId: Long): List<ChatSession>
    fun findByUserIdAndClosedAtIsNull(userId: Long): ChatSession?
    fun deleteById(id: Long)
}