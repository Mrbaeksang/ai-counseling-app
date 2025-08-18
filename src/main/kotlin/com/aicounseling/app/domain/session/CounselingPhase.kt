package com.aicounseling.app.domain.session

/**
 * 상담 단계를 나타내는 열거형
 * AI가 대화 맥락을 보고 자동으로 판단
 */
enum class CounselingPhase(
    val koreanName: String,
    val description: String,
) {
    RAPPORT_BUILDING(
        "라포 형성",
        "따뜻한 인사와 편안한 분위기 조성",
    ),

    PROBLEM_EXPLORATION(
        "문제 탐색",
        "구체적인 상황과 감정 파악",
    ),

    PATTERN_ANALYSIS(
        "패턴 분석",
        "반복되는 패턴과 인지 왜곡 파악",
    ),

    INTERVENTION(
        "개입",
        "대안적 관점 제시와 통찰 제공",
    ),

    ACTION_PLANNING(
        "실행 계획",
        "구체적인 행동 계획 수립",
    ),

    CLOSING(
        "마무리",
        "핵심 통찰 정리와 격려",
    ),
    // 메서드 제거 - 비즈니스 로직은 UseCase에서 처리
}
