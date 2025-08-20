plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" // Kotlin 린터
    id("io.gitlab.arturbosch.detekt") version "1.23.6" // 코드 품질 분석
}

group = "com.aicounseling"
version = "0.0.1-SNAPSHOT"
description = "app"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // 헬스체크용

    // WebFlux (OpenRouter API 호출용)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // JDSL (Type-safe JPQL)
    implementation("com.linecorp.kotlin-jdsl:spring-data-kotlin-jdsl-starter-jakarta:2.2.1.RELEASE")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // API Documentation (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // .env 파일 지원
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("io.kotest:kotest-property:5.7.2")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Ktlint 설정
ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Detekt 설정
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml") // 설정 파일 (나중에 생성)
    autoCorrect = true
}

// Detekt를 위한 별도 Kotlin 컴파일러 버전 설정
configurations.named("detekt").configure {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23") // detekt 1.23.6이 사용하는 Kotlin 버전
        }
    }
}

// 통합 검사 task
tasks.register("check-all") {
    group = "verification"
    description = "모든 코드 품질 검사 실행"
    dependsOn("ktlintCheck", "detekt", "test")
}

// Git Hook 설치 task
tasks.register("installGitHooks") {
    group = "git hooks"
    description = "Git hooks 설치"
    doLast {
        val hookScript =
            """
            #!/bin/sh
            echo "🔍 코드 품질 검사 시작..."
            ./gradlew ktlintCheck --daemon
            if [ ${'$'}? -ne 0 ]; then
                echo "❌ Ktlint 검사 실패! 'gradlew ktlintFormat'으로 수정하세요."
                exit 1
            fi
            echo "✅ 코드 품질 검사 통과!"
            """.trimIndent()

        val hookFile = file(".git/hooks/pre-commit")
        hookFile.parentFile.mkdirs()
        hookFile.writeText(hookScript)
        hookFile.setExecutable(true)
        println("✅ Git pre-commit hook 설치 완료!")
    }
}
