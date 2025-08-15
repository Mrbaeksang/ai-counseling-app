package com.aicounseling.app.domain.counselor.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Counselor 엔티티 - AI 상담사 정보 (순수 데이터만)
 */
@Entity
@Table(name = "counselors")
data class Counselor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 50)
    val name: String,  // "소크라테스"
    
    @Column(nullable = false, length = 100)
    val title: String,  // "고대 그리스 철학자"
    
    @Column(nullable = false, length = 500)
    val description: String,  // "질문을 통해 스스로 답을 찾도록 돕습니다"
    
    @Column(name = "personality_matrix", nullable = false, columnDefinition = "TEXT")
    val personalityMatrix: String,  // JSON {"logical": 95, "directness": 80, ...}
    
    @Column(name = "base_prompt", nullable = false, columnDefinition = "TEXT")
    var basePrompt: String,  // AI 프롬프트
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,  // 활성화 여부
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // @Transient 필드들 (DB 저장 안함, 런타임 계산용)
    @Transient
    var totalSessions: Int = 0  // COUNT 쿼리로 계산
    
    @Transient
    var averageRating: Double = 0.0  // AVG 쿼리로 계산
    
    @Transient
    var specialtyTags: List<String> = emptyList()  // 전문 분야 태그
}