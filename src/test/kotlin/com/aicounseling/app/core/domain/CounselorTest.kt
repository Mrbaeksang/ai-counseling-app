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
        assertThat(counselor.basePrompt).contains("산파술")
        assertThat(counselor.isActive).isTrue()
        assertThat(counselor.createdAt).isNotNull()
    }
    
    @Test
    @DisplayName("상담사 성격 매트릭스를 저장할 수 있다")
    fun `성격 매트릭스 저장 테스트`() {
        // given & when
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
        
        // then - JSON 문자열이 정상 저장되는지 확인
        assertThat(counselor.personalityMatrix).isNotBlank()
        assertThat(counselor.personalityMatrix).contains("logical")
        assertThat(counselor.personalityMatrix).contains("100")
    }
    
    @Test
    @DisplayName("상담사 활성화 상태를 확인할 수 있다")
    fun `상담사 활성화 상태 확인 테스트`() {
        // given
        val activeCounselor = Counselor(
            name = "프로이트",
            title = "정신분석학의 창시자",
            description = "무의식과 꿈의 의미를 탐구합니다",
            personalityMatrix = """{"logical": 80}""",
            basePrompt = "무의식을 탐색해봅시다"
        )
        
        val inactiveCounselor = Counselor(
            name = "칼 융",
            title = "분석심리학의 창시자",
            description = "집단 무의식과 원형을 다룹니다",
            personalityMatrix = """{"logical": 75}""",
            basePrompt = "원형을 탐구합니다",
            isActive = false  // 비활성화 상태로 생성
        )
        
        // then
        assertThat(activeCounselor.isActive).isTrue()
        assertThat(inactiveCounselor.isActive).isFalse()
    }
    
    @Test
    @DisplayName("상담사 통계 정보를 임시 저장할 수 있다")
    fun `Transient 필드 테스트`() {
        // given
        val counselor = Counselor(
            name = "마르쿠스 아우렐리우스",
            title = "스토아 철학자 황제",
            description = "역경을 지혜로 극복하는 법을 가르칩니다",
            personalityMatrix = """{"logical": 85}""",
            basePrompt = "스토아 철학으로 접근합니다"
        )
        
        // when - 런타임에 통계 정보 설정
        counselor.totalSessions = 150
        counselor.averageRating = 4.5
        counselor.specialtyTags = listOf("스트레스", "인내", "자기통제")
        
        // then - Transient 필드는 DB에 저장 안되지만 런타임엔 사용 가능
        assertThat(counselor.totalSessions).isEqualTo(150)
        assertThat(counselor.averageRating).isEqualTo(4.5)
        assertThat(counselor.specialtyTags).contains("스트레스")
    }
}