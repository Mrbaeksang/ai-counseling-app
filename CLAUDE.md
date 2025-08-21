# AI 상담 서비스 - 프로젝트 지침서

## 프로젝트 개요
AI 기반 심리 상담 서비스 백엔드 API (Spring Boot 3.5.4 + Kotlin 1.9.25)

## 기술 스택
- **Framework**: Spring Boot 3.5.4
- **Language**: Kotlin 1.9.25
- **Database**: PostgreSQL / H2 (개발용)
- **AI Model**: OpenRouter API (다양한 LLM 모델 활용)
- **Testing**: Kotest, MockK, Spring Boot Test
- **Code Quality**: Ktlint, Detekt
- **CI/CD**: GitHub Actions (AI PR 리뷰 자동화)

## 프로젝트 구조
```
src/main/kotlin/com/aicounseling/app/
├── domain/                    # 도메인별 패키지 (Feature-based)
│   ├── auth/                 # 인증/인가
│   │   ├── controller/
│   │   ├── service/
│   │   └── dto/
│   ├── counselor/            # 상담사 관리
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── controller/
│   │   └── dto/
│   ├── session/              # 상담 세션
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── controller/
│   │   └── dto/
│   └── user/                 # 사용자 관리
│       ├── entity/
│       ├── repository/
│       ├── service/
│       ├── controller/
│       └── dto/
└── global/                    # 공통 컴포넌트
    ├── aspect/               # AOP (ResponseAspect)
    ├── config/               # 설정 (JPA, Security, OpenAPI)
    ├── constants/            # 상수 (AppConstants)
    ├── entity/               # BaseEntity
    ├── exception/            # 전역 예외 처리
    ├── openrouter/           # OpenRouter API 클라이언트
    ├── pagination/           # 페이징 유틸리티
    ├── rq/                   # Request 컨텍스트
    ├── rsData/               # 표준 응답 포맷
    └── security/             # JWT, 인증
```

## 핵심 비즈니스 로직

### 1. 상담 프로세스 (5단계 심리학 모델)
```kotlin
enum class CounselingPhase(val koreanName: String) {
    ENGAGEMENT("관계 형성"),                    // 라포 형성, 신뢰 구축
    ASSESSMENT_AND_CONCEPTUALIZATION("평가 및 개념화"), // 문제 탐색
    INTERVENTION_AND_SKILL_BUILDING("개입 및 기술 구축"), // 해결책 제시
    ACTION_AND_GENERALIZATION("실행 및 일반화"),      // 실천 계획
    TERMINATION_AND_RELAPSE_PREVENTION("종결 및 재발 방지") // 마무리
}
```

### 2. 주요 특징
- **다중 세션 지원**: 사용자당 여러 개의 활성 세션 허용 (ChatGPT처럼)
- **상담 단계 추적**: 각 메시지별로 AI가 상담 단계 판단
- **상담사 페르소나**: 각 상담사별 고유한 성격과 프롬프트
- **평가 시스템**: 세션별 평가 및 피드백

## API 엔드포인트 (8개 핵심)

### 세션 관리 (/api/sessions)
1. `GET /sessions` - 세션 목록 조회 (페이징, 북마크 필터)
2. `POST /sessions` - 새 세션 시작
3. `DELETE /sessions/{id}` - 세션 종료
4. `GET /sessions/{id}/messages` - 메시지 목록 조회
5. `POST /sessions/{id}/messages` - 메시지 전송 (AI 응답 포함)
6. `POST /sessions/{id}/rate` - 세션 평가
7. `PATCH /sessions/{id}/bookmark` - 북마크 토글
8. `PATCH /sessions/{id}/title` - 제목 수정

## 테스트 전략

### Kotest BehaviorSpec 예제
```kotlin
@SpringBootTest
class ChatSessionControllerTest : BehaviorSpec({
    Given("인증된 사용자가") {
        When("세션 목록을 조회할 때") {
            Then("페이징된 세션 목록을 반환한다") {
                // 테스트 구현
            }
        }
    }
})
```

### 테스트 실행
```bash
./gradlew test           # 전체 테스트
./gradlew kotest        # Kotest만 실행
./gradlew check-all     # Ktlint + Detekt + Test
```

## 코드 품질 도구

### Ktlint (코드 스타일)
```bash
./gradlew ktlintCheck    # 검사
./gradlew ktlintFormat   # 자동 수정
```

### Detekt (정적 분석)
```bash
./gradlew detekt         # 코드 품질 분석
```

## GitHub Actions AI PR 리뷰

### 설정 (.github/workflows/ai-pr-review.yml)
- **모델 사용**:
  - `qwen/qwen3-coder:free` - 코드 리뷰 (Services, Controllers)
  - `deepseek/deepseek-r1-0528-qwen3-8b:free` - 구조 분석 (Entities, DTOs)
  - `deepseek/deepseek-r1-0528:free` - 아키텍처 분석

### GitHub Secrets 설정 필요
- `OPENROUTER_API_KEY`: OpenRouter API 키

## 환경 설정

### 필수 환경 변수 (.env)
```env
# OpenRouter API
OPENROUTER_API_KEY=your-api-key

# JWT
JWT_SECRET=your-jwt-secret

# Database (Production)
DATABASE_URL=postgresql://user:password@localhost:5432/dbname
```

### application.yml 주요 설정
```yaml
spring:
  profiles:
    active: local  # local, dev, prod
  
  jpa:
    hibernate:
      ddl-auto: validate  # production에서는 validate 사용
    properties:
      hibernate:
        default_batch_fetch_size: 100
        
openrouter:
  api:
    key: ${OPENROUTER_API_KEY}
    url: https://openrouter.ai/api/v1
```

## 주요 명령어

### 빌드 및 실행
```bash
./gradlew build          # 빌드
./gradlew bootRun        # 실행
./gradlew clean build    # 클린 빌드
```

### Git 작업 흐름
```bash
# 새 기능 개발
git checkout -b feature/기능명
git add .
git commit -m "feat: 기능 설명"
git push -u origin feature/기능명
gh pr create  # PR 생성 (AI 리뷰 자동 실행)
```

## 현재 작업 상태

### 완료된 작업
- ✅ 세션 API 8개 엔드포인트 구현
- ✅ GitHub Actions AI PR 리뷰 설정
- ✅ 글로벌 아키텍처 컴포넌트 정리
- ✅ Kotest 테스트 프레임워크 도입
- ✅ 다중 세션 지원 구현

### 진행 중/예정
- 🔄 DTO 클래스 타입 불일치 수정
- 📝 통합 테스트 작성
- 🔐 OAuth 앱 등록 및 설정
- 🚀 배포 환경 구성

## 주의사항

### 코드 작성 시
1. **Kotlin 관용구 사용**: data class, extension functions, scope functions
2. **Spring Boot 베스트 프랙티스**: Constructor injection, @Transactional 적절히 사용
3. **테스트 우선**: 모든 Service 메서드에 대한 테스트 작성
4. **보안**: 절대 시크릿 키를 코드에 하드코딩하지 않음

### PR 제출 전
1. `./gradlew ktlintFormat` 실행
2. `./gradlew test` 통과 확인
3. 의미 있는 커밋 메시지 작성
4. PR 템플릿 활용

## 문제 해결

### 일반적인 이슈
1. **Ktlint 오류**: `./gradlew ktlintFormat --daemon`으로 자동 수정
2. **테스트 실패**: MockK 설정 확인, @Transactional 추가
3. **컴파일 오류**: DTO와 Entity 간 타입 매칭 확인

### 디버깅 팁
- **로그 레벨 조정**: `application-local.yml`에서 DEBUG 레벨 설정
- **H2 콘솔**: http://localhost:8080/h2-console (개발 환경)
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## 참고 자료
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)
- [OpenRouter API 문서](https://openrouter.ai/docs)
- [Kotest 문서](https://kotest.io/)

---
*Last Updated: 2024-12-21*
*Version: 1.0.0*