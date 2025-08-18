# AI 철학자 상담 앱 - AI 개발 규칙

## 🚨 절대 준수 규칙

### 1. 코드 수정 규칙
- **함부로 연속 수정 금지** - 에러 원인 먼저 파악하고 사용자와 상의
- **API 에러 시 코드 수정 금지** - 429(Rate Limit), 503(Timeout)은 외부 문제
- **기존 코드 스타일 따르기** - 주변 코드의 패턴과 라이브러리 사용법 확인
- **막 파일 만들지 말기** - 사용자와 상의 후 진행
- **디렉토리 구조 확인 먼저** - 중복 폴더나 잘못된 위치에 파일 생성 금지

### 2. Git 규칙  
- **커밋 메시지 한글로만** - 영어 절대 금지
- **명시적 요청 시에만 커밋** - 사용자가 "커밋해", "푸시해" 라고 할 때만
- **커밋 형식**: `feat: 기능추가`, `fix: 버그수정`, `docs: 문서수정`, `refactor: 리팩토링`, `test: 테스트추가`

### 3. 개발 프로세스
- **정석 순서 준수**: 요구사항 → 설계 → 구현 → 테스트
- **설계 먼저, 코딩 나중** - 무작정 코드 작성 금지
- **테스트 필수** - 기능 구현 후 반드시 테스트 코드
- **TDD 권장** - 테스트 먼저 작성하고 구현

### 4. 보안 규칙
- **API 키는 .env 파일에** - application.yml에 직접 작성 금지  
- **시크릿 절대 커밋 금지** - API 키, 비밀번호 등
- **환경변수로 관리**: `${OPENROUTER_API_KEY}`

## 📦 기술 스택

### Backend
- **Kotlin + Spring Boot 3.5** (WebFlux 비동기)
- **Feature-based Architecture** (domain/기능별 패키지)
- **OpenRouter API** (Spring AI 없이 직접 WebClient 구현)
- **PostgreSQL** (운영), **H2** (개발/테스트)
- **JPA** (ORM)
- **JWT 인증** (Spring OAuth2 Client 사용 안함)

### Frontend (예정)
- **Android Native** (Kotlin + Jetpack Compose)
- **MVVM 패턴**

## 🏗️ 프로젝트 구조 (Feature-based)
```
com.aicounseling.app/
├── domain/           # 기능별 패키지
│   ├── user/         # User.kt, UserRepository.kt, UserService.kt, UserController.kt
│   ├── counselor/    # 상담사 관련 모든 파일
│   ├── session/      # 세션 관련 모든 파일
│   └── auth/         # 인증 관련 모든 파일
├── global/           # 전역 설정
│   ├── config/       # Spring 설정
│   ├── security/     # JWT, 필터
│   ├── exception/    # 예외 처리
│   ├── jpa/          # BaseEntity
│   ├── rsData/       # 응답 포맷
│   └── openrouter/   # AI API 연동
└── standard/         # 유틸리티
    └── util/         # Ut 클래스
```

## 🔧 Global 설정 체크리스트
- ✅ SecurityConfig - JWT 필터 체인
- ✅ JwtTokenProvider - 토큰 생성/검증
- ✅ JwtAuthenticationFilter - 요청 토큰 검증
- ✅ GlobalExceptionHandler - 통합 에러 처리
- ✅ CorsConfig - 프로파일별 CORS
- ✅ RsData - 표준 응답 포맷
- ✅ Ut - 유틸리티 함수
- ✅ BaseEntity - JPA Auditing
- ✅ ValidationConfig - Bean Validation

## 💬 OpenRouter 설정
- **엔드포인트**: `https://openrouter.ai/api/v1/chat/completions`
- **모델**: openai/gpt-4o-mini (비용 효율)
- **타임아웃**: 60초
- **Max Tokens**: 2000 (상담 답변용)
- **필수 헤더**: Authorization, HTTP-Referer, X-Title

## 🔄 개발 순서 (Feature-based)
1. **Entity** - JPA 엔티티
2. **Repository** - JPA Repository 인터페이스
3. **Service** - 비즈니스 로직
4. **Controller** - REST API
5. **DTO** - 요청/응답 DTO
6. **Test** - 단위/통합 테스트

## 🔧 개발 명령어
```bash
./gradlew bootRun           # 실행
./gradlew test             # 테스트
./gradlew test --rerun-tasks  # 테스트 강제 재실행
./gradlew build            # 빌드
```

## 📝 자주 하는 실수 (AI가)
1. PlantUML 사용 → **Mermaid 사용할 것**
2. 영어 커밋 메시지 → **한글로만**
3. 에러 시 바로 코드 수정 → **원인 파악 먼저**
4. Spring AI 의존성 추가 → **OpenRouter 직접 구현**
5. 설계 없이 코딩 → **설계 문서 먼저**
6. 막 파일 생성 → **사용자와 상의 먼저**
7. 중복 폴더 생성 → **기존 구조 확인 먼저**
