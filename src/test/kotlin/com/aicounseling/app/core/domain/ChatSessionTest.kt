package com.aicounseling.app.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import java.time.Duration

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
        assertThat(session.phase).isEqualTo(CounselingPhase.RAPPORT_BUILDING)  // 초기값
        assertThat(session.phaseMetadata).isEmpty()  // 초기엔 비어있음
        assertThat(session.closingSuggested).isFalse()  // 초기엔 false
        assertThat(session.createdAt).isNotNull()
        assertThat(session.lastMessageAt).isNull()  // 아직 메시지 없음
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
        val aiAssessment = "사용자가 충분히 편안해 보임. 문제 탐색 시작 가능"
        session.updatePhase(CounselingPhase.PROBLEM_EXPLORATION, aiAssessment)
        
        // then
        assertThat(session.phase).isEqualTo(CounselingPhase.PROBLEM_EXPLORATION)
        assertThat(session.phaseMetadata).isEqualTo(aiAssessment)
    }
    
    @Test
    @DisplayName("마무리 제안 플래그를 설정할 수 있다")
    fun `마무리 제안 설정 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        assertThat(session.closingSuggested).isFalse()
        
        // when - AI가 마무리 적절하다고 판단
        session.suggestClosing()
        
        // then
        assertThat(session.closingSuggested).isTrue()
    }
    
    @Test
    @DisplayName("마지막 메시지 시간을 업데이트할 수 있다")
    fun `마지막 메시지 시간 업데이트 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        assertThat(session.lastMessageAt).isNull()
        
        // when - 메시지 전송
        session.updateLastMessageTime()
        
        // then
        assertThat(session.lastMessageAt).isNotNull()
        assertThat(session.lastMessageAt).isAfterOrEqualTo(session.createdAt)
    }
    
    @Test
    @DisplayName("세션이 활성 상태인지 확인할 수 있다")
    fun `세션 활성 상태 확인 테스트`() {
        // given - 방금 생성된 세션
        val activeSession = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        activeSession.updateLastMessageTime()
        
        // when & then - 활성 상태
        assertThat(activeSession.isActive()).isTrue()
        
        // given - 오래된 세션 시뮬레이션
        val inactiveSession = ChatSession(
            userId = 2L,
            counselorId = 3L
        )
        // 31분 전으로 설정 (테스트를 위해 직접 설정)
        inactiveSession.lastMessageAt = LocalDateTime.now().minusMinutes(31)
        
        // when & then - 비활성 상태
        assertThat(inactiveSession.isActive()).isFalse()
    }
    
    @Test
    @DisplayName("세션 진행 시간을 계산할 수 있다")
    fun `세션 진행 시간 계산 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        
        // 10분 전에 생성된 것으로 설정 (테스트용)
        val tenMinutesAgo = LocalDateTime.now().minusMinutes(10)
        session.createdAt = tenMinutesAgo
        session.updateLastMessageTime()
        
        // when
        val duration = session.getSessionDuration()
        
        // then
        assertThat(duration.toMinutes()).isGreaterThanOrEqualTo(9)  // 약 10분
        assertThat(duration.toMinutes()).isLessThanOrEqualTo(11)
    }
    
    @Test
    @DisplayName("메시지 수를 기록할 수 있다")
    fun `메시지 카운트 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        assertThat(session.messageCount).isEqualTo(0)
        
        // when - 메시지 추가
        session.incrementMessageCount()
        session.incrementMessageCount()
        session.incrementMessageCount()
        
        // then
        assertThat(session.messageCount).isEqualTo(3)
    }
    
    @Test
    @DisplayName("상담 단계별로 적절한 설명을 반환한다")
    fun `상담 단계 설명 테스트`() {
        // given
        val session = ChatSession(
            userId = 1L,
            counselorId = 2L
        )
        
        // when & then - 각 단계별 설명 확인
        session.updatePhase(CounselingPhase.RAPPORT_BUILDING, "")
        assertThat(session.getPhaseDescription()).contains("라포")
        
        session.updatePhase(CounselingPhase.PROBLEM_EXPLORATION, "")
        assertThat(session.getPhaseDescription()).contains("문제")
        
        session.updatePhase(CounselingPhase.PATTERN_ANALYSIS, "")
        assertThat(session.getPhaseDescription()).contains("패턴")
        
        session.updatePhase(CounselingPhase.INTERVENTION, "")
        assertThat(session.getPhaseDescription()).contains("개입")
        
        session.updatePhase(CounselingPhase.ACTION_PLANNING, "")
        assertThat(session.getPhaseDescription()).contains("계획")
        
        session.updatePhase(CounselingPhase.CLOSING, "")
        assertThat(session.getPhaseDescription()).contains("마무리")
    }
}