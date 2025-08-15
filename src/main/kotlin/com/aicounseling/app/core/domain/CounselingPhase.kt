package com.aicounseling.app.core.domain

/**
 * 상담 단계를 나타내는 열거형
 * AI가 대화 맥락을 보고 자동으로 판단
 */
enum class CounselingPhase(
    val koreanName: String,
    val description: String
) {
    RAPPORT_BUILDING(
        "라포 형성",
        "따뜻한 인사와 편안한 분위기 조성"
    ),
    
    PROBLEM_EXPLORATION(
        "문제 탐색",
        "구체적인 상황과 감정 파악"
    ),
    
    PATTERN_ANALYSIS(
        "패턴 분석",
        "반복되는 패턴과 인지 왜곡 파악"
    ),
    
    INTERVENTION(
        "개입",
        "대안적 관점 제시와 통찰 제공"
    ),
    
    ACTION_PLANNING(
        "실행 계획",
        "구체적인 행동 계획 수립"
    ),
    
    CLOSING(
        "마무리",
        "핵심 통찰 정리와 격려"
    );
    
    /**
     * 다음 단계 추천 (AI가 참고용으로 사용)
     */
    fun getNextPhase(): CounselingPhase? {
        return when(this) {
            RAPPORT_BUILDING -> PROBLEM_EXPLORATION
            PROBLEM_EXPLORATION -> PATTERN_ANALYSIS
            PATTERN_ANALYSIS -> INTERVENTION
            INTERVENTION -> ACTION_PLANNING
            ACTION_PLANNING -> CLOSING
            CLOSING -> null  // 마지막 단계
        }
    }
    
    /**
     * 이 단계에서 일반적인 대화 턴 수 (가이드라인)
     * AI는 이걸 참고만 하고, 실제로는 맥락 보고 판단
     */
    fun getTypicalTurns(): IntRange {
        return when(this) {
            RAPPORT_BUILDING -> 2..4
            PROBLEM_EXPLORATION -> 5..8
            PATTERN_ANALYSIS -> 5..8
            INTERVENTION -> 5..10
            ACTION_PLANNING -> 3..5
            CLOSING -> 2..3
        }
    }
}