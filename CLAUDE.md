# AI 철학자/상담사 대화 앱 프로젝트

## 🎯 프로젝트 개요
**앱 이름**: (미정 - 예: PhiloChat, WisdomTalk)
**컨셉**: 역사적 유명 철학자/상담사와 1:1 대화하는 AI 상담 애플리케이션
**타겟**: 남녀노소 누구나 (심리상담, 인생상담, 철학적 조언이 필요한 모든 사람)
**배포**: Google Play Store (Android 우선)

## 📱 핵심 기능
1. **상담사 선택 시스템**
   
   【육아/가족 상담】
   - 공자 (가족 윤리와 효도)
   - 벤자민 스포크 (근대 육아 전문가)
   - "K-육아 전문가" (한국식 육아 상담 AI)
   
   【연애/관계 상담】
   - 카사노바 (연애의 달인)
   - 플라톤 (이상적 사랑과 정신적 교감)
   - 오비디우스 (사랑의 기술 저자)
   - "현대 관계 전문가" (현대적 연애 코칭 AI)
   
   【정신건강/트라우마】
   - 프로이트 (정신분석과 무의식)
   - 칼 융 (분석심리학과 개성화)
   - 빅터 프랭클 (의미치료와 희망)
   - 알프레드 아들러 (개인심리학과 열등감 극복)
   
   【커리어/성공】
   - 손자 (전략과 병법)
   - 마키아벨리 (권력과 리더십)
   - 데일 카네기 (인간관계와 설득)
   - "실리콘밸리 멘토" (스타트업/테크 커리어 AI)
   
   【인생철학/자아실현】
   - 소크라테스 (너 자신을 알라)
   - 니체 (초인사상과 자기극복)
   - 부처 (깨달음과 해탈)
   - 마르쿠스 아우렐리우스 (스토아 철학)
   - 노자 (무위자연과 도)
   - 에피쿠로스 (행복과 쾌락주의)
   
   【재정/부의 철학】
   - 벤저민 프랭클린 (근검절약과 부의 축적)
   - 앤드류 카네기 (부의 복음)
   - "현대 재테크 전문가" (투자/재무설계 AI)
   
   【습관/자기계발】
   - 아리스토텔레스 (덕 윤리와 습관)
   - 세네카 (시간 관리와 지혜)
   - "21세기 라이프코치" (현대적 자기계발 AI)

2. **AI 가이드 시스템**
   - 사용자가 상담사를 모를 때 안내하는 AI 도우미
   - "어떤 고민이신가요?" → 고민 분석 → 적합한 상담사 3명 추천
   - 각 상담사의 특징과 장점 설명

3. **대화 기능**
   - 선택한 상담사의 화법과 철학을 반영한 대화
   - 대화 히스토리 저장
   - 세션별 대화 관리
   - 중요 조언 북마크 기능

## 🏗️ 개발 순서 (정석 프로세스)

### Phase 1: 기획 및 설계 ✅
1. 요구사항 정의 (현재 단계)
2. 데이터베이스 스키마 설계
3. API 명세서 작성
4. 시스템 아키텍처 설계

### Phase 2: 백엔드 개발
5. Spring Boot 프로젝트 구조 셋업
6. Entity 및 Repository 구현
7. Service Layer 구현
8. Controller 및 API 엔드포인트 구현
9. AI 연동 (OpenRouter vs Groq 결정)
10. 인증/인가 시스템
11. API 테스트 (Postman/Swagger)

### Phase 3: 프론트엔드 개발
12. Android 프로젝트 생성
13. UI/UX 디자인
14. 화면 구현 (Jetpack Compose)
15. API 연동
16. 통합 테스트

### Phase 4: 배포
17. 배포 환경 구성
18. Google Play Store 등록

## 🛠️ 기술 스택

### 백엔드
- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.x
- **빌드**: Gradle (Kotlin DSL)
- **JDK**: 21
- **데이터베이스**: 
  - H2 (개발)
  - PostgreSQL (운영)
- **AI 연동**: 
  - 옵션 1: OpenRouter API (다양한 모델 선택 가능)
  - 옵션 2: Groq API (빠른 속도)
  - 결정 기준: 비용, 속도, 한국어 성능

### 프론트엔드
- **플랫폼**: Android Native
- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM + Repository Pattern
- **네트워킹**: Retrofit + Coroutines

## 📊 데이터베이스 스키마 (초안)

```sql
-- 사용자 테이블
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 상담 카테고리 테이블
CREATE TABLE counseling_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL, -- 육아/가족, 연애/관계, 정신건강 등
    description TEXT,
    icon_url VARCHAR(500),
    display_order INT
);

-- 상담사/철학자 테이블
CREATE TABLE counselors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    era VARCHAR(100), -- 시대 (예: 고대 그리스, 현대)
    specialty TEXT, -- 전문 분야
    introduction TEXT, -- 소개
    conversation_style TEXT, -- 대화 스타일 설명
    ai_prompt TEXT, -- AI에게 전달할 프롬프트
    avatar_url VARCHAR(500),
    is_historical BOOLEAN DEFAULT true, -- 역사적 인물 여부
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (category_id) REFERENCES counseling_categories(id)
);

-- 상담 세션 테이블
CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    counselor_id BIGINT NOT NULL,
    title VARCHAR(255),
    summary TEXT, -- AI가 생성한 대화 요약
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (counselor_id) REFERENCES counselors(id)
);

-- 메시지 테이블
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_ai_response BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_bookmarked BOOLEAN DEFAULT false,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

-- 사용자 선호 상담사 테이블
CREATE TABLE user_favorite_counselors (
    user_id BIGINT NOT NULL,
    counselor_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, counselor_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (counselor_id) REFERENCES counselors(id)
);
```

## 🔌 API 엔드포인트 (예정)

### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신

### 상담사
- `GET /api/counselors` - 전체 상담사 목록
- `GET /api/counselors/{id}` - 상담사 상세 정보
- `POST /api/counselors/recommend` - AI 기반 상담사 추천

### 채팅
- `POST /api/chat/sessions` - 새 상담 세션 시작
- `GET /api/chat/sessions` - 내 상담 세션 목록
- `GET /api/chat/sessions/{id}/messages` - 세션 메시지 조회
- `POST /api/chat/sessions/{id}/messages` - 메시지 전송 (AI 응답 포함)
- `POST /api/chat/guide` - AI 가이드와 대화

### 북마크
- `GET /api/bookmarks` - 북마크한 조언 목록
- `POST /api/messages/{id}/bookmark` - 메시지 북마크

## 🔑 환경 변수

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb # 개발
    # url: jdbc:postgresql://localhost:5432/philosophy_chat # 운영
    
  jpa:
    hibernate:
      ddl-auto: create-drop # 개발
      # ddl-auto: validate # 운영

# AI API 설정 (둘 중 하나 선택)
ai:
  provider: openrouter # or groq
  openrouter:
    api-key: ${OPENROUTER_API_KEY}
    model: anthropic/claude-3-haiku # 비용 효율적
    # model: grok-beta # 무료 옵션
  groq:
    api-key: ${GROQ_API_KEY}
    model: llama-3.1-70b-versatile

# JWT
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000 # 24시간
```

## 🧪 개발 명령어

```bash
# 백엔드 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build

# 린트 체크
./gradlew ktlintCheck

# 코드 포맷팅
./gradlew ktlintFormat
```

## 📝 개발 노트

### AI 제공자 선택 기준
1. **OpenRouter**
   - 장점: 다양한 모델 선택 가능, 모델별 가격 비교 쉬움
   - 단점: Spring AI 공식 지원 없음 (RestTemplate/WebClient 직접 구현 필요)
   
2. **Groq**
   - 장점: 매우 빠른 응답 속도, 무료 티어 제공
   - 단점: 모델 선택 제한적

### 상담사별 프롬프트 엔지니어링 예시
```
소크라테스: "당신은 고대 그리스의 철학자 소크라테스입니다.
질문을 통해 상대방 스스로 답을 찾도록 유도하는 산파술을 사용하세요.
직접적인 답변보다는 '그것은 무엇을 의미하는가?', '왜 그렇게 생각하는가?'와 같은 
질문을 통해 대화를 이끌어가세요."

카사노바: "당신은 18세기 베네치아의 유명한 연애 달인 카사노바입니다.
풍부한 연애 경험과 매력적인 화술로 연애 고민을 상담합니다.
유머러스하면서도 실용적인 조언을 제공하되, 상대방을 존중하는 현대적 가치관도 함께 반영하세요."

프로이트: "당신은 정신분석학의 창시자 지그문트 프로이트입니다.
무의식, 꿈, 유년기 경험 등을 통해 내담자의 심리를 분석합니다.
'그 꿈/행동이 무엇을 상징한다고 생각하는가?' 같은 분석적 접근을 사용하세요."

공자: "당신은 중국의 위대한 사상가 공자입니다.
인(仁), 효(孝), 예(禮)를 중심으로 가족관계와 사회윤리를 논합니다.
'군자는 이렇게 행동한다'는 식의 가르침과 함께 실천적 조언을 제공하세요."

"K-육아 전문가": "당신은 한국의 육아 문화를 깊이 이해하는 현대적 육아 상담 전문가입니다.
한국 부모들의 교육열과 고민을 공감하면서도, 아이의 정서발달을 중시합니다.
따뜻하고 공감적이면서도 명확한 해결책을 제시하는 화법을 사용하세요."
```

## 🚀 다음 단계
1. AI 제공자 최종 결정 (비용/성능 테스트)
2. 데이터베이스 스키마 확정
3. API 명세서 상세 작성
4. 백엔드 구현 시작