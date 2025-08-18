package com.aicounseling.app.domain.counselor

import com.aicounseling.app.domain.session.ChatSessionRepository
import com.aicounseling.app.domain.user.User
import com.aicounseling.app.global.security.AuthProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("CounselorService 테스트")
class CounselorServiceTest {
    // Mock 객체들
    private lateinit var counselorRepository: CounselorRepository
    private lateinit var favoriteCounselorRepository: FavoriteCounselorRepository
    private lateinit var sessionRepository: ChatSessionRepository
    private lateinit var ratingRepository: CounselorRatingRepository
    private lateinit var objectMapper: ObjectMapper

    // 테스트 대상
    private lateinit var counselorService: CounselorService

    // 테스트용 데이터
    private lateinit var testCounselor: Counselor
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // Mock 생성
        counselorRepository = mockk()
        favoriteCounselorRepository = mockk()
        sessionRepository = mockk()
        ratingRepository = mockk()
        objectMapper = jacksonObjectMapper()

        // Service 생성
        counselorService =
            CounselorService(
                counselorRepository,
                favoriteCounselorRepository,
                sessionRepository,
                ratingRepository,
                objectMapper,
            )

        // 테스트 데이터 준비
        testCounselor =
            Counselor(
                id = 1L,
                name = "소크라테스",
                title = "고대 그리스 철학자",
                description = "질문을 통해 스스로 답을 찾도록 돕습니다",
                personalityMatrix = """{"logical": 95, "empathy": 60}""",
                basePrompt = "당신은 소크라테스입니다...",
                specialties = """["자아탐구", "진리추구", "논리적사고"]""",
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        testUser =
            User(
                id = 1L,
                email = "test@test.com",
                nickname = "테스터",
                authProvider = AuthProvider.GOOGLE,
                providerId = "google123",
            )
    }

    @Test
    @DisplayName("findById - 상담사 조회 성공")
    fun findById_success() {
        // given
        every { counselorRepository.findById(1L) } returns Optional.of(testCounselor)
        every { sessionRepository.countByCounselorId(1L) } returns 10
        every { ratingRepository.findByCounselorId(1L) } returns
            listOf(
                CounselorRating(
                    id = 1L,
                    user = testUser,
                    counselor = testCounselor,
                    session = mockk(),
                    rating = 4.5,
                    review = "좋았어요",
                ),
            )

        // when
        val result = counselorService.findById(1L)

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.name).isEqualTo("소크라테스")
        assertThat(result.totalSessions).isEqualTo(10)
        assertThat(result.averageRating).isEqualTo(4.5)
        assertThat(result.specialtyTags).containsExactly("자아탐구", "진리추구", "논리적사고")
    }

    @Test
    @DisplayName("findById - 없는 상담사 조회시 예외")
    fun findById_notFound() {
        // given
        every { counselorRepository.findById(999L) } returns Optional.empty()

        // when & then
        assertThatThrownBy { counselorService.findById(999L) }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("상담사를 찾을 수 없습니다")
    }

    @Test
    @DisplayName("findAllWithSort - 인기순 정렬")
    fun findAllWithSort_popular() {
        // given
        val counselor1 = testCounselor.copy(id = 1L)
        val counselor2 = testCounselor.copy(id = 2L, name = "니체")

        every { counselorRepository.findByIsActiveTrue() } returns listOf(counselor1, counselor2)
        every { sessionRepository.countByCounselorId(1L) } returns 5
        every { sessionRepository.countByCounselorId(2L) } returns 10 // 더 많은 세션
        every { ratingRepository.findByCounselorId(any()) } returns emptyList()

        // when
        val result = counselorService.findAllWithSort("popular")

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(2L) // 세션 많은 순
        assertThat(result[1].id).isEqualTo(1L)
    }

    @Test
    @DisplayName("findAllWithSort - 평점순 정렬")
    fun findAllWithSort_rating() {
        // given
        val counselor1 = testCounselor.copy(id = 1L)
        val counselor2 = testCounselor.copy(id = 2L, name = "니체")

        every { counselorRepository.findByIsActiveTrue() } returns listOf(counselor1, counselor2)
        every { sessionRepository.countByCounselorId(any()) } returns 5
        every { ratingRepository.findByCounselorId(1L) } returns
            listOf(
                mockk { every { rating } returns 3.0 },
            )
        every { ratingRepository.findByCounselorId(2L) } returns
            listOf(
                mockk { every { rating } returns 5.0 },
            )

        // when
        val result = counselorService.findAllWithSort("rating")

        // then
        assertThat(result[0].id).isEqualTo(2L) // 평점 높은 순
        assertThat(result[1].id).isEqualTo(1L)
    }

    @Test
    @DisplayName("findAllWithSort - 빈 리스트 반환")
    fun findAllWithSort_empty() {
        // given
        every { counselorRepository.findByIsActiveTrue() } returns emptyList()

        // when
        val result = counselorService.findAllWithSort(null)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getFavoriteCounselors - 즐겨찾기 목록 조회")
    fun getFavoriteCounselors_success() {
        // given
        val favorite =
            FavoriteCounselor(
                id = 1L,
                user = testUser,
                counselor = testCounselor,
            )

        every { favoriteCounselorRepository.findByUser(testUser) } returns listOf(favorite)
        every { sessionRepository.countByCounselorId(1L) } returns 5
        every { ratingRepository.findByCounselorId(1L) } returns emptyList()

        // when
        val result = counselorService.getFavoriteCounselors(testUser)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("소크라테스")
    }

    @Test
    @DisplayName("addFavorite - 즐겨찾기 추가 성공")
    fun addFavorite_success() {
        // given
        every { counselorRepository.findById(1L) } returns Optional.of(testCounselor)
        every { favoriteCounselorRepository.existsByUserAndCounselor(testUser, testCounselor) } returns false
        every { favoriteCounselorRepository.save(any()) } returns mockk()

        // when
        counselorService.addFavorite(testUser, 1L)

        // then
        verify { favoriteCounselorRepository.save(any()) }
    }

    @Test
    @DisplayName("addFavorite - 중복 즐겨찾기시 예외")
    fun addFavorite_duplicate() {
        // given
        every { counselorRepository.findById(1L) } returns Optional.of(testCounselor)
        every { favoriteCounselorRepository.existsByUserAndCounselor(testUser, testCounselor) } returns true

        // when & then
        assertThatThrownBy { counselorService.addFavorite(testUser, 1L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("이미 즐겨찾기한 상담사")
    }

    @Test
    @DisplayName("removeFavorite - 즐겨찾기 삭제 성공")
    fun removeFavorite_success() {
        // given
        every { counselorRepository.findById(1L) } returns Optional.of(testCounselor)
        every { favoriteCounselorRepository.deleteByUserAndCounselor(testUser, testCounselor) } returns Unit

        // when
        counselorService.removeFavorite(testUser, 1L)

        // then
        verify { favoriteCounselorRepository.deleteByUserAndCounselor(testUser, testCounselor) }
    }
}
