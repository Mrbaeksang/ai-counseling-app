package com.aicounseling.app.domain.user.service

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UserService - 사용자 비즈니스 로직
 *
 * API 명세서 매핑:
 * - GET /users/me → getUser()
 * - PATCH /users/nickname → changeNickname()
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    companion object {
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20
    }

    /**
     * GET /users/me 지원 - 사용자 조회
     * @throws NoSuchElementException 사용자 없을 때
     */
    fun getUser(userId: Long): User {
        return userRepository.findById(userId).orElseThrow {
            NoSuchElementException("사용자를 찾을 수 없습니다: $userId")
        }
    }

    /**
     * PATCH /users/nickname - 닉네임 변경
     * @throws IllegalArgumentException 닉네임 길이 제한
     * @throws NoSuchElementException 사용자 없을 때
     */
    @Transactional
    fun changeNickname(
        userId: Long,
        newNickname: String,
    ): User {
        val trimmedNickname = newNickname.trim()
        require(trimmedNickname.length in MIN_NICKNAME_LENGTH..MAX_NICKNAME_LENGTH) {
            "닉네임은 ${MIN_NICKNAME_LENGTH}자 이상 ${MAX_NICKNAME_LENGTH}자 이하여야 합니다"
        }

        val user = getUser(userId) // 위 메서드 재사용
        user.nickname = trimmedNickname

        return userRepository.save(user)
    }

    /**
     * OAuth 로그인시 이메일로 사용자 찾기 (AuthService용)
     * null 반환 = 신규 가입 필요
     */
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
}
