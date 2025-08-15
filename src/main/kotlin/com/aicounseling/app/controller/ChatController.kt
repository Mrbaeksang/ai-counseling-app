package com.aicounseling.app.controller

import com.aicounseling.app.service.OpenRouterService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val openRouterService: OpenRouterService
) {
    
    @PostMapping("/test")
    suspend fun testChat(@RequestBody request: TestChatRequest): ChatResponse {
        val counselorPrompt = getCounselorPrompt(request.counselorType)
        val aiResponse = openRouterService.sendCounselingMessage(
            userMessage = request.message,
            counselorPrompt = counselorPrompt
        )
        
        return ChatResponse(
            message = aiResponse,
            counselor = request.counselorType
        )
    }
    
    private fun getCounselorPrompt(counselorType: String): String {
        return when(counselorType) {
            "socrates" -> """
                당신은 고대 그리스의 철학자 소크라테스입니다.
                질문을 통해 상대방 스스로 답을 찾도록 유도하는 산파술을 사용하세요.
                직접적인 답변보다는 '그것은 무엇을 의미하는가?', '왜 그렇게 생각하는가?'와 같은 
                질문을 통해 대화를 이끌어가세요.
            """.trimIndent()
            
            "freud" -> """
                당신은 정신분석학의 창시자 지그문트 프로이트입니다.
                무의식, 꿈, 유년기 경험 등을 통해 내담자의 심리를 분석합니다.
                '그 꿈/행동이 무엇을 상징한다고 생각하는가?' 같은 분석적 접근을 사용하세요.
            """.trimIndent()
            
            "casanova" -> """
                당신은 18세기 베네치아의 유명한 연애 달인 카사노바입니다.
                풍부한 연애 경험과 매력적인 화술로 연애 고민을 상담합니다.
                유머러스하면서도 실용적인 조언을 제공하되, 상대방을 존중하는 현대적 가치관도 함께 반영하세요.
            """.trimIndent()
            
            else -> "당신은 친절하고 공감적인 AI 상담사입니다."
        }
    }
}

data class TestChatRequest(
    val message: String,
    val counselorType: String = "default"
)

data class ChatResponse(
    val message: String,
    val counselor: String
)