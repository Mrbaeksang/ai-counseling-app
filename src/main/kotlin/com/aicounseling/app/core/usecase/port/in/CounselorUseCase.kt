package com.aicounseling.app.core.usecase.port.`in`

import com.aicounseling.app.core.domain.Counselor

/**
 * Counselor 관련 비즈니스 로직 인터페이스
 */
interface CounselorUseCase {
    
    /**
     * 상담사 활성화/비활성화
     */
    fun deactivateCounselor(counselorId: Long): Counselor
    fun activateCounselor(counselorId: Long): Counselor
    
    /**
     * 상담사 프롬프트 업데이트
     * - 관리자만 수정 가능 (권한 체크는 Controller에서)
     */
    fun updatePrompt(counselorId: Long, newPrompt: String): Counselor
    
    /**
     * 상담사 조회
     */
    fun findById(counselorId: Long): Counselor?
    fun findAllActive(): List<Counselor>
    fun findBySpecialty(category: String): List<Counselor>
    
    /**
     * 상담사 통계 조회
     * - 총 세션 수
     * - 평균 평점
     * - 전문 분야 태그
     */
    fun getCounselorStatistics(counselorId: Long): CounselorStatistics
}

/**
 * 상담사 통계 DTO
 */
data class CounselorStatistics(
    val totalSessions: Int,
    val averageRating: Double,
    val specialtyTags: List<String>,
    val mostRecentSessionDate: java.time.LocalDateTime?
)