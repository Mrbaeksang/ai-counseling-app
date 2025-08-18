package com.aicounseling.app.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * JPA Auditing 설정
 * @CreatedDate, @LastModifiedDate 자동 설정
 */
@Configuration
@EnableJpaAuditing
class JpaConfig
