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
            // .env 파일에서 로드한 값을 Spring 프로퍼티로 등록
            registry.add("openrouter.api-key") {
                dotenv["OPENROUTER_API_KEY"] ?: error("OPENROUTER_API_KEY not found in .env file")
            }
            registry.add("jwt.secret") {
                dotenv["JWT_SECRET"]
                    ?: "test-jwt-secret-key-for-jwt-auth-256-bits-long-2024"
            }
        }
    }

    @Autowired
    private lateinit var openRouterService: OpenRouterService

    @Test
    fun `OpenRouter API 연결 테스트`() {
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
