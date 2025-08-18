package com.aicounseling.app.global.rq

import com.aicounseling.app.domain.user.User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

/**
 * Request 컨텍스트
 * 현재 요청의 사용자 정보, 요청/응답 객체 등을 관리
 */
@Component
@RequestScope
class Rq(
    private val request: HttpServletRequest,
    @Suppress("unused") private val response: HttpServletResponse,
) {
    companion object {
        private const val BEARER_PREFIX_LENGTH = 7
    }

    /**
     * 현재 로그인한 사용자 정보
     */
    val user: User?
        get() {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.principal is User) {
                return authentication.principal as User
            }
            return null
        }

    /**
     * 로그인 여부
     */
    val isLogin: Boolean
        get() = user != null

    /**
     * 관리자 여부
     */
    val isAdmin: Boolean
        get() = user?.email?.contains("admin") ?: false

    /**
     * 요청 헤더 가져오기
     */
    fun getHeader(name: String): String? {
        return request.getHeader(name)
    }

    /**
     * JWT 토큰 추출 (Bearer 제거)
     */
    val accessToken: String?
        get() {
            val bearerToken = getHeader("Authorization")
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(BEARER_PREFIX_LENGTH)
            }
            return null
        }
}
