package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.dto.RateSessionRequest
import com.aicounseling.app.domain.session.dto.SendMessageRequest
import com.aicounseling.app.domain.session.dto.SendMessageResponse
import com.aicounseling.app.domain.session.dto.UpdateSessionTitleRequest
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.service.ChatSessionService
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
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
    private val rq: Rq,
) {
    /**
     * 1. GET /sessions - 내 상담 세션 목록 조회
     */
    @Operation(summary = "내 상담 세션 목록 조회")
    @GetMapping
    fun getUserSessions(
        @RequestParam(required = false) bookmarked: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<List<ChatSession>> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"))
        val sessions = sessionService.getUserSessions(userId, bookmarked ?: false, pageable)

        return RsData.of(
            "S-1",
            if (bookmarked == true) "북마크된 세션 조회 성공" else "세션 목록 조회 성공",
            sessions.content,
        )
    }

    /**
     * 2. POST /sessions - 새 상담 세션 시작
     */
    @Operation(summary = "새 상담 세션 시작")
    @PostMapping
    fun startSession(
        @RequestParam counselorId: Long,
    ): RsData<ChatSession> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val session = sessionService.startSession(userId, counselorId)

        return RsData.of(
            "S-1",
            "세션 시작 성공",
            session,
        )
    }

    /**
     * 3. DELETE /sessions/{id} - 세션 종료
     */
    @Operation(summary = "세션 종료")
    @DeleteMapping("/{sessionId}")
    fun closeSession(
        @PathVariable sessionId: Long,
    ): RsData<ChatSession> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val session = sessionService.closeSession(sessionId, userId)

        return RsData.of(
            "S-1",
            "세션 종료 성공",
            session,
        )
    }

    /**
     * 4. GET /sessions/{id}/messages - 세션 메시지 목록 조회
     */
    @Operation(summary = "세션 메시지 목록 조회")
    @GetMapping("/{sessionId}/messages")
    fun getSessionMessages(
        @PathVariable sessionId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): RsData<List<Message>> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
        val messages = sessionService.getSessionMessages(sessionId, userId, pageable)

        return RsData.of(
            "S-1",
            "메시지 조회 성공",
            messages.content,
        )
    }

    /**
     * 5. POST /sessions/{id}/messages - 메시지 전송 (AI 응답 포함)
     */
    @Operation(summary = "메시지 전송 (AI 응답 포함)")
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: SendMessageRequest,
    ): RsData<SendMessageResponse> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val (userMessage, aiMessage) =
            sessionService.sendMessage(
                sessionId = sessionId,
                userId = userId,
                content = request.content,
            )

        // 세션 정보 조회 (제목 업데이트 확인용)
        val session = sessionService.getSession(sessionId, userId)

        return RsData.of(
            "S-1",
            "메시지 전송 성공",
            SendMessageResponse.from(userMessage, aiMessage, session),
        )
    }

    /**
     * 6. POST /sessions/{id}/rate - 세션 평가
     */
    @Operation(summary = "세션 평가")
    @PostMapping("/{sessionId}/rate")
    fun rateSession(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: RateSessionRequest,
    ): RsData<Any> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val rating =
            sessionService.rateSession(
                sessionId = sessionId,
                userId = userId,
                rating = request.rating,
                feedback = request.feedback,
            )

        return RsData.of(
            "S-1",
            "평가 완료",
            mapOf(
                "id" to rating.id,
                "sessionId" to sessionId,
                "counselorId" to rating.counselor.id,
                "rating" to rating.rating,
                "feedback" to rating.review,
                "createdAt" to rating.createdAt,
            ),
        )
    }

    /**
     * 7. PATCH /sessions/{id}/bookmark - 세션 북마크 토글
     */
    @Operation(summary = "세션 북마크 토글")
    @PatchMapping("/{sessionId}/bookmark")
    fun toggleBookmark(
        @PathVariable sessionId: Long,
    ): RsData<Map<String, Any>> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val isBookmarked = sessionService.toggleBookmark(sessionId, userId)

        return RsData.of(
            "S-1",
            if (isBookmarked) "북마크 추가 성공" else "북마크 제거 성공",
            mapOf(
                "sessionId" to sessionId,
                "isBookmarked" to isBookmarked,
            ),
        )
    }

    /**
     * 8. PATCH /sessions/{id}/title - 세션 제목 수정
     */
    @Operation(summary = "세션 제목 수정")
    @PatchMapping("/{sessionId}/title")
    fun updateSessionTitle(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: UpdateSessionTitleRequest,
    ): RsData<ChatSession> {
        val userId =
            rq.currentUserId
                ?: return RsData.of("F-401", "로그인이 필요합니다", null)

        val session = sessionService.updateSessionTitle(sessionId, userId, request.title)

        return RsData.of(
            "S-1",
            "세션 제목 변경 성공",
            session,
        )
    }
}
