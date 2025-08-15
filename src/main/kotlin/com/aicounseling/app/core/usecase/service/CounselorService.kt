package com.aicounseling.app.core.usecase.service

import com.aicounseling.app.core.domain.Counselor
import com.aicounseling.app.core.usecase.port.`in`.CounselorStatistics
import com.aicounseling.app.core.usecase.port.`in`.CounselorUseCase
import com.aicounseling.app.core.usecase.port.out.CounselorRepository
import com.aicounseling.app.core.usecase.port.out.ChatSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Counselor 비즈니스 로직 구현
 */
@Service
@Transactional
class CounselorService(
    private val counselorRepository: CounselorRepository,
    private val sessionRepository: ChatSessionRepository
) : CounselorUseCase {
    
    override fun deactivateCounselor(counselorId: Long): Counselor {
        val counselor = counselorRepository.findById(counselorId)
            ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        
        counselor.isActive = false
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    override fun activateCounselor(counselorId: Long): Counselor {
        val counselor = counselorRepository.findById(counselorId)
            ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        
        counselor.isActive = true
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    override fun updatePrompt(counselorId: Long, newPrompt: String): Counselor {
        require(newPrompt.isNotBlank()) { "프롬프트는 비어있을 수 없습니다" }
        
        val counselor = counselorRepository.findById(counselorId)
            ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        
        counselor.basePrompt = newPrompt
        counselor.updatedAt = LocalDateTime.now()
        
        return counselorRepository.save(counselor)
    }
    
    @Transactional(readOnly = true)
    override fun findById(counselorId: Long): Counselor? {
        return counselorRepository.findById(counselorId)
    }
    
    @Transactional(readOnly = true)
    override fun findAllActive(): List<Counselor> {
        return counselorRepository.findByIsActiveTrue()
    }
    
    @Transactional(readOnly = true)
    override fun findBySpecialty(category: String): List<Counselor> {
        // TODO: 카테고리별 조회 구현 (CounselingCategory 엔티티 필요)
        return counselorRepository.findByIsActiveTrue()
    }
    
    @Transactional(readOnly = true)
    override fun getCounselorStatistics(counselorId: Long): CounselorStatistics {
        val sessions = sessionRepository.findByCounselorId(counselorId)
        
        // TODO: 평점은 CounselorRating 엔티티 구현 후 계산
        return CounselorStatistics(
            totalSessions = sessions.size,
            averageRating = 0.0,  // 추후 구현
            specialtyTags = emptyList(),  // 추후 구현
            mostRecentSessionDate = sessions.maxByOrNull { it.createdAt }?.createdAt
        )
    }
}