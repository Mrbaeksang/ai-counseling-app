package com.aicounseling.app.domain.session

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * ChatSession 엔티티 - 상담 대화 세션 (순수 데이터만)
 * 비즈니스 로직은 ChatSessionService로 이동
 */
@Entity
@Table(name = "chat_sessions")
data class ChatSession(
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
    var phaseMetadata: String = "",
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,
    @Column(name = "is_bookmarked", nullable = false)
    var isBookmarked: Boolean = false,
)
