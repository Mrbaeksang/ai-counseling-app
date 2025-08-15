package com.aicounseling.app.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

class ChatSessionTest {
    
    @Test
    @DisplayName("상담 세션을 생성할 수 있다")
    fun `상담 세션 생성 테스트`() {
        // given
        val userId = 1L
        val counselorId = 2L
        
        // when
        val session = ChatSession(
            userId = userId,
            counselorId = counselorId
        )
        
        // then
        assertThat(session.userId).isEqualTo(userId)
        assertThat(session.counselorId).isEqualTo(counselorId)
        assertThat(session.phase).isEqualTo(CounselingPhase.RAPPORT_BUILDING)
        assertThat(session.phaseMetadata).isEmpty()
        assertThat(session.createdAt).isNotNull()
        assertThat(session.closedAt).isNull()  // 아직 종료 안됨
        assertThat(session.isActive()).isTrue()  // 활성 상태
    }
    
    @Test
    @DisplayName("상담 단계를 변경할 수 있다")
    fun `상담 단계 변경 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        
        // when - AI가 다음 단계로 판단
        val aiReason = "사용자가 충분히 편안해 보임. 문제 탐색 시작 가능"
        session.updatePhase(CounselingPhase.PROBLEM_EXPLORATION, aiReason)
        
        // then
        assertThat(session.phase).isEqualTo(CounselingPhase.PROBLEM_EXPLORATION)
        assertThat(session.phaseMetadata).isEqualTo(aiReason)
    }
    
    @Test
    @DisplayName("세션을 종료할 수 있다")
    fun `세션 종료 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        assertThat(session.isActive()).isTrue()
        assertThat(session.closedAt).isNull()
        
        // when - 사용자가 대화 종료
        session.close()
        
        // then
        assertThat(session.closedAt).isNotNull()
        assertThat(session.isActive()).isFalse()
        assertThat(session.closedAt).isAfterOrEqualTo(session.createdAt)
    }
    
    @Test
    @DisplayName("종료된 세션은 비활성 상태다")
    fun `종료된 세션 활성 상태 확인`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        
        // when
        session.close()
        
        // then
        assertThat(session.isActive()).isFalse()
    }
    
    @Test
    @DisplayName("진행중인 세션은 활성 상태다")
    fun `진행중 세션 활성 상태 확인`() {
        // given & when
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        
        // then - closedAt이 null이면 활성
        assertThat(session.closedAt).isNull()
        assertThat(session.isActive()).isTrue()
    }
}