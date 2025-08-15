package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.entity.Counselor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CounselorRepository : JpaRepository<Counselor, Long> {
    fun findByIsActiveTrue(): List<Counselor>
}