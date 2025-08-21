package com.aicounseling.app.domain.counselor.entity

import com.aicounseling.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient

/**
 * Counselor 엔티티 - AI 상담사 정보 (순수 데이터만)
 *
 * name: 상담사 이름 (예: "소크라테스")
 * title: 직함 (예: "고대 그리스 철학자")
 * description: 상담사 소개 (예: "질문을 통해 스스로 답을 찾도록 돕습니다")
 * personalityMatrix: 성격 특성 JSON
 * basePrompt: AI 프롬프트
 * specialties: 전문 분야 JSON 배열
 *
 * @Transient 필드들은 DB 저장 안함, 런타임 계산용
 * totalSessions: COUNT 쿼리로 계산
 * averageRating: AVG 쿼리로 계산
 */
@Entity
@Table(name = "counselors")
class Counselor(
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(nullable = false, length = 100)
    val title: String,
    @Column(nullable = false, length = 500)
    val description: String,
    @Column(name = "personality_matrix", nullable = false, columnDefinition = "TEXT")
    val personalityMatrix: String,
    @Column(name = "base_prompt", nullable = false, columnDefinition = "TEXT")
    var basePrompt: String,
    @Column(name = "specialties", nullable = false, columnDefinition = "TEXT")
    val specialties: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity() {
    @Transient
    var totalSessions: Int = 0

    @Transient
    var averageRating: Double = 0.0

    @Transient
    var specialtyTags: List<String> = emptyList() // 전문 분야 태그
}
