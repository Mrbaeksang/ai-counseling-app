package com.aicounseling.app.core.usecase.port.`in`

import com.aicounseling.app.core.domain.User
import java.time.LocalDateTime

/**
 * User 관련 비즈니스 로직 인터페이스
 * 엔티티에서 분리한 메서드들을 여기서 정의
 */
interface UserUseCase {
    
    /**
     * 사용자 로그인 처리
     * - lastLoginAt 업데이트
     * - 로그인 이력 저장 (추후 구현)
     */
    fun processLogin(userId: Long): User
    
    /**
     * 닉네임 변경
     * - 비즈니스 규칙: 2자 이상 20자 이하
     * - 금지어 체크 (추후 구현)
     */
    fun changeNickname(userId: Long, newNickname: String): User
    
    /**
     * 사용자 생성
     * - 이메일 중복 체크
     * - 기본 닉네임 생성
     */
    fun createUser(email: String, nickname: String): User
    
    /**
     * 사용자 조회
     */
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
}