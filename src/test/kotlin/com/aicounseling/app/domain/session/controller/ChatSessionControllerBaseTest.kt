package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.openrouter.OpenRouterService
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional

/**
 * 공통 테스트 설정
 */
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun mockOpenRouterService(): OpenRouterService = mockk()
}

/**
 * ChatSessionController 테스트 기본 클래스
 * 공통 설정과 헬퍼 메서드 제공
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ChatSessionControllerBaseTest(
    protected val mockMvc: MockMvc,
    protected val objectMapper: ObjectMapper,
    protected val jwtTokenProvider: JwtTokenProvider,
    protected val userRepository: UserRepository,
    protected val counselorRepository: CounselorRepository,
    protected val sessionRepository: ChatSessionRepository,
    protected val messageRepository: MessageRepository,
    protected val openRouterService: OpenRouterService,
) : BehaviorSpec() {
    protected lateinit var testUser: User
    protected lateinit var testCounselor: Counselor
    protected lateinit var authToken: String

    init {
        beforeEach {
            setupTestData()
        }

        afterEach {
            cleanupTestData()
        }
    }

    /**
     * 테스트 데이터 초기화
     */
    private fun setupTestData() {
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

    /**
     * 테스트 데이터 정리
     */
    private fun cleanupTestData() {
        messageRepository.deleteAll()
        sessionRepository.deleteAll()
        counselorRepository.deleteAll()
        userRepository.deleteAll()
    }
}
