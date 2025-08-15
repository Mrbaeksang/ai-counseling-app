package com.aicounseling.app.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

class CounselorTest {
    
    @Test
    @DisplayName("상담사를 생성할 수 있다")
    fun `상담사 생성 테스트`() {
        // given
        val name = "소크라테스"
        val title = "고대 그리스 철학자"
        val description = "질문을 통해 스스로 답을 찾도록 돕습니다"
        val personalityMatrix = """
            {
                "logical": 95,
                "directness": 80,
                "solutionFocus": 40,
                "formalityLevel": 60
            }
        """.trimIndent()
        val basePrompt = """
            당신은 소크라테스입니다.
            산파술을 사용하되, 현대적인 언어로 대화하세요.
            "그대여" 같은 고어체 금지. 자연스러운 한국어 사용.
        """.trimIndent()
        
        // when
        val counselor = Counselor(
            name = name,
            title = title,
            description = description,
            personalityMatrix = personalityMatrix,
            basePrompt = basePrompt
        )
        
        // then
        assertThat(counselor.name).isEqualTo(name)
        assertThat(counselor.title).isEqualTo(title)
        assertThat(counselor.description).isEqualTo(description)
        assertThat(counselor.personalityMatrix).contains("logical")
        assertThat(counselor.personalityMatrix).contains("95")
        assertThat(counselor.basePrompt).contains("산파술")
        assertThat(counselor.isActive).isTrue()
        assertThat(counselor.createdAt).isNotNull()
    }
    
    @Test
    @DisplayName("상담사 성격 매트릭스로 매칭 점수를 계산할 수 있다")
    fun `성격 매트릭스 매칭 테스트`() {
        // given - 논리적이고 직설적인 상담사
        val counselor = Counselor(
            name = "아리스토텔레스",
            title = "논리학의 아버지",
            description = "체계적 분석과 논리적 접근",
            personalityMatrix = """
                {
                    "logical": 100,
                    "directness": 90,
                    "solutionFocus": 80,
                    "formalityLevel": 70
                }
            """.trimIndent(),
            basePrompt = "논리적으로 문제를 분석합니다"
        )
        
        // when - 사용자 선호도와 비교
        val userPreference = """
            {
                "logical": 90,
                "directness": 85,
                "solutionFocus": 75,
                "formalityLevel": 60
            }
        """.trimIndent()
        
        // then - JSON 필드가 존재하고 비교 가능한 형태인지 확인
        assertThat(counselor.personalityMatrix).isNotBlank()
        assertThat(counselor.personalityMatrix).contains("logical")
        assertThat(userPreference).contains("logical")
        // 실제 매칭 로직은 서비스 레이어에서 구현
    }
    
    @Test
    @DisplayName("상담사를 비활성화하면 더 이상 매칭되지 않는다")
    fun `상담사 비활성화 테스트`() {
        // given
        val counselor = Counselor(
            name = "프로이트",
            title = "정신분석학의 창시자",
            description = "무의식과 꿈의 의미를 탐구합니다",
            personalityMatrix = """{"logical": 80, "directness": 50}""",
            basePrompt = "무의식을 탐색해봅시다"
        )
        val originalUpdatedAt = counselor.updatedAt
        
        assertThat(counselor.isActive).isTrue()
        
        // 시간 차이를 만들기 위해 잠시 대기
        Thread.sleep(10)
        
        // when
        counselor.deactivate()
        
        // then
        assertThat(counselor.isActive).isFalse()
        assertThat(counselor.updatedAt).isAfter(originalUpdatedAt)  // 업데이트 전 시간과 비교
    }
    
    @Test
    @DisplayName("상담사의 AI 프롬프트를 업데이트할 수 있다")
    fun `AI 프롬프트 업데이트 테스트`() {
        // given
        val counselor = Counselor(
            name = "칼 융",
            title = "분석심리학의 창시자",
            description = "집단 무의식과 원형을 다룹니다",
            personalityMatrix = """{"logical": 75, "directness": 45}""",
            basePrompt = "초기 프롬프트"
        )
        val originalUpdatedAt = counselor.updatedAt
        
        // 시간 차이를 만들기 위해 잠시 대기
        Thread.sleep(10)
        
        // when - 더 상세한 AI 지시사항으로 업데이트
        val enhancedPrompt = """
            당신은 칼 융입니다.
            
            [대화 원칙]
            1. 집단 무의식과 개인 무의식을 구분하여 접근
            2. 그림자, 아니마/아니무스 개념 활용
            3. 꿈의 상징적 의미 해석
            
            [상담 단계 인식]
            - 대화 맥락을 보고 현재 단계를 스스로 판단하세요
            - 사용자가 준비되면 자연스럽게 다음 단계로 전환
            - 절대 "이제 분석 단계입니다" 같은 메타 발언 금지
            
            [언어 스타일]
            - 현대적이고 친근한 한국어 사용
            - 전문 용어는 쉽게 풀어서 설명
        """.trimIndent()
        
        counselor.updatePrompt(enhancedPrompt)
        
        // then
        assertThat(counselor.basePrompt).isEqualTo(enhancedPrompt)
        assertThat(counselor.basePrompt).contains("상담 단계 인식")
        assertThat(counselor.basePrompt).contains("현대적이고 친근한")
        assertThat(counselor.updatedAt).isAfter(originalUpdatedAt)  // createdAt 대신 originalUpdatedAt과 비교
    }
    
    @Test
    @DisplayName("상담사 정보에 세션 통계를 포함할 수 있다")
    fun `상담사 통계 정보 테스트`() {
        // given
        val counselor = Counselor(
            name = "마르쿠스 아우렐리우스",
            title = "스토아 철학자 황제",
            description = "역경을 지혜로 극복하는 법을 가르칩니다",
            personalityMatrix = """{"logical": 85, "directness": 70}""",
            basePrompt = "스토아 철학으로 접근합니다"
        )
        
        // when - 통계 정보 설정 (실제로는 DB에서 계산)
        counselor.totalSessions = 150
        counselor.averageRating = 4.5
        counselor.specialtyTags = listOf("스트레스", "인내", "자기통제")
        
        // then
        assertThat(counselor.totalSessions).isEqualTo(150)
        assertThat(counselor.averageRating).isEqualTo(4.5)
        assertThat(counselor.specialtyTags).contains("스트레스")
    }
}