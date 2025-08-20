package com.aicounseling.app.domain.session.repository

import com.aicounseling.app.domain.session.entity.ChatSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
    fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<ChatSession>

    fun findByUserIdAndIsBookmarked(
        userId: Long,
        isBookmarked: Boolean,
        pageable: Pageable,
    ): Page<ChatSession>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): ChatSession?

    fun findByUserIdAndClosedAtIsNull(userId: Long): List<ChatSession>

    fun findByUserIdAndCounselorIdAndClosedAtIsNull(
        userId: Long,
        counselorId: Long,
    ): ChatSession?

    fun countByUserIdAndClosedAtIsNull(userId: Long): Long

    fun countByCounselorId(counselorId: Long): Long
}
