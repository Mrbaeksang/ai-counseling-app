package com.aicounseling.app.domain.user

import com.aicounseling.app.global.security.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun findByAuthProviderAndProviderId(
        authProvider: AuthProvider,
        providerId: String,
    ): User?
}
