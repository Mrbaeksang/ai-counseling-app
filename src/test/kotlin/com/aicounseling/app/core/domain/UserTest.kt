package com.aicounseling.app.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

class UserTest {

    @Test
    @DisplayName("User 엔티티를 생성할 수 있다")
    fun `User 엔티티 생성 테스트`() {
        // given (준비)
        val email = "student@test.com"
        val nickname = "학생1"
        
        // when (실행)
        val user = User(
            email = email,
            nickname = nickname
        )
        
        // then (검증)
        assertThat(user.email).isEqualTo(email)
        assertThat(user.nickname).isEqualTo(nickname)
        assertThat(user.id).isEqualTo(0L)  // 아직 저장 전이니 ID는 0
    }
    
    @Test
    @DisplayName("User는 생성 시간을 자동으로 기록한다")
    fun `User 생성시간 자동 기록 테스트`() {
        // when
        val user = User(
            email = "test@test.com",
            nickname = "테스터"
        )
        
        // then
        assertThat(user.createdAt).isNotNull()
        assertThat(user.createdAt).isBefore(LocalDateTime.now().plusSeconds(1))
    }
}