package com.aicounseling.app.domain.counselor

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * CounselorRepository - 상담사 데이터 액세스
 */
@Repository
interface CounselorRepository : JpaRepository<Counselor, Long> {
    /**
     * 활성화된 상담사만 조회
     */
    fun findByIsActiveTrue(): List<Counselor>

    /**
     * 활성화된 상담사 최신순 조회
     */
    fun findByIsActiveTrueOrderByCreatedAtDesc(): List<Counselor>
}
