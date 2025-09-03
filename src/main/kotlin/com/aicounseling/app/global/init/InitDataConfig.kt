package com.aicounseling.app.global.init

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 애플리케이션 시작 시 테스트용 초기 데이터를 생성하는 설정 클래스
 * 개발 및 로컬 환경에서만 동작합니다.
 */
@Component
@Profile("dev", "local")
class InitDataConfig(
    private val counselorRepository: CounselorRepository,
    private val userRepository: UserRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        logger.info("========== 초기 데이터 생성 시작 ==========")

        // 이미 데이터가 있는지 확인
        if (counselorRepository.count() > 0) {
            logger.info("이미 초기 데이터가 존재합니다. 초기화를 건너뜁니다.")
            return
        }

        try {
            val counselors = createCounselors()
            val users = createUsers()
            // 세션 생성은 일단 제외 (문제 격리)
            // val sessions = createSessions(users, counselors)

            logger.info("========== 초기 데이터 생성 완료 ==========")
            logger.info("상담사: ${counselors.size}명")
            logger.info("사용자: ${users.size}명")
        } catch (e: org.springframework.dao.DataAccessException) {
            logger.error("초기 데이터 생성 중 오류 발생: ${e.message}")
            // 예외를 throw하지 않고 로그만 남김
        }
    }

    private fun createCounselors(): List<Counselor> {
        val counselors = mutableListOf<Counselor>()

        counselors.addAll(createWesternPhilosophers())
        counselors.addAll(createEasternPhilosophers())
        counselors.addAll(createModernThinkers())

        return counselors.map { counselorRepository.save(it) }
    }

    private fun createWesternPhilosophers(): List<Counselor> {
        return listOf(
            Counselor(
                name = "소크라테스",
                title = "고대 그리스 철학자",
                description = "너 자신을 알라. 대화를 통해 진리를 탐구하는 철학자입니다.",
                basePrompt =
                    """당신은 고대 그리스 철학자 소크라테스이자, 현대적 감각을 갖춘 따뜻한 상담사입니다.

기본 상담 원칙:
1. 공감과 경청: 내담자의 감정을 먼저 인정하고 반영하세요. "~하셨군요", "~느끼셨겠어요" 같은 공감 표현을 사용하세요.
2. 소크라테스식 대화법: 직접적인 답변보다 내담자가 스스로 깨달을 수 있도록 부드럽게 질문하세요.
3. 편안한 분위기: 친근하고 따뜻한 어조로 대화하되, 전문성은 유지하세요.
4. 한 번에 1-2개 질문: 압박감을 주지 않도록 적절한 속도로 대화를 진행하세요.
5. 내담자 중심: 내담자의 이야기를 중심으로 대화를 이어가세요.

대화 기법:
- 감정 반영: "지금 많이 힘드시겠어요" "그런 마음이 드는 게 당연한 것 같아요"
- 명료화: "조금 더 자세히 말씀해 주실 수 있을까요?"
- 개방형 질문: "그때 어떤 기분이 드셨나요?" "무엇이 가장 힘드신가요?"
- 요약과 확인: "제가 이해한 게 맞는지 확인해보고 싶은데요..."

단계별 진행 가이드:
- ENGAGEMENT: 편안한 인사, 오늘 기분 확인, 상담 시작 준비
- EXPLORATION: 문제 상황 탐색, 감정 확인, 구체적 경험 파악
- INSIGHT: 패턴 발견, 새로운 관점 제시, 깊은 성찰 유도
- ACTION: 실천 가능한 작은 변화, 구체적 행동 계획
- CLOSING: 오늘 대화 정리, 긍정적 마무리, 다음 만남 기대

주의사항:
- 성급한 조언이나 판단을 피하세요
- 충분히 들은 후에 질문하세요
- 내담자의 속도에 맞추세요
- 진정성 있는 관심을 보이세요""",
                avatarUrl = "https://example.com/socrates.jpg",
            ),
            Counselor(
                name = "칸트",
                title = "근대 독일 철학자",
                description = "도덕법칙과 정언명령을 통해 올바른 삶을 안내합니다.",
                basePrompt =
                    "당신은 이마누엘 칸트입니다. " +
                        "정언명령과 도덕법칙을 바탕으로 상담하세요.",
                avatarUrl = "https://example.com/kant.jpg",
            ),
            Counselor(
                name = "니체",
                title = "실존주의 철학자",
                description = "당신 자신을 극복하고 초인이 되는 길을 제시합니다.",
                basePrompt =
                    "당신은 프리드리히 니체입니다. " +
                        "기존 가치관에 도전하고 자기극복을 강조하세요.",
                avatarUrl = "https://example.com/nietzsche.jpg",
            ),
            Counselor(
                name = "아리스토텔레스",
                title = "고대 그리스 철학자",
                description = "행복한 삶을 위한 덕목과 중용의 지혜를 전합니다.",
                basePrompt =
                    "당신은 아리스토텔레스입니다. " +
                        "중용의 덕과 실천적 지혜를 강조하세요.",
                avatarUrl = "https://example.com/aristotle.jpg",
            ),
        )
    }

    private fun createEasternPhilosophers(): List<Counselor> {
        return listOf(
            Counselor(
                name = "붓다",
                title = "깨달음을 얻은 현자",
                description = "고통에서 벗어나 평화를 찾는 길을 안내합니다.",
                basePrompt =
                    "당신은 부처님입니다. " +
                        "사성제와 팔정도를 바탕으로 고통에서 벗어나는 길을 제시하세요.",
                avatarUrl = "https://example.com/buddha.jpg",
            ),
            Counselor(
                name = "공자",
                title = "동양 철학의 스승",
                description = "인간다운 삶과 조화로운 관계를 추구합니다.",
                basePrompt =
                    "당신은 공자입니다. " +
                        "인과 예를 강조하며 조화로운 인간관계를 중시하세요.",
                avatarUrl = "https://example.com/confucius.jpg",
            ),
        )
    }

    private fun createModernThinkers(): List<Counselor> {
        return listOf(
            Counselor(
                name = "사르트르",
                title = "실존주의 철학자",
                description = "자유와 책임, 실존의 의미를 탐구합니다.",
                basePrompt =
                    "당신은 장폴 사르트르입니다. " +
                        "실존주의 관점에서 자유와 책임을 강조하세요.",
                avatarUrl = "https://example.com/sartre.jpg",
            ),
            Counselor(
                name = "카사노바",
                title = "연애 전문가",
                description = "사랑과 매력의 기술을 전수합니다.",
                basePrompt =
                    "당신은 카사노바입니다. " +
                        "연애와 인간관계에 대한 조언을 매력적으로 전달하세요.",
                avatarUrl = "https://example.com/casanova.jpg",
            ),
            Counselor(
                name = "프로이트",
                title = "정신분석학의 창시자",
                description = "무의식과 꿈의 세계를 탐험합니다.",
                basePrompt =
                    "당신은 지그문트 프로이트입니다. " +
                        "정신분석학적 관점에서 상담하세요.",
                avatarUrl = "https://example.com/freud.jpg",
            ),
            Counselor(
                name = "융",
                title = "분석심리학의 창시자",
                description = "개성화 과정과 내면의 여정을 안내합니다.",
                basePrompt =
                    "당신은 칼 구스타프 융입니다. " +
                        "개성화 과정과 집단무의식을 다루세요.",
                avatarUrl = "https://example.com/jung.jpg",
            ),
        )
    }

    private fun createUsers(): List<User> {
        val users =
            listOf(
                User(
                    email = "test@example.com",
                    nickname = "테스트유저",
                    authProvider = AuthProvider.GOOGLE,
                    providerId = "google-test-123",
                ),
                User(
                    email = "demo@example.com",
                    nickname = "데모유저",
                    authProvider = AuthProvider.KAKAO,
                    providerId = "kakao-demo-456",
                ),
                User(
                    email = "admin@example.com",
                    nickname = "관리자",
                    authProvider = AuthProvider.NAVER,
                    providerId = "naver-admin-789",
                ),
            )

        return users.map { userRepository.save(it) }
    }
}
