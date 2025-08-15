package com.aicounseling.app.core.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Counselor 엔티티 - AI 상담사 정보를 담는 도메인 객체
 * 
 * @Entity = "이거 DB 테이블이야!"
 * @Table = "테이블 이름은 counselors로 해줘" (counsel 아니고 counselors!)
 */
@Entity
@Table(name = "counselors")  // ❌ counsel → ✅ counselors
class Counselor(
    @Id  // Primary Key (고유 식별자)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB가 자동으로 ID 생성
    val id: Long = 0,  // 기본값 0 (아직 저장 안 됨)
    
    @Column(nullable = false, length = 50)  // NULL 안됨, 최대 50자
    val name: String,  // ⭐ "소크라테스" - 이 필드가 빠져있었음!
    
    @Column(nullable = false, length = 100)  // 최대 100자
    val title: String,  // "고대 그리스 철학자"
    
    @Column(nullable = false, length = 500)  // 최대 500자
    val description: String,  // "질문을 통해 스스로 답을 찾도록 돕습니다"
    
    @Column(name = "personality_matrix", nullable = false, columnDefinition = "TEXT")
    val personalityMatrix: String,  // JSON {"logical": 95, "directness": 80, ...}
    // val = 상담사 성격은 안 바뀜 (소크라테스는 영원히 논리적)
    
    @Column(name = "base_prompt", nullable = false, columnDefinition = "TEXT")
    var basePrompt: String,  // AI 프롬프트 (개선 가능하니까 var)
    // var = 사용자 피드백으로 프롬프트 개선 가능
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,  // ❌ false → ✅ true (처음엔 활성화)
    // var = 상담사 비활성화 가능 (부적절한 응답 시)
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),  // 생성 시간 자동 기록
    
    @Column(name = "updated_at", nullable = false) 
    var updatedAt: LocalDateTime = LocalDateTime.now()  // 수정 시간 (프롬프트 개선 시)
) {
    /**
     * @Transient = "이건 DB에 저장 안 해!"
     * 실행 시에만 계산해서 쓰는 임시 값들
     */
    @Transient
    var totalSessions: Int = 0  // DB에서 COUNT 쿼리로 계산
    
    @Transient
    var averageRating: Double = 0.0  // DB에서 AVG 쿼리로 계산
    
    @Transient
    var specialtyTags: List<String> = emptyList()  // ❌ specialityTags → ✅ specialtyTags
    // 예: ["연애", "진로", "가족"] - 이 상담사가 잘하는 분야
    
    /**
     * 상담사 비활성화 메서드
     * 부적절한 응답이나 문제 발생 시 관리자가 호출
     */
    fun deactivate() {
        this.isActive = false
        this.updatedAt = LocalDateTime.now()  // 언제 비활성화했는지 기록
    }
    
    /**
     * AI 프롬프트 업데이트 메서드
     * 사용자 피드백 기반으로 더 나은 프롬프트로 개선
     * 
     * @param newPrompt 개선된 새 프롬프트
     */
    fun updatePrompt(newPrompt: String) {
        this.basePrompt = newPrompt  // 새 프롬프트로 교체
        this.updatedAt = LocalDateTime.now()  // 언제 수정했는지 기록
    }
    
    // toString은 디버깅할 때 유용!
    override fun toString(): String {
        return "Counselor(id=$id, name='$name', title='$title', active=$isActive)"
    }
}