# Tasks: 뱁둥이봇 웹훅 연동 (Issue #325)

**Issue**: [#325 [Backend] 뱁둥이봇 연동](https://github.com/Yun-YeoJun/IGRUS-Web/issues/325)
**Prerequisites**: 회원가입(Signup) + 이메일 인증(VerifyEmail) 기능 구현 완료
**Tech Stack**: Java 21, Spring Boot 3.5.9, RestClient, Spring Retry, Spring Async
**Tests**: 테스트 코드 작성 포함 (backend/CLAUDE.md 개발 규칙에 따름)

## 개요

회원가입 완료(이메일 인증 성공) 시 뱁둥이봇 웹훅 엔드포인트를 호출하여 신규 가입자 정보를 전송한다.

### 웹훅 스펙

- **Endpoint**: `POST https://v2zb74shbf.execute-api.ap-northeast-2.amazonaws.com/prod/api/webhooks/submissions`
- **인증**: `X-Webhook-Secret` 헤더
- **필수 필드**: `name`, `studentId`
- **선택 필드**: `email`, `department`, `phone`, `gender`, `grade`, `enrollmentStatus`, `hasPaid`, `submittedAt`

### 트리거 시점

`VerifyEmailService.verifyEmail()` 성공 후 — User 상태가 `PENDING_VERIFICATION` → `ACTIVE`로 변경된 직후

## Format: `[ID] [P?] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/`
- **Tests**: `backend/src/test/java/igrus/web/`
- **Config**: `backend/src/main/resources/`

## 데이터 매핑 분석

| 웹훅 필드 | User 엔티티 필드 | 변환 | 비고 |
|-----------|-----------------|------|------|
| `name` (필수) | `User.name` | 그대로 | - |
| `studentId` (필수) | `User.studentId` | 그대로 | - |
| `email` | `User.email` | 그대로 | - |
| `department` | `User.department` | 그대로 | - |
| `phone` | `User.phoneNumber` | 그대로 | 형식 동일 (000-0000-0000) |
| `gender` | `User.gender` | MALE→"남", FEMALE→"여" | enum 변환 필요 |
| `grade` | `User.grade` | 1→"1학년", 2→"2학년" 등 | int→String 변환 |
| `enrollmentStatus` | 없음 | - | 미존재 필드, 논의 필요 |
| `hasPaid` | 없음 | - | 미존재 필드, 논의 필요 |
| `submittedAt` | `User.createdAt` | Instant→ISO 8601 String | - |

### 미존재 필드 처리 방안

`enrollmentStatus`와 `hasPaid`는 현재 User 엔티티에 없다. 두 필드 모두 웹훅에서 필수가 아니므로 아래 방안 중 선택 필요:

- **방안 A (권장)**: null로 전송 — 추후 가입 폼에 필드 추가 시 별도 이슈로 처리
- **방안 B**: 회원가입 폼(PasswordSignupRequest)에 두 필드 추가 — 이 이슈 범위를 확장

---

## Phase 1: 설정 및 인프라 (Configuration & Infrastructure)

**Purpose**: 웹훅 호출을 위한 설정과 RestClient 인프라 구축

### 1.1 Application Properties 설정

- [ ] T001 application.yml에 웹훅 설정 추가 in `backend/src/main/resources/application.yml`
  - `app.webhook.babdungi.url`: 웹훅 엔드포인트 URL
  - `app.webhook.babdungi.secret`: 웹훅 시크릿 (환경변수로 주입)
  - `app.webhook.babdungi.enabled`: 웹훅 활성화 여부 (기본: true)
  - `app.webhook.babdungi.timeout`: 타임아웃 (기본: 5000ms)

- [ ] T002 [P] application-local.yml에 로컬 개발용 웹훅 설정 추가 in `backend/src/main/resources/application-local.yml`
  - `app.webhook.babdungi.enabled: false` — 로컬 환경에서는 비활성화

### 1.2 설정 클래스

- [ ] T003 BabdungiWebhookProperties 설정 클래스 생성 in `backend/src/main/java/igrus/web/common/config/BabdungiWebhookProperties.java`
  - `@ConfigurationProperties(prefix = "app.webhook.babdungi")`
  - 필드: `url` (String), `secret` (String), `enabled` (boolean), `timeout` (int)

### 1.3 RestClient 빈 설정

- [ ] T004 WebhookConfig 설정 클래스 생성 in `backend/src/main/java/igrus/web/common/config/WebhookConfig.java`
  - `@Configuration`
  - `babdungiRestClient` 빈: RestClient.builder()로 생성
    - baseUrl 설정
    - 기본 헤더: `Content-Type: application/json`, `X-Webhook-Secret: {secret}`
    - 타임아웃 설정 (connect: 3초, read: 5초)
  - `@ConditionalOnProperty(name = "app.webhook.babdungi.enabled", havingValue = "true", matchIfMissing = true)` — enabled=false일 때 빈 생성 안 함

### 1.4 비동기 실행기

- [ ] T005 AsyncConfig에 webhookTaskExecutor 추가 in `backend/src/main/java/igrus/web/common/config/AsyncConfig.java`
  - Bean name: `webhookTaskExecutor`
  - CorePoolSize: 1, MaxPoolSize: 3, QueueCapacity: 50
  - ThreadNamePrefix: "webhook-"

---

## Phase 2: 서비스 구현 (Service Implementation)

**Purpose**: 웹훅 호출 서비스 인터페이스 및 구현체 생성

### 2.1 DTO

- [ ] T006 [P] BabdungiSubmissionRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/BabdungiSubmissionRequest.java`
  - record 타입
  - 필드: `name`, `studentId`, `email`, `department`, `phone`, `gender`, `grade`, `enrollmentStatus`, `hasPaid`, `submittedAt`
  - 모든 필드 String 타입 (웹훅 API 스펙에 맞춤)
  - `static fromUser(User user)` 팩토리 메서드: User 엔티티 → DTO 변환
    - `gender`: MALE→"남", FEMALE→"여"
    - `grade`: int→"{N}학년" 문자열 변환
    - `enrollmentStatus`: null (미존재 필드)
    - `hasPaid`: null (미존재 필드)
    - `submittedAt`: `user.getCreatedAt()` → ISO 8601 문자열

- [ ] T007 [P] BabdungiSubmissionResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/BabdungiSubmissionResponse.java`
  - record 타입
  - 성공: `success` (boolean), `submissionId` (String), `message` (String)
  - 실패: `error` (String)

### 2.2 서비스 인터페이스

- [ ] T008 BabdungiWebhookService 인터페이스 생성 in `backend/src/main/java/igrus/web/security/auth/common/service/BabdungiWebhookService.java`
  - `void sendSubmission(User user)` — 회원 가입 정보를 웹훅으로 전송

### 2.3 프로덕션 구현체

- [ ] T009 RestClientBabdungiWebhookService 구현 in `backend/src/main/java/igrus/web/security/auth/common/service/RestClientBabdungiWebhookService.java`
  - `@Service`, `@Slf4j`
  - `@Profile("!local & !test")` 또는 `@ConditionalOnBean(name = "babdungiRestClient")`
  - `@Async("webhookTaskExecutor")` — 비동기 실행
  - `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000))`
    - 재시도 대상: 5xx 서버 에러, 타임아웃, 네트워크 에러
    - 재시도 제외: 4xx 클라이언트 에러 (잘못된 요청)
  - RestClient를 사용한 POST 요청
  - 응답 로깅 (submissionId 등)
  - `@Recover` 메서드: 최종 실패 시 ERROR 로그 기록 (회원가입 자체에는 영향 없음)

### 2.4 로컬/테스트 구현체

- [ ] T010 LoggingBabdungiWebhookService 구현 in `backend/src/main/java/igrus/web/security/auth/common/service/LoggingBabdungiWebhookService.java`
  - `@Service`, `@Slf4j`
  - `@Profile({"local", "test"})` 또는 `@ConditionalOnMissingBean(name = "babdungiRestClient")`
  - 실제 HTTP 호출 없이 INFO 로그만 출력
  - 로그 내용: 학번, 이름, 이메일 등 전송할 데이터 출력

---

## Phase 3: 통합 (Integration)

**Purpose**: 이메일 인증 완료 시 웹훅 호출 연동

- [ ] T011 VerifyEmailService에 BabdungiWebhookService 주입 및 호출 추가 in `backend/src/main/java/igrus/web/security/auth/password/service/signup/VerifyEmailService.java`
  - `BabdungiWebhookService` 의존성 주입 (생성자 주입)
  - `user.verifyEmail()` 및 저장 성공 후 `babdungiWebhookService.sendSubmission(user)` 호출
  - 웹훅 호출 실패가 이메일 인증 응답에 영향을 주지 않아야 함 (비동기이므로 자연스럽게 보장)

---

## Phase 4: 테스트 (Testing)

**Purpose**: 웹훅 연동 기능의 정확성 검증

### 4.1 단위 테스트

- [ ] T012 [P] BabdungiSubmissionRequest.fromUser() 변환 테스트 in `backend/src/test/java/igrus/web/security/auth/common/dto/BabdungiSubmissionRequestTest.java`
  - User 엔티티 → DTO 변환 정확성 검증
  - gender 변환: MALE→"남", FEMALE→"여"
  - grade 변환: 1→"1학년", 2→"2학년" 등
  - null 필드 처리 (enrollmentStatus, hasPaid)
  - submittedAt ISO 8601 형식 검증

- [ ] T013 [P] RestClientBabdungiWebhookService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/common/service/RestClientBabdungiWebhookServiceTest.java`
  - Mock RestClient 사용
  - 성공 시나리오: 200 응답 처리
  - 실패 시나리오: 4xx/5xx 에러 처리
  - 요청 헤더/바디 정확성 검증

### 4.2 통합 테스트

- [ ] T014 VerifyEmailService 통합 테스트 보강 in `backend/src/test/java/igrus/web/security/auth/password/service/signup/VerifyEmailServiceTest.java`
  - 이메일 인증 성공 시 BabdungiWebhookService.sendSubmission() 호출 검증
  - 이메일 인증 실패 시 웹훅 미호출 검증
  - Mock BabdungiWebhookService 사용 (외부 HTTP 호출 방지)

---

## Phase 5: 검증 (Verification)

- [ ] T015 전체 빌드 및 테스트 실행
  - `./gradlew build` 성공 확인
  - 기존 테스트 깨지지 않음 확인
  - 로컬 프로파일에서 LoggingBabdungiWebhookService 동작 확인

---

## 구현 순서 요약

```
T001, T002 (설정)
    ↓
T003, T004, T005 (인프라, 병렬 가능)
    ↓
T006, T007 (DTO, 병렬 가능)
    ↓
T008 (인터페이스)
    ↓
T009, T010 (구현체, 병렬 가능)
    ↓
T011 (통합)
    ↓
T012, T013 (단위 테스트, 병렬 가능)
    ↓
T014 (통합 테스트)
    ↓
T015 (전체 검증)
```

## 설계 결정사항

### 1. 패키지 위치

`security.auth.common.service` — 기존 `SmtpAuthEmailService`와 동일한 위치. 회원가입 인증 도메인에 속하는 알림 서비스이므로.

### 2. 프로파일 기반 빈 전환

기존 이메일 서비스 패턴 따름:
- 프로덕션: `@Profile("!local & !test")` — RestClient 사용
- 로컬/테스트: `@Profile({"local", "test"})` — 로그만 출력

### 3. 비동기 + 재시도

- `@Async("webhookTaskExecutor")` — 회원가입 응답 지연 방지
- `@Retryable` — 일시적 네트워크 오류 대응 (3회, 2초→4초→8초)
- `@Recover` — 최종 실패 시 로그 기록만 (회원가입 자체는 영향 없음)

### 4. RestClient 선택 이유

Spring Boot 3.x 권장 동기 HTTP 클라이언트. 외부 라이브러리 의존 없이 Spring 기본 제공. `@Async`와 결합하여 비동기 처리.

### 5. Fire-and-Forget 방식

웹훅 호출 실패가 회원가입 프로세스에 영향을 주지 않는다. 실패 시 로그만 남기고, 재시도 후에도 실패하면 수동 대응.

---

## 논의 필요 사항

1. **`enrollmentStatus` / `hasPaid` 필드**: 현재 User 엔티티에 없는 필드. null로 전송할지, 가입 폼에 추가할지 결정 필요.
2. **웹훅 시크릿 관리**: 프로덕션 환경에서 AWS Secrets Manager를 통해 주입할지, 환경변수로 직접 설정할지 결정 필요.
3. **모니터링**: 웹훅 실패 시 알림 체계 (로그만으로 충분한지, 별도 모니터링 필요한지).
