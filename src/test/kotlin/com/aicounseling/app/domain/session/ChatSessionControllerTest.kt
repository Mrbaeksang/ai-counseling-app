package com.aicounseling.app.domain.session

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.session.dto.*
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.openrouter.OpenRouterService
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// 테스트용 Mock 설정
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun mockOpenRouterService(): OpenRouterService = mockk()
}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatSessionControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val counselorRepository: CounselorRepository,
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val openRouterService: OpenRouterService,
) : BehaviorSpec({

        lateinit var testUser: User
        lateinit var testCounselor: Counselor
        lateinit var authToken: String

        beforeEach {
            // 테스트 사용자 생성
            testUser =
                userRepository.save(
                    User(
                        email = "test@example.com",
                        nickname = "테스트유저",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-test-id",
                    ),
                )

            // 테스트 상담사 생성
            testCounselor =
                counselorRepository.save(
                    Counselor(
                        name = "아리스토텔레스",
                        title = "고대 그리스의 철학자",
                        description = "실용적 윤리학과 행복론의 대가",
                        personalityMatrix = """{"wisdom": 9, "empathy": 8, "logic": 10}""",
                        basePrompt = "당신은 아리스토텔레스입니다.",
                        specialties = """["윤리학", "논리학", "형이상학"]""",
                    ),
                )

            // JWT 토큰 생성
            authToken = jwtTokenProvider.createToken(testUser.id!!, testUser.email)
        }

        Given("세션 API 테스트") {

            When("1. GET /api/sessions - 세션 목록 조회") {
                Then("인증된 사용자는 세션 목록을 조회할 수 있다") {
                    // 테스트 세션 생성
                    val session1 =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "첫 번째 상담",
                                isBookmarked = false,
                            ),
                        )
                    val session2 =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "두 번째 상담",
                                isBookmarked = true,
                            ),
                        )

                    // 전체 세션 조회
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "0")
                            .param("size", "10"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.resultCode").value("200"))
                        .andExpect(jsonPath("$.data.content").isArray)
                        .andExpect(jsonPath("$.data.content.length()").value(2))
                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
                }

                Then("북마크된 세션만 필터링할 수 있다") {
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("bookmarked", "true")
                            .param("page", "0")
                            .param("size", "10"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(1))
                        .andExpect(jsonPath("$.data.content[0].isBookmarked").value(true))
                }

                Then("인증되지 않은 사용자는 401 에러") {
                    mockMvc.perform(get("/api/sessions"))
                        .andExpect(status().isUnauthorized)
                }
            }

            When("2. POST /api/sessions - 새 세션 시작") {
                Then("인증된 사용자는 새 세션을 시작할 수 있다") {
                    val request = StartSessionRequest(counselorId = testCounselor.id!!)

                    mockMvc.perform(
                        post("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.resultCode").value("201"))
                        .andExpect(jsonPath("$.data.id").exists())
                        .andExpect(jsonPath("$.data.counselorId").value(testCounselor.id))
                        .andExpect(jsonPath("$.data.counselorName").value(testCounselor.name))
                }

                Then("여러 세션을 동시에 시작할 수 있다") {
                    val request = StartSessionRequest(counselorId = testCounselor.id!!)

                    // 첫 번째 세션
                    mockMvc.perform(
                        post("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)

                    // 두 번째 세션 (활성 세션 체크 제거되어 성공해야 함)
                    mockMvc.perform(
                        post("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                }

                Then("존재하지 않는 상담사 ID로 요청하면 400 에러") {
                    val request = StartSessionRequest(counselorId = 99999L)

                    mockMvc.perform(
                        post("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                }
            }

            When("3. DELETE /api/sessions/{id} - 세션 종료") {
                Then("인증된 사용자는 자신의 세션을 종료할 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "테스트 세션",
                            ),
                        )

                    mockMvc.perform(
                        delete("/api/sessions/${session.id}")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.resultCode").value("200"))
                        .andExpect(jsonPath("$.data.id").value(session.id))
                        .andExpect(jsonPath("$.data.closedAt").exists())
                }

                Then("이미 종료된 세션을 다시 종료하면 400 에러") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    mockMvc.perform(
                        delete("/api/sessions/${session.id}")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.msg").value(AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED))
                }

                Then("다른 사용자의 세션은 종료할 수 없다") {
                    val otherUser =
                        userRepository.save(
                            User(
                                email = "other@example.com",
                                nickname = "다른유저",
                                authProvider = AuthProvider.GOOGLE,
                                providerId = "google-other-id",
                            ),
                        )

                    val otherSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = otherUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "다른 사용자 세션",
                            ),
                        )

                    mockMvc.perform(
                        delete("/api/sessions/${otherSession.id}")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isNotFound)
                }
            }

            When("4. GET /api/sessions/{id}/messages - 메시지 목록 조회") {
                Then("인증된 사용자는 세션의 메시지를 조회할 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "테스트 세션",
                            ),
                        )

                    // 테스트 메시지 생성
                    messageRepository.saveAll(
                        listOf(
                            Message(
                                session = session,
                                senderType = SenderType.USER,
                                content = "안녕하세요",
                                phase = CounselingPhase.ENGAGEMENT,
                            ),
                            Message(
                                session = session,
                                senderType = SenderType.AI,
                                content = "안녕하세요! 무엇을 도와드릴까요?",
                                phase = CounselingPhase.ENGAGEMENT,
                            ),
                        ),
                    )

                    mockMvc.perform(
                        get("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "0")
                            .param("size", "20"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.resultCode").value("200"))
                        .andExpect(jsonPath("$.data.content").isArray)
                        .andExpect(jsonPath("$.data.content.length()").value(2))
                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
                }

                Then("다른 사용자의 세션 메시지는 조회할 수 없다") {
                    val otherUser =
                        userRepository.save(
                            User(
                                email = "other@example.com",
                                nickname = "다른유저",
                                authProvider = AuthProvider.GOOGLE,
                                providerId = "google-other-id",
                            ),
                        )

                    val otherSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = otherUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "다른 사용자 세션",
                            ),
                        )

                    mockMvc.perform(
                        get("/api/sessions/${otherSession.id}/messages")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isNotFound)
                }
            }

            When("5. POST /api/sessions/{id}/messages - 메시지 전송") {
                Then("인증된 사용자는 메시지를 전송하고 AI 응답을 받을 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = null, // 첫 메시지로 제목 생성 테스트
                            ),
                        )

                    val request = SendMessageRequest(content = "오늘 너무 힘든 일이 있었어요")

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.resultCode").value("201"))
                        .andExpect(jsonPath("$.data.userMessage.content").value("오늘 너무 힘든 일이 있었어요"))
                        .andExpect(jsonPath("$.data.userMessage.senderType").value("USER"))
                        .andExpect(jsonPath("$.data.aiResponse").exists())
                        .andExpect(jsonPath("$.data.aiResponse.senderType").value("AI"))
                        .andExpect(jsonPath("$.data.currentPhase").exists())
                        .andExpect(jsonPath("$.data.sessionTitle").exists()) // 첫 메시지이므로 제목 포함
                }

                Then("빈 메시지는 전송할 수 없다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "테스트 세션",
                            ),
                        )

                    val request = SendMessageRequest(content = "")

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                }

                Then("종료된 세션에는 메시지를 전송할 수 없다") {
                    val closedSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    val request = SendMessageRequest(content = "메시지")

                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.msg").value(AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED))
                }
            }

            When("6. PATCH /api/sessions/{id}/bookmark - 북마크 토글") {
                Then("인증된 사용자는 세션을 북마크할 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "테스트 세션",
                                isBookmarked = false,
                            ),
                        )

                    // 북마크 추가
                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/bookmark")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.resultCode").value("200"))
                        .andExpect(jsonPath("$.data.sessionId").value(session.id))
                        .andExpect(jsonPath("$.data.bookmarked").value(true))

                    // 북마크 제거 (토글)
                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/bookmark")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.bookmarked").value(false))
                }

                Then("다른 사용자의 세션은 북마크할 수 없다") {
                    val otherUser =
                        userRepository.save(
                            User(
                                email = "other@example.com",
                                nickname = "다른유저",
                                authProvider = AuthProvider.GOOGLE,
                                providerId = "google-other-id",
                            ),
                        )

                    val otherSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = otherUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "다른 사용자 세션",
                            ),
                        )

                    mockMvc.perform(
                        patch("/api/sessions/${otherSession.id}/bookmark")
                            .header("Authorization", "Bearer $authToken"),
                    )
                        .andExpect(status().isNotFound)
                }
            }

            When("7. PATCH /api/sessions/{id}/title - 제목 수정") {
                Then("인증된 사용자는 세션 제목을 수정할 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "기존 제목",
                            ),
                        )

                    val request = UpdateSessionTitleRequest(title = "새로운 제목")

                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/title")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.resultCode").value("200"))
                        .andExpect(jsonPath("$.data.title").value("새로운 제목"))
                }

                Then("빈 제목으로는 수정할 수 없다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "기존 제목",
                            ),
                        )

                    val request = UpdateSessionTitleRequest(title = "")

                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/title")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                }

                Then("너무 긴 제목은 잘려서 저장된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "기존 제목",
                            ),
                        )

                    val longTitle = "a".repeat(200) // 100자 제한 초과
                    val request = UpdateSessionTitleRequest(title = longTitle)

                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/title")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.title.length()").value(100))
                }
            }

            When("8. POST /api/sessions/{id}/rate - 세션 평가") {
                Then("종료된 세션은 평가할 수 있다") {
                    val closedSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    val request =
                        RateSessionRequest(
                            rating = 5,
                            feedback = "매우 도움이 되었습니다",
                        )

                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.resultCode").value("201"))
                        .andExpect(jsonPath("$.data.sessionId").value(closedSession.id))
                        .andExpect(jsonPath("$.data.rating").value(5))
                        .andExpect(jsonPath("$.data.feedback").value("매우 도움이 되었습니다"))
                }

                Then("진행 중인 세션은 평가할 수 없다") {
                    val activeSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "진행 중인 세션",
                            ),
                        )

                    val request = RateSessionRequest(rating = 5)

                    mockMvc.perform(
                        post("/api/sessions/${activeSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.msg").value("진행 중인 세션은 평가할 수 없습니다"))
                }

                Then("평점은 1-5 범위여야 한다") {
                    val closedSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    // 범위 초과
                    val request = RateSessionRequest(rating = 6)

                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)

                    // 범위 미만
                    val request2 = RateSessionRequest(rating = 0)

                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)),
                    )
                        .andExpect(status().isBadRequest)
                }

                Then("같은 세션은 한 번만 평가할 수 있다") {
                    val closedSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    val request = RateSessionRequest(rating = 5)

                    // 첫 번째 평가 - 성공
                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)

                    // 두 번째 평가 - 실패
                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.msg").value("이미 평가한 세션입니다"))
                }
            }

            When("인증되지 않은 사용자의 요청") {
                Then("모든 세션 API는 401 에러를 반환한다") {
                    // GET /sessions
                    mockMvc.perform(get("/api/sessions"))
                        .andExpect(status().isUnauthorized)

                    // POST /sessions
                    mockMvc.perform(
                        post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"counselorId": 1}"""),
                    )
                        .andExpect(status().isUnauthorized)

                    // DELETE /sessions/{id}
                    mockMvc.perform(delete("/api/sessions/1"))
                        .andExpect(status().isUnauthorized)

                    // GET /sessions/{id}/messages
                    mockMvc.perform(get("/api/sessions/1/messages"))
                        .andExpect(status().isUnauthorized)

                    // POST /sessions/{id}/messages
                    mockMvc.perform(
                        post("/api/sessions/1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content": "test"}"""),
                    )
                        .andExpect(status().isUnauthorized)

                    // PATCH /sessions/{id}/bookmark
                    mockMvc.perform(patch("/api/sessions/1/bookmark"))
                        .andExpect(status().isUnauthorized)

                    // PATCH /sessions/{id}/title
                    mockMvc.perform(
                        patch("/api/sessions/1/title")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"title": "test"}"""),
                    )
                        .andExpect(status().isUnauthorized)

                    // POST /sessions/{id}/rate
                    mockMvc.perform(
                        post("/api/sessions/1/rate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"rating": 5}"""),
                    )
                        .andExpect(status().isUnauthorized)
                }
            }

            // ===== 추가 테스트: 고급 시나리오 =====

            When("OpenRouter API 관련 테스트") {
                Then("AI 응답이 실패하면 에러 메시지가 저장된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "테스트 세션",
                            ),
                        )

                    // OpenRouter 서비스 모킹 - IOException 발생
                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), any())
                    } throws IOException("OpenRouter API 연결 실패")

                    val request = SendMessageRequest(content = "도움이 필요해요")

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(
                            jsonPath("$.data.aiResponse.content")
                                .value(AppConstants.ErrorMessages.AI_RESPONSE_ERROR),
                        )
                }

                Then("AI가 상담 단계를 변경하면 phase가 업데이트된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "단계 전환 테스트",
                            ),
                        )

                    // 첫 메시지 - ENGAGEMENT 단계
                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), true)
                    } returns
                        """
                        {
                            "content": "안녕하세요! 무엇을 도와드릴까요?",
                            "currentPhase": "ENGAGEMENT",
                            "sessionTitle": "오늘의 고민 상담"
                        }
                        """.trimIndent()

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content": "안녕하세요"}"""),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.currentPhase").value("ENGAGEMENT"))
                        .andExpect(jsonPath("$.data.sessionTitle").value("오늘의 고민 상담"))

                    // 두 번째 메시지 - ASSESSMENT 단계로 전환
                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), false)
                    } returns
                        """
                        {
                            "content": "구체적으로 어떤 상황인지 설명해주시겠어요?",
                            "currentPhase": "ASSESSMENT_AND_CONCEPTUALIZATION"
                        }
                        """.trimIndent()

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content": "직장에서 스트레스를 받고 있어요"}"""),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(
                            jsonPath("$.data.currentPhase")
                                .value("ASSESSMENT_AND_CONCEPTUALIZATION"),
                        )
                        .andExpect(jsonPath("$.data.sessionTitle").doesNotExist()) // 두 번째 메시지는 제목 없음
                }

                Then("AI 응답이 잘못된 JSON이면 원본 텍스트가 저장된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "잘못된 응답 테스트",
                            ),
                        )

                    // 잘못된 JSON 응답
                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), any())
                    } returns "이것은 JSON이 아닌 일반 텍스트 응답입니다"

                    val request = SendMessageRequest(content = "테스트 메시지")

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(
                            jsonPath("$.data.aiResponse.content")
                                .value("이것은 JSON이 아닌 일반 텍스트 응답입니다"),
                        )
                        .andExpect(jsonPath("$.data.currentPhase").value("ENGAGEMENT")) // 기본값
                }
            }

            When("대용량 데이터 페이징 테스트") {
                Then("100개의 세션이 있을 때 페이징이 올바르게 동작한다") {
                    // 100개 세션 생성
                    repeat(100) { index ->
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "세션 #${index + 1}",
                                isBookmarked = index % 3 == 0, // 33개만 북마크
                            ),
                        )
                    }

                    // 첫 페이지
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "0")
                            .param("size", "10"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(10))
                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(100))
                        .andExpect(jsonPath("$.data.pageInfo.totalPages").value(10))
                        .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))

                    // 마지막 페이지
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "9")
                            .param("size", "10"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(10))
                        .andExpect(jsonPath("$.data.pageInfo.currentPage").value(9))

                    // 북마크 필터링
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("bookmarked", "true")
                            .param("page", "0")
                            .param("size", "50"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(34)) // 0포함 33개 + 기존 1개
                }

                Then("세션에 1000개의 메시지가 있을 때 페이징이 동작한다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "대용량 메시지 세션",
                            ),
                        )

                    // 1000개 메시지 생성
                    val messages = mutableListOf<Message>()
                    repeat(1000) { index ->
                        messages.add(
                            Message(
                                session = session,
                                senderType = if (index % 2 == 0) SenderType.USER else SenderType.AI,
                                content = "메시지 #${index + 1}",
                                phase = CounselingPhase.ENGAGEMENT,
                            ),
                        )
                    }
                    messageRepository.saveAll(messages)

                    // 첫 페이지
                    mockMvc.perform(
                        get("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "0")
                            .param("size", "20"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(20))
                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1000))
                        .andExpect(jsonPath("$.data.pageInfo.totalPages").value(50))

                    // 중간 페이지
                    mockMvc.perform(
                        get("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "25")
                            .param("size", "20"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content.length()").value(20))
                        .andExpect(jsonPath("$.data.pageInfo.currentPage").value(25))
                }

                Then("빈 페이지를 요청해도 에러가 발생하지 않는다") {
                    // 세션이 하나도 없는 상태에서
                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "100")
                            .param("size", "20"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content").isEmpty)
                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
                }
            }

            When("특수문자 및 이모지 처리 테스트") {
                Then("제목에 이모지와 특수문자가 포함되어도 정상 처리된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "기존 제목",
                            ),
                        )

                    val specialTitle = "😊 제목 <script>alert('xss')</script> & \"quotes\" 'single' 🎉"
                    val request = UpdateSessionTitleRequest(title = specialTitle)

                    mockMvc.perform(
                        patch("/api/sessions/${session.id}/title")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.title").value(specialTitle))

                    // DB에 정확히 저장되었는지 확인
                    val updatedSession = sessionRepository.findById(session.id!!).get()
                    updatedSession.title shouldBe specialTitle
                }

                Then("메시지에 멀티라인 텍스트와 이모지가 포함되어도 처리된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "이모지 테스트",
                            ),
                        )

                    val multilineContent =
                        """
                        첫 번째 줄 😊
                        두 번째 줄 with "quotes"
                        세 번째 줄 <tag>test</tag>
                        네 번째 줄 🎉🎊🎈
                        """.trimIndent()

                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), any())
                    } returns
                        """
                        {
                            "content": "이모지 응답입니다 😊",
                            "currentPhase": "ENGAGEMENT"
                        }
                        """.trimIndent()

                    val request = SendMessageRequest(content = multilineContent)

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.userMessage.content").value(multilineContent))
                        .andExpect(jsonPath("$.data.aiResponse.content").value("이모지 응답입니다 😊"))
                }
            }

            When("동시성 테스트") {
                Then("동시에 여러 요청이 와도 데이터 정합성이 유지된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "동시성 테스트",
                                isBookmarked = false,
                            ),
                        )

                    val threadCount = 10
                    val latch = CountDownLatch(threadCount)
                    val executor = Executors.newFixedThreadPool(threadCount)
                    val errors = mutableListOf<Exception>()

                    // 10개 스레드에서 동시에 북마크 토글
                    repeat(threadCount) {
                        executor.submit {
                            try {
                                mockMvc.perform(
                                    patch("/api/sessions/${session.id}/bookmark")
                                        .header("Authorization", "Bearer $authToken"),
                                )
                            } catch (e: Exception) {
                                errors.add(e)
                            } finally {
                                latch.countDown()
                            }
                        }
                    }

                    latch.await(10, TimeUnit.SECONDS)
                    executor.shutdown()

                    // 에러가 없어야 함
                    errors shouldBe emptyList()

                    // 최종 상태 확인 (짝수번 토글이므로 false여야 함)
                    val finalSession = sessionRepository.findById(session.id!!).get()
                    finalSession.isBookmarked shouldBe false
                }

                Then("동시에 같은 세션을 종료하려고 해도 한 번만 종료된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "동시 종료 테스트",
                            ),
                        )

                    val threadCount = 5
                    val latch = CountDownLatch(threadCount)
                    val executor = Executors.newFixedThreadPool(threadCount)
                    var successCount = 0
                    var failCount = 0

                    repeat(threadCount) {
                        executor.submit {
                            try {
                                val result =
                                    mockMvc.perform(
                                        delete("/api/sessions/${session.id}")
                                            .header("Authorization", "Bearer $authToken"),
                                    ).andReturn()

                                if (result.response.status == 200) {
                                    successCount++
                                } else {
                                    failCount++
                                }
                            } finally {
                                latch.countDown()
                            }
                        }
                    }

                    latch.await(10, TimeUnit.SECONDS)
                    executor.shutdown()

                    // 한 번만 성공해야 함
                    successCount shouldBe 1
                    failCount shouldBe 4
                }
            }

            When("엣지 케이스 테스트") {
                Then("매우 긴 메시지도 처리할 수 있다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "긴 메시지 테스트",
                            ),
                        )

                    val longContent = "a".repeat(5000) // 5000자

                    coEvery {
                        openRouterService.sendCounselingMessage(any(), any(), any(), any())
                    } returns
                        """
                        {
                            "content": "긴 메시지에 대한 응답",
                            "currentPhase": "ENGAGEMENT"
                        }
                        """.trimIndent()

                    val request = SendMessageRequest(content = longContent)

                    mockMvc.perform(
                        post("/api/sessions/${session.id}/messages")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.userMessage.content.length()").value(5000))
                }

                Then("평점에 소수점이 포함되어도 정수로 처리된다") {
                    val closedSession =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = "종료된 세션",
                                closedAt = LocalDateTime.now(),
                            ),
                        )

                    // JSON에 소수점 포함
                    val jsonRequest =
                        """
                        {
                            "rating": 4.5,
                            "feedback": "좋았습니다"
                        }
                        """.trimIndent()

                    mockMvc.perform(
                        post("/api/sessions/${closedSession.id}/rate")
                            .header("Authorization", "Bearer $authToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest),
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.data.rating").value(4)) // 정수로 변환
                }

                Then("세션 제목이 null인 경우 빈 문자열로 처리된다") {
                    val sessionWithNullTitle =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = null,
                            ),
                        )

                    mockMvc.perform(
                        get("/api/sessions")
                            .header("Authorization", "Bearer $authToken")
                            .param("page", "0")
                            .param("size", "10"),
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.content[0].title").value(""))
                }
            }

            When("Phase 전환 시나리오 테스트") {
                Then("5단계 상담 모델이 순차적으로 진행된다") {
                    val session =
                        sessionRepository.save(
                            ChatSession(
                                userId = testUser.id!!,
                                counselorId = testCounselor.id!!,
                                title = null,
                            ),
                        )

                    val phases =
                        listOf(
                            "ENGAGEMENT" to "안녕하세요, 무엇을 도와드릴까요?",
                            "ASSESSMENT_AND_CONCEPTUALIZATION" to "상황을 더 자세히 알려주세요",
                            "INTERVENTION_AND_SKILL_BUILDING" to "이런 방법을 시도해보세요",
                            "ACTION_AND_GENERALIZATION" to "실천 계획을 세워봅시다",
                            "TERMINATION_AND_RELAPSE_PREVENTION" to "마무리하며 정리해봅시다",
                        )

                    phases.forEachIndexed { index, (phase, aiContent) ->
                        coEvery {
                            openRouterService.sendCounselingMessage(any(), any(), any(), index == 0)
                        } returns
                            if (index == 0) {
                                """
                                {
                                    "content": "$aiContent",
                                    "currentPhase": "$phase",
                                    "sessionTitle": "단계별 상담 진행"
                                }
                                """.trimIndent()
                            } else {
                                """
                                {
                                    "content": "$aiContent",
                                    "currentPhase": "$phase"
                                }
                                """.trimIndent()
                            }

                        val request = SendMessageRequest(content = "사용자 메시지 ${index + 1}")

                        mockMvc.perform(
                            post("/api/sessions/${session.id}/messages")
                                .header("Authorization", "Bearer $authToken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        )
                            .andExpect(status().isCreated)
                            .andExpect(jsonPath("$.data.currentPhase").value(phase))
                            .andExpect(jsonPath("$.data.aiResponse.content").value(aiContent))
                    }

                    // DB에서 메시지들의 phase 확인
                    val messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.id!!)
                    messages.size shouldBe 10 // 사용자 5개, AI 5개

                    // AI 메시지들의 phase 확인
                    val aiMessages = messages.filter { it.senderType == SenderType.AI }
                    aiMessages[0].phase shouldBe CounselingPhase.ENGAGEMENT
                    aiMessages[1].phase shouldBe CounselingPhase.ASSESSMENT_AND_CONCEPTUALIZATION
                    aiMessages[2].phase shouldBe CounselingPhase.INTERVENTION_AND_SKILL_BUILDING
                    aiMessages[3].phase shouldBe CounselingPhase.ACTION_AND_GENERALIZATION
                    aiMessages[4].phase shouldBe CounselingPhase.TERMINATION_AND_RELAPSE_PREVENTION
                }
            }
        }
    })
