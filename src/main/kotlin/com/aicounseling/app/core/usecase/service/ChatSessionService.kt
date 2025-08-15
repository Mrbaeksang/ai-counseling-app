package com.aicounseling.app.core.usecase.service

import com.aicounseling.app.core.domain.ChatSession
import com.aicounseling.app.core.domain.CounselingPhase
import com.aicounseling.app.core.usecase.port.`in`.ChatSessionUseCase
import com.aicounseling.app.core.usecase.port.`in`.PhaseTransition
import com.aicounseling.app.core.usecase.port.`in`.SessionStatistics
import com.aicounseling.app.core.usecase.port.out.ChatSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * ChatSession 비즈니스 로직 구현
 */
@Service
@Transactional
class ChatSessionService(
    private val sessionRepository: ChatSessionRepository
) : ChatSessionUseCase {
    
    override fun startSession(userId: Long, counselorId: Long): ChatSession {
        // 기존 활성 세션이 있는지 확인
        getActiveSession(userId)?.let {
            throw IllegalStateException("이미 진행 중인 세션이 있습니다")
        }
        
        val session = ChatSession(
            userId = userId,
            counselorId = counselorId
        )
        
        return sessionRepository.save(session)
    }
    
    override fun closeSession(sessionId: Long): ChatSession {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
        
        if (session.closedAt != null) {
            throw IllegalStateException("이미 종료된 세션입니다")
        }
        
        session.closedAt = LocalDateTime.now()
        
        return sessionRepository.save(session)
    }
    
    override fun updatePhase(sessionId: Long, newPhase: CounselingPhase, reason: String): ChatSession {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
        
        if (session.closedAt != null) {
            throw IllegalStateException("종료된 세션은 단계를 변경할 수 없습니다")
        }
        
        session.phase = newPhase
        session.phaseMetadata = reason
        
        return sessionRepository.save(session)
    }
    
    @Transactional(readOnly = true)
    override fun calculateSessionDuration(sessionId: Long): Duration? {
        val session = sessionRepository.findById(sessionId)
            ?: return null
        
        val endTime = session.closedAt ?: LocalDateTime.now()
        return Duration.between(session.createdAt, endTime)
    }
    
    @Transactional(readOnly = true)
    override fun getActiveSession(userId: Long): ChatSession? {
        return sessionRepository.findByUserIdAndClosedAtIsNull(userId)
    }
    
    @Transactional(readOnly = true)
    override fun getUserSessions(userId: Long): List<ChatSession> {
        return sessionRepository.findByUserId(userId)
    }
    
    @Transactional(readOnly = true)
    override fun getCounselorSessions(counselorId: Long): List<ChatSession> {
        return sessionRepository.findByCounselorId(counselorId)
    }
    
    @Transactional(readOnly = true)
    override fun getSessionStatistics(sessionId: Long): SessionStatistics {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
        
        // TODO: Message 엔티티 구현 후 실제 카운트
        // TODO: PhaseTransition 추적 기능 구현
        
        return SessionStatistics(
            messageCount = 0,  // 추후 구현
            duration = calculateSessionDuration(sessionId),
            phaseTransitions = emptyList(),  // 추후 구현
            bookmarkedMessageCount = 0  // 추후 구현
        )
    }
}