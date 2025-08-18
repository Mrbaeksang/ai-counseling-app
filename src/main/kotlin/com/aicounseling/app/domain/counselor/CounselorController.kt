package com.aicounseling.app.domain.counselor

import com.aicounseling.app.domain.user.UserService
import com.aicounseling.app.global.exception.UnauthorizedException
import com.aicounseling.app.global.rsData.RsData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/counselors")
class CounselorController(
    private val counselorService: CounselorService,
    private val objectMapper: ObjectMapper,
    private val userService: UserService,
) {
    /**
     * GET /counselors?sort={sort} - 상담사 목록 조회
     * @param sort: popular(인기순), rating(평점순), recent(최신순)
     */
    @GetMapping
    fun getCounselors(
        @RequestParam(required = false) sort: String?,
    ): RsData<List<CounselorListResponse>> {
        val counselors = counselorService.findAllWithSort(sort)

        val response =
            counselors.map {
                CounselorListResponse(
                    id = it.id,
                    name = it.name,
                    title = it.title,
                    description = it.description,
                    totalSessions = it.totalSessions,
                    averageRating = it.averageRating,
                )
            }

        return RsData.of(
            "200",
            "상담사 목록 조회 성공",
            response,
        )
    }

    /**
     * GET /counselors/{id} - 상담사 상세 조회
     */
    @GetMapping("/{id}")
    fun getCounselor(
        @PathVariable id: Long,
    ): RsData<CounselorDetailResponse> {
        val counselor = counselorService.findById(id)

        val response =
            CounselorDetailResponse(
                id = counselor.id,
                name = counselor.name,
                title = counselor.title,
                description = counselor.description,
                personalityMatrix = objectMapper.readValue(counselor.personalityMatrix),
                specialties = counselor.specialtyTags,
                totalSessions = counselor.totalSessions,
                averageRating = counselor.averageRating,
            )

        return RsData.of("200", "상담사 조회 성공", response)
    }

    @GetMapping("/favorites")
    fun getFavoriteCounselors(
        @AuthenticationPrincipal userId: Long?,
    ): RsData<List<CounselorListResponse>> {
        if (userId == null) {
            throw UnauthorizedException("인증이 필요합니다")
        }
        val user = userService.getUser(userId)
        val counselors = counselorService.getFavoriteCounselors(user)

        val response =
            counselors.map {
                CounselorListResponse(
                    id = it.id,
                    name = it.name,
                    title = it.title,
                    description = it.description,
                    totalSessions = it.totalSessions,
                    averageRating = it.averageRating,
                )
            }

        return RsData.of("200", "즐겨찾기 목록 조회 성공", response)
    }

    @PostMapping("/{id}/favorite")
    fun addFavorite(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long?,
    ): RsData<String> {
        if (userId == null) {
            throw UnauthorizedException("인증이 필요합니다")
        }
        val user = userService.getUser(userId)
        val counselor =
            counselorService.addFavorite(
                user = user,
                counselorId = id,
            )

        return RsData.of("201", "즐겨찾기 추가 성공", "상담사 ${counselor.name}을(를) 즐겨찾기에 추가했습니다.")
    }

    @DeleteMapping("/{id}/favorite")
    fun removeFavorite(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long?,
    ): RsData<String> {
        if (userId == null) {
            throw UnauthorizedException("인증이 필요합니다")
        }
        val user = userService.getUser(userId)
        val counselor =
            counselorService.removeFavorite(
                user = user,
                counselorId = id,
            )

        return RsData.of("204", "즐겨찾기 제거 성공", "상담사 ${counselor.name}을(를) 즐겨찾기에서 제거했습니다.")
    }
}
