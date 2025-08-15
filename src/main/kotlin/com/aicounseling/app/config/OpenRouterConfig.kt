package com.aicounseling.app.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class OpenRouterConfig {
    
    @Bean
    fun openRouterWebClient(properties: OpenRouterProperties): WebClient {
        return WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}

@ConfigurationProperties(prefix = "openrouter")
data class OpenRouterProperties(
    val apiKey: String,
    val model: String = "anthropic/claude-3-haiku",
    val siteUrl: String = "http://localhost:8080",
    val siteName: String = "AI Counseling App"
)