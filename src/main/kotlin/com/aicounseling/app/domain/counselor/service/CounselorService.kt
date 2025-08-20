package com.aicounseling.app.domain.counselor.service

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CounselorService - 상담사 비즈니스 로직
 *
 * API 명세서 매핑:
 * - GET /counselors → findAllWithSort()
 * - GET /counselors/{id} → findById()
 * - GET /counselors/favorites → getFavoriteCounselors()
 * - POST /counselors/{id}/favorite → addFavorite()
 * - DELETE /counselors/{id}/favorite → removeFavorite()
 */
@Service
@Transactional(readOnly = true)
class CounselorService(
    private val counselorRepository: CounselorRepository,
    private val favoriteCounselorRepository: FavoriteCounselorRepository,
    private val sessionRepository: ChatSessionRepository,
    private val ratingRepository: CounselorRatingRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val MIN_RATING = 1
        private const val MAX_RATING = 5
    }

    /**
     * GET /counselors/{id} - 상담사 상세 조회
     * 통계 정보 포함해서 반환
     */
    fun findById(counselorId: Long): Counselor {
        val counselor =
            counselorRepository.findById(counselorId).orElseThrow {
                NoSuchElementException("상담사를 찾을 수 없습니다: $counselorId")
            }

        // 통계 정보 설정
        counselor.totalSessions = sessionRepository.countByCounselorId(counselor.id).toInt()
        counselor.averageRating = calculateAverageRating(counselor.id)
        counselor.specialtyTags = objectMapper.readValue<List<String>>(counselor.specialties)

        return counselor
    }

    /**
     * GET /counselors?sort={sort} - 상담사 목록 조회
     * @param sort: popular(인기순), rating(평점순), recent(최신순), null(기본)
     * @return 상담사 목록 (없으면 빈 리스트)
     */
    fun findAllWithSort(sort: String?): List<Counselor> {
        // Repository 메서드로 조회 (없으면 빈 리스트 반환)
        val counselors =
            when (sort) {
                "recent" -> counselorRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                else -> counselorRepository.findByIsActiveTrue() // popular, rating은 메모리 정렬
            }

        // 상담사가 없으면 빈 리스트 즉시 반환
        if (counselors.isEmpty()) {
            return emptyList()
        }

        // 각 상담사의 통계 정보 계산
        counselors.forEach { counselor ->
            counselor.totalSessions = sessionRepository.countByCounselorId(counselor.id).toInt()
            counselor.averageRating = calculateAverageRating(counselor.id)
            counselor.specialtyTags = objectMapper.readValue<List<String>>(counselor.specialties)
        }

        // 메모리에서 정렬 (popular, rating)
        return when (sort) {
            "popular" -> counselors.sortedByDescending { it.totalSessions }
            "rating" -> counselors.sortedByDescending { it.averageRating }
            else -> counselors
        }
    }

    /**
     * GET /counselors/favorites - 사용자의 즐겨찾기 상담사 목록
     */
    fun getFavoriteCounselors(user: User): List<Counselor> {
        val favorites = favoriteCounselorRepository.findByUser(user)
        val counselors = favorites.map { it.counselor }

        // 통계 정보 추가
        counselors.forEach { counselor ->
            counselor.totalSessions = sessionRepository.countByCounselorId(counselor.id).toInt()
            counselor.averageRating = calculateAverageRating(counselor.id)
            counselor.specialtyTags = objectMapper.readValue<List<String>>(counselor.specialties)
        }

        return counselors
    }

    /**
     * POST /counselors/{id}/favorite - 즐겨찾기 추가
     * @return 즐겨찾기에 추가된 상담사
     */
    @Transactional
    fun addFavorite(
        user: User,
        counselorId: Long,
    ): Counselor {
        val counselor =
            counselorRepository.findById(counselorId).orElseThrow {
                NoSuchElementException("상담사를 찾을 수 없습니다: $counselorId")
            }

        // 중복 체크
        check(!favoriteCounselorRepository.existsByUserAndCounselor(user, counselor)) {
            "이미 즐겨찾기한 상담사입니다"
        }

        val favorite =
            FavoriteCounselor(
                user = user,
                counselor = counselor,
            )
        favoriteCounselorRepository.save(favorite)

        return counselor
    }

    /**
     * DELETE /counselors/{id}/favorite - 즐겨찾기 삭제
     * @return 즐겨찾기에서 제거된 상담사
     */
    @Transactional
    fun removeFavorite(
        user: User,
        counselorId: Long,
    ): Counselor {
        val counselor =
            counselorRepository.findById(counselorId).orElseThrow {
                NoSuchElementException("상담사를 찾을 수 없습니다: $counselorId")
            }
        favoriteCounselorRepository.deleteByUserAndCounselor(user, counselor)

        return counselor
    }

    @Transactional
    fun addRating(
        sessionId: Long,
        userId: Long,
        counselorId: Long,
        rating: Int,
        feedback: String?,
    ): CounselorRating {
        check(!ratingRepository.existsBySessionId(sessionId)) {
            "이미 평가한 세션입니다"
        }

        check(rating in MIN_RATING..MAX_RATING) {
            "평점은 $MIN_RATING-$MAX_RATING 사이여야 합니다"
        }

        val user =
            userRepository.findById(userId).orElseThrow {
                IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
            }

        val counselor =
            counselorRepository.findById(counselorId).orElseThrow {
                IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
            }

        val session =
            sessionRepository.findById(sessionId).orElseThrow {
                IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
            }

        val counselorRating =
            CounselorRating(
                user = user,
                counselor = counselor,
                session = session,
                rating = rating.toDouble(),
                review = feedback,
            )

        val savedRating = ratingRepository.save(counselorRating)

        counselor.averageRating = calculateAverageRating(counselorId)
        counselorRepository.save(counselor)

        return savedRating
    }

    /**
     * 평균 평점 계산 (내부 헬퍼 메서드)
     * CounselorRating 테이블에서 평점만 추출해서 평균 계산
     */
    private fun calculateAverageRating(counselorId: Long): Double {
        val ratings = ratingRepository.findByCounselorId(counselorId)

        return if (ratings.isNotEmpty()) {
            ratings.map { it.rating }.average() // rating 필드만 추출해서 평균
        } else {
            0.0 // 평가 없으면 0점
        }
    }
}
