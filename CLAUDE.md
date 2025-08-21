package com.aicounseling.app.domain.session

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.service.CounselorService
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.session.service.ChatSessionService
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(useDefaultConstructor = true)
@Transactional
class ChatSessionControllerTest(
    private val context: WebApplicationContext,
    private val objectMapper: ObjectMapper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val counselorRepository: CounselorRepository,
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val sessionService: ChatSessionService,
    private val counselorService: CounselorService,
) : BehaviorSpec({
    
    val mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    
    lateinit var testUser: User
    lateinit var testCounselor: Counselor
    lateinit var testSession: ChatSession
    lateinit var authToken: String

    beforeEach {
        // 테스트 사용자 생성
        testUser = userRepository.save(
            User(
                email = "test@example.com",
                nickname = "테스트유저",
                authProvider = AuthProvider.GOOGLE,
                providerId = "google-test-id"
            )
        )

        // 테스트 상담사 생성
        testCounselor = counselorRepository.save(
            Counselor(
                name = "아리스토텔레스",
                title = "고대 그리스의 철학자",
                description = "실용적 윤리학과 행복론의 대가",
                personalityMatrix = """{"wisdom": 9, "empathy": 8, "logic": 10}""",
                basePrompt = "당신은 아리스토텔레스입니다."
            )
        )

        // JWT 토큰 생성
        authToken = jwtTokenProvider.createToken(testUser.id, testUser.email)

        // 테스트 세션 생성
        testSession = sessionRepository.save(
            ChatSession(
                userId = testUser.id,
                counselorId = testCounselor.id,
                title = "테스트 상담 세션",
                isBookmarked = false,
                lastMessageAt = LocalDateTime.now()
            )
        )
    }

    Given("인증된 사용자가") {
        When("세션 목록을 조회할 때") {
            Then("페이징된 세션 목록을 반환한다") {
                val result = mockMvc.perform(
                    get("/api/sessions")
                        .header("Authorization", "Bearer $authToken")
                        .param("page", "0")
                        .param("size", "20")
                        .param("bookmarked", "false")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.content").isArray)
                    .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
                    .andReturn()

                println("세션 목록 조회 응답: ${result.response.contentAsString}")
            }
        }

        When("북마크된 세션만 조회할 때") {
            Then("북마크된 세션 목록만 반환한다") {
                // 세션 북마크 설정
                testSession.isBookmarked = true
                sessionRepository.save(testSession)

                mockMvc.perform(
                    get("/api/sessions")
                        .header("Authorization", "Bearer $authToken")
                        .param("page", "0")
                        .param("size", "20")
                        .param("bookmarked", "true")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.content").isArray)
            }
        }

        When("새 세션을 시작할 때") {
            Then("새 세션이 생성된다") {
                val request = mapOf("counselorId" to testCounselor.id)

                mockMvc.perform(
                    post("/api/sessions")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.resultCode").value("201"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.counselorId").value(testCounselor.id))
            }
        }

        When("잘못된 상담사 ID로 세션을 시작할 때") {
            Then("400 에러를 반환한다") {
                val request = mapOf("counselorId" to 99999L)

                mockMvc.perform(
                    post("/api/sessions")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.resultCode").value("400"))
            }
        }

        When("세션 메시지 목록을 조회할 때") {
            Then("페이징된 메시지 목록을 반환한다") {
                // 테스트 메시지 생성
                messageRepository.save(
                    Message(
                        sessionId = testSession.id,
                        senderType = SenderType.USER,
                        content = "안녕하세요",
                        phase = CounselingPhase.ENGAGEMENT
                    )
                )

                mockMvc.perform(
                    get("/api/sessions/${testSession.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .param("page", "0")
                        .param("size", "20")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.content").isArray)
                    .andExpect(jsonPath("$.data.pageInfo.currentPage").value(0))
            }
        }

        When("메시지를 전송할 때") {
            Then("사용자 메시지와 AI 응답을 반환한다") {
                val request = mapOf("content" to "안녕하세요, 고민이 있어요")

                mockMvc.perform(
                    post("/api/sessions/${testSession.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.resultCode").value("201"))
                    .andExpect(jsonPath("$.data.userMessage").exists())
                    .andExpect(jsonPath("$.data.aiResponse").exists())
                    .andExpect(jsonPath("$.data.currentPhase").exists())
            }
        }

        When("빈 메시지를 전송할 때") {
            Then("400 에러를 반환한다") {
                val request = mapOf("content" to "")

                mockMvc.perform(
                    post("/api/sessions/${testSession.id}/messages")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.resultCode").value("400"))
            }
        }

        When("세션을 종료할 때") {
            Then("세션이 종료된다") {
                mockMvc.perform(
                    delete("/api/sessions/${testSession.id}")
                        .header("Authorization", "Bearer $authToken")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.id").value(testSession.id))
            }
        }

        When("세션을 평가할 때") {
            Then("평가가 저장된다") {
                val request = mapOf(
                    "rating" to 5,
                    "feedback" to "매우 도움이 되었습니다"
                )

                mockMvc.perform(
                    post("/api/sessions/${testSession.id}/rate")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.resultCode").value("201"))
                    .andExpect(jsonPath("$.data.sessionId").value(testSession.id))
                    .andExpect(jsonPath("$.data.rating").value(5))
            }
        }

        When("잘못된 평점으로 세션을 평가할 때") {
            Then("400 에러를 반환한다") {
                val request = mapOf("rating" to 6) // 1-5 범위 초과

                mockMvc.perform(
                    post("/api/sessions/${testSession.id}/rate")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.resultCode").value("400"))
            }
        }

        When("세션을 북마크 토글할 때") {
            Then("북마크 상태가 변경된다") {
                mockMvc.perform(
                    post("/api/sessions/${testSession.id}/bookmark")
                        .header("Authorization", "Bearer $authToken")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.sessionId").value(testSession.id))
                    .andExpect(jsonPath("$.data.bookmarked").value(true))
            }
        }

        When("세션 제목을 수정할 때") {
            Then("제목이 변경된다") {
                val request = mapOf("title" to "새로운 세션 제목")

                mockMvc.perform(
                    patch("/api/sessions/${testSession.id}/title")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.title").value("새로운 세션 제목"))
            }
        }

        When("빈 제목으로 세션 제목을 수정할 때") {
            Then("400 에러를 반환한다") {
                val request = mapOf("title" to "")

                mockMvc.perform(
                    patch("/api/sessions/${testSession.id}/title")
                        .header("Authorization", "Bearer $authToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.resultCode").value("400"))
            }
        }
    }

    Given("인증되지 않은 사용자가") {
        When("세션 목록을 조회할 때") {
            Then("401 에러를 반환한다") {
                mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isUnauthorized)
                    .andExpect(jsonPath("$.resultCode").value("401"))
                    .andExpect(jsonPath("$.msg").value("인증이 필요합니다"))
            }
        }

        When("다른 모든 세션 API를 호출할 때") {
            Then("401 에러를 반환한다") {
                // POST /sessions
                mockMvc.perform(
                    post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"counselorId": 1}""")
                )
                    .andExpect(status().isUnauthorized)

                // GET /sessions/{id}/messages  
                mockMvc.perform(get("/api/sessions/1/messages"))
                    .andExpect(status().isUnauthorized)

                // POST /sessions/{id}/messages
                mockMvc.perform(
                    post("/api/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content": "test"}""")
                )
                    .andExpect(status().isUnauthorized)

                // DELETE /sessions/{id}
                mockMvc.perform(delete("/api/sessions/1"))
                    .andExpect(status().isUnauthorized)

                // POST /sessions/{id}/rate
                mockMvc.perform(
                    post("/api/sessions/1/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"rating": 5}""")
                )
                    .andExpect(status().isUnauthorized)

                // POST /sessions/{id}/bookmark
                mockMvc.perform(post("/api/sessions/1/bookmark"))
                    .andExpect(status().isUnauthorized)

                // PATCH /sessions/{id}/title
                mockMvc.perform(
                    patch("/api/sessions/1/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"title": "test"}""")
                )
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    Given("다른 사용자의 세션에 접근할 때") {
        When("다른 사용자가 세션을 조작하려고 하면") {
            Then("403 또는 404 에러를 반환한다") {
                // 다른 사용자 생성
                val otherUser = userRepository.save(
                    User(
                        email = "other@example.com",
                        nickname = "다른유저",
                        authProvider = AuthProvider.GOOGLE,
                        providerId = "google-other-id"
                    )
                )
                val otherToken = jwtTokenProvider.createToken(otherUser.id, otherUser.email)

                // 다른 사용자가 testSession에 접근 시도
                mockMvc.perform(
                    get("/api/sessions/${testSession.id}/messages")
                        .header("Authorization", "Bearer $otherToken")
                )
                    .andExpect(status().is4xxClientError) // 403 또는 404
            }
        }
    }
})
