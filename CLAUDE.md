# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🎯 프로젝트: AI 철학자 상담 앱
AI 철학자들이 사용자의 고민을 상담해주는 Kotlin Spring Boot 애플리케이션

## 🚨 절대 준수 규칙

### 코드 수정 원칙
- **에러 시 즉시 수정 금지** - 원인 파악 후 사용자와 상의
- **API 429/503 에러는 외부 문제** - 코드 수정하지 말 것
- **파일 생성 전 사용자 확인 필수** - 중복 폴더/파일 방지
- **설계 → 구현 → 테스트** 순서 준수

### Git 규칙
- **커밋 메시지 한글만** - 영어 절대 금지
- **명시적 요청 시에만 커밋** - "커밋해", "푸시해" 명령 대기
- **형식**: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`

### 보안 규칙
- **API 키는 .env 파일에만** - application.yml 직접 작성 금지
- **환경변수 사용**: `${OPENROUTER_API_KEY}`, `${JWT_SECRET}`

## 📦 핵심 명령어

```bash
# 개발 실행
./gradlew bootRun

# 테스트
./gradlew test                    # 전체 테스트
./gradlew test --rerun-tasks      # 캐시 무시
./gradlew test --tests "패키지.*" # 특정 테스트

# 코드 품질 (필수 실행)
./gradlew ktlintCheck    # 스타일 검사
./gradlew ktlintFormat   # 자동 수정
./gradlew detekt         # 정적 분석

# 통합 검증
./gradlew check          # 모든 검사 + 테스트
./gradlew check-all      # 커스텀 통합 검사

# 빌드
./gradlew clean build
```

## 🏗️ 아키텍처 핵심

### Feature-based Package Structure
```
domain/
├── user/          # User, UserRepository, UserService, UserController, UserDto
├── counselor/     # + CounselorRating, FavoriteCounselor
├── session/       # ChatSession + CounselingPhase enum
└── auth/          # AuthController, OAuthTokenVerifier

global/
├── config/        # SecurityConfig, WebClientConfig, CorsConfig
├── security/      # JwtTokenProvider, JwtAuthenticationFilter  
├── exception/     # GlobalExceptionHandler (모든 예외 처리)
├── openrouter/    # OpenRouterService (AI API 연동)
└── rsData/        # RsData<T> 표준 응답 포맷
```

### 핵심 패턴
1. **RsData<T> 응답 포맷** - 모든 API 응답 통일
   ```kotlin
   RsData.of("200", "성공", data)
   ```

2. **BaseEntity 상속** - JPA Auditing 자동화
   ```kotlin
   @MappedSuperclass
   abstract class BaseEntity : @Id, @CreatedDate, @LastModifiedDate
   ```

3. **GlobalExceptionHandler** - 중앙 집중식 예외 처리
   - `@RestControllerAdvice`로 모든 예외 캐치
   - RsData 형식으로 에러 응답

4. **WebFlux WebClient** - OpenRouter API 비동기 호출
   ```kotlin
   webClient.post()
       .bodyValue(request)
       .retrieve()
       .bodyToMono<Response>()
   ```

## ⚠️ 주의사항

### Detekt 규칙
- **매직넘버 금지** - 상수 추출 필수
- **Generic Exception 금지** - 구체적 예외만
- **와일드카드 임포트 금지** - 명시적 임포트
- **메서드 길이 100줄 제한**
- **파라미터 6개 제한**

### 테스트 전략
- **MockK 사용** - Mockito 대신
- **@SpringBootTest** - 통합 테스트
- **@WebMvcTest** - 컨트롤러 테스트
- **H2 인메모리 DB** - 테스트용

### OpenRouter 연동
- **모델**: `openai/gpt-oss-20b`
- **엔드포인트**: `https://openrouter.ai/api/v1/chat/completions`
- **인증**: Bearer token in header
- **WebClient 빈 사용** - RestTemplate 금지

## 📋 현재 상태
- ✅ User, Counselor, Auth 도메인 완성
- ✅ JWT 인증, 코드 품질 도구 설정
- 🚧 ChatSession API, Message 엔티티 진행 중
- ❌ OAuth 소셜 로그인, WebSocket 미구현

## 🔄 개발 워크플로우
1. Feature 브랜치 생성
2. Entity → Repository → Service → Controller → Test 순서
3. `./gradlew ktlintCheck detekt test` 통과 확인
4. 한글 커밋 메시지로 커밋
5. PR 생성 및 리뷰
