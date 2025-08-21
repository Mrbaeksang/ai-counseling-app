package com.aicounseling.app.domain.auth.service

import com.aicounseling.app.domain.auth.dto.AuthResponse
import com.aicounseling.app.domain.auth.dto.OAuthUserInfo
import com.aicounseling.app.domain.user.entity.User
import com.aicounseling.app.domain.user.repository.UserRepository
import com.aicounseling.app.global.exception.UnauthorizedException
import com.aicounseling.app.global.security.AuthProvider
import com.aicounseling.app.global.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
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

    fun refreshToken(refreshToken: String): AuthResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val user =
            userRepository.findById(userId)
                .orElseThrow { UnauthorizedException("사용자를 찾을 수 없습니다") }

        return createAuthResponse(user)
    }

    private fun findOrCreateUser(
        oauthInfo: OAuthUserInfo,
        authProvider: AuthProvider,
    ): User {
        val existingUser = userRepository.findByAuthProviderAndProviderId(authProvider, oauthInfo.providerId)

        return if (existingUser != null) {
            existingUser.lastLoginAt = LocalDateTime.now()
            userRepository.save(existingUser)
        } else {
            val newUser =
                User(
                    email = oauthInfo.email,
                    nickname = oauthInfo.name ?: oauthInfo.email.substringBefore("@"),
                    authProvider = authProvider,
                    providerId = oauthInfo.providerId,
                    isActive = true,
                    lastLoginAt = LocalDateTime.now(),
                )
            userRepository.save(newUser)
        }
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
