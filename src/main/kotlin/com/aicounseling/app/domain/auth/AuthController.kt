package com.aicounseling.app.domain.auth

import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    
    @PostMapping("/login/google")
    fun loginWithGoogle(@Valid @RequestBody request: OAuthLoginRequest): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "GOOGLE")
            .map { RsData("200", "로그인 성공", it) }
    }
    
    @PostMapping("/login/kakao")
    fun loginWithKakao(@Valid @RequestBody request: OAuthLoginRequest): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "KAKAO")
            .map { RsData("200", "로그인 성공", it) }
    }
    
    @PostMapping("/login/naver")
    fun loginWithNaver(@Valid @RequestBody request: OAuthLoginRequest): Mono<RsData<AuthResponse>> {
        return authService.loginWithOAuth(request.token, "NAVER")
            .map { RsData("200", "로그인 성공", it) }
    }
    
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): RsData<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return RsData("200", "토큰 갱신 성공", response)
    }
}

data class OAuthLoginRequest(
    @field:NotBlank(message = "토큰은 필수입니다")
    val token: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다")
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String,
    val nickname: String
)