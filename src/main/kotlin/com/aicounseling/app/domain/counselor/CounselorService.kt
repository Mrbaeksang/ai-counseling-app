package com.aicounseling.app.domain.counselor.service

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CounselorService(
    private val counselorRepository: CounselorRepository,
    private val sessionRepository: ChatSessionRepository
) {
    
    fun deactivateCounselor(counselorId: Long): Counselor {
        val counselor = counselorRepository.findById(counselorId).orElseThrow {
            IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        }
        
        counselor.isActive = false
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    fun activateCounselor(counselorId: Long): Counselor {
        val counselor = counselorRepository.findById(counselorId).orElseThrow {
            IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        }
        
        counselor.isActive = true
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    fun updatePrompt(counselorId: Long, newPrompt: String): Counselor {
        require(newPrompt.isNotBlank()) { "프롬프트는 비어있을 수 없습니다" }
        
        val counselor = counselorRepository.findById(counselorId).orElseThrow {
            IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        }
        
        counselor.basePrompt = newPrompt
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    @Transactional(readOnly = true)
    fun findById(counselorId: Long): Counselor? {
        return counselorRepository.findById(counselorId).orElse(null)
    }
    
    @Transactional(readOnly = true)
    fun findAllActive(): List<Counselor> {
        return counselorRepository.findByIsActiveTrue()
    }
    
    @Transactional(readOnly = true)
    fun getCounselorStatistics(counselorId: Long): CounselorStatistics {
        val sessions = sessionRepository.findByCounselorId(counselorId)
        
        return CounselorStatistics(
            totalSessions = sessions.size,
            averageRating = 0.0,
            specialtyTags = emptyList(),
            mostRecentSessionDate = sessions.maxByOrNull { it.createdAt }?.createdAt
        )
    }
}

data class CounselorStatistics(
    val totalSessions: Int,
    val averageRating: Double,
    val specialtyTags: List<String>,
    val mostRecentSessionDate: LocalDateTime?
)