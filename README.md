# AI 철학자 상담 앱

AI 철학자들과 1:1 상담을 진행할 수 있는 Android 애플리케이션입니다.

## 📱 프로젝트 소개

역사적 인물들의 철학과 화법으로 개인 상담을 제공하는 AI 기반 모바일 앱입니다.
- 소크라테스의 산파술
- 프로이트의 정신분석
- 카사노바의 연애 상담
- 공자의 가족 윤리
- 그 외 30명 이상의 철학자/상담사

## 🛠 기술 스택

### Backend
- Kotlin + Spring Boot 3.5
- Spring WebFlux (비동기)
- PostgreSQL / H2
- JPA + QueryDSL
- OAuth2 (Google, Kakao, Naver)
- OpenRouter API (GPT-OSS-20B)

### Frontend (예정)
- Android Native (Kotlin)
- Jetpack Compose
- MVVM Architecture
- Retrofit2 + Coroutines

## 📁 프로젝트 구조

```
src/main/kotlin/com/aicounseling/app/
├── domain/           # 도메인별 비즈니스 로직
│   ├── user/        # 사용자 관리
│   ├── counselor/   # 상담사 관리
│   └── session/     # 상담 세션
├── global/          # 전역 설정
│   ├── config/      # Spring 설정
│   ├── security/    # 보안 설정
│   ├── exception/   # 예외 처리
│   └── openrouter/  # AI API 연동
└── AiCounselingApplication.kt
```

## 🚀 시작하기

### 사전 요구사항
- JDK 17+
- Gradle 8.5+
- PostgreSQL 14+ (운영)

### 환경 설정

1. 환경 변수 설정 (.env)
```bash
OPENROUTER_API_KEY=your_api_key_here
DB_URL=jdbc:postgresql://localhost:5432/aicounseling
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

2. 애플리케이션 실행
```bash
./gradlew bootRun
```

3. 테스트 실행
```bash
./gradlew test
```

## 📱 주요 기능

### 구현 완료
- ✅ 도메인 엔티티 설계
- ✅ 서비스 레이어
- ✅ Repository 패턴
- ✅ 테스트 코드

### 개발 중
- ⏳ REST API 엔드포인트
- ⏳ OAuth2 소셜 로그인
- ⏳ OpenRouter AI 연동
- ⏳ 실시간 채팅

### 예정
- 📋 Android 앱 개발
- 📋 상담 내용 요약
- 📋 북마크 기능
- 📋 상담사 평가 시스템

## 🌐 API 문서

### 인증
```http
POST /api/v1/auth/login
GET /api/v1/auth/oauth2/google
GET /api/v1/auth/oauth2/kakao
```

### 상담사
```http
GET /api/v1/counselors
GET /api/v1/counselors/{id}
GET /api/v1/counselors/specialty/{tag}
```

### 채팅
```http
POST /api/v1/chat/session
POST /api/v1/chat/message
GET /api/v1/chat/session/{id}/messages
DELETE /api/v1/chat/session/{id}
```

## 🔒 보안

- OAuth2 기반 소셜 로그인만 지원 (비밀번호 저장 안함)
- JWT 토큰 기반 인증
- API Key는 환경변수로 관리
- HTTPS 전용

## 📊 데이터베이스 스키마

### 주요 테이블
- `users` - 사용자 정보
- `counselors` - AI 상담사 정보
- `chat_sessions` - 상담 세션
- `messages` - 대화 메시지
- `session_summaries` - 세션 요약

## 🚢 배포

### 백엔드 서버
- 개발: localhost:8080
- 스테이징: Railway.app
- 운영: AWS Lightsail / Vultr

### 데이터베이스
- 개발: H2 인메모리
- 운영: Supabase PostgreSQL

### Android 앱
- Google Play Store 배포 예정

## 📝 라이센스

MIT License

## 👨‍💻 개발자

- 1인 개발 프로젝트
- Contact: [이메일]

## 🤝 기여하기

이슈와 PR은 언제나 환영합니다!