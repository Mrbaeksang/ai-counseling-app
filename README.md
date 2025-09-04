<div align="center">

# 🧠 AI 철학 상담 앱

### **AI 철학자들과 함께하는 맞춤형 상담 서비스**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Tests](https://img.shields.io/badge/Tests-103%20Passed-success)](https://github.com/Mrbaeksang/ai-counseling-app/actions)
[![License](https://img.shields.io/badge/License-Private-red)]()

**[API 명세서](docs/api-specification.yaml)** | **[시스템 아키텍처](docs/system-architecture.md)** | **[ERD](docs/erd-diagram.md)** | **[유스케이스](docs/use-case-diagram.md)** | **[요구사항](docs/SRS.md)**

</div>

---

## 📱 **프로젝트 소개**

역사적 철학자들의 사상과 화법을 AI로 구현한 1:1 맞춤형 상담 서비스입니다.

### **주요 특징**
- 🎭 **30명 이상의 철학자/상담사** - 소크라테스, 프로이트, 카사노바, 공자 등
- 🤖 **5단계 상담 프로세스** - AI가 자동으로 상담 단계 전환
- 🔐 **OAuth 2.0 소셜 로그인** - Google, Kakao, Naver
- ⚡ **비동기 처리** - WebFlux 기반 높은 처리량

---

## 🛠️ **기술 스택**

| 분야 | 기술 |
|------|------|
| **Backend** | Kotlin 1.9.25, Spring Boot 3.5.4, Spring WebFlux |
| **Database** | PostgreSQL (Production), H2 (Development) |
| **AI** | OpenRouter API (meta-llama/llama-3.2-3b-instruct) |
| **Architecture** | DDD, Layered Architecture, JWT + OAuth2 |
| **Testing** | JUnit 5, MockK, Spring MockMvc |
| **Code Quality** | Ktlint, Detekt, JaCoCo |

---

## 📁 **프로젝트 구조**

```
src/main/kotlin/com/aicounseling/app/
├── domain/                    # 비즈니스 도메인 (DDD)
│   ├── user/                 # 사용자 관리
│   ├── counselor/           # 상담사 관리
│   └── session/            # 세션 및 메시지
├── global/                  # 공통 관심사
│   ├── auth/               # OAuth 인증
│   ├── config/             # Spring 설정
│   ├── security/           # JWT 보안
│   ├── openrouter/         # AI API 연동
│   └── exception/          # 전역 예외 처리
└── AiCounselingApplication.kt
```

---

## 🚀 **시작하기**

### **환경 설정**

```bash
# 1. 저장소 클론
git clone https://github.com/Mrbaeksang/ai-counseling-app.git

# 2. 환경 변수 설정 (.env)
OPENROUTER_API_KEY=your_api_key_here
JWT_SECRET=your_secret_key
DB_URL=jdbc:postgresql://localhost:5432/aicounseling
DB_USERNAME=your_username
DB_PASSWORD=your_password

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. 테스트 실행
./gradlew test
```

---

## 🌟 **주요 기능**

### **✅ Phase 1 - 완료 (2025년 1월)**
- OAuth 2.0 소셜 로그인 (Google, Kakao, Naver)
- JWT 기반 인증 시스템
- 상담사 목록 조회 및 상세 정보
- AI와 실시간 대화 (5단계 상담 프로세스)
- 세션 북마크 및 제목 관리
- 세션 평가 시스템 (1-10점)
- 즐겨찾기 상담사 관리
- 103개 테스트 통과 (100% 성공률)

### **📋 Phase 2 - 개발 예정**
- 대화 내용 검색
- 세션 요약 기능
- 상담 통계 대시보드
- Android 클라이언트 앱

---

## 🌐 **API 엔드포인트**

### **인증 API**
| 메소드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/login/google` | Google 로그인 |
| POST | `/api/auth/login/kakao` | Kakao 로그인 |
| POST | `/api/auth/login/naver` | Naver 로그인 |
| POST | `/api/auth/refresh` | JWT 토큰 갱신 |

### **상담사 API**
| 메소드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/counselors` | 상담사 목록 조회 |
| GET | `/api/counselors/{id}` | 상담사 상세 정보 |
| POST | `/api/counselors/{id}/favorite` | 즐겨찾기 추가 |
| DELETE | `/api/counselors/{id}/favorite` | 즐겨찾기 제거 |

### **세션 API**
| 메소드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/sessions` | 내 세션 목록 조회 |
| POST | `/api/sessions` | 새 세션 시작 |
| POST | `/api/sessions/{id}/messages` | 메시지 전송 |
| POST | `/api/sessions/{id}/rating` | 세션 평가 |

> 📖 **전체 API 문서**: [api-specification.yaml](docs/api-specification.yaml)

---

## 🧪 **테스트 결과**

```
Test Results: 103 passed (100%)
├── AuthControllerTest: 15 passed ✅
├── UserControllerTest: 12 passed ✅
├── CounselorControllerTest: 18 passed ✅
├── ChatSessionControllerTest: 25 passed ✅
├── Service Layer Tests: 20 passed ✅
└── Integration Tests: 13 passed ✅
```

---

## 🤝 **기여 방법**

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: 놀라운 기능 추가'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

---

## 📝 **라이센스**

Private Repository - 상업적 사용 금지

---

## 👨‍💻 **개발자**

**Baek Sang** - Backend Developer  
[GitHub](https://github.com/Mrbaeksang) | [LinkedIn](#) | [Email](mailto:your-email@example.com)

---

<div align="center">

**Built with ❤️ using Spring Boot & Kotlin**

</div>