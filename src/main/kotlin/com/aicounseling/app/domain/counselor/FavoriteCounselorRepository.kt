package com.aicounseling.app.domain.counselor

import com.aicounseling.app.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * FavoriteCounselorRepository - 즐겨찾기 관리
 *
 *    - Counselor 기능에 즐겨찾기도 포함
 *    - 즐겨찾기는 독립적 기능이 아니라 Counselor의 부가 기능
 *    - 만약 즐겨찾기가 복잡해지면 그때 domain/favorite로 분리
 */
interface FavoriteCounselorRepository : JpaRepository<FavoriteCounselor, Long> {
    // 이미 즐겨찾기 했는지 확인 (중복 방지용)
    fun existsByUserAndCounselor(
        user: User,
        counselor: Counselor,
    ): Boolean

    // 특정 사용자의 모든 즐겨찾기 목록 조회
    fun findByUser(user: User): List<FavoriteCounselor>

    // 즐겨찾기 삭제 (취소)
    fun deleteByUserAndCounselor(
        user: User,
        counselor: Counselor,
    )

    // 특정 즐겨찾기 조회 (삭제 전 확인용)
    fun findByUserAndCounselor(
        user: User,
        counselor: Counselor,
    ): FavoriteCounselor?

    // 추가 가능한 유용한 메서드들 (필요시)
    // fun countByUser(user: User): Long  // 사용자의 즐겨찾기 개수
    // fun findByUserOrderByCreatedAtDesc(user: User): List<FavoriteCounselor>  // 최신순 정렬
}
