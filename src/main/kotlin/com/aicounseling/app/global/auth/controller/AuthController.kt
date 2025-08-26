package com.aicounseling.app.global.auth.controller

import com.aicounseling.app.global.auth.dto.AuthResponse
import com.aicounseling.app.global.auth.dto.OAuthLoginRequest
import com.aicounseling.app.global.auth.dto.RefreshTokenRequest
import com.aicounseling.app.global.auth.service.AuthService
import com.aicounseling.app.global.constants.AppConstants
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
) {
    @PostMapping("/login/google")
    fun loginWithGoogle(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "GOOGLE").awaitSingle()
            RsData.of(
                AppConstants.Response.SUCCESS_CODE,
                "구글 로그인 성공",
                response,
            )
        }

    @PostMapping("/login/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "KAKAO").awaitSingle()
            RsData.of(
                AppConstants.Response.SUCCESS_CODE,
                "카카오 로그인 성공",
                response,
            )
        }

    @PostMapping("/login/naver")
    fun loginWithNaver(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): RsData<AuthResponse> =
        runBlocking {
            val response = authService.loginWithOAuth(request.token, "NAVER").awaitSingle()
            RsData.of(
                AppConstants.Response.SUCCESS_CODE,
                "네이버 로그인 성공",
                response,
            )
        }

    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): RsData<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return RsData.of(
            AppConstants.Response.SUCCESS_CODE,
            "토큰 갱신 성공",
            response,
        )
    }
}
