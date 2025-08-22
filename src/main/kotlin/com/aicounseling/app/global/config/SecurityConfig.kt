package com.aicounseling.app.global.config

import com.aicounseling.app.global.security.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
    private val corsConfigurationSource: CorsConfigurationSource,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    // Public counselor endpoints (목록, 상세 조회는 인증 불필요)
                    .requestMatchers("GET", "/api/counselors").permitAll()
                    .requestMatchers("GET", "/api/counselors/*").permitAll()
                    // Protected endpoints (나머지 /api/** 는 인증 필요)
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint { _, response, _ ->
                        response.status = HttpStatus.UNAUTHORIZED.value()
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "resultCode" to "F-401",
                                    "msg" to "로그인이 필요합니다",
                                    "data" to null,
                                ),
                            ),
                        )
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.status = HttpStatus.FORBIDDEN.value()
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "resultCode" to "F-403",
                                    "msg" to "권한이 없습니다",
                                    "data" to null,
                                ),
                            ),
                        )
                    }
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() } // H2 Console을 위해
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
