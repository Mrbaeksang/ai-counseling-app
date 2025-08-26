package com.aicounseling.app.global.auth.service

import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.service.UserService
import com.aicounseling.app.global.auth.dto.AuthResponse
import com.aicounseling.app.global.auth.dto.OAuthUserInfo
import com.aicounseling.app.global.exception.UnauthorizedException
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional
class AuthService(
    // UserRepository 대신 UserService 주입
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val kakaoTokenVerifier: KakaoTokenVerifier,
    private val naverTokenVerifier: NaverTokenVerifier,
) {
    fun loginWithOAuth(
        token: String,
        provider: String,
    ): Mono<AuthResponse> {
        val verifier =
            when (provider) {
                "GOOGLE" -> googleTokenVerifier
                "KAKAO" -> kakaoTokenVerifier
                "NAVER" -> naverTokenVerifier
                else -> throw UnauthorizedException("지원하지 않는 인증 제공자입니다")
            }

        return verifier.verifyToken(token)
            .map { oauthInfo ->
                val authProvider = AuthProvider.valueOf(provider)
                val user = findOrCreateUser(oauthInfo, authProvider)
                createAuthResponse(user)
            }
    }

    @Suppress("SwallowedException")
    fun refreshToken(refreshToken: String): AuthResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)

        // UserService를 통해 사용자 조회
        val user =
            try {
                userService.getUser(userId)
            } catch (e: NoSuchElementException) {
                // 원본 예외 메시지를 포함하여 UnauthorizedException으로 변환
                throw UnauthorizedException("사용자를 찾을 수 없습니다: ${e.message}")
            }

        return createAuthResponse(user)
    }

    private fun findOrCreateUser(
        oauthInfo: OAuthUserInfo,
        authProvider: AuthProvider,
    ): User {
        // UserService로 위임 (도메인 로직은 Service에서 처리)
        return userService.findOrCreateOAuthUser(
            provider = authProvider,
            providerId = oauthInfo.providerId,
            email = oauthInfo.email,
            nickname = oauthInfo.name ?: oauthInfo.email.substringBefore("@"),
            profileImageUrl = oauthInfo.picture,
        )
    }

    private fun createAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenProvider.createToken(user.id, user.email)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            email = user.email,
            nickname = user.nickname,
        )
    }
}
