package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.session.entity.ChatSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
    fun findByUserId(userId: Long): List<ChatSession>
    fun findByCounselorId(counselorId: Long): List<ChatSession>
    fun findByUserIdAndClosedAtIsNull(userId: Long): ChatSession?
}