package com.aicounseling.app.config

import com.aicounseling.app.global.openrouter.OpenRouterService
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * 테스트용 OpenRouterService 설정
 * CI 환경에서는 Mock 서비스를 사용하고, 로컬에서는 실제 서비스 사용
 */
@TestConfiguration
@Profile("test")
class TestOpenRouterConfig {
    @Autowired(required = false)
    private var realOpenRouterService: OpenRouterService? = null

    @Bean
    @Primary
    fun testOpenRouterService(): OpenRouterService {
        val isCI = System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null
        val apiKey = System.getenv("OPENROUTER_API_KEY")
        val hasValidApiKey = apiKey != null && apiKey.startsWith("sk-or-")

        return if (isCI && !hasValidApiKey) {
            // CI 환경이고 유효한 API 키가 없으면 Mock 사용
            mockk<OpenRouterService>().apply {
                coEvery { sendMessage(any(), any()) } returns "테스트 AI 응답입니다. 철학적 상담을 제공합니다."
                coEvery { sendCounselingMessage(any(), any(), any()) } returns
                    """
                    {
                        "content": "당신의 마음을 이해합니다. 함께 이야기를 나누어보시겠어요?",
                        "currentPhase": "UNDERSTANDING",
                        "sessionTitle": "철학 상담 세션"
                    }
                    """.trimIndent()
            }
        } else {
            // 로컬 환경이거나 유효한 API 키가 있으면 실제 서비스 사용
            realOpenRouterService ?: error("Real OpenRouterService not available")
        }
    }
}
