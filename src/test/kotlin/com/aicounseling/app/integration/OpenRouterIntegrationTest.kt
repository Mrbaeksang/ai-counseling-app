package com.aicounseling.app.integration

import com.aicounseling.app.global.openrouter.OpenRouterService
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles("test")
class OpenRouterIntegrationTest {
    companion object {
        private val dotenv =
            dotenv {
                ignoreIfMissing = true
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // CI 환경에서는 환경변수, 로컬에서는 .env 파일 사용
            val apiKey =
                System.getenv("OPENROUTER_API_KEY")
                    ?: dotenv["OPENROUTER_API_KEY"]
                    ?: "test-api-key" // CI에서는 실제 API를 호출하지 않도록 더미 키 사용

            registry.add("openrouter.api-key") { apiKey }
            registry.add("jwt.secret") {
                System.getenv("JWT_SECRET")
                    ?: dotenv["JWT_SECRET"]
                    ?: "test-jwt-secret-key-for-jwt-auth-256-bits-long-2024"
            }
        }
    }

    @Autowired
    private lateinit var openRouterService: OpenRouterService

    @Test
    fun `OpenRouter API 연결 테스트`() {
        // CI 환경에서는 실제 API 키가 없으면 테스트 스킵
        val isCI = System.getenv("CI") == "true"
        val hasRealApiKey =
            System.getenv("OPENROUTER_API_KEY") != null ||
                dotenv["OPENROUTER_API_KEY"] != null

        if (isCI && !hasRealApiKey) {
            println("CI 환경에서 API 키가 없어 테스트 스킵")
            return
        }

        runBlocking {
            val response =
                openRouterService.sendMessage(
                    message = "안녕하세요. 간단히 응답해주세요.",
                    systemPrompt = "당신은 친절한 상담사입니다. 한국어로 짧게 응답하세요.",
                )

            println("API Response: $response")
            assert(response.isNotBlank())
        }
    }

    @Test
    @Disabled("실제 API 호출 테스트 - 수동으로만 실행")
    fun `상담 메시지 JSON 형식 응답 테스트`() {
        runBlocking {
            val response =
                openRouterService.sendCounselingMessage(
                    userMessage = "요즘 너무 우울해요",
                    counselorPrompt = "당신은 공감적인 상담사입니다.",
                    includeTitle = true,
                )

            println("Counseling Response: $response")
            assert(response.contains("content"))
            assert(response.contains("aiPhaseAssessment"))
            assert(response.contains("sessionTitle"))
        }
    }
}
