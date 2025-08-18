package com.aicounseling.app.domain.counselor

import com.aicounseling.app.domain.session.ChatSession
import com.aicounseling.app.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * CounselorRating 엔티티 - 상담사 평가
 * 세션 종료 후 사용자가 남기는 평가
 */
@Entity
@Table(name = "counselor_ratings")
data class CounselorRating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    val counselor: Counselor,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ChatSession,
    @Column(nullable = false)
    val rating: Double, // 0.5~5.0 (0.5 단위, 예: 3.5, 4.0, 4.5)
    @Column(columnDefinition = "TEXT")
    val review: String? = null, // 선택적 텍스트 리뷰
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
