# Feature Specification: 모니터링 환경 구축

**Feature Branch**: feature/monitoring
**Created**: 2026-02-23
**Status**: Draft
**Input**: 프로덕션 운영 안정성 확보를 위한 관측성(Observability) 체계 구축

---

## 현황 분석

### 기존 모니터링 기반

| 구분 | 현재 상태 | 파일 위치 |
|------|----------|----------|
| 구조화된 로깅 | LogstashEncoder로 JSON 로깅 (prod) | `backend/src/main/resources/logback-spring.xml` |
| 요청 추적 | MDC 기반 traceId/userId 전파 | `backend/src/main/java/igrus/web/common/filter/MdcLoggingFilter.java` |
| 비동기 MDC 전파 | MdcTaskDecorator로 @Async MDC 유지 | `backend/src/main/java/igrus/web/common/config/MdcTaskDecorator.java` |
| 예외 처리 | GlobalExceptionHandler (18+ 핸들러) | `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java` |
| 헬스체크 | `/api/v1/health` 엔드포인트 | `backend/src/main/java/igrus/web/common/controller/HealthController.java` |
| 프론트엔드 에러 | ApiError 클래스 기반 에러 매핑 | `frontend/src/api/client.ts` |

### 부재 영역

- Spring Boot Actuator (메트릭, 헬스 인디케이터)
- Micrometer/Prometheus 메트릭 수집
- 에러 트래킹 서비스 (Sentry)
- 프론트엔드 성능 모니터링 (Web Vitals)
- 로그 중앙화 및 검색
- 대시보드 시각화
- 알림/경보 시스템
- 커스텀 비즈니스 메트릭

---

## User Scenarios & Testing

### User Story 1 - 장애 감지 및 대응 (Priority: P1)

운영자가 프로덕션 서비스에서 발생하는 장애를 실시간으로 감지하고, 근본 원인을 추적하여 빠르게 대응한다.

**Why this priority**: 서비스 가용성은 사용자 경험의 기본 전제이며, 장애를 빠르게 감지하지 못하면 사용자 이탈로 이어진다.

**Independent Test**: 의도적으로 500 에러를 발생시킨 후, 알림이 수신되고 Grafana 대시보드에서 에러율 스파이크가 확인되는지 검증한다.

**Acceptance Scenarios**:

1. **Given** 백엔드 서비스가 운영 중인 상태에서, **When** 500 에러가 5분간 5회 이상 발생하면, **Then** Slack 채널에 경보 알림이 발송된다
2. **Given** 에러 알림을 받은 운영자가, **When** Grafana 대시보드에 접속하면, **Then** 에러율, 응답시간, JVM 메트릭을 한눈에 확인할 수 있다
3. **Given** 에러가 발생한 요청의 traceId를 확인한 운영자가, **When** 로그 검색 시스템에서 해당 traceId를 검색하면, **Then** 해당 요청의 전체 로그 체인을 추적할 수 있다
4. **Given** ECS 태스크가 비정상 종료된 상태에서, **When** Actuator 헬스체크가 실패하면, **Then** ECS가 자동으로 태스크를 재시작하고, 운영자에게 알림이 전송된다

---

### User Story 2 - 성능 모니터링 (Priority: P1)

개발자가 API 응답시간, JVM 상태, DB 커넥션 풀 등 핵심 성능 지표를 모니터링하여 성능 저하를 사전에 감지한다.

**Why this priority**: 성능 저하는 장애로 이어질 수 있으며, 사전 감지를 통해 예방적 조치가 가능하다.

**Independent Test**: 부하 테스트 후 Prometheus 메트릭에서 HTTP 응답시간, JVM heap 사용량, DB 커넥션 수가 정상적으로 수집되는지 확인한다.

**Acceptance Scenarios**:

1. **Given** Actuator와 Micrometer가 설정된 상태에서, **When** `/actuator/prometheus` 엔드포인트에 접근하면, **Then** JVM 메모리, GC, 스레드, HTTP 요청 메트릭이 Prometheus 포맷으로 노출된다
2. **Given** API 요청이 처리되는 상태에서, **When** 특정 엔드포인트의 평균 응답시간이 2초를 초과하면, **Then** Grafana 대시보드에서 해당 엔드포인트가 하이라이트되고 알림이 발송된다
3. **Given** DB 커넥션 풀 사용률이 80%를 초과한 상태에서, **When** 메트릭이 수집되면, **Then** 경고 알림이 발송된다

---

### User Story 3 - 프론트엔드 에러 추적 (Priority: P2)

개발자가 프론트엔드에서 발생하는 JavaScript 에러와 API 호출 실패를 자동으로 수집하고, 사용자 환경 정보와 함께 분석한다.

**Why this priority**: 프론트엔드 에러는 사용자에게 직접 영향을 미치지만, 백엔드 로그에서는 감지되지 않는 경우가 많다.

**Independent Test**: 의도적으로 프론트엔드 에러를 발생시킨 후 Sentry 대시보드에서 에러가 스택 트레이스와 함께 확인되는지 검증한다.

**Acceptance Scenarios**:

1. **Given** Sentry SDK가 프론트엔드에 설정된 상태에서, **When** 처리되지 않은 JavaScript 에러가 발생하면, **Then** Sentry에 에러가 자동 리포팅되며 브라우저 정보, 소스맵 기반 스택 트레이스가 포함된다
2. **Given** API 호출이 실패한 상태에서, **When** 4xx/5xx 응답이 반환되면, **Then** Sentry에 에러 컨텍스트(URL, 상태코드, traceId)가 포함되어 리포팅된다
3. **Given** Sentry에 에러가 수집된 상태에서, **When** 개발자가 에러 상세를 확인하면, **Then** 소스맵을 통해 원본 TypeScript 코드의 정확한 위치가 표시된다

---

### User Story 4 - 프론트엔드 성능 측정 (Priority: P2)

개발자가 실제 사용자의 웹 페이지 로딩 성능(Core Web Vitals)을 측정하여 사용자 경험을 개선한다.

**Why this priority**: 페이지 로딩 속도는 사용자 만족도와 직결되며, 객관적 데이터 없이는 개선 방향을 설정하기 어렵다.

**Independent Test**: 프로덕션 배포 후 Sentry Performance에서 LCP, FID, CLS 지표가 수집되는지 확인한다.

**Acceptance Scenarios**:

1. **Given** Web Vitals 수집이 설정된 상태에서, **When** 사용자가 페이지를 로딩하면, **Then** LCP, FID, CLS, TTFB 지표가 Sentry Performance에 기록된다
2. **Given** 성능 지표가 수집된 상태에서, **When** LCP가 2.5초를 초과하는 페이지가 있으면, **Then** 해당 페이지가 성능 이슈로 표시된다

---

### User Story 5 - 비즈니스 메트릭 대시보드 (Priority: P3)

운영자가 서비스의 핵심 비즈니스 지표(회원가입, 게시글, 행사 참여 등)를 대시보드에서 확인하여 서비스 성장을 추적한다.

**Why this priority**: 기술 메트릭만으로는 서비스의 실질적 가치를 판단하기 어려우며, 비즈니스 지표는 의사결정의 기반이 된다.

**Independent Test**: 회원가입, 게시글 작성 후 Grafana 대시보드에서 해당 카운터가 증가하는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 커스텀 비즈니스 메트릭이 설정된 상태에서, **When** 신규 사용자가 회원가입하면, **Then** `igrus_user_signup_total` 카운터가 증가하고 대시보드에 반영된다
2. **Given** 비즈니스 대시보드가 구성된 상태에서, **When** 운영자가 접속하면, **Then** 일일 가입자 수, 활성 사용자 수, 게시글 수, 행사 참여율을 확인할 수 있다

---

### Edge Cases

- Actuator 엔드포인트가 외부에 노출되지 않도록 보안 설정 필수 (Prometheus 스크레이핑 경로만 허용)
- Sentry에 민감한 사용자 정보(비밀번호, 토큰 등)가 전송되지 않도록 PII scrubbing 설정
- 메트릭 수집이 서비스 성능에 영향을 주지 않도록 오버헤드 관리
- CloudWatch Logs 보존 기간 설정으로 비용 관리
- Grafana Cloud 무료 티어 한도(10K active metrics) 초과 시 대응 방안

---

## Requirements

### Functional Requirements

#### Backend 모니터링

- **FR-001**: Spring Boot Actuator를 통해 `/actuator/health`, `/actuator/info`, `/actuator/prometheus` 엔드포인트를 제공한다
- **FR-002**: Micrometer + Prometheus Registry를 통해 JVM 메트릭(heap, GC, thread), HTTP 요청 메트릭(count, duration, status), DB 커넥션 풀 메트릭을 자동 수집한다
- **FR-003**: Actuator health 엔드포인트에 DB, 디스크 공간 헬스 인디케이터를 포함한다
- **FR-004**: Sentry Java SDK를 통해 처리되지 않은 예외와 GlobalExceptionHandler에서 잡힌 에러를 자동 리포팅한다
- **FR-005**: Sentry 이벤트에 기존 MDC traceId를 태그로 포함하여 로그와 연관분석을 지원한다

#### Frontend 모니터링

- **FR-006**: Sentry React SDK를 통해 처리되지 않은 JavaScript 에러를 자동 캡처한다
- **FR-007**: Sentry에 소스맵을 업로드하여 난독화된 에러를 원본 TypeScript 코드로 매핑한다
- **FR-008**: Web Vitals(LCP, FID, CLS, TTFB)를 Sentry Performance로 수집한다
- **FR-009**: API 호출 실패 시 에러 컨텍스트(URL, status, traceId)를 Sentry에 breadcrumb으로 기록한다

#### 인프라 모니터링

- **FR-010**: ECS 태스크의 헬스체크를 Actuator `/actuator/health` 엔드포인트로 교체한다
- **FR-011**: CloudWatch Logs에서 ECS 로그를 수집하고, 로그 그룹 보존 기간을 설정한다
- **FR-012**: Grafana Cloud에 Prometheus 데이터소스를 연결하여 메트릭 대시보드를 구성한다
- **FR-013**: Grafana Loki를 통해 로그를 중앙화하고 검색 가능하게 한다
- **FR-014**: 경보 규칙을 설정하고 Slack 웹훅으로 알림을 전송한다

#### 비즈니스 메트릭

- **FR-015**: Micrometer Counter/Gauge를 사용하여 회원가입, 로그인 성공/실패, 게시글 작성, 행사 참여 등 핵심 비즈니스 이벤트를 메트릭으로 수집한다

### Key Entities

- **ActuatorEndpoint** *(Spring Boot Config)*: Actuator 엔드포인트 노출 설정 (`health`, `info`, `prometheus`)
- **MicrometerRegistry** *(Config)*: Prometheus 메트릭 레지스트리, 공통 태그 설정 (`application=igrus-web`)
- **SentryConfig** *(Backend Config)*: DSN, 환경, 샘플링 레이트, traceId 태그 설정
- **SentryInit** *(Frontend Config)*: DSN, 환경, 릴리즈 버전, 소스맵 설정
- **GrafanaDashboard** *(Infra Config)*: 대시보드 JSON 정의 (인프라, 앱, 비즈니스 패널)
- **AlertRule** *(Grafana Config)*: 경보 조건, 임계값, 알림 채널 설정

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: `/actuator/health` 엔드포인트가 DB 상태를 포함하여 200 OK를 반환한다
- **SC-002**: `/actuator/prometheus` 엔드포인트에서 JVM, HTTP, DB 메트릭이 노출된다
- **SC-003**: Grafana 대시보드에서 최근 24시간의 API 응답시간, 에러율, JVM 상태를 확인할 수 있다
- **SC-004**: 프론트엔드에서 발생한 에러가 10초 이내에 Sentry에 리포팅된다
- **SC-005**: 500 에러 발생 시 5분 이내에 Slack 알림이 수신된다
- **SC-006**: 로그 검색에서 traceId로 요청의 전체 로그 체인을 조회할 수 있다

### Non-Functional Requirements

- **NFR-001**: 모니터링 인프라로 인한 API 응답 지연은 평균 5ms 이하여야 한다 (오버헤드 < 5%)
- **NFR-002**: 메트릭 데이터 보존 기간은 최소 15일 이상이어야 한다
- **NFR-003**: 로그 데이터 보존 기간은 최소 30일 이상이어야 한다
- **NFR-004**: 경보 알림은 조건 충족 후 5분 이내에 발송되어야 한다
- **NFR-005**: Sentry에 사용자 PII(비밀번호, 토큰, 이메일)가 전송되지 않아야 한다
- **NFR-006**: Actuator 엔드포인트는 인증 없이 외부에서 직접 접근할 수 없어야 한다
- **NFR-007**: 모니터링 추가 비용은 월 $10 이하여야 한다

---

## 구현 로드맵

### Phase 1: 기본 인프라 (1~2주)

- Spring Boot Actuator + Micrometer + Prometheus 설정
- ECS 헬스체크를 Actuator로 교체
- CloudWatch Logs 로그 그룹 설정
- **대상**: FR-001, FR-002, FR-003, FR-010, FR-011

### Phase 2: 에러 트래킹 (1~2주)

- Backend Sentry SDK 설정 및 GlobalExceptionHandler 연동
- Frontend Sentry SDK + Web Vitals 설정
- CI/CD에 소스맵 업로드 추가
- **대상**: FR-004, FR-005, FR-006, FR-007, FR-008, FR-009

### Phase 3: 시각화 + 알림 (2~3주)

- Grafana Cloud 계정 설정 및 Prometheus 데이터소스 연결
- Loki로 로그 중앙화
- 대시보드 구성 (인프라/앱/비즈니스)
- Slack 알림 규칙 설정
- **대상**: FR-012, FR-013, FR-014

### Phase 4: 고급 관측성 (선택)

- 커스텀 비즈니스 메트릭 추가
- RDS Performance Insights 활성화
- 외부 Uptime 모니터링 설정
- **대상**: FR-015

---

## 비용 추정

| 구성요소 | 월간 예상 비용 | 비고 |
|---------|-------------|------|
| CloudWatch Logs | ~$3~5 | ECS 기본 연동, 보존 기간에 따라 변동 |
| Sentry Free | $0 | 5K events/월, 1GB 저장소 |
| Grafana Cloud Free | $0 | 10K active metrics, 50GB logs |
| UptimeRobot Free | $0 | 50개 모니터, 5분 간격 |
| **총 추가 비용** | **~$5/월 이하** | |

---

## Assumptions

- AWS ECS의 stdout 로그는 이미 CloudWatch Logs로 전송되고 있거나, 로그 드라이버 설정으로 쉽게 연동 가능하다
- Grafana Cloud Free Tier (10K active metrics, 50GB logs/month)가 동아리 규모에 충분하다
- Sentry Free Tier (5K events/month)가 동아리 규모에 충분하다
- 기존 MDC 기반 traceId 체계를 그대로 활용한다
- SecurityPaths에 Actuator 경로를 추가하되, Prometheus 스크레이핑은 네트워크 레벨에서 제한한다

---

## Out of Scope

- 분산 추적 시스템 (Jaeger, Zipkin, AWS X-Ray) — 단일 서비스이므로 현재 불필요
- APM 상용 솔루션 (DataDog, New Relic) — 비용 대비 효과가 낮음
- 로그 기반 이상 탐지 (ML) — 현재 트래픽 규모에서 불필요
- 사용자 세션 리플레이 (FullStory, LogRocket) — 프라이버시 고려 필요
- 부하 테스트 자동화 — 별도 기능으로 분리
