package com.aicounseling.app.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    companion object {
        private const val MAX_MEMORY_SIZE_MB = 5
        private const val BYTES_PER_KB = 1024
        private const val KB_PER_MB = 1024
        private const val MAX_IN_MEMORY_SIZE = MAX_MEMORY_SIZE_MB * KB_PER_MB * BYTES_PER_KB
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE)
            }
            .build()
    }
}
