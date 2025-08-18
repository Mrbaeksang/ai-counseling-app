package com.aicounseling.app.domain.auth

import com.aicounseling.app.global.exception.UnauthorizedException
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface OAuthTokenVerifier {
    fun verifyToken(token: String): Mono<OAuthUserInfo>
}

data class OAuthUserInfo(
    val providerId: String,
    val email: String,
    val name: String?,
    val provider: String,
)

@Service
class GoogleTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=$token")
            .retrieve()
            .bodyToMono(GoogleTokenInfo::class.java)
            .map { info ->
                if (info.emailVerified != true) {
                    throw UnauthorizedException("이메일 인증이 필요합니다")
                }
                OAuthUserInfo(
                    providerId = info.sub,
                    email = info.email,
                    name = info.name,
                    provider = "GOOGLE",
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Google 토큰입니다") }
    }

    data class GoogleTokenInfo(
        val sub: String,
        val email: String,
        @JsonProperty("email_verified")
        val emailVerified: Boolean?,
        val name: String?,
    )
}

@Service
class KakaoTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono(KakaoUserInfo::class.java)
            .map { info ->
                OAuthUserInfo(
                    providerId = info.id.toString(),
                    email = info.kakao_account?.email ?: throw UnauthorizedException("이메일 정보가 없습니다"),
                    name = info.kakao_account?.profile?.nickname,
                    provider = "KAKAO",
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Kakao 토큰입니다") }
    }

    data class KakaoUserInfo(
        val id: Long,
        @Suppress("ConstructorParameterNaming") val kakao_account: KakaoAccount?,
    )

    data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    data class KakaoProfile(
        val nickname: String?,
    )
}

@Service
class NaverTokenVerifier(
    private val webClient: WebClient,
) : OAuthTokenVerifier {
    override fun verifyToken(token: String): Mono<OAuthUserInfo> {
        return webClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono(NaverUserResponse::class.java)
            .map { response ->
                val info = response.response
                OAuthUserInfo(
                    providerId = info.id,
                    email = info.email ?: throw UnauthorizedException("이메일 정보가 없습니다"),
                    name = info.name,
                    provider = "NAVER",
                )
            }
            .onErrorMap { UnauthorizedException("유효하지 않은 Naver 토큰입니다") }
    }

    data class NaverUserResponse(
        val response: NaverUserInfo,
    )

    data class NaverUserInfo(
        val id: String,
        val email: String?,
        val name: String?,
    )
}
