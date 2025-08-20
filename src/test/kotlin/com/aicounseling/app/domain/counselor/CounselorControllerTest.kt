package com.aicounseling.app.domain.counselor

import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.entity.CounselorRating
import com.aicounseling.app.domain.counselor.entity.FavoriteCounselor
import com.aicounseling.app.domain.counselor.repository.CounselorRatingRepository
import com.aicounseling.app.domain.counselor.repository.CounselorRepository
import com.aicounseling.app.domain.counselor.repository.FavoriteCounselorRepository
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CounselorControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var counselorRepository: CounselorRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var favoriteCounselorRepository: FavoriteCounselorRepository

    @Autowired
    private lateinit var sessionRepository: ChatSessionRepository

    @Autowired
    private lateinit var ratingRepository: CounselorRatingRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var testCounselor: Counselor
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        testUser =
            User(
                email = "test@test.com",
                nickname = "테스트유저",
                authProvider = AuthProvider.GOOGLE,
                providerId = "google123",
            )
        testUser = userRepository.save(testUser)

        // 테스트용 상담사 생성
        testCounselor =
            Counselor(
                name = "니체",
                title = "실존주의 철학자",
                description = "신은 죽었다",
                personalityMatrix = """{"insight": 95, "empathy": 60}""",
                basePrompt = "당신은 니체입니다. 실존주의적 관점에서 상담합니다.",
                specialties = """["실존주의", "허무주의"]""",
            )
        testCounselor = counselorRepository.save(testCounselor)

        // JWT 토큰 생성
        token = jwtTokenProvider.createToken(testUser.id, testUser.email)
    }

    @Test
    @DisplayName("GET /api/counselors - 상담사 목록 조회 성공")
    fun `should return counselors list`() {
        // Given: 추가 상담사 생성
        val counselor2 =
            Counselor(
                name = "칸트",
                title = "이성주의 철학자",
                description = "정언명령",
                personalityMatrix = """{"insight": 90, "empathy": 70}""",
                basePrompt = "당신은 칸트입니다. 이성과 도덕률의 관점에서 상담합니다.",
                specialties = """["윤리학", "인식론"]""",
            )
        counselorRepository.save(counselor2)

        // When & Then
        mockMvc.perform(
            get("/api/counselors")
                .param("sort", "recent"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("상담사 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].name").exists())
    }

    @Test
    @DisplayName("GET /api/counselors/{id} - 상담사 상세 조회 성공")
    fun `should return counselor detail`() {
        // When & Then
        mockMvc.perform(
            get("/api/counselors/${testCounselor.id}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("상담사 조회 성공"))
            .andExpect(jsonPath("$.data.id").value(testCounselor.id))
            .andExpect(jsonPath("$.data.name").value("니체"))
            .andExpect(jsonPath("$.data.personalityMatrix").exists())
            .andExpect(jsonPath("$.data.specialties").isArray)
            .andExpect(jsonPath("$.data.specialties[0]").value("실존주의"))
    }

    @Test
    @DisplayName("GET /api/counselors/{id} - 존재하지 않는 상담사 조회 시 404")
    fun `should return 404 when counselor not found`() {
        // When & Then
        mockMvc.perform(
            get("/api/counselors/99999"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.msg").value("상담사를 찾을 수 없습니다: 99999"))
    }

    @Test
    @DisplayName("GET /api/counselors/favorites - 즐겨찾기 목록 조회 (빈 목록)")
    fun `should return empty favorites list`() {
        // When & Then
        mockMvc.perform(
            get("/api/counselors/favorites")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("즐겨찾기 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    @DisplayName("GET /api/counselors/favorites - 즐겨찾기 목록 조회 성공")
    fun `should return favorites list`() {
        // Given: 즐겨찾기 추가
        val favorite =
            FavoriteCounselor(
                user = testUser,
                counselor = testCounselor,
            )
        favoriteCounselorRepository.save(favorite)

        // When & Then
        mockMvc.perform(
            get("/api/counselors/favorites")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("니체"))
    }

    @Test
    @DisplayName("POST /api/counselors/{id}/favorite - 즐겨찾기 추가 성공")
    fun `should add counselor to favorites`() {
        // When & Then
        mockMvc.perform(
            post("/api/counselors/${testCounselor.id}/favorite")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk) // RsData는 항상 200 반환
            .andExpect(jsonPath("$.resultCode").value("201"))
            .andExpect(jsonPath("$.msg").value("즐겨찾기 추가 성공"))
            .andExpect(jsonPath("$.data").value("상담사 니체을(를) 즐겨찾기에 추가했습니다."))

        // 실제로 저장되었는지 확인
        val exists = favoriteCounselorRepository.existsByUserAndCounselor(testUser, testCounselor)
        assert(exists) { "즐겨찾기가 저장되지 않았습니다" }
    }

    @Test
    @DisplayName("POST /api/counselors/{id}/favorite - 중복 추가 시 409 에러")
    fun `should return 409 when duplicate favorite`() {
        // Given: 이미 즐겨찾기 추가
        val favorite =
            FavoriteCounselor(
                user = testUser,
                counselor = testCounselor,
            )
        favoriteCounselorRepository.save(favorite)

        // When & Then
        mockMvc.perform(
            post("/api/counselors/${testCounselor.id}/favorite")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.msg").value("이미 즐겨찾기한 상담사입니다"))
    }

    @Test
    @DisplayName("DELETE /api/counselors/{id}/favorite - 즐겨찾기 제거 성공")
    fun `should remove counselor from favorites`() {
        // Given: 즐겨찾기 추가
        val favorite =
            FavoriteCounselor(
                user = testUser,
                counselor = testCounselor,
            )
        favoriteCounselorRepository.save(favorite)

        // When & Then
        mockMvc.perform(
            delete("/api/counselors/${testCounselor.id}/favorite")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk) // RsData는 항상 200 반환
            .andExpect(jsonPath("$.resultCode").value("204"))
            .andExpect(jsonPath("$.msg").value("즐겨찾기 제거 성공"))
            .andExpect(jsonPath("$.data").value("상담사 니체을(를) 즐겨찾기에서 제거했습니다."))

        // 실제로 삭제되었는지 확인
        val exists = favoriteCounselorRepository.existsByUserAndCounselor(testUser, testCounselor)
        assert(!exists) { "즐겨찾기가 제거되지 않았습니다" }
    }

    @Test
    @DisplayName("인증 없이 즐겨찾기 API 호출 시 401")
    fun `should return 401 without authentication`() {
        // GET /favorites
        mockMvc.perform(get("/api/counselors/favorites"))
            .andExpect(status().isUnauthorized)

        // POST /favorite
        mockMvc.perform(post("/api/counselors/${testCounselor.id}/favorite"))
            .andExpect(status().isUnauthorized)

        // DELETE /favorite
        mockMvc.perform(delete("/api/counselors/${testCounselor.id}/favorite"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("GET /api/counselors?sort=popular - 인기순 정렬")
    fun `should sort counselors by popularity`() {
        // Given: totalSessions 다른 상담사들 생성
        val counselor2 =
            Counselor(
                name = "칸트",
                title = "이성주의",
                description = "정언명령",
                personalityMatrix = """{}""",
                basePrompt = "당신은 칸트입니다.",
                specialties = """[]""",
            )
        val savedCounselor2 = counselorRepository.save(counselor2)

        // 실제 세션 데이터 생성
        // 칸트: 3개 세션 생성
        repeat(3) {
            sessionRepository.save(
                ChatSession(
                    userId = testUser.id,
                    counselorId = savedCounselor2.id,
                ),
            )
        }

        // 니체: 1개 세션 생성
        sessionRepository.save(
            ChatSession(
                userId = testUser.id,
                counselorId = testCounselor.id,
            ),
        )

        // When & Then
        mockMvc.perform(
            get("/api/counselors")
                .param("sort", "popular"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("칸트")) // 세션 많은 순
            .andExpect(jsonPath("$.data[1].name").value("니체"))
    }

    @Test
    @DisplayName("GET /api/counselors?sort=rating - 평점순 정렬")
    fun `should sort counselors by rating`() {
        // Given: averageRating 다른 상담사들 생성
        val counselor2 =
            Counselor(
                name = "칸트",
                title = "이성주의",
                description = "정언명령",
                personalityMatrix = """{}""",
                basePrompt = "당신은 칸트입니다.",
                specialties = """[]""",
            )
        val savedCounselor2 = counselorRepository.save(counselor2)

        // 세션 생성
        val session1 =
            sessionRepository.save(
                ChatSession(userId = testUser.id, counselorId = savedCounselor2.id),
            )
        val session2 =
            sessionRepository.save(
                ChatSession(userId = testUser.id, counselorId = savedCounselor2.id),
            )
        val session3 =
            sessionRepository.save(
                ChatSession(userId = testUser.id, counselorId = testCounselor.id),
            )

        // 칸트: 평점 4.8 (2개 평가: 5.0, 4.6)
        ratingRepository.save(
            CounselorRating(
                user = testUser,
                counselor = savedCounselor2,
                session = session1,
                rating = 5.0,
            ),
        )
        ratingRepository.save(
            CounselorRating(
                user = testUser,
                counselor = savedCounselor2,
                session = session2,
                rating = 4.6,
            ),
        )

        // 니체: 평점 3.5 (1개 평가)
        ratingRepository.save(
            CounselorRating(
                user = testUser,
                counselor = testCounselor,
                session = session3,
                rating = 3.5,
            ),
        )

        // When & Then
        mockMvc.perform(
            get("/api/counselors")
                .param("sort", "rating"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("칸트")) // 평점 높은 순
            .andExpect(jsonPath("$.data[1].name").value("니체"))
    }
}
