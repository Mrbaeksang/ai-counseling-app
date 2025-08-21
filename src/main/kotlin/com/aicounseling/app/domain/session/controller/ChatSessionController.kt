package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.counselor.service.CounselorService
import com.aicounseling.app.domain.session.dto.BookmarkResponse
import com.aicounseling.app.domain.session.dto.MessageResponse
import com.aicounseling.app.domain.session.dto.RateSessionRequest
import com.aicounseling.app.domain.session.dto.RatingResponse
import com.aicounseling.app.domain.session.dto.SendMessageRequest
import com.aicounseling.app.domain.session.dto.SendMessageResponse
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.dto.SessionResponse
import com.aicounseling.app.domain.session.dto.StartSessionRequest
import com.aicounseling.app.domain.session.dto.UpdateSessionTitleRequest
import com.aicounseling.app.domain.session.service.ChatSessionService
import com.aicounseling.app.global.pagination.PageUtils
import com.aicounseling.app.global.pagination.PagedResponse
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ChatSessionController - 상담 세션 관련 API
 *
 * API 명세서 매핑 (8개 엔드포인트):
 * 1. GET /sessions - 세션 목록 조회
 * 2. POST /sessions - 새 세션 시작
 * 3. DELETE /sessions/{id} - 세션 종료
 * 4. GET /sessions/{id}/messages - 메시지 목록 조회
 * 5. POST /sessions/{id}/messages - 메시지 전송 (AI 응답 포함)
 * 6. POST /sessions/{id}/rate - 세션 평가
 * 7. PATCH /sessions/{id}/bookmark - 북마크 토글
 * 8. PATCH /sessions/{id}/title - 제목 수정
 */
@Tag(name = "sessions", description = "상담 세션 관련 API")
@RestController
@RequestMapping("/api/sessions")
class ChatSessionController(
    private val sessionService: ChatSessionService,
    private val counselorService: CounselorService,
    private val rq: Rq,
) {
    /**
     * 1. GET /sessions - 내 상담 세션 목록 조회
     * - 페이징 지원 (page, size)
     * - 북마크 필터링 (bookmarked)
     */
    @Operation(summary = "내 상담 세션 목록 조회")
    @GetMapping
    fun getUserSessions(
        @RequestParam(required = false) bookmarked: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<SessionListResponse>> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val safePagable =
            PageUtils.createPageRequest(
                page = page,
                size = size,
                sortBy = "lastMessageAt",
                direction = Sort.Direction.DESC,
            )

        val sessions = sessionService.getUserSessions(userId, bookmarked ?: false, safePagable)

        val responses =
            sessions.map { session ->
                val counselor = counselorService.findById(session.counselorId)
                SessionListResponse.from(session, counselor)
            }

        return rq.successResponse(
            data = PagedResponse.from(responses),
            message = if (bookmarked == true) "북마크된 세션 조회 성공" else "세션 목록 조회 성공",
        )
    }

    /**
     * 2. POST /sessions - 새 상담 세션 시작
     * - 상담사 ID 필수
     * - 여러 개의 활성 세션 허용 (ChatGPT처럼)
     */
    @Operation(summary = "새 상담 세션 시작")
    @PostMapping
    fun startSession(
        @Valid @RequestBody request: StartSessionRequest,
    ): RsData<SessionResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val session = sessionService.startSession(userId, request.counselorId)
        val counselor = counselorService.findById(session.counselorId)

        return RsData.of(
            "201",
            "세션 시작 성공",
            SessionResponse.from(session, counselor),
        )
    }

    /**
     * 3. DELETE /sessions/{id} - 세션 종료
     * - closedAt 타임스탬프 설정
     */
    @Operation(summary = "세션 종료")
    @DeleteMapping("/{sessionId}")
    fun closeSession(
        @PathVariable sessionId: Long,
    ): RsData<SessionResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val session = sessionService.closeSession(sessionId, userId)
        val counselor = counselorService.findById(session.counselorId)

        return rq.successResponse(
            data = SessionResponse.from(session, counselor),
            message = "세션 종료 성공",
        )
    }

    /**
     * 4. GET /sessions/{id}/messages - 세션 메시지 목록 조회
     * - 페이징 지원
     * - 시간순 정렬 (오래된 것부터)
     */
    @Operation(summary = "세션 메시지 목록 조회")
    @GetMapping("/{sessionId}/messages")
    fun getSessionMessages(
        @PathVariable sessionId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<PagedResponse<MessageResponse>> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val safePagable =
            PageUtils.createPageRequest(
                page = page,
                size = size,
                sortBy = "createdAt",
                direction = Sort.Direction.ASC,
            )

        val messages = sessionService.getSessionMessages(sessionId, userId, safePagable)
        val responses = PagedResponse.from(messages) { MessageResponse.from(it) }

        return rq.successResponse(
            data = responses,
            message = "메시지 조회 성공",
        )
    }

    /**
     * 5. POST /sessions/{id}/messages - 메시지 전송 (AI 응답 포함)
     * - 사용자 메시지 저장
     * - OpenRouter API 호출
     * - AI 응답 저장
     * - 상담 단계(phase) 자동 갱신
     */
    @Operation(summary = "메시지 전송 (AI 응답 포함)")
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: SendMessageRequest,
    ): RsData<SendMessageResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val response =
            sessionService.sendMessage(
                sessionId = sessionId,
                userId = userId,
                content = request.content,
            )

        return RsData.of(
            "201",
            "메시지 전송 성공",
            response,
        )
    }

    /**
     * 6. POST /sessions/{id}/rate - 세션 평가
     * - 1-5점 평점
     * - 선택적 피드백 텍스트
     * - 세션당 1회만 평가 가능
     */
    @Operation(summary = "세션 평가")
    @PostMapping("/{sessionId}/rate")
    fun rateSession(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: RateSessionRequest,
    ): RsData<RatingResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val rating =
            sessionService.rateSession(
                sessionId = sessionId,
                userId = userId,
                rating = request.rating,
                feedback = request.feedback,
            )

        return RsData.of(
            "201",
            "평가 완료",
            RatingResponse(
                id = rating.id,
                sessionId = sessionId,
                counselorId = rating.counselor.id,
                rating = rating.rating.toInt(),
                review = rating.review,
                createdAt = rating.createdAt,
            ),
        )
    }

    /**
     * 7. PATCH /sessions/{id}/bookmark - 세션 북마크 토글
     * - 북마크 상태 반전 (true ↔ false)
     */
    @Operation(summary = "세션 북마크 토글")
    @PatchMapping("/{sessionId}/bookmark")
    fun toggleBookmark(
        @PathVariable sessionId: Long,
    ): RsData<BookmarkResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val session = sessionService.toggleBookmark(sessionId, userId)

        return rq.successResponse(
            data = BookmarkResponse(sessionId, session.isBookmarked),
            message = if (session.isBookmarked) "북마크 추가 성공" else "북마크 제거 성공",
        )
    }

    /**
     * 8. PATCH /sessions/{id}/title - 세션 제목 수정
     * - 1-100자 제한
     */
    @Operation(summary = "세션 제목 수정")
    @PatchMapping("/{sessionId}/title")
    fun updateSessionTitle(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: UpdateSessionTitleRequest,
    ): RsData<SessionResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val session = sessionService.updateSessionTitle(sessionId, userId, request.title)
        val counselor = counselorService.findById(session.counselorId)

        return rq.successResponse(
            data = SessionResponse.from(session, counselor),
            message = "세션 제목 변경 성공",
        )
    }
}
