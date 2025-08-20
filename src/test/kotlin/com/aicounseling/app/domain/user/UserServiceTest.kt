package com.aicounseling.app.domain.user

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.domain.user.service.UserService
import com.aicounseling.app.global.security.AuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("UserService 테스트")
class UserServiceTest {
    // Mock 객체
    private lateinit var userRepository: UserRepository

    // 테스트 대상
    private lateinit var userService: UserService

    // 테스트 데이터
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userService = UserService(userRepository)

        testUser =
            User(
                id = 1L,
                email = "test@test.com",
                nickname = "테스트유저",
                authProvider = AuthProvider.GOOGLE,
                providerId = "google123",
            )
    }

    @Test
    @DisplayName("getUser - 사용자 조회 성공")
    fun getUser_success() {
        // given
        every { userRepository.findById(1L) } returns Optional.of(testUser)

        // when
        val result = userService.getUser(1L)

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("test@test.com")
        assertThat(result.nickname).isEqualTo("테스트유저")
        verify(exactly = 1) { userRepository.findById(1L) }
    }

    @Test
    @DisplayName("getUser - 없는 사용자 조회시 예외")
    fun getUser_notFound() {
        // given
        every { userRepository.findById(999L) } returns Optional.empty()

        // when & then
        assertThatThrownBy { userService.getUser(999L) }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("사용자를 찾을 수 없습니다")
    }

    @Test
    @DisplayName("changeNickname - 닉네임 변경 성공")
    fun changeNickname_success() {
        // given
        val newNickname = "새닉네임"
        val updatedUser = testUser.copy(nickname = newNickname)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns updatedUser

        // when
        val result = userService.changeNickname(1L, newNickname)

        // then
        assertThat(result.nickname).isEqualTo(newNickname)
        verify { userRepository.save(any()) }
    }

    @Test
    @DisplayName("changeNickname - 닉네임 길이 제한 (너무 짧음)")
    fun changeNickname_tooShort() {
        // given
        val shortNickname = "a"

        // when & then
        assertThatThrownBy { userService.changeNickname(1L, shortNickname) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("2자 이상 20자 이하")
    }

    @Test
    @DisplayName("changeNickname - 닉네임 길이 제한 (너무 김)")
    fun changeNickname_tooLong() {
        // given
        val longNickname = "a".repeat(21)

        // when & then
        assertThatThrownBy { userService.changeNickname(1L, longNickname) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("2자 이상 20자 이하")
    }

    @Test
    @DisplayName("changeNickname - 없는 사용자 닉네임 변경시 예외")
    fun changeNickname_userNotFound() {
        // given
        every { userRepository.findById(999L) } returns Optional.empty()

        // when & then
        assertThatThrownBy { userService.changeNickname(999L, "새닉네임") }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("사용자를 찾을 수 없습니다")
    }

    @Test
    @DisplayName("findByEmail - 이메일로 사용자 조회 성공")
    fun findByEmail_success() {
        // given
        every { userRepository.findByEmail("test@test.com") } returns testUser

        // when
        val result = userService.findByEmail("test@test.com")

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo("test@test.com")
    }

    @Test
    @DisplayName("findByEmail - 없는 이메일 조회시 null 반환")
    fun findByEmail_notFound() {
        // given
        every { userRepository.findByEmail("notfound@test.com") } returns null

        // when
        val result = userService.findByEmail("notfound@test.com")

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("changeNickname - 공백만 있는 닉네임 거부")
    fun changeNickname_onlySpaces() {
        // given
        val spacesNickname = "   "

        // when & then
        assertThatThrownBy { userService.changeNickname(1L, spacesNickname) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("2자 이상 20자 이하")
    }

    @Test
    @DisplayName("changeNickname - 특수문자 포함 닉네임 허용")
    fun changeNickname_withSpecialCharacters() {
        // given
        val specialNickname = "테스트@123!"
        val updatedUser = testUser.copy(nickname = specialNickname)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns updatedUser

        // when
        val result = userService.changeNickname(1L, specialNickname)

        // then
        assertThat(result.nickname).isEqualTo(specialNickname)
    }

    @Test
    @DisplayName("changeNickname - 이모지 포함 닉네임 허용")
    fun changeNickname_withEmoji() {
        // given
        val emojiNickname = "테스트😀👍"
        val updatedUser = testUser.copy(nickname = emojiNickname)

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns updatedUser

        // when
        val result = userService.changeNickname(1L, emojiNickname)

        // then
        assertThat(result.nickname).isEqualTo(emojiNickname)
    }

    @Test
    @DisplayName("changeNickname - 같은 닉네임으로 변경 허용")
    fun changeNickname_sameNickname() {
        // given
        val sameNickname = testUser.nickname

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { userRepository.save(any()) } returns testUser

        // when
        val result = userService.changeNickname(1L, sameNickname)

        // then
        assertThat(result.nickname).isEqualTo(sameNickname)
        verify { userRepository.save(any()) }
    }
}
