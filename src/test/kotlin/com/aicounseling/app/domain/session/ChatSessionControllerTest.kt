package com.aicounseling.app.domain.session

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.session.controller.ChatSessionController
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.service.ChatSessionService
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.security.AuthProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(ChatSessionController::class)
@AutoConfigureMockMvc(addFilters = false)
class ChatSessionControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    @MockkBean private val chatSessionService: ChatSessionService,
) : BehaviorSpec({

        given("세션 목록 조회 API") {
            val userId = 1L
            val sessions =
                listOf(
                    ChatSession(
                        userId = userId,
                        counselorId = 1L,
                    ).apply {
                        lastMessageAt = LocalDateTime.now()
                    },
                )
            val pageable = PageRequest.of(0, 10)
            val page = PageImpl(sessions, pageable, sessions.size.toLong())

            `when`("인증된 사용자가 자신의 세션 목록을 조회하면") {
                every { chatSessionService.getUserSessions(userId, any()) } returns page

                then("200 OK와 함께 세션 목록이 반환된다") {
                    mockMvc.get("/api/v1/sessions") {
                        header("Authorization", "Bearer valid-token")
                        param("page", "0")
                        param("size", "10")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.content[0].userId") { value(userId) }
                    }

                    verify(exactly = 1) { chatSessionService.getUserSessions(userId, any()) }
                }
            }

            `when`("인증되지 않은 사용자가 세션 목록을 조회하면") {
                then("401 Unauthorized가 반환된다") {
                    mockMvc.get("/api/v1/sessions") {
                        param("page", "0")
                        param("size", "10")
                    }.andExpect {
                        status { isUnauthorized() }
                    }
                }
            }
        }

        given("새 세션 시작 API") {
            val userId = 1L
            val counselorId = 2L
            val newSession =
                ChatSession(
                    userId = userId,
                    counselorId = counselorId,
                )

            `when`("인증된 사용자가 새 세션을 시작하면") {
                every { chatSessionService.startSession(userId, counselorId) } returns newSession

                val request = mapOf("counselorId" to counselorId)

                then("201 Created와 함께 새 세션이 반환된다") {
                    mockMvc.post("/api/v1/sessions") {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isCreated() }
                        jsonPath("$.resultCode") { value("201") }
                        jsonPath("$.data.counselorId") { value(counselorId) }
                    }

                    verify(exactly = 1) { chatSessionService.startSession(userId, counselorId) }
                }
            }

            `when`("이미 진행 중인 세션이 있으면") {
                every { chatSessionService.startSession(userId, counselorId) } throws
                    IllegalStateException("이미 진행 중인 세션이 있습니다")

                val request = mapOf("counselorId" to counselorId)

                then("400 Bad Request가 반환된다") {
                    mockMvc.post("/api/v1/sessions") {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.resultCode") { value("400") }
                        jsonPath("$.msg") { value("이미 진행 중인 세션이 있습니다") }
                    }
                }
            }
        }

        given("메시지 전송 API") {
            val userId = 1L
            val sessionId = 10L
            val messageContent = "안녕하세요, 고민이 있어요"

            val userMessage =
                Message(
                    session = ChatSession(userId = userId, counselorId = 1L),
                    senderType = SenderType.USER,
                    content = messageContent,
                )

            val aiMessage =
                Message(
                    session = ChatSession(userId = userId, counselorId = 1L),
                    senderType = SenderType.AI,
                    content = "안녕하세요. 어떤 고민이신지 천천히 말씀해주세요.",
                    aiPhaseAssessment = "RAPPORT 단계: 라포 형성 중",
                )

            `when`("인증된 사용자가 메시지를 전송하면") {
                every { chatSessionService.sendMessage(sessionId, userId, messageContent) } returns
                    Pair(userMessage, aiMessage)

                val request = mapOf("content" to messageContent)

                then("200 OK와 함께 사용자 메시지와 AI 응답이 반환된다") {
                    mockMvc.post("/api/v1/sessions/{sessionId}/messages", sessionId) {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.userMessage.content") { value(messageContent) }
                        jsonPath("$.data.aiMessage.content") { value("안녕하세요. 어떤 고민이신지 천천히 말씀해주세요.") }
                    }

                    verify(exactly = 1) { chatSessionService.sendMessage(sessionId, userId, messageContent) }
                }
            }

            `when`("빈 메시지를 전송하면") {
                val request = mapOf("content" to "")

                then("400 Bad Request가 반환된다") {
                    mockMvc.post("/api/v1/sessions/{sessionId}/messages", sessionId) {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }

        given("세션 종료 API") {
            val userId = 1L
            val sessionId = 10L
            val closedSession =
                ChatSession(
                    userId = userId,
                    counselorId = 1L,
                ).apply {
                    closedAt = LocalDateTime.now()
                }

            `when`("인증된 사용자가 세션을 종료하면") {
                every { chatSessionService.closeSession(sessionId, userId) } returns closedSession

                then("200 OK와 함께 종료된 세션이 반환된다") {
                    mockMvc.patch("/api/v1/sessions/{sessionId}/close", sessionId) {
                        header("Authorization", "Bearer valid-token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.closedAt") { exists() }
                    }

                    verify(exactly = 1) { chatSessionService.closeSession(sessionId, userId) }
                }
            }
        }

        given("세션 평가 API") {
            val userId = 1L
            val sessionId = 10L
            val rating = 5
            val feedback = "정말 도움이 되었습니다"

            val mockUser =
                User(
                    email = "test@example.com",
                    nickname = "테스터",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google123",
                )

            val mockCounselor =
                Counselor(
                    name = "소크라테스",
                    title = "고대 그리스 철학자",
                    description = "너 자신을 알라",
                    personalityMatrix = "{}",
                    basePrompt = "소크라테스의 문답법으로 상담합니다",
                    specialties = "[\"인생\", \"철학\"]",
                )

            val mockSession =
                ChatSession(
                    userId = userId,
                    counselorId = 1L,
                )

            val counselorRating =
                CounselorRating(
                    user = mockUser,
                    counselor = mockCounselor,
                    session = mockSession,
                    rating = rating.toDouble(),
                    review = feedback,
                )

            `when`("인증된 사용자가 종료된 세션을 평가하면") {
                every { chatSessionService.rateSession(sessionId, userId, rating, feedback) } returns counselorRating

                val request =
                    mapOf(
                        "rating" to rating,
                        "feedback" to feedback,
                    )

                then("200 OK와 함께 평가 결과가 반환된다") {
                    mockMvc.post("/api/v1/sessions/{sessionId}/rate", sessionId) {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.rating") { value(rating) }
                    }

                    verify(exactly = 1) { chatSessionService.rateSession(sessionId, userId, rating, feedback) }
                }
            }

            `when`("잘못된 평점(6)으로 평가하면") {
                val request =
                    mapOf(
                        "rating" to 6,
                        "feedback" to feedback,
                    )

                then("400 Bad Request가 반환된다") {
                    mockMvc.post("/api/v1/sessions/{sessionId}/rate", sessionId) {
                        header("Authorization", "Bearer valid-token")
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }

        given("북마크 토글 API") {
            val userId = 1L
            val sessionId = 10L

            `when`("인증된 사용자가 북마크를 토글하면") {
                every { chatSessionService.toggleBookmark(sessionId, userId) } returns true

                then("200 OK와 함께 북마크 상태가 반환된다") {
                    mockMvc.patch("/api/v1/sessions/{sessionId}/bookmark", sessionId) {
                        header("Authorization", "Bearer valid-token")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.bookmarked") { value(true) }
                    }

                    verify(exactly = 1) { chatSessionService.toggleBookmark(sessionId, userId) }
                }
            }
        }

        given("세션 메시지 조회 API") {
            val userId = 1L
            val sessionId = 10L
            val messages =
                listOf(
                    Message(
                        session = ChatSession(userId = userId, counselorId = 1L),
                        senderType = SenderType.USER,
                        content = "안녕하세요",
                    ),
                    Message(
                        session = ChatSession(userId = userId, counselorId = 1L),
                        senderType = SenderType.AI,
                        content = "안녕하세요, 무엇을 도와드릴까요?",
                        aiPhaseAssessment = "RAPPORT 단계",
                    ),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(messages, pageable, messages.size.toLong())

            `when`("인증된 사용자가 세션의 메시지를 조회하면") {
                every { chatSessionService.getSessionMessages(sessionId, userId, any()) } returns page

                then("200 OK와 함께 메시지 목록이 반환된다") {
                    mockMvc.get("/api/v1/sessions/{sessionId}/messages", sessionId) {
                        header("Authorization", "Bearer valid-token")
                        param("page", "0")
                        param("size", "20")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.resultCode") { value("200") }
                        jsonPath("$.data.content[0].content") { value("안녕하세요") }
                        jsonPath("$.data.content[1].senderType") { value("AI") }
                    }

                    verify(exactly = 1) { chatSessionService.getSessionMessages(sessionId, userId, any()) }
                }
            }
        }
    })
