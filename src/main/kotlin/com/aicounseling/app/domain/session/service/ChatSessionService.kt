package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.service.CounselorService
import com.aicounseling.app.domain.session.dto.CreateSessionResponse
import com.aicounseling.app.domain.session.dto.MessageItem
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.openrouter.OpenRouterService
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.LocalDateTime

@Service
@Transactional
class ChatSessionService(
    private val sessionRepository: ChatSessionRepository,
    private val counselorService: CounselorService,
    private val messageRepository: MessageRepository,
    private val openRouterService: OpenRouterService,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatSessionService::class.java)
    }

    /**
     * 사용자의 상담 세션 목록 조회 (N+1 문제 해결)
     * @param userId 조회할 사용자 ID
     * @param bookmarked 북마크 필터 (null이면 전체, true면 북마크만)
     * @param pageable 페이징 정보
     * @return Page<SessionListResponse> 페이징 정보를 포함한 세션 목록
     */
    @Transactional(readOnly = true)
    fun getUserSessions(
        userId: Long,
        bookmarked: Boolean?,
        pageable: Pageable,
    ): Page<SessionListResponse> {
        // Custom Repository 메서드를 사용하여 N+1 문제 해결
        // 한 번의 쿼리로 Session과 Counselor 정보를 함께 조회
        return sessionRepository.findSessionsWithCounselor(userId, bookmarked, pageable)
    }

    /**
     * 새로운 상담 세션 시작
     * @param userId 사용자 ID
     * @param counselorId 상담사 ID
     * @return 생성된 세션 응답 DTO
     */
    fun startSession(
        userId: Long,
        counselorId: Long,
    ): CreateSessionResponse {
        // 상담사 존재 여부 확인
        val counselor = counselorService.findById(counselorId)

        // 세션 생성
        val session =
            ChatSession(
                userId = userId,
                counselorId = counselorId,
            )

        val savedSession = sessionRepository.save(session)

        // DTO 변환 및 반환
        return CreateSessionResponse(
            sessionId = savedSession.id,
            counselorName = counselor.name,
            title = savedSession.title ?: AppConstants.Session.DEFAULT_SESSION_TITLE,
        )
    }

    /**
     * 상담 세션 종료
     * @param sessionId 종료할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 이미 종료된 세션인 경우
     */
    fun closeSession(
        sessionId: Long,
        userId: Long,
    ) {
        val session = getSession(sessionId, userId)

        check(session.closedAt == null) {
            AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED
        }

        session.closedAt = LocalDateTime.now()
        sessionRepository.save(session)
    }

    /**
     * 종료된 세션에 대한 상담사 평가
     * @param sessionId 평가할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param rating 평점 (1-5)
     * @param feedback 피드백 텍스트 (선택사항)
     * @return 생성된 평가 정보
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 진행 중인 세션인 경우
     */
    fun rateSession(
        sessionId: Long,
        userId: Long,
        rating: Int,
        feedback: String?,
    ): CounselorRating {
        val session = getSession(sessionId, userId)

        check(session.closedAt != null) {
            "진행 중인 세션은 평가할 수 없습니다"
        }

        return counselorService.addRating(
            sessionId = sessionId,
            userId = userId,
            counselorId = session.counselorId,
            rating = rating,
            feedback = feedback,
        )
    }

    /**
     * 세션 북마크 토글 (추가/제거)
     * @param sessionId 북마크할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 토글 후 북마크 상태 (true: 북마크됨, false: 북마크 해제됨)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Transactional
    fun toggleBookmark(
        sessionId: Long,
        userId: Long,
    ): Boolean {
        val session = getSession(sessionId, userId)

        session.isBookmarked = !session.isBookmarked
        sessionRepository.save(session)

        return session.isBookmarked
    }

    /**
     * 특정 세션 조회 (사용자 권한 확인 포함)
     * @param sessionId 조회할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 조회된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    private fun getSession(
        sessionId: Long,
        userId: Long,
    ): ChatSession {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            ?: throw IllegalArgumentException("${AppConstants.ErrorMessages.SESSION_NOT_FOUND}: $sessionId")
    }

    /**
     * 세션 제목 수정
     * @param sessionId 수정할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param newTitle 새로운 제목
     * @return 수정된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    fun updateSessionTitle(
        sessionId: Long,
        userId: Long,
        newTitle: String,
    ): ChatSession {
        val session = getSession(sessionId, userId)

        session.title = newTitle.trim()
        return sessionRepository.save(session)
    }

    /**
     * 세션의 메시지 목록 조회
     * @param sessionId 조회할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param pageable 페이징 정보
     * @return Page<MessageItem> 페이징 정보를 포함한 메시지 목록
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    fun getSessionMessages(
        sessionId: Long,
        userId: Long,
        pageable: Pageable,
    ): Page<MessageItem> {
        getSession(sessionId, userId) // 권한 확인용

        val messages = messageRepository.findBySessionId(sessionId, pageable)

        val content =
            messages.content.map { message ->
                MessageItem(
                    content = message.content,
                    senderType = message.senderType.name,
                )
            }

        return PageImpl(content, messages.pageable, messages.totalElements)
    }

    /**
     * 사용자 메시지 전송 및 AI 응답 생성
     * 5단계 상담 모델을 기반으로 AI가 적절한 상담 단계를 판단하여 응답
     * @param sessionId 메시지를 전송할 세션 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param content 사용자 메시지 내용
     * @return 사용자 메시지, AI 응답 메시지, 업데이트된 세션
     * @throws IllegalArgumentException 세션을 찾을 수 없거나 메시지 내용이 비어있는 경우
     * @throws IllegalStateException 이미 종료된 세션인 경우
     */
    fun sendMessage(
        sessionId: Long,
        userId: Long,
        content: String,
    ): Triple<Message, Message, ChatSession> {
        check(content.isNotBlank()) { "메시지 내용을 입력해주세요" }

        val session = getSession(sessionId, userId)

        check(session.closedAt == null) { AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED }

        // 1. 첫 메시지 여부 확인
        val isFirstMessage = messageRepository.countBySessionId(sessionId) == 0L

        // 2. 사용자 메시지 phase 결정: 첫 메시지면 ENGAGEMENT, 아니면 마지막 AI 메시지의 phase 따라감
        val userPhase =
            if (isFirstMessage) {
                CounselingPhase.ENGAGEMENT
            } else {
                // 마지막 AI 메시지의 phase를 가져와서 사용자 메시지도 같은 단계로 설정
                messageRepository.findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(sessionId, SenderType.AI)?.phase
                    ?: CounselingPhase.ENGAGEMENT
            }

        // 3. 사용자 메시지 저장 (일단 ENGAGEMENT로 저장)
        val userMessage =
            Message(
                session = session,
                senderType = SenderType.USER,
                content = content,
                phase = userPhase,
            )
        val savedUserMessage = messageRepository.save(userMessage)

        // 4. 상담사 정보 조회
        val counselor = counselorService.findById(session.counselorId)

        // 5. 대화 히스토리 구성 (모든 메시지에 phase 정보 포함)
        val history =
            messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .dropLast(1) // 방금 저장한 사용자 메시지 제외
                .takeLast(AppConstants.Session.MAX_CONVERSATION_HISTORY)
                .map { message ->
                    com.aicounseling.app.global.openrouter.Message(
                        role = if (message.senderType == SenderType.USER) "user" else "assistant",
                        content = "${message.content} [단계: ${message.phase.koreanName}]",
                    )
                }

        // 6. AI 프롬프트 구성 (모든 가능한 단계와 현재 상황 판단 요청)
        val phaseOptions =
            CounselingPhase.entries.joinToString("\n") {
                "- ${it.name}(${it.koreanName}): ${it.description}"
            }

        val systemPrompt =
            """
            ${counselor.basePrompt}

            상담 단계 안내:
            $phaseOptions

            위 대화 히스토리와 사용자의 현재 메시지를 보고, 이 상황에 가장 적합한 상담 단계를 판단해주세요.
            단계를 무조건 높일 필요는 없고, 현재 상황에 맞는 단계를 선택하세요.

            응답 형식 (반드시 JSON으로):
            {
                "content": "사용자에게 전달할 상담 응답",
                "currentPhase": "이 대화에 적합한 현재 단계 ENUM 이름 (예: ENGAGEMENT, ASSESSMENT_AND_CONCEPTUALIZATION 등)"
                ${if (isFirstMessage) """, "sessionTitle": "이 대화를 요약한 15자 이내 제목"""" else ""}
            }
            """.trimIndent()

        // 7. AI 응답 요청
        val aiResponseRaw =
            try {
                runBlocking {
                    openRouterService.sendCounselingMessage(
                        userMessage = content,
                        counselorPrompt = systemPrompt,
                        conversationHistory = history,
                        includeTitle = isFirstMessage,
                    )
                }
            } catch (e: IOException) {
                // 에러 발생 시 로깅 및 처리
                logger.error("AI 응답 요청 실패 - sessionId: {}, error: {}", sessionId, e.message, e)
                handleAiResponseError(session, content, isFirstMessage)
                val errorMessage = createErrorMessage(session, userPhase)
                return Triple(savedUserMessage, errorMessage, session)
            }

        // 8. AI 응답 파싱
        val (responseContent, currentPhase, sessionTitle) = parseAiResponse(aiResponseRaw, isFirstMessage)

        // 9. 첫 메시지일 경우 세션 제목 설정
        if (isFirstMessage) {
            session.title = sessionTitle?.take(AppConstants.Session.TITLE_MAX_LENGTH)?.trim()
                ?: content.take(AppConstants.Session.TITLE_MAX_LENGTH).trim().ifEmpty {
                    AppConstants.Session.DEFAULT_SESSION_TITLE
                }
        }

        // 10. AI 메시지 저장 (AI가 판단한 currentPhase로)
        val aiMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = responseContent,
                phase = currentPhase,
            )
        messageRepository.save(aiMessage)

        // 11. 세션 정보 업데이트
        session.lastMessageAt = LocalDateTime.now()
        val updatedSession = sessionRepository.save(session)

        return Triple(savedUserMessage, aiMessage, updatedSession)
    }

    /**
     * AI 응답 JSON 파싱
     * @param rawResponse AI로부터 받은 원본 응답 문자열
     * @param expectTitle 세션 제목 포함 여부 (첫 메시지인 경우 true)
     * @return (응답 내용, 상담 단계, 세션 제목?)
     */
    private fun parseAiResponse(
        rawResponse: String,
        expectTitle: Boolean = false,
    ): Triple<String, CounselingPhase, String?> {
        return try {
            val jsonNode = objectMapper.readTree(rawResponse)
            val content = jsonNode.get("content")?.asText() ?: rawResponse

            // AI가 판단한 현재 적절한 단계 파싱
            val currentPhaseString = jsonNode.get("currentPhase")?.asText() ?: "ENGAGEMENT"
            val currentPhase =
                try {
                    CounselingPhase.valueOf(currentPhaseString)
                } catch (e: IllegalArgumentException) {
                    // 예외 처리: 잘못된 단계명은 기본값으로 처리
                    logger.warn(
                        "잘못된 상담 단계명 수신: {}, 기본값(ENGAGEMENT) 사용 - error: {}",
                        currentPhaseString,
                        e.message,
                    )
                    CounselingPhase.ENGAGEMENT
                }

            val title =
                if (expectTitle) {
                    jsonNode.get("sessionTitle")?.asText()
                } else {
                    null
                }

            Triple(content, currentPhase, title)
        } catch (e: JsonProcessingException) {
            // JSON 파싱 실패시 기본값들로 처리
            logger.error("AI 응답 JSON 파싱 실패: {}", e.message, e)
            Triple(rawResponse, CounselingPhase.ENGAGEMENT, null)
        }
    }

    /**
     * AI 응답 에러 발생 시 세션 상태 정리
     * @param session 현재 세션
     * @param content 메시지 내용
     * @param isFirstMessage 첫 메시지 여부
     */
    private fun handleAiResponseError(
        session: ChatSession,
        content: String,
        isFirstMessage: Boolean,
    ) {
        if (isFirstMessage) {
            session.title =
                content.take(AppConstants.Session.TITLE_MAX_LENGTH).trim().ifEmpty {
                    AppConstants.Session.DEFAULT_SESSION_TITLE
                }
        }
        session.lastMessageAt = LocalDateTime.now()
        sessionRepository.save(session)
    }

    /**
     * AI 응답 실패 시 에러 메시지 생성
     * @param session 현재 세션
     * @param phase 현재 상담 단계
     * @return 생성된 에러 메시지
     */
    private fun createErrorMessage(
        session: ChatSession,
        phase: CounselingPhase,
    ): Message {
        val errorMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = AppConstants.ErrorMessages.AI_RESPONSE_ERROR,
                // 에러 시에는 현재 단계 유지
                phase = phase,
            )
        return messageRepository.save(errorMessage)
    }
}
