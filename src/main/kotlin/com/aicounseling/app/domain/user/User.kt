package com.aicounseling.app.domain.user

import com.aicounseling.app.global.security.AuthProvider
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

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false, length = 100)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    val authProvider: AuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,  // Google/Kakao/Naver에서 제공하는 고유 ID

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
)