package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.entity.CounselorRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * CounselorRatingRepository - 상담사 평가 데이터 액세스
 */
@Repository
interface CounselorRatingRepository : JpaRepository<CounselorRating, Long> {
    /**
     * 특정 상담사의 모든 평가 조회
     */
    fun findByCounselorId(counselorId: Long): List<CounselorRating>

    /**
     * 특정 세션에 대한 평가 조회 (중복 방지용)
     */
    fun findBySessionId(sessionId: Long): CounselorRating?

    /**
     * 특정 세션에 평가가 있는지 확인
     */
    fun existsBySessionId(sessionId: Long): Boolean
}
