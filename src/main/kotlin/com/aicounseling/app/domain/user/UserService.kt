package com.aicounseling.app.domain.user
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    
    fun processLogin(userId: Long): User {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
        }
        
        user.lastLoginAt = LocalDateTime.now()
        
        return userRepository.save(user)
    }
    
    fun changeNickname(userId: Long, newNickname: String): User {
        require(newNickname.length in 2..20) { 
            "닉네임은 2자 이상 20자 이하여야 합니다" 
        }
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
        }
        
        user.nickname = newNickname
        
        return userRepository.save(user)
    }
    
    // OAuth 인증을 통해서만 사용자 생성 (AuthService에서 처리)
    // createUser는 제거됨
    
    @Transactional(readOnly = true)
    fun findById(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)
    }
    
    @Transactional(readOnly = true)
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
}