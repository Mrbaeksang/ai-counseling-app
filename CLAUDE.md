# AI 철학자 상담 앱 - AI 개발 규칙

## 🎯 프로젝트 개요
**AI 철학자/상담사와 1:1 대화하는 상담 애플리케이션**
- 역사적 인물들이 각자의 철학과 화법으로 상담 제공
- 소크라테스(산파술), 프로이트(정신분석), 카사노바(연애), 공자(가족윤리) 등 30명+
- 타겟: Google Play Store 배포 (Android 앱)

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

### 5. Clean Architecture 규칙
- **Anemic Domain Model 사용** - 엔티티는 순수 데이터만 (메서드 최소화)
- **비즈니스 로직은 Service에** - 엔티티에 복잡한 로직 금지
- **의존성 방향**: Domain ← UseCase ← Adapter (안쪽으로만)
- **인터페이스 먼저 정의** - 구현체 만들기 전에 인터페이스부터

## 📦 기술 스택

### Backend
- **Kotlin + Spring Boot 3.5** (WebFlux 비동기)
- **Clean Architecture** (domain/usecase/adapter 분리)
- **OpenRouter API** (Spring AI 없이 직접 WebClient 구현)
- **PostgreSQL + Redis** (운영), H2 (개발/테스트)
- **JPA + QueryDSL** (ORM)

### Frontend (예정)
- **Android Native** (Kotlin + Jetpack Compose)
- **MVVM 패턴**

## 🏗️ 프로젝트 구조 (Clean Architecture)
```
com.aicounseling.app/
├── core/
│   ├── domain/           # 순수 엔티티 (데이터만)
│   └── usecase/         
│       ├── port/
│       │   ├── in/      # UseCase 인터페이스
│       │   └── out/     # Repository 인터페이스
│       └── service/     # UseCase 구현체
├── adapter/
│   ├── in/
│   │   └── web/         # Controller, REST API
│   └── out/
│       ├── persistence/ # JPA Repository 구현
│       └── ai/         # OpenRouter 연동
├── config/             # Spring 설정
└── common/            # 공통 유틸, 예외처리
```

## 🔄 구현 순서 (Clean Architecture)
1. **Domain Entity** - 순수 데이터 클래스
2. **UseCase Interface** (port/in) - 비즈니스 로직 인터페이스
3. **Repository Interface** (port/out) - 데이터 접근 인터페이스
4. **Service** (usecase/service) - UseCase 구현체
5. **Repository 구현** (adapter/out/persistence) - JPA Repository
6. **Controller** (adapter/in/web) - REST API

## 💬 OpenRouter 설정
- **엔드포인트**: `https://openrouter.ai/api/v1/chat/completions`
- **모델**: openai/gpt-4o-mini 또는 openai/gpt-3.5-turbo (비용 효율)
- **타임아웃**: 60초
- **Max Tokens**: 2000 (상담 답변용)
- **필수 헤더**: Authorization, HTTP-Referer, X-Title

## ⚠️ 사용자 성향
1. **학생** - 정석적인 개발 프로세스 학습 목적
2. **간단명료한 설명 선호** - 장황한 설명 싫어함
3. **원인 분석 중시** - "왜 그런지" 설명 필요
4. **최신 기술 선호** - 2025년 기준 베스트 프랙티스
5. **초보자** - 기본 개념도 모를 수 있음 (인터페이스, DTO 등)

## 📋 엔티티 구조
- **User**: 사용자 정보, 소셜 로그인
- **Counselor**: 상담사 정보, AI 프롬프트
- **ChatSession**: 대화 세션, 요약
- **Message**: 개별 메시지, 북마크
- **SessionSummary**: 세션 요약 (예정)
- **CounselingCategory**: 육아, 연애, 정신건강 등 (예정)
- **UserFavoriteCounselor**: 선호 상담사 (예정)
- **CounselorRating**: 평점 (예정)
- **CounselorReview**: 리뷰 (예정)

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
8. Repository 인터페이스 없이 Service 구현 → **인터페이스 먼저**