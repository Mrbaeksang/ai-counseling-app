package com.aicounseling.app.core.usecase.port.`in`

import com.aicounseling.app.core.domain.ChatSession
import com.aicounseling.app.core.domain.CounselingPhase
import java.time.Duration

/**
 * ChatSession 관련 비즈니스 로직 인터페이스
 */
interface ChatSessionUseCase {
    
    /**
     * 새 상담 세션 시작
     */
    fun startSession(userId: Long, counselorId: Long): ChatSession
    
    /**
     * 세션 종료
     */
    fun closeSession(sessionId: Long): ChatSession
    
    /**
     * 상담 단계 전환
     * - AI가 대화 맥락 분석 후 호출
     */
    fun updatePhase(sessionId: Long, newPhase: CounselingPhase, reason: String): ChatSession
    
    /**
     * 세션 지속 시간 계산
     */
    fun calculateSessionDuration(sessionId: Long): Duration?
    
    /**
     * 활성 세션 조회
     */
    fun getActiveSession(userId: Long): ChatSession?
    
    /**
     * 사용자의 모든 세션 조회
     */
    fun getUserSessions(userId: Long): List<ChatSession>
    
    /**
     * 상담사별 세션 조회
     */
    fun getCounselorSessions(counselorId: Long): List<ChatSession>
    
    /**
     * 세션 통계
     */
    fun getSessionStatistics(sessionId: Long): SessionStatistics
}

/**
 * 세션 통계 DTO
 */
data class SessionStatistics(
    val messageCount: Int,
    val duration: Duration?,
    val phaseTransitions: List<PhaseTransition>,
    val bookmarkedMessageCount: Int
)

data class PhaseTransition(
    val from: CounselingPhase?,
    val to: CounselingPhase,
    val reason: String,
    val timestamp: java.time.LocalDateTime
)