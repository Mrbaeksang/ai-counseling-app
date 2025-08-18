package com.aicounseling.app.global.openrouter

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class OpenRouterConfig {
    @Bean
    fun openRouterWebClient(properties: OpenRouterProperties): WebClient {
        val httpClient =
            HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60)) // 60초 타임아웃

        return WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("HTTP-Referer", properties.siteUrl)
            .defaultHeader("X-Title", properties.siteName)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}

@ConfigurationProperties(prefix = "openrouter")
data class OpenRouterProperties(
    val apiKey: String,
    val model: String = "openai/gpt-oss-20b",
    val siteUrl: String = "http://localhost:8080",
    val siteName: String = "AI Counseling App",
)
