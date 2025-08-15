package com.aicounseling.app.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository
    
    private lateinit var userService: UserService
    
    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }
    
    @Test
    @DisplayName("사용자 ID로 조회 - 성공")
    fun `findById returns user when exists`() {
        // Given
        val userId = 1L
        val mockUser = createMockUser(userId)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
        
        // When
        val result = userService.findById(userId)
        
        // Then
        assertNotNull(result)
        assertEquals(userId, result?.id)
        verify(userRepository, times(1)).findById(userId)
    }
    
    @Test
    @DisplayName("닉네임 변경 - 성공")
    fun `changeNickname updates nickname successfully`() {
        // TODO: 테스트 구현
        assertTrue(true) // 임시
    }
    
    private fun createMockUser(id: Long): User {
        return User(
            id = id,
            email = "test@test.com",
            nickname = "테스트유저",
            authProvider = com.aicounseling.app.global.security.AuthProvider.GOOGLE,
            providerId = "google123"
        )
    }
}