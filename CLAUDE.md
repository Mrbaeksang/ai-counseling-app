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

### Database Strategy
- JPA with Kotlin JDSL for type-safe queries
- H2 for development, PostgreSQL for production
- Entity relationships properly mapped with lazy loading
- Auditing enabled via BaseEntity

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

- Unit tests with MockK for mocking
- Integration tests for API endpoints
- Kotest for BDD-style testing
- Spring MockMvc for controller tests
- Test fixtures in domain test packages

## Environment Configuration

Required environment variables (.env file):
- `OPENROUTER_API_KEY` - OpenRouter API key
- `JWT_SECRET` - JWT signing secret (production)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - Database credentials (production)

Spring profiles:
- `dev` - H2 in-memory database, debug logging
- `prod` - PostgreSQL, optimized settings

## Code Style Guidelines

- Kotlin idioms preferred (data classes, extension functions, null safety)
- Line length limit: 120 characters
- No wildcard imports except java.util.*
- Naming: PascalCase for classes, camelCase for functions/variables
- Use dependency injection via constructor
- Prefer immutable data structures
- Follow existing patterns in codebase for consistency

## Git Commit Rules

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