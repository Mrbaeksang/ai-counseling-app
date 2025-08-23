# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Counseling App - A Spring Boot Kotlin application providing AI-powered philosophical counseling services through integration with OpenRouter API. The system allows users to have 1-on-1 conversations with AI counselors embodying historical philosophers and thinkers.

## Development Commands

### Build & Run
```bash
# Run application
./gradlew bootRun

# Build project
./gradlew build

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.UserServiceTest"

# Run with coverage
./gradlew test jacocoTestReport
```

### Code Quality
```bash
# Ktlint check (code style)
./gradlew ktlintCheck

# Ktlint format (auto-fix style issues)
./gradlew ktlintFormat

# Detekt analysis (code quality)
./gradlew detekt

# Run all quality checks
./gradlew check-all

# Install git pre-commit hooks
./gradlew installGitHooks
```

## Architecture & Code Organization

### Domain-Driven Design Structure
The application follows DDD principles with clear bounded contexts:

- **domain/** - Core business logic organized by aggregate roots
  - Each domain module contains: controller, dto, entity, repository, service
  - Key domains: user, counselor, session, auth
  
- **global/** - Cross-cutting concerns and infrastructure
  - config: Spring configurations (Security, CORS, WebClient)
  - security: JWT authentication and OAuth2 integration
  - openrouter: AI API integration layer
  - exception: Global error handling
  - rsData/rq: Response/Request wrapper patterns

### Global Components Detail

#### AOP (Aspect-Oriented Programming)
- **ResponseAspect** (`global/aspect/ResponseAspect.kt`)
  - Intercepts all Controller methods returning RsData
  - Automatically sets HTTP status codes based on result codes (S-1 → 200, F-* → 4xx/5xx)
  - Applied via `@Aspect` and `@Around` annotations
  - Ensures consistent API response handling

#### Base Entity
- **BaseEntity** (`global/entity/BaseEntity.kt`)
  - Abstract class with common fields: id, createdAt, updatedAt
  - Uses `@MappedSuperclass` for JPA inheritance
  - Enables JPA Auditing with `@EntityListeners(AuditingEntityListener::class)`
  - All domain entities extend this for consistent timestamps

#### Global Exception Handler
- **GlobalExceptionHandler** (`global/exception/GlobalExceptionHandler.kt`)
  - `@RestControllerAdvice` for centralized error handling
  - Catches and transforms exceptions to RsData format
  - Handles: MethodArgumentNotValidException, HttpMessageNotReadableException, custom exceptions
  - Returns consistent error responses with appropriate HTTP status codes
  - Example: `@ExceptionHandler(IllegalArgumentException::class)` → RsData with F-400 code

#### Request/Response Wrappers
- **RsData** (`global/rsData/RsData.kt`)
  - Standard response wrapper: `RsData<T>(resultCode, msg, data)`
  - Result codes: S-* for success, F-* for failure
  - Factory methods: `RsData.of()` for success, `RsData.failOf()` for errors
  - Integrates with ResponseAspect for automatic status mapping

- **Rq** (`global/rq/Rq.kt`)
  - Request context holder injected via `@Component` and `@RequestScope`
  - Provides: `member` (current authenticated user), `isLogin`, `isAdmin`
  - Simplifies authentication checks in controllers/services
  - Usage: `rq.member ?: throw UnauthorizedException()`

#### Configuration Classes
- **SecurityConfig** (`global/config/SecurityConfig.kt`)
  - JWT authentication filter configuration
  - OAuth2 login settings for Google/Kakao/Naver
  - CORS and CSRF policies
  - Public endpoints whitelist

- **JpaConfig** (`global/config/JpaConfig.kt`)
  - `@EnableJpaAuditing` for automatic timestamps
  - `@EnableJpaRepositories` with base packages
  - Transaction management settings

- **JdslConfig** (`global/config/JdslConfig.kt`)
  - JpqlRenderContext bean for JDSL query rendering
  - JpqlRenderer bean for query execution
  - JDSL 3.5.5 configuration for type-safe JPQL

- **WebClientConfig** (`global/config/WebClientConfig.kt`)
  - WebClient bean for OpenRouter API calls
  - Timeout settings and error handling
  - Connection pooling configuration

#### Constants
- **AppConstants** (`global/constants/AppConstants.kt`)
  - Centralized constants for the entire application
  - Error messages, default values, limits
  - Session constants (MAX_CONVERSATION_HISTORY, TITLE_MAX_LENGTH)
  - API response codes and messages

### Key Architectural Patterns

1. **Layered Architecture**: Controller → Service → Repository
2. **Response Wrapper Pattern**: All API responses use RsData wrapper for consistent structure
3. **Base Entity Pattern**: Common fields (id, timestamps) in BaseEntity
4. **JWT + OAuth2**: Dual authentication strategy supporting social logins
5. **Reactive Programming**: WebFlux for non-blocking OpenRouter API calls
6. **PagedResponse Pattern**: Standardized pagination with metadata (`PagedResponse<T>`)
7. **Custom Repository Pattern**: Complex queries via `RepositoryCustom` + `RepositoryImpl`

### Database Strategy  
- JPA with Kotlin JDSL 3.5.5 for type-safe queries
- H2 for development, PostgreSQL for production
- Entity relationships properly mapped with lazy loading
- Auditing enabled via BaseEntity
- Custom Repository pattern for complex queries (N+1 문제 해결)

## API Integration Points

### OpenRouter AI Service
- Configuration in `OpenRouterService.kt`
- Async/reactive calls using WebClient
- Message context management for conversations
- Counselor personality prompts in entity definitions

### OAuth2 Providers
- Google: `GoogleTokenVerifier`
- Kakao: `KakaoTokenVerifier`  
- Naver: `NaverTokenVerifier`
- Token verification and user info extraction

## Testing Approach

### Testing Stack & Versions
- **Spring Boot 3.5.4** with **Kotlin 1.9.25** and **JDK 21**
- **JUnit 5**: Primary testing framework
- **MockK 1.13.8**: Kotlin-first mocking library (NOT Mockito)
- **SpringMockK 4.0.2**: Spring integration for MockK
- **Spring MockMvc**: Controller/API testing
- **Kotest 5.7.2**: Available for BDD-style specs

### Critical Configuration for Spring Boot 3.5 + Kotlin

#### 1. AOP and Test Profile Separation
```kotlin
// ResponseAspect.kt - MUST be disabled in tests
@Aspect
@Component
@Profile("!test")  // Critical: Prevents ClassCastException in tests
class ResponseAspect { ... }
```

#### 2. Test Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // MUST exclude Mockito and use MockK for Kotlin
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

// JDK 21 compatibility settings
tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",  // Required for JDK 21+
        "-Djdk.instrument.traceUsage=false"
    )
}
```

#### 3. Test Annotations Pattern
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GetUserSessionsApiTest { ... }
```

### Test Organization
Each controller should have multiple focused test files:
- One test file per API endpoint (max 150 lines for Detekt)
- Base test class for common setup and fixtures
- Separate configuration for mocked services

Example structure:
```
src/test/kotlin/.../controller/
├── ChatSessionControllerBaseTest.kt  # Common setup & TestConfig
├── GetUserSessionsApiTest.kt        # GET /sessions
├── StartSessionApiTest.kt           # POST /sessions
└── SendMessageApiTest.kt            # POST /sessions/{id}/messages
```

### Base Test Class Pattern
```kotlin
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun mockOpenRouterService(): OpenRouterService = mockk()
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ChatSessionControllerBaseTest(
    protected val mockMvc: MockMvc,
    protected val objectMapper: ObjectMapper,
    // ... other dependencies
) {
    @BeforeEach
    fun setupTestData() { ... }
    
    @AfterEach
    fun cleanupTestData() { ... }
}
```

### MockMvc Testing with RsData
```kotlin
// Testing with JSON path assertions
mockMvc.perform(
    get("/api/sessions")
        .header("Authorization", "Bearer $authToken")
)
    .andExpect(status().isOk)
    .andExpect(jsonPath("$.resultCode").value("S-1"))
    .andExpect(jsonPath("$.msg").value("세션 목록 조회 성공"))
    .andExpect(jsonPath("$.data").isArray)
```

### Security Configuration in Tests
```kotlin
// SecurityConfig must return consistent error format
.authenticationEntryPoint { _, response, _ ->
    response.writer.write(
        objectMapper.writeValueAsString(
            mapOf(
                "resultCode" to "F-401",  // Use F- prefix for failures
                "msg" to "로그인이 필요합니다",
                "data" to null
            )
        )
    )
}
```

### Common Testing Pitfalls & Solutions

| Problem | Solution |
|---------|----------|
| ClassCastException: ResponseEntity cannot be cast to RsData | Add `@Profile("!test")` to ResponseAspect |
| Mockito warnings in JDK 21 | Add JVM args: `-XX:+EnableDynamicAgentLoading` |
| Wrong resultCode format (401 vs F-401) | Update SecurityConfig error responses |
| @Transactional on private methods | Remove or make methods public |
| Kotlin Enum deprecation | Use `.entries` instead of `.values()` |

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.GetUserSessionsApiTest"

# Run with detailed output
./gradlew test --info
```

### Test Best Practices for Spring Boot 3.5
- **Always use MockK, never Mockito** for Kotlin projects
- **Disable AOP components** in test profile to avoid proxy issues
- **Use @Transactional** on test classes, not private methods
- **Mock external services** with @TestConfiguration beans
- **Test actual JSON responses** with jsonPath assertions
- **Clean up test data** in @AfterEach to prevent conflicts
- **Keep test files under 150 lines** for Detekt compliance
- **Use Kotest 5.7.2** for BDD-style testing when needed

## Environment Configuration

Required environment variables (.env file):
- `OPENROUTER_API_KEY` - OpenRouter API key
- `JWT_SECRET` - JWT signing secret (production)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - Database credentials (production)

Default AI Model: `meta-llama/llama-3.2-3b-instruct` (configured in OpenRouterConfig)

Spring profiles:
- `dev` - H2 in-memory database, debug logging
- `prod` - PostgreSQL, optimized settings

## Code Style Guidelines

### General Kotlin Style
- Kotlin idioms preferred (data classes, extension functions, null safety)
- Line length limit: 120 characters
- No wildcard imports except java.util.*
- Naming: PascalCase for classes, camelCase for functions/variables
- Use dependency injection via constructor
- Prefer immutable data structures
- Follow existing patterns in codebase for consistency

### Comment Placement Rules (Ktlint)
- **NEVER place comments inside value argument lists** (인라인 주석 금지)
- **ALWAYS place comments on separate lines above the code** (주석은 항상 별도 줄에)
- Incorrect example:
  ```kotlin
  val request = RateSessionRequest(
      rating = 10, // 1~10 범위 (별 5개 = 10) ❌
      feedback = "좋아요"
  )
  ```
- Correct example:
  ```kotlin
  // 1~10 범위 (별 5개 = 10)
  val request = RateSessionRequest(
      rating = 10,
      feedback = "좋아요"
  )
  ```
- For multi-line expressions, place comments before or after the entire block
- Trailing comments on the same line are only allowed for variable declarations

## Git Workflow & Commit Rules

### Branch Strategy & PR Workflow
- **NEVER push directly to main branch** (절대 main 브랜치에 직접 푸시 금지)
- **ALWAYS create feature branch and PR** (항상 feature 브랜치 생성 후 PR 생성)
- Branch naming convention:
  - `feat/기능명` - 새로운 기능
  - `fix/버그명` - 버그 수정
  - `refactor/대상` - 리팩토링
  - `test/테스트명` - 테스트 추가/수정
  - `docs/문서명` - 문서 수정
- PR creation command: `gh pr create --title "제목" --body "설명"`

### Commit Messages
- **MUST use Korean commit messages** (한국어로 커밋 메시지 작성 필수)
- **NEVER include AI-generated markers** (절대 "Generated with Claude" 등의 AI 표시 금지)
- Commit format: `type: 한국어 설명`
  - feat: 새로운 기능
  - fix: 버그 수정
  - refactor: 리팩토링
  - test: 테스트 추가/수정
  - docs: 문서 수정
  - style: 코드 스타일 수정
- Example: `fix: 로그인 인증 버그 수정`

## Security Considerations

- JWT tokens in Authorization header (Bearer scheme)
- OAuth2 tokens verified with provider APIs
- CORS configured for specific origins
- API keys stored in environment variables
- Password-less authentication (OAuth2 only)

## Common Development Tasks

When implementing new features:
1. Create entity in appropriate domain package
2. Add repository interface extending JpaRepository
3. Implement service with business logic
4. Create DTOs for request/response
5. Add controller with proper validation
6. Write unit and integration tests
7. Update API documentation if needed

When modifying AI behavior:
1. Check counselor prompts in Counselor entity
2. Adjust OpenRouterService for API interaction
3. Test with OpenRouterIntegrationTest

## Performance Optimization Points

- JPA lazy loading for relationships
- Pagination utilities in PageUtils
- WebFlux for non-blocking I/O
- Response caching where appropriate
- Database indexes on frequently queried fields
- N+1 query prevention with JDSL Custom Repository pattern

## N+1 Query Problem Solutions

### Using JDSL Custom Repository Pattern
When dealing with N+1 queries, use the Custom Repository pattern with JDSL:

```kotlin
// 1. Create Custom interface
interface ChatSessionRepositoryCustom {
    fun findSessionsWithCounselor(
        userId: Long, 
        bookmarked: Boolean?,
        pageable: Pageable
    ): Page<SessionListResponse>
}

// 2. Implement with JDSL (no @Repository annotation)
class ChatSessionRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : ChatSessionRepositoryCustom {
    override fun findSessionsWithCounselor(...): Page<SessionListResponse> {
        return kotlinJdslJpqlExecutor.findPage(pageable) {
            selectNew<SessionListResponse>(
                path(ChatSession::id),
                path(Counselor::name),
                // ... other fields
            ).from(
                entity(ChatSession::class),
                join(Counselor::class).on(
                    path(ChatSession::counselorId).eq(path(Counselor::id))
                )
            )
        }
    }
}

// 3. Main repository extends both interfaces
@Repository
interface ChatSessionRepository : 
    JpaRepository<ChatSession, Long>, 
    ChatSessionRepositoryCustom
```

### Key Points for N+1 Prevention
- Use `selectNew<DTO>()` for direct DTO projection
- JOIN related entities in single query
- Don't extend `KotlinJdslJpqlExecutor` directly in main repository
- Return non-nullable types after INNER JOIN