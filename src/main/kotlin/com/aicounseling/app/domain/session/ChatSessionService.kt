package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional
class ChatSessionService(
    private val sessionRepository: ChatSessionRepository
) {
    
    fun startSession(userId: Long, counselorId: Long): ChatSession {
        getActiveSession(userId)?.let {
            throw IllegalStateException("이미 진행 중인 세션이 있습니다")
        }
        
        val session = ChatSession(
            userId = userId,
            counselorId = counselorId
        )
        
        return sessionRepository.save(session)
    }
    
    fun closeSession(sessionId: Long): ChatSession {
        val session = sessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
        }
        
        if (session.closedAt != null) {
            throw IllegalStateException("이미 종료된 세션입니다")
        }
        
        session.closedAt = LocalDateTime.now()
        
        return sessionRepository.save(session)
    }
    
    fun updatePhase(sessionId: Long, newPhase: CounselingPhase, reason: String): ChatSession {
        val session = sessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
        }
        
        if (session.closedAt != null) {
            throw IllegalStateException("종료된 세션은 단계를 변경할 수 없습니다")
        }
        
        session.phase = newPhase
        session.phaseMetadata = reason
        
        return sessionRepository.save(session)
    }
    
    @Transactional(readOnly = true)
    fun calculateSessionDuration(sessionId: Long): Duration? {
        val session = sessionRepository.findById(sessionId).orElse(null)
            ?: return null
        
        val endTime = session.closedAt ?: LocalDateTime.now()
        return Duration.between(session.createdAt, endTime)
    }
    
    @Transactional(readOnly = true)
    fun getActiveSession(userId: Long): ChatSession? {
        return sessionRepository.findByUserIdAndClosedAtIsNull(userId)
    }
    
    @Transactional(readOnly = true)
    fun getUserSessions(userId: Long): List<ChatSession> {
        return sessionRepository.findByUserId(userId)
    }
    
    @Transactional(readOnly = true)
    fun getCounselorSessions(counselorId: Long): List<ChatSession> {
        return sessionRepository.findByCounselorId(counselorId)
    }
}