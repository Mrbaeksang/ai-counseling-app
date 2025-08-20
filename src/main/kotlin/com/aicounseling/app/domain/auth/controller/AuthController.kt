package com.aicounseling.app.domain.auth.controller

import com.aicounseling.app.domain.auth.dto.AuthResponse
import com.aicounseling.app.domain.auth.dto.OAuthLoginRequest
import com.aicounseling.app.domain.auth.dto.RefreshTokenRequest
import com.aicounseling.app.domain.auth.service.AuthService
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login/google")
    fun loginWithGoogle(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "GOOGLE")
            .map { RsData("200", "로그인 성공", it) }
    }

    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "KAKAO")
            .map { RsData("200", "로그인 성공", it) }
    }

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "NAVER")
            .map { RsData("200", "로그인 성공", it) }
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): RsData<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return RsData("200", "토큰 갱신 성공", response)
    }
}
