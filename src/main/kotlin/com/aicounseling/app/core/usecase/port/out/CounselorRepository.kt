package com.aicounseling.app.core.usecase.port.out

import com.aicounseling.app.core.domain.Counselor

/**
 * Counselor Repository 인터페이스
 */
interface CounselorRepository {
    fun save(counselor: Counselor): Counselor
    fun findById(id: Long): Counselor?
    fun findAll(): List<Counselor>
    fun findByIsActiveTrue(): List<Counselor>
    fun deleteById(id: Long)
}