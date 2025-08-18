package com.aicounseling.app.domain.session

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * ChatSessionRepository - 상담 세션 데이터 액세스
 */
@Repository
interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
    /**
     * 사용자의 모든 세션 조회
     */
    fun findByUserId(userId: Long): List<ChatSession>

    /**
     * 상담사의 모든 세션 조회
     */
    fun findByCounselorId(counselorId: Long): List<ChatSession>

    /**
     * 사용자의 진행 중인 세션 조회 (closedAt이 null)
     */
    fun findByUserIdAndClosedAtIsNull(userId: Long): ChatSession?

    /**
     * 상담사의 총 세션 수 카운트
     */
    fun countByCounselorId(counselorId: Long): Int

    /**
     * 북마크된 세션만 조회
     */
    fun findByUserIdAndIsBookmarkedTrue(userId: Long): List<ChatSession>
}
