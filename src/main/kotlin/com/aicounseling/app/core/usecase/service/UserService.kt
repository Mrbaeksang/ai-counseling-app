package com.aicounseling.app.core.usecase.service

import com.aicounseling.app.core.domain.User
import com.aicounseling.app.core.usecase.port.`in`.UserUseCase
import com.aicounseling.app.core.usecase.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * User 비즈니스 로직 구현
 * 엔티티에서 분리한 메서드들을 여기서 구현
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) : UserUseCase {
    
    override fun processLogin(userId: Long): User {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
        
        // 엔티티에 있던 updateLastLogin() 로직
        user.lastLoginAt = LocalDateTime.now()
        
        return userRepository.save(user)
    }
    
    override fun changeNickname(userId: Long, newNickname: String): User {
        // 엔티티에 있던 changeNickname() 로직 + 비즈니스 규칙
        require(newNickname.length in 2..20) { 
            "닉네임은 2자 이상 20자 이하여야 합니다" 
        }
        
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
        
        user.nickname = newNickname
        
        return userRepository.save(user)
    }
    
    override fun createUser(email: String, nickname: String): User {
        // 이메일 중복 체크
        userRepository.findByEmail(email)?.let {
            throw IllegalArgumentException("이미 존재하는 이메일입니다: $email")
        }
        
        val user = User(
            email = email,
            nickname = nickname
        )
        
        return userRepository.save(user)
    }
    
    @Transactional(readOnly = true)
    override fun findById(userId: Long): User? {
        return userRepository.findById(userId)
    }
    
    @Transactional(readOnly = true)
    override fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
}