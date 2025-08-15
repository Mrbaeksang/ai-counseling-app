package com.aicounseling.app.core.usecase.port.out

import com.aicounseling.app.core.domain.User

/**
 * User Repository 인터페이스
 * 데이터 접근 계층 추상화
 */
interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun findAll(): List<User>
    fun deleteById(id: Long)
    fun existsByEmail(email: String): Boolean
}