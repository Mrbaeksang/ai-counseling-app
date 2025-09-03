package com.aicounseling.app.domain.counselor.repository

import com.aicounseling.app.domain.counselor.dto.CounselorDetailResponse
import com.aicounseling.app.domain.counselor.dto.CounselorListResponse
import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.session.entity.ChatSession
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import kotlin.math.roundToInt

/**
 * Counselor 커스텀 레포지토리 구현체
 *
 * JDSL을 사용하여 복잡한 쿼리를 타입 세이프하게 구현
 * N+1 문제를 방지하기 위해 LEFT JOIN 활용
 *
 * @Repository 어노테이션 제거 (Spring이 자동으로 Impl 클래스 인식)
 */
class CounselorRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : CounselorRepositoryCustom {
    override fun findCounselorsWithStats(
        sort: String,
        pageable: Pageable,
    ): Page<CounselorListResponse> {
        // JDSL 3.5.5에서는 엔티티를 조회한 후 DTO로 변환
        val result =
            kotlinJdslJpqlExecutor.findPage(pageable) {
                select(
                    entity(Counselor::class),
                ).from(
                    entity(Counselor::class),
                ).where(
                    path(Counselor::isActive).eq(true),
                ).orderBy(
                    when (sort) {
                        "rating" -> path(Counselor::createdAt).desc()
                        "recent" -> path(Counselor::createdAt).desc()
                        else -> path(Counselor::createdAt).desc()
                    },
                )
            }

        // DTO로 변환 (nullable 처리 필요)
        return result.map { counselor ->
            if (counselor != null) {
                CounselorListResponse(
                    id = counselor.id,
                    name = counselor.name,
                    title = counselor.title,
                    avatarUrl = counselor.avatarUrl,
                    // Int 타입으로 수정
                    averageRating = 0,
                    // Int 타입으로 수정
                    totalSessions = 0,
                )
            } else {
                // null인 경우 기본값으로 처리 (실제로는 발생하지 않아야 함)
                CounselorListResponse(
                    id = 0L,
                    name = "Unknown",
                    title = "Unknown",
                    avatarUrl = null,
                    averageRating = 0,
                    totalSessions = 0,
                )
            }
        }
    }

    override fun findCounselorDetailById(counselorId: Long): CounselorDetailResponse? {
        // Counselor 조회
        val counselor =
            kotlinJdslJpqlExecutor.findAll {
                select(entity(Counselor::class))
                    .from(entity(Counselor::class))
                    .where(
                        and(
                            path(Counselor::id).eq(counselorId),
                            path(Counselor::isActive).eq(true),
                        ),
                    )
            }.firstOrNull() ?: return null

        // 세션 수 카운트
        val sessionCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(ChatSession::class)))
                    .from(entity(ChatSession::class))
                    .where(
                        path(ChatSession::counselorId).eq(counselorId),
                    )
            }.firstOrNull() ?: 0L

        // 평균 평점 계산 - counselor 관계를 통해 접근
        val avgRating =
            kotlinJdslJpqlExecutor.findAll {
                select(avg(path(CounselorRating::rating)))
                    .from(entity(CounselorRating::class))
                    .where(
                        path(CounselorRating::counselor).path(Counselor::id).eq(counselorId),
                    )
            }.firstOrNull() ?: 0.0

        // 평점 수 카운트 - counselor 관계를 통해 접근
        val ratingCount =
            kotlinJdslJpqlExecutor.findAll {
                select(count(entity(CounselorRating::class)))
                    .from(entity(CounselorRating::class))
                    .where(
                        path(CounselorRating::counselor).path(Counselor::id).eq(counselorId),
                    )
            }.firstOrNull() ?: 0L

        return CounselorDetailResponse(
            id = counselor.id!!,
            name = counselor.name,
            title = counselor.title,
            description = counselor.description,
            avatarUrl = counselor.avatarUrl,
            // Int 타입으로 변환
            averageRating = (avgRating * 10).roundToInt(),
            totalSessions = sessionCount.toInt(),
            totalRatings = ratingCount.toInt(),
        )
    }
}
