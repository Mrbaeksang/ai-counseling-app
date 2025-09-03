package com.aicounseling.app.global.openrouter
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class OpenRouterService(
    private val openRouterWebClient: WebClient,
    private val properties: OpenRouterProperties,
) {
    companion object {
        private const val DEFAULT_MAX_TOKENS = 2000
        private const val DEFAULT_TEMPERATURE = 0.7
    }

    suspend fun sendMessage(
        message: String,
        systemPrompt: String? = null,
        model: String? = null,
    ): String {
        val request =
            ChatRequest(
                model = model ?: properties.model,
                messages =
                    buildList {
                        systemPrompt?.let {
                            add(Message(role = "system", content = it))
                        }
                        add(Message(role = "user", content = message))
                    },
            )

        return openRouterWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .map { it.choices.firstOrNull()?.message?.content ?: "응답을 받을 수 없습니다." }
            .awaitSingle()
    }

    suspend fun sendCounselingMessage(
        userMessage: String,
        counselorPrompt: String,
        conversationHistory: List<Message> = emptyList(),
        includeTitle: Boolean = false,
    ): String {
        val responseFormat =
            if (includeTitle) {
                """
            반드시 아래 형식으로만 응답하세요 (JSON 금지!):
            
            [응답 내용]
            (여기에 사용자에게 전달할 상담 내용을 작성하세요. 공감적이고 따뜻하게)
            
            [현재 단계]  
            (ENGAGEMENT, EXPLORATION, INSIGHT, ACTION, CLOSING 중 하나만)
            
            [세션 제목]
            (대화를 요약한 15자 이내 제목)
            """
            } else {
                """
            반드시 아래 형식으로만 응답하세요 (JSON 금지!):
            
            [응답 내용]
            (여기에 사용자에게 전달할 상담 내용을 작성하세요. 공감적이고 따뜻하게)
            
            [현재 단계]
            (ENGAGEMENT, EXPLORATION, INSIGHT, ACTION, CLOSING 중 하나만)
            """
            }

        val enhancedPrompt =
            """
            $counselorPrompt

            === 중요한 응답 규칙 ===
            1. 절대 JSON 형식으로 응답하지 마세요
            2. 아래 형식을 정확히 따라주세요
            3. [응답 내용], [현재 단계] 라벨을 반드시 포함하세요
            
            $responseFormat
            
            예시:
            [응답 내용]
            안녕하세요. 오늘 어떤 마음으로 찾아오셨나요?
            
            [현재 단계]
            ENGAGEMENT
            """.trimIndent()

        val messages =
            buildList {
                add(Message(role = "system", content = enhancedPrompt))
                addAll(conversationHistory)
                add(Message(role = "user", content = userMessage))
            }

        val request =
            ChatRequest(
                model = properties.model,
                messages = messages,
                temperature = DEFAULT_TEMPERATURE,
                max_tokens = DEFAULT_MAX_TOKENS,
            )

        return openRouterWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .doOnError { error ->
                println("OpenRouter API Error: ${error.message}")
            }
            .map { it.choices.firstOrNull()?.message?.content ?: "응답을 받을 수 없습니다." }
            .awaitSingle()
    }
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    @Suppress("ConstructorParameterNaming") val max_tokens: Int? = null,
    val stream: Boolean = false,
)

data class Message(
    val role: String,
    val content: String,
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null,
)

data class Choice(
    val index: Int,
    val message: Message,
    @Suppress("ConstructorParameterNaming") val finish_reason: String? = null,
)

data class Usage(
    @Suppress("ConstructorParameterNaming") val prompt_tokens: Int,
    @Suppress("ConstructorParameterNaming") val completion_tokens: Int,
    @Suppress("ConstructorParameterNaming") val total_tokens: Int,
)
