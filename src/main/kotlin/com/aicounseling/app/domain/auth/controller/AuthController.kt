package com.aicounseling.app.domain.auth.controller

import com.aicounseling.app.domain.auth.dto.AuthResponse
import com.aicounseling.app.domain.auth.dto.OAuthLoginRequest
import com.aicounseling.app.domain.auth.dto.RefreshTokenRequest
import com.aicounseling.app.domain.auth.service.AuthService
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val rq: Rq,
) {
    @PostMapping("/login/google")
    fun loginWithGoogle(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "GOOGLE").awaitSingle()
            rq.successResponse(
                data = response,
                message = "구글 로그인 성공",
            )
        }

    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "KAKAO").awaitSingle()
            rq.successResponse(
                data = response,
                message = "카카오 로그인 성공",
            )
        }

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "NAVER").awaitSingle()
            rq.successResponse(
                data = response,
                message = "네이버 로그인 성공",
            )
        }

    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): RsData<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return rq.successResponse(
            data = response,
            message = "토큰 갱신 성공",
        )
    }
}
