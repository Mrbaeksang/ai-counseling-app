package com.aicounseling.app.core.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User 엔티티 - 사용자 정보 (순수 데이터만)
 * 비즈니스 로직은 UserService로 이동
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val email: String,
    
    @Column(nullable = false, length = 100)
    var nickname: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
)