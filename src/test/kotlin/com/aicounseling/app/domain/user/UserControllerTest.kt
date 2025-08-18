package com.aicounseling.app.domain.user

import com.aicounseling.app.global.security.JwtTokenProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    @DisplayName("GET /api/users/me - 내 정보 조회 성공")
    fun `should return current user info`() {
        // Given: 테스트용 사용자 생성
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)

        // When & Then: API 호출 및 검증
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.email").value(user.email))
            .andExpect(jsonPath("$.data.nickname").value(user.nickname))
    }

    @Test
    @DisplayName("GET /api/users/me - 토큰 없으면 401")
    fun `should return 401 without token`() {
        // When & Then
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("PATCH /api/users/nickname - 닉네임 변경 성공")
    fun `should update nickname successfully`() {
        // Given
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)
        val newNickname = "새닉네임"

        // When & Then
        mockMvc.perform(
            patch("/api/users/nickname")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname": "$newNickname"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.nickname").value(newNickname))
    }

    @Test
    @DisplayName("PATCH /api/users/nickname - 토큰 없으면 401")
    fun `should return 401 when updating nickname without token`() {
        // When & Then
        mockMvc.perform(
            patch("/api/users/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname": "새닉네임"}"""),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("PATCH /api/users/nickname - 닉네임 너무 짧으면 400")
    fun `should return 400 when nickname is too short`() {
        // Given
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)

        // When & Then
        mockMvc.perform(
            patch("/api/users/nickname")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname": "a"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.msg").value("입력값이 올바르지 않습니다"))
    }

    @Test
    @DisplayName("PATCH /api/users/nickname - 닉네임 너무 길면 400")
    fun `should return 400 when nickname is too long`() {
        // Given
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)
        val longNickname = "a".repeat(21)

        // When & Then
        mockMvc.perform(
            patch("/api/users/nickname")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname": "$longNickname"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.msg").value("입력값이 올바르지 않습니다"))
    }

    @Test
    @DisplayName("GET /api/users/me - 잘못된 토큰이면 401")
    fun `should return 401 with invalid token`() {
        // When & Then
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer invalid.token.here"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("GET /api/users/me - Bearer 없는 토큰이면 401")
    fun `should return 401 with token without Bearer`() {
        // Given
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)

        // When & Then
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", token),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("PATCH /api/users/nickname - 이모지 닉네임 허용")
    fun `should allow emoji in nickname`() {
        // Given
        val user = createTestUser()
        val token = jwtTokenProvider.createToken(user.id, user.email)
        val emojiNickname = "닉네임😀"

        // When & Then
        mockMvc.perform(
            patch("/api/users/nickname")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname": "$emojiNickname"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname").value(emojiNickname))
    }

    private fun createTestUser(): User {
        val user =
            User(
                email = "test@test.com",
                nickname = "테스트유저",
                authProvider = com.aicounseling.app.global.security.AuthProvider.GOOGLE,
                providerId = "google123",
            )
        return userRepository.save(user)
    }
}
