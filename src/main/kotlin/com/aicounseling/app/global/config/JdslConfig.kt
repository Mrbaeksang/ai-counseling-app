package com.aicounseling.app.global.config

import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Kotlin JDSL Configuration
 * Spring Boot AutoConfiguration으로 JpqlRenderContext가 자동 생성되지만,
 * 명시적 설정을 위해 Configuration 클래스 생성
 */
@Configuration
class JdslConfig {
    /**
     * JpqlRenderContext Bean
     * JDSL 쿼리를 JPQL 문자열로 렌더링하는 컨텍스트
     */
    @Bean
    fun jpqlRenderContext(): JpqlRenderContext = JpqlRenderContext()

    /**
     * JpqlRenderer Bean
     * 실제 렌더링을 수행하는 렌더러
     */
    @Bean
    fun jpqlRenderer(): JpqlRenderer = JpqlRenderer()
}