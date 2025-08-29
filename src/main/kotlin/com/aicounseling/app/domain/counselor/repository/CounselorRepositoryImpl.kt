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
        // selectNew 패턴으로 DTO 직접 생성 (Double과 Long으로 받음)
        val result =
            kotlinJdslJpqlExecutor.findPage(pageable) {
                selectNew<TempCounselorListResult>(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    // 평균 평점 (Double로 받음)
                    coalesce(
                        avg(path(CounselorRating::rating)),
                        value(0.0),
                    ),
                    // 총 세션 수 (Long으로 받음)
                    countDistinct(path(ChatSession::id)),
                ).from(
                    entity(Counselor::class),
                    leftJoin(CounselorRating::class).on(
                        path(CounselorRating::counselor).eq(entity(Counselor::class)),
                    ),
                    leftJoin(ChatSession::class).on(
                        path(ChatSession::counselorId).eq(path(Counselor::id)),
                    ),
                ).where(
                    path(Counselor::isActive).eq(true),
                ).groupBy(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    path(Counselor::createdAt),
                ).orderBy(
                    when (sort) {
                        "popular" -> countDistinct(path(ChatSession::id)).desc()
                        "rating" -> coalesce(avg(path(CounselorRating::rating)), value(0.0)).desc()
                        else -> path(Counselor::createdAt).desc()
                    },
                )
            }

        // 임시 결과를 최종 DTO로 변환 (타입 변환 처리)
        return result.map { temp ->
            requireNotNull(temp) { "TempCounselorListResult should not be null" }
            CounselorListResponse(
                id = temp.id,
                name = temp.name,
                title = temp.title,
                avatarUrl = temp.avatarUrl,
                averageRating = temp.averageRating.roundToInt(),
                totalSessions = temp.totalSessions.toInt(),
            )
        }
    }

    override fun findCounselorDetailById(counselorId: Long): CounselorDetailResponse? {
        // selectNew으로 임시 결과 생성
        val result =
            kotlinJdslJpqlExecutor.findAll {
                selectNew<TempCounselorDetailResult>(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    // 평균 평점 (Double로 받음)
                    coalesce(
                        avg(path(CounselorRating::rating)),
                        value(0.0),
                    ),
                    // 총 세션 수 (Long으로 받음)
                    countDistinct(path(ChatSession::id)),
                    // 상세 설명
                    path(Counselor::description),
                    // 총 평가 수 (Long으로 받음)
                    countDistinct(path(CounselorRating::id)),
                ).from(
                    entity(Counselor::class),
                    leftJoin(CounselorRating::class).on(
                        path(CounselorRating::counselor).eq(entity(Counselor::class)),
                    ),
                    leftJoin(ChatSession::class).on(
                        path(ChatSession::counselorId).eq(path(Counselor::id)),
                    ),
                ).where(
                    and(
                        path(Counselor::id).eq(counselorId),
                        path(Counselor::isActive).eq(true),
                    ),
                ).groupBy(
                    path(Counselor::id),
                    path(Counselor::name),
                    path(Counselor::title),
                    path(Counselor::avatarUrl),
                    path(Counselor::description),
                )
            }

        // 임시 결과를 최종 DTO로 변환
        return result.firstOrNull()?.let { temp ->
            CounselorDetailResponse(
                id = temp.id,
                name = temp.name,
                title = temp.title,
                avatarUrl = temp.avatarUrl,
                averageRating = temp.averageRating.roundToInt(),
                totalSessions = temp.totalSessions.toInt(),
                description = temp.description,
                totalRatings = temp.totalRatings.toInt(),
                // Service에서 사용자별로 설정
                isFavorite = false,
            )
        }
    }
}

/**
 * JDSL selectNew용 임시 결과 클래스 (목록용)
 * Double과 Long 타입으로 받아서 나중에 Int로 변환
 */
data class TempCounselorListResult(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Double,
    val totalSessions: Long,
)

/**
 * JDSL selectNew용 임시 결과 클래스 (상세용)
 * Double과 Long 타입으로 받아서 나중에 Int로 변환
 */
data class TempCounselorDetailResult(
    val id: Long,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val averageRating: Double,
    val totalSessions: Long,
    val description: String?,
    val totalRatings: Long,
)
