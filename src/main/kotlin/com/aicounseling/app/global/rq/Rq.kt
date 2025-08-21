package com.aicounseling.app.global.rq

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.rsData.RsData
import com.aicounseling.app.global.security.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class Rq(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    val authentication
        get() = SecurityContextHolder.getContext().authentication

    val isAuthenticated: Boolean
        get() =
            authentication?.isAuthenticated == true &&
                authentication.principal != "anonymousUser"

    val currentUserId: Long?
        get() =
            if (isAuthenticated) {
                authentication?.name?.toLongOrNull()
            } else {
                null
            }

    val currentUser: User?
        get() =
            if (isAuthenticated) {
                authentication?.principal as? User
            } else {
                null
            }

    fun getHeader(name: String): String? = request.getHeader(name)

    fun getParameter(name: String): String? = request.getParameter(name)

    fun setAttribute(
        name: String,
        value: Any,
    ) {
        request.setAttribute(name, value)
    }

    fun getAttribute(name: String): Any? = request.getAttribute(name)

    fun getCookie(name: String): String? {
        return request.cookies?.find { it.name == name }?.value
    }

    fun getRequestURI(): String = request.requestURI

    fun getMethod(): String = request.method

    fun isGet(): Boolean = request.method == "GET"

    fun isPost(): Boolean = request.method == "POST"

    fun isPut(): Boolean = request.method == "PUT"

    fun isDelete(): Boolean = request.method == "DELETE"

    fun extractTokenFromHeader(): String? {
        val bearerToken = getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    fun <T> successResponse(
        data: T,
        message: String = "성공",
    ): RsData<T> {
        return RsData.of("200", message, data)
    }

    fun <T> errorResponse(
        message: String,
        data: T? = null,
    ): RsData<T> {
        return RsData.of("400", message, data)
    }

    fun <T> unauthorizedResponse(message: String = "인증이 필요합니다"): RsData<T> {
        return RsData.of("401", message, null)
    }

    fun <T> forbiddenResponse(message: String = "권한이 없습니다"): RsData<T> {
        return RsData.of("403", message, null)
    }

    fun <T> notFoundResponse(message: String = "리소스를 찾을 수 없습니다"): RsData<T> {
        return RsData.of("404", message, null)
    }
}
