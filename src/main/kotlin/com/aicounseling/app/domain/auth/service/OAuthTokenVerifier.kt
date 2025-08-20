package com.aicounseling.app.domain.auth.service

import com.aicounseling.app.domain.auth.dto.OAuthUserInfo
import reactor.core.publisher.Mono

interface OAuthTokenVerifier {
    fun verifyToken(token: String): Mono<OAuthUserInfo>
}
