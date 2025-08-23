package com.aicounseling.app.global.rq

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.global.rsData.RsData
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class Rq {
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

    fun <T> successResponse(
        data: T,
        message: String = "성공",
    ): RsData<T> {
        return RsData.of("S-1", message, data)
    }

    fun <T> unauthorizedResponse(message: String = "인증이 필요합니다"): RsData<T> {
        return RsData.of("F-401", message, null)
    }
}
