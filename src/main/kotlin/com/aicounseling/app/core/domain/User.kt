package com.aicounseling.app.core.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User 엔티티 - 사용자 정보를 담는 도메인 객체
 * 
 * @Entity = "이거 DB 테이블이야!"
 * @Table = "테이블 이름은 users로 해줘"
 */
@Entity
@Table(name = "users")
class User(
    @Id  // Primary Key (주민번호 같은 고유 식별자)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB가 자동으로 ID 생성
    val id: Long = 0,  // 기본값 0 (아직 저장 안 됨)
    
    @Column(nullable = false, unique = true)  // NULL 안됨, 중복 안됨
    val email: String,
    
    @Column(nullable = false, length = 100)  // 최대 100자
    var nickname: String,  // var = 수정 가능 (닉네임 변경)
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),  // 자동으로 현재 시간
    
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null  // ? = null 가능 (처음엔 로그인 기록 없음)
) {
    /**
     * 로그인 시간 업데이트 메서드
     * Service Layer에서 호출할 비즈니스 로직
     */
    fun updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now()
    }
    
    /**
     * 닉네임 변경 메서드
     * 비즈니스 규칙: 닉네임은 2자 이상
     */
    fun changeNickname(newNickname: String) {
        require(newNickname.length >= 2) { "닉네임은 2자 이상이어야 합니다" }
        this.nickname = newNickname
    }
    
    // toString은 디버깅할 때 유용!
    override fun toString(): String {
        return "User(id=$id, email='$email', nickname='$nickname')"
    }
}