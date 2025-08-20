package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.service.CounselorService
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.openrouter.OpenRouterService
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
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
        private const val MAX_CONVERSATION_HISTORY = 9
        private const val TITLE_MAX_LENGTH = 15
    }

    @Transactional(readOnly = true)
    fun getUserSessions(
        userId: Long,
        pageable: Pageable,
    ): Page<ChatSession> {
        return sessionRepository.findByUserId(userId, pageable)
    }

    @Transactional(readOnly = true)
    fun getBookmarkedSessions(
        userId: Long,
        pageable: Pageable,
    ): Page<ChatSession> {
        return sessionRepository.findByUserIdAndIsBookmarked(userId, true, pageable)
    }

    fun startSession(
        userId: Long,
        counselorId: Long,
    ): ChatSession {
        getActiveSession(userId)?.let {
            throw IllegalStateException("이미 진행 중인 세션이 있습니다")
        }

        val session =
            ChatSession(
                userId = userId,
                counselorId = counselorId,
            )

        return sessionRepository.save(session)
    }

    @Transactional(readOnly = true)
    fun getSession(
        sessionId: Long,
        userId: Long,
    ): ChatSession {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
    }

    fun closeSession(
        sessionId: Long,
        userId: Long,
    ): ChatSession {
        val session =
            sessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

        check(session.closedAt == null) {
            "이미 종료된 세션입니다"
        }

        session.closedAt = LocalDateTime.now()
        return sessionRepository.save(session)
    }

    fun rateSession(
        sessionId: Long,
        userId: Long,
        rating: Int,
        feedback: String?,
    ): CounselorRating {
        val session =
            sessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

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

    fun toggleBookmark(
        sessionId: Long,
        userId: Long,
    ): Boolean {
        val session =
            sessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

        session.isBookmarked = !session.isBookmarked
        sessionRepository.save(session)

        return session.isBookmarked
    }

    fun updatePhase(
        sessionId: Long,
        newPhase: CounselingPhase,
        reason: String,
    ): ChatSession {
        val session =
            sessionRepository.findById(sessionId).orElseThrow {
                IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")
            }

        check(session.closedAt == null) {
            "종료된 세션은 단계를 변경할 수 없습니다"
        }

        session.phase = newPhase
        session.phaseMetadata = reason

        return sessionRepository.save(session)
    }

    @Transactional(readOnly = true)
    fun getActiveSession(userId: Long): ChatSession? {
        val sessions = sessionRepository.findByUserIdAndClosedAtIsNull(userId)
        return sessions.firstOrNull()
    }

    fun updateSessionTitle(
        sessionId: Long,
        userId: Long,
        newTitle: String,
    ): ChatSession {
        val session =
            sessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

        session.title = newTitle.trim()
        return sessionRepository.save(session)
    }

    @Transactional(readOnly = true)
    fun getSessionMessageCount(sessionId: Long): Int {
        return messageRepository.countBySessionId(sessionId).toInt()
    }

    @Transactional(readOnly = true)
    fun getCounselorSessionCount(counselorId: Long): Long {
        return sessionRepository.countByCounselorId(counselorId)
    }

    @Transactional(readOnly = true)
    fun getSessionMessages(
        sessionId: Long,
        userId: Long,
        pageable: Pageable,
    ): Page<Message> {
        sessionRepository.findByIdAndUserId(sessionId, userId)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

        return messageRepository.findBySessionId(sessionId, pageable)
    }

    fun sendMessage(
        sessionId: Long,
        userId: Long,
        content: String,
    ): Pair<Message, Message> {
        check(content.isNotBlank()) { "메시지 내용을 입력해주세요" }

        val session =
            sessionRepository.findByIdAndUserId(sessionId, userId)
                ?: throw IllegalArgumentException("세션을 찾을 수 없습니다: $sessionId")

        check(session.closedAt == null) { "종료된 세션에는 메시지를 보낼 수 없습니다" }

        val isFirstMessage = session.title == null

        val userMessage =
            Message(
                session = session,
                senderType = SenderType.USER,
                content = content,
            )
        val savedUserMessage = messageRepository.save(userMessage)

        val counselor = counselorService.findById(session.counselorId)

        val history =
            messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .dropLast(1)
                .takeLast(MAX_CONVERSATION_HISTORY)
                .map {
                    com.aicounseling.app.global.openrouter.Message(
                        role = if (it.senderType == SenderType.USER) "user" else "assistant",
                        content = it.content,
                    )
                }

        val systemPrompt =
            """
            ${counselor.basePrompt}
            현재 상담 단계: ${session.phase.koreanName}
            """.trimIndent()

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
                if (isFirstMessage) {
                    session.title = content.take(TITLE_MAX_LENGTH).trim().ifEmpty { "새 상담" }
                }

                val errorMessage = "AI 응답을 가져오는데 실패했습니다. 잠시 후 다시 시도해주세요."
                val aiMessage =
                    Message(
                        session = session,
                        senderType = SenderType.AI,
                        content = errorMessage,
                        aiPhaseAssessment = "오류 발생: ${e.message}",
                    )
                messageRepository.save(aiMessage)
                session.lastMessageAt = LocalDateTime.now()
                sessionRepository.save(session)
                return Pair(savedUserMessage, aiMessage)
            }

        val (responseContent, phaseAssessment, sessionTitle) = parseAiResponse(aiResponseRaw, isFirstMessage)

        if (isFirstMessage) {
            session.title = sessionTitle?.take(TITLE_MAX_LENGTH)?.trim()
                ?: content.take(TITLE_MAX_LENGTH).trim().ifEmpty { "새 상담" }
        }

        val aiMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = responseContent,
                aiPhaseAssessment = phaseAssessment,
            )
        messageRepository.save(aiMessage)

        session.lastMessageAt = LocalDateTime.now()
        sessionRepository.save(session)

        return Pair(savedUserMessage, aiMessage)
    }

    private fun parseAiResponse(
        rawResponse: String,
        expectTitle: Boolean = false,
    ): Triple<String, String, String?> {
        return try {
            val jsonNode = objectMapper.readTree(rawResponse)
            val content = jsonNode.get("content")?.asText() ?: rawResponse
            val assessment = jsonNode.get("aiPhaseAssessment")?.asText() ?: "단계 판단 정보 없음"
            val title =
                if (expectTitle) {
                    jsonNode.get("sessionTitle")?.asText()
                } else {
                    null
                }

            Triple(content, assessment, title)
        } catch (e: JsonProcessingException) {
            Triple(rawResponse, "JSON 파싱 실패: ${e.message}", null)
        }
    }
}
