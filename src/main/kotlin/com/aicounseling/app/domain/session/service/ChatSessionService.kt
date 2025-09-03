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
        val systemPrompt = buildSystemPrompt(counselor, isFirstMessage, session.id)

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
                    // 단계 정보 제거, 순수 대화 내용만 전달
                    content = message.content,
                )
            }
    }

    /**
     * 현재 AI의 마지막 단계 가져오기
     */
    private fun getLastAiPhase(sessionId: Long): CounselingPhase {
        return messageRepository.findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(
            sessionId,
            SenderType.AI,
        )?.phase ?: CounselingPhase.ENGAGEMENT
    }

    /**
     * 대화 진행도에 따른 최소 요구 단계 계산
     */
    private fun calculateMinimumPhase(messageCount: Long): CounselingPhase {
        return when {
            messageCount < AppConstants.Session.PHASE_ENGAGEMENT_MAX -> CounselingPhase.ENGAGEMENT // 1-3번: 인사 및 관계 형성
            messageCount < AppConstants.Session.PHASE_EXPLORATION_START -> CounselingPhase.EXPLORATION // 4-9번: 문제 탐색
            messageCount < AppConstants.Session.PHASE_EXPLORATION_DEEP -> CounselingPhase.EXPLORATION // 10-19번: 깊은 탐색
            messageCount < AppConstants.Session.PHASE_INSIGHT_START -> CounselingPhase.INSIGHT // 20-29번: 통찰 유도
            messageCount < AppConstants.Session.PHASE_ACTION_START -> CounselingPhase.ACTION // 30-39번: 행동 계획
            else -> CounselingPhase.ACTION // 40번+: 행동 계획 또는 마무리
        }
    }

    /**
     * 선택 가능한 단계 목록 생성
     */
    private fun getAvailablePhases(
        currentPhase: CounselingPhase,
        minimumPhase: CounselingPhase,
    ): String {
        val minOrdinal = maxOf(currentPhase.ordinal, minimumPhase.ordinal)
        return CounselingPhase.entries
            .filter { it.ordinal >= minOrdinal }
            .joinToString(", ") { "${it.name}(${it.koreanName})" }
    }

    /**
     * 시스템 프롬프트 구성
     */
    private fun buildSystemPrompt(
        counselor: Counselor,
        isFirstMessage: Boolean,
        sessionId: Long,
    ): String {
        // 현재 상태 파악
        val messageCount = messageRepository.countBySessionId(sessionId)
        val lastAiPhase = if (!isFirstMessage) getLastAiPhase(sessionId) else CounselingPhase.ENGAGEMENT
        val minimumPhase = calculateMinimumPhase(messageCount)
        val availablePhases = getAvailablePhases(lastAiPhase, minimumPhase)

        val phaseOptions =
            CounselingPhase.entries.joinToString("\n") { phase ->
                "- ${phase.name}(${phase.koreanName}): ${phase.description}"
            }

        val basePrompt =
            """
            |${counselor.basePrompt}
            |
            |[현재 상담 상태]
            |- 대화 횟수: ${messageCount}회
            |- 현재 단계: ${lastAiPhase.koreanName}(${lastAiPhase.name})
            |- 최소 요구 단계: ${minimumPhase.koreanName} 이상
            |
            |[상담 단계 안내]
            |$phaseOptions
            |
            |[단계 선택 규칙 - 매우 중요!]
            |1. **절대 규칙**: 이전 단계(${lastAiPhase.name})보다 낮은 단계로 돌아가지 마세요
            |2. **최소 단계**: ${messageCount}번째 대화이므로 최소 ${minimumPhase.name} 이상이어야 합니다
            |3. **선택 가능한 단계**: $availablePhases
            |
            |[키워드 기반 단계 판단 가이드]
            |- ENGAGEMENT 키워드: 안녕, 처음, 시작, 만남
            |- EXPLORATION 키워드: 고민, 문제, 어려움, 힘든, 때문에, 걱정
            |- INSIGHT 키워드: 깨달음, 알게, 이해, 패턴, 반복, 왜
            |- ACTION 키워드: 해볼게, 시도, 실천, 계획, 목표, 방법
            |- CLOSING 키워드: 감사, 마무리, 정리, 다음에
            |
            |사용자 메시지에 위 키워드가 포함되면 해당 단계를 우선 고려하되,
            |반드시 선택 가능한 단계 중에서만 선택하세요.
            |
            |[응답 형식]
            |반드시 아래 JSON 형식으로만 응답하세요.
            |코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
            |{
            |  "content": "상담 응답 내용 (공감적이고 따뜻하게)",
            |  "phase": "선택한 단계 ($availablePhases 중 하나)"
            |}
            |
            |예시:
            |{
            |  "content": "그런 고민이 있으셨군요. 어떤 부분이 가장 힘드신가요?",
            |  "phase": "${if (minimumPhase.ordinal > CounselingPhase.ENGAGEMENT.ordinal) minimumPhase.name else "EXPLORATION"}"
            |}
            """.trimMargin()

        return if (isFirstMessage) {
            """
            |$basePrompt
            |
            |첫 메시지이므로 세션 제목도 포함하세요.
            |코드블록(```)을 사용하지 마세요. 순수 JSON만 반환하세요.
            |{
            |  "content": "상담 응답 내용",
            |  "phase": "ENGAGEMENT",
            |  "title": "대화를 요약한 15자 이내 제목"
            |}
            """.trimMargin()
        } else {
            basePrompt
        }
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

                while (retryCount < AppConstants.Session.AI_RETRY_MAX_COUNT) {
                    val response =
                        openRouterService.sendCounselingMessage(
                            userMessage = userMessage,
                            counselorPrompt = systemPrompt,
                            conversationHistory = history,
                            includeTitle = isFirstMessage,
                        )

                    // 응답이 유효하면 반환
                    if (response.isNotBlank() && response.length > AppConstants.Session.AI_RESPONSE_MIN_LENGTH) {
                        return@runBlocking response
                    }

                    retryCount++
                    if (retryCount < AppConstants.Session.AI_RETRY_MAX_COUNT) {
                        logger.warn(
                            "빈 AI 응답 수신, 재시도 {}/{} - sessionId: {}",
                            retryCount,
                            AppConstants.Session.AI_RETRY_MAX_COUNT,
                            sessionId,
                        )
                        delay(AppConstants.Session.AI_RETRY_DELAY_BASE * retryCount)
                    }
                }

                // 모든 재시도 실패 시
                logger.error("AI 응답 실패 ({}회 재시도 후) - sessionId: {}", AppConstants.Session.AI_RETRY_MAX_COUNT, sessionId)
                throw IOException("AI 응답을 받을 수 없습니다")
            }
        } catch (e: IOException) {
            logger.error("AI 응답 요청 실패 - sessionId: {}, error: {}", sessionId, e.message, e)
            throw e
        }
    }

    /**
     * 단계 전환 검증
     */
    private fun validatePhaseTransition(
        previousPhase: CounselingPhase,
        newPhase: CounselingPhase,
        messageCount: Long,
    ): CounselingPhase {
        // 역행 방지
        if (newPhase.ordinal < previousPhase.ordinal) {
            logger.warn("단계 역행 시도 감지: {} → {}, 이전 단계 유지", previousPhase.name, newPhase.name)
            return previousPhase // 이전 단계 유지
        }

        // 최소 단계 강제
        val minimumPhase = calculateMinimumPhase(messageCount)
        if (newPhase.ordinal < minimumPhase.ordinal) {
            logger.info("최소 단계 미충족: {} < {}, 최소 단계로 조정", newPhase.name, minimumPhase.name)
            return minimumPhase
        }

        return newPhase
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
        val (responseContent, parsedPhase, sessionTitle) = parseAiResponse(aiResponse, isFirstMessage)

        // 단계 전환 검증
        val messageCount = messageRepository.countBySessionId(session.id)
        val lastAiPhase = if (!isFirstMessage) getLastAiPhase(session.id) else CounselingPhase.ENGAGEMENT
        val validatedPhase = validatePhaseTransition(lastAiPhase, parsedPhase, messageCount)

        // 검증 결과 로깅
        if (validatedPhase != parsedPhase) {
            logger.info("단계 조정됨: {} → {}", parsedPhase.name, validatedPhase.name)
        }

        // 첫 메시지일 경우 세션 제목 설정
        if (isFirstMessage) {
            session.title = sessionTitle?.take(AppConstants.Session.TITLE_MAX_LENGTH)?.trim()
                ?: userMessage.content.take(AppConstants.Session.TITLE_MAX_LENGTH).trim().ifEmpty {
                    AppConstants.Session.DEFAULT_SESSION_TITLE
                }
        }

        // AI 메시지 저장 (검증된 단계 사용)
        val aiMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = responseContent,
                // 검증된 단계 사용
                phase = validatedPhase,
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
        // 응답이 비어있는 경우
        if (rawResponse.isBlank()) {
            logger.error("AI 응답이 비어있음")
            return Triple("죄송합니다. 다시 말씀해 주시겠어요?", CounselingPhase.ENGAGEMENT, null)
        }

        // 마크다운 코드블록 제거 (```json ... ``` 형태)
        val cleanedResponse =
            rawResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

        // JSON 형식 우선 처리 (중괄호로 시작하는 경우)
        return if (cleanedResponse.startsWith("{") && cleanedResponse.endsWith("}")) {
            parseJsonResponse(cleanedResponse, expectTitle)
        } else {
            // JSON이 아닌 경우 폴백 처리
            logger.warn("AI가 JSON 형식으로 응답하지 않음: {}", cleanedResponse.take(AppConstants.Session.LOG_PREVIEW_LENGTH))
            parseFallbackResponse(rawResponse)
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
     * JSON 형식 응답 파싱
     */
    private fun parseJsonResponse(
        rawResponse: String,
        expectTitle: Boolean,
    ): Triple<String, CounselingPhase, String?> {
        return try {
            // JSON 파싱
            val jsonNode = objectMapper.readTree(rawResponse.trim())

            // content 필드 추출 (필수)
            val content =
                jsonNode.get("content")?.asText()
                    ?: throw IllegalArgumentException("content 필드가 없습니다")

            // Jackson의 asText()는 자동으로 언이스케이프 처리하지만 추가 안전장치
            // \n, \t 등의 이스케이프 문자를 실제 문자로 변환
            val unescapedContent =
                content
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")

            // phase 필드 추출 (필수)
            val phaseString =
                jsonNode.get("phase")?.asText()?.uppercase()
                    ?: throw IllegalArgumentException("phase 필드가 없습니다")

            val phase =
                try {
                    CounselingPhase.valueOf(phaseString)
                } catch (e: IllegalArgumentException) {
                    logger.warn("잘못된 phase 값: {}, 기본값 사용", phaseString, e)
                    CounselingPhase.ENGAGEMENT
                }

            // title 필드 추출 (첫 메시지일 때만)
            val title =
                if (expectTitle) {
                    jsonNode.get("title")?.asText()?.take(AppConstants.Session.TITLE_PARSE_MAX_LENGTH)
                } else {
                    null
                }

            Triple(unescapedContent, phase, title)
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error("JSON 파싱 실패: {}", e.message, e)
            // JSON 파싱 실패 시 폴백 처리
            parseFallbackResponse(rawResponse)
        } catch (e: IllegalArgumentException) {
            logger.error("필수 필드 누락: {}", e.message, e)
            // 필수 필드 누락 시 폴백 처리
            parseFallbackResponse(rawResponse)
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
