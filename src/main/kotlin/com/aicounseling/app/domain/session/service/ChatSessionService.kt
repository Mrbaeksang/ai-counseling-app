package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.counselor.dto.RateSessionRequest
import com.aicounseling.app.domain.counselor.entity.Counselor
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
import com.aicounseling.app.global.rsData.RsData
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
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
        private const val AI_RETRY_MAX_COUNT = 3
        private const val AI_RETRY_DELAY_BASE = 1000L
        private const val AI_RESPONSE_MIN_LENGTH = 10
        private const val TITLE_PARSE_MAX_LENGTH = 15
        private const val DEBUG_LOG_MAX_LENGTH = 200
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
        val counselor =
            counselorService.findById(counselorId)
                ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")

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
     * @param request 평가 요청 (rating 1-10, feedback)
     * @return 평가 결과 (RsData)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 진행 중인 세션인 경우
     */
    fun rateSession(
        sessionId: Long,
        userId: Long,
        request: RateSessionRequest,
    ): RsData<String> {
        val session = getSession(sessionId, userId)

        check(session.closedAt != null) {
            "진행 중인 세션은 평가할 수 없습니다"
        }

        // 중복 평가 체크
        if (counselorService.isSessionRated(sessionId)) {
            return RsData.of(
                "F-400",
                "이미 평가가 완료된 세션입니다",
                null,
            )
        }

        return counselorService.addRating(
            sessionId = sessionId,
            userId = userId,
            counselorId = session.counselorId,
            session = session,
            request = request,
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

        // 1. 사용자 메시지 저장
        val isFirstMessage = messageRepository.countBySessionId(sessionId) == 0L
        val userMessage = saveUserMessage(session, content, isFirstMessage)

        // 2. AI 응답 생성
        val counselor =
            counselorService.findById(session.counselorId)
                ?: error("상담사를 찾을 수 없습니다: ${session.counselorId}")

        val aiResponse =
            try {
                generateAiResponse(
                    session = session,
                    counselor = counselor,
                    userMessage = content,
                    isFirstMessage = isFirstMessage,
                )
            } catch (e: IOException) {
                // AI 응답 실패 시 에러 메시지 반환
                logger.error("AI 응답 생성 실패: {}", e.message, e)
                val errorMessage = createErrorMessage(session, userMessage.phase)
                return Triple(userMessage, errorMessage, session)
            }

        // 3. 응답 처리 및 저장
        return processAiResponse(
            session = session,
            userMessage = userMessage,
            aiResponse = aiResponse,
            isFirstMessage = isFirstMessage,
        )
    }

    /**
     * 사용자 메시지 저장
     */
    private fun saveUserMessage(
        session: ChatSession,
        content: String,
        isFirstMessage: Boolean,
    ): Message {
        val userPhase =
            if (isFirstMessage) {
                CounselingPhase.ENGAGEMENT
            } else {
                messageRepository.findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(
                    session.id,
                    SenderType.AI,
                )?.phase ?: CounselingPhase.ENGAGEMENT
            }

        val userMessage =
            Message(
                session = session,
                senderType = SenderType.USER,
                content = content,
                phase = userPhase,
            )
        return messageRepository.save(userMessage)
    }

    /**
     * AI 응답 생성
     */
    private fun generateAiResponse(
        session: ChatSession,
        counselor: Counselor,
        userMessage: String,
        isFirstMessage: Boolean,
    ): String {
        // 대화 히스토리 구성
        val history = buildConversationHistory(session.id)

        // 프롬프트 구성
        val systemPrompt = buildSystemPrompt(counselor, isFirstMessage)

        // AI 응답 요청 (재시도 로직 포함)
        return try {
            requestAiResponseWithRetry(
                sessionId = session.id,
                userMessage = userMessage,
                systemPrompt = systemPrompt,
                history = history,
                isFirstMessage = isFirstMessage,
            )
        } catch (e: IOException) {
            // 에러 발생 시 처리
            handleAiResponseError(session, userMessage, isFirstMessage)
            // 에러 로그만 남기고 재throw (sendMessage에서 처리)
            logger.error("AI 응답 생성 실패", e)
            throw e
        }
    }

    /**
     * 대화 히스토리 구성
     */
    private fun buildConversationHistory(sessionId: Long): List<com.aicounseling.app.global.openrouter.Message> {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .dropLast(1) // 방금 저장한 사용자 메시지 제외
            .takeLast(AppConstants.Session.MAX_CONVERSATION_HISTORY)
            .map { message ->
                com.aicounseling.app.global.openrouter.Message(
                    role = if (message.senderType == SenderType.USER) "user" else "assistant",
                    content = "${message.content} [단계: ${message.phase.koreanName}]",
                )
            }
    }

    /**
     * 시스템 프롬프트 구성
     */
    private fun buildSystemPrompt(
        counselor: Counselor,
        isFirstMessage: Boolean,
    ): String {
        val phaseOptions =
            CounselingPhase.entries.joinToString("\n") { phase ->
                "- ${phase.name}(${phase.koreanName}): ${phase.description}"
            }

        return """
${counselor.basePrompt}

상담 단계 안내:
$phaseOptions

단계 전환 기준:
- ENGAGEMENT: 첫 인사, 내담자의 기분 확인, 편안한 분위기 조성 및 상담 목표 설정
- EXPLORATION: 내담자의 고민, 감정, 구체적인 경험과 배경을 깊이 있게 탐색할 때
- INSIGHT: 내담자가 자신의 문제 패턴을 발견하고, 새로운 관점을 얻거나 자기 이해를 심화할 때
- ACTION: 내담자가 문제 해결을 위한 구체적인 실천 방안이나 작은 변화를 계획할 때
- CLOSING: 상담 내용을 정리하고, 긍정적인 메시지로 마무리하며 다음 단계를 기대할 때

현재 대화의 흐름과 내용의 깊이를 고려하여 가장 적절한 단계를 정확히 선택하고 [현재 단계]에 해당 ENUM 이름만 작성하세요.
**내담자의 대화 내용 변화에 따라 상담 단계를 적극적으로 전환하세요.**

응답 형식 (아래 형식을 정확히 따라주세요):
[응답 내용]
(여기에 사용자에게 전달할 상담 응답을 작성하세요)

[현재 단계]
(여기에 현재 적합한 단계의 ENUM 이름만 작성. 예: ENGAGEMENT 또는 EXPLORATION)
${if (isFirstMessage) {
"""

[세션 제목]
(여기에 대화를 요약한 15자 이내 제목 작성)"""
} else {
    ""
}}
        """.trimIndent()
    }

    /**
     * AI 응답 요청 (재시도 로직 포함)
     */
    private fun requestAiResponseWithRetry(
        sessionId: Long,
        userMessage: String,
        systemPrompt: String,
        history: List<com.aicounseling.app.global.openrouter.Message>,
        isFirstMessage: Boolean,
    ): String {
        return try {
            runBlocking {
                var retryCount = 0

                while (retryCount < AI_RETRY_MAX_COUNT) {
                    val response =
                        openRouterService.sendCounselingMessage(
                            userMessage = userMessage,
                            counselorPrompt = systemPrompt,
                            conversationHistory = history,
                            includeTitle = isFirstMessage,
                        )

                    // 응답이 유효하면 반환
                    if (response.isNotBlank() && response.length > AI_RESPONSE_MIN_LENGTH) {
                        return@runBlocking response
                    }

                    retryCount++
                    if (retryCount < AI_RETRY_MAX_COUNT) {
                        logger.warn(
                            "빈 AI 응답 수신, 재시도 {}/{} - sessionId: {}",
                            retryCount,
                            AI_RETRY_MAX_COUNT,
                            sessionId,
                        )
                        delay(AI_RETRY_DELAY_BASE * retryCount)
                    }
                }

                // 모든 재시도 실패 시
                logger.error("AI 응답 실패 ({}회 재시도 후) - sessionId: {}", AI_RETRY_MAX_COUNT, sessionId)
                throw IOException("AI 응답을 받을 수 없습니다")
            }
        } catch (e: IOException) {
            logger.error("AI 응답 요청 실패 - sessionId: {}, error: {}", sessionId, e.message, e)
            throw e
        }
    }

    /**
     * AI 응답 처리 및 저장
     */
    private fun processAiResponse(
        session: ChatSession,
        userMessage: Message,
        aiResponse: String,
        isFirstMessage: Boolean,
    ): Triple<Message, Message, ChatSession> {
        // AI 응답 파싱
        logger.info("AI 원본 응답 (sessionId: {}): {}", session.id, aiResponse)
        val (responseContent, currentPhase, sessionTitle) = parseAiResponse(aiResponse, isFirstMessage)
        logger.info("파싱 결과 - 단계: {}, 제목: {}, 내용 길이: {}", currentPhase, sessionTitle, responseContent.length)

        // 첫 메시지일 경우 세션 제목 설정
        if (isFirstMessage) {
            session.title = sessionTitle?.take(AppConstants.Session.TITLE_MAX_LENGTH)?.trim()
                ?: userMessage.content.take(AppConstants.Session.TITLE_MAX_LENGTH).trim().ifEmpty {
                    AppConstants.Session.DEFAULT_SESSION_TITLE
                }
        }

        // AI 메시지 저장
        val aiMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = responseContent,
                phase = currentPhase,
            )
        messageRepository.save(aiMessage)

        // 세션 정보 업데이트
        session.lastMessageAt = LocalDateTime.now()
        val updatedSession = sessionRepository.save(session)

        return Triple(userMessage, aiMessage, updatedSession)
    }

    private fun parseAiResponse(
        rawResponse: String,
        expectTitle: Boolean = false,
    ): Triple<String, CounselingPhase, String?> {
        logger.debug("AI 원본 응답: {}", rawResponse.take(DEBUG_LOG_MAX_LENGTH))

        // 응답이 비어있는 경우
        if (rawResponse.isBlank()) {
            logger.error("AI 응답이 비어있음")
            return Triple("죄송합니다. 다시 말씀해 주시겠어요?", CounselingPhase.ENGAGEMENT, null)
        }

        // 구조화된 텍스트 형식 파싱 시도
        return if (rawResponse.contains("[응답 내용]") && rawResponse.contains("[현재 단계]")) {
            parseStructuredResponse(rawResponse, expectTitle)
        } else {
            // JSON 형식 파싱 (폴백) - 하위 호환성
            parseJsonResponse(rawResponse, expectTitle)
        }
    }

    /**
     * 구조화된 텍스트 형식 파싱
     */
    private fun parseStructuredResponse(
        rawResponse: String,
        expectTitle: Boolean,
    ): Triple<String, CounselingPhase, String?> {
        return try {
            // 1. 응답 내용 추출
            val contentPattern =
                Regex(
                    """[응답 내용].*?
?(.*?)(?=[현재 단계]|[세션 제목]|$)""",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
                )
            val content =
                contentPattern.find(rawResponse)?.groups?.get(1)?.value?.trim()
                    ?: throw IllegalArgumentException("응답 내용을 찾을 수 없음")

            // 2. 현재 단계 추출
            val phasePattern =
                Regex(
                    """[현재 단계].*?
?([A-Z_]+)""",
                    RegexOption.MULTILINE,
                )
            val phaseString = phasePattern.find(rawResponse)?.groups?.get(1)?.value?.trim()?.uppercase()

            val currentPhase =
                try {
                    CounselingPhase.valueOf(phaseString ?: "ENGAGEMENT")
                } catch (e: IllegalArgumentException) {
                    logger.warn("알 수 없는 단계: {}, 기본값 사용 - error: {}", phaseString, e.message)
                    CounselingPhase.ENGAGEMENT
                }

            // 3. 세션 제목 추출 (첫 메시지인 경우만)
            val title =
                if (expectTitle) {
                    extractSessionTitle(rawResponse)
                } else {
                    null
                }

            logger.info("파싱 성공 - 단계: {}, 제목: {}", currentPhase, title)
            Triple(content, currentPhase, title)
        } catch (e: IllegalArgumentException) {
            logger.error("구조화된 텍스트 파싱 실패: {}", e.message)
            parseFallbackResponse(rawResponse)
        }
    }

    /**
     * 세션 제목 추출
     */
    private fun extractSessionTitle(rawResponse: String): String? {
        val titlePattern =
            Regex(
                """[세션 제목].*?
?(.+?)(?:
|$)""",
                RegexOption.MULTILINE,
            )
        return titlePattern.find(rawResponse)?.groups?.get(1)?.value?.trim()?.take(TITLE_PARSE_MAX_LENGTH)
    }

    /**
     * JSON 형식 파싱 (하위 호환성)
     */
    private fun parseJsonResponse(
        rawResponse: String,
        expectTitle: Boolean,
    ): Triple<String, CounselingPhase, String?> {
        val cleanedResponse =
            rawResponse
                .replace(Regex("""^```json.*""", RegexOption.MULTILINE), "")
                .replace(Regex("""```.*$""", RegexOption.MULTILINE), "")
                .trim()

        return try {
            val jsonNode = objectMapper.readTree(cleanedResponse)
            val content = jsonNode.get("content")?.asText() ?: rawResponse

            val currentPhaseString = jsonNode.get("currentPhase")?.asText() ?: "ENGAGEMENT"
            val currentPhase =
                try {
                    CounselingPhase.valueOf(currentPhaseString)
                } catch (e: IllegalArgumentException) {
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

            logger.debug("JSON 파싱 - 단계: {}, 제목: {}", currentPhase, title)
            Triple(content, currentPhase, title)
        } catch (e: JsonProcessingException) {
            logger.error("JSON 파싱 실패: {}", e.message)
            parseFallbackResponse(cleanedResponse)
        }
    }

    /**
     * 파싱 실패 시 폴백 응답
     */
    private fun parseFallbackResponse(rawResponse: String): Triple<String, CounselingPhase, String?> {
        val fallbackContent =
            rawResponse
                .replace(Regex("""\{.*?\}""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""\[.*?\]"""), "")
                .trim()
                .ifEmpty { "죄송합니다. 다시 한 번 말씀해 주시겠어요?" }
        return Triple(fallbackContent, CounselingPhase.ENGAGEMENT, null)
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
