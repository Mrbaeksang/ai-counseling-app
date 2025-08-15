package com.aicounseling.app.core.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * ChatSession 엔티티 - 상담 대화 세션
 * 심플하게 구현 (YAGNI 원칙 적용)
 */
@Entity
@Table(name = "chat_sessions")
class ChatSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(name = "counselor_id", nullable = false)
    val counselorId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var phase: CounselingPhase = CounselingPhase.RAPPORT_BUILDING,
    
    @Column(name = "phase_metadata", columnDefinition = "TEXT")
    var phaseMetadata: String = "",  // AI가 단계 전환한 이유/근거
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null  // null이면 진행중, 값 있으면 종료됨
) {
    /**
     * 상담 단계 업데이트
     * AI가 대화 맥락 보고 판단해서 호출
     */
    fun updatePhase(newPhase: CounselingPhase, reason: String) {
        this.phase = newPhase
        this.phaseMetadata = reason
    }
    
    /**
     * 세션 종료
     * 사용자가 명시적으로 종료하거나 AI가 마무리 제안 후 동의 시
     */
    fun close() {
        this.closedAt = LocalDateTime.now()
    }
    
    /**
     * 세션 활성 여부 확인
     */
    fun isActive(): Boolean = closedAt == null
}