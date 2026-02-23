# 행사 통합 테스트 케이스

**작성일**: 2026-02-21
**버전**: 2.1
**관련 스펙**: [행사 검증 기준서](../../criteria/event/event-verification-criteria.md), [행사 신청 검증 기준서](../../criteria/event/event-registration-verification-criteria.md)
**우선순위**: P0

> **2축 모델 기반**: 이 문서는 Event + EventRegistration 도메인을 관통하는 통합 테스트 케이스를 기술한다. 2축 모델 E2E 흐름, 동시성 제어, 컨트롤러 RBAC를 검증한다.
> - ✅ 구현 완료 (현재 코드와 일치)

---

## 1. 개요

행사 시스템의 통합 테스트 케이스이다. 단위 테스트에서 검증하기 어려운 다음 영역을 커버한다:

- **DB 영속성 검증**: flush/clear 이후 실제 DB 상태 확인
- **2축 모델 E2E**: registrationStatus + eventStatus 교차 시나리오의 전체 흐름
- **동시성 제어**: 원자적 UPDATE의 실제 동시 접근 안전성
- **컨트롤러 RBAC**: MockMvc를 통한 HTTP 레벨 권한 검증

---

## 2. 테스트 케이스

### 2.1 기존 통합 테스트 (DB 영속성 검증)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-001 | 선착순 신청→취소 DB 검증 | AUTO_APPROVE, OPEN | 1. 신청 → DB 확인(REGISTERED, currentCount=1) 2. 취소 → DB 확인(CANCELED, currentCount=0) | flush/clear 영속성 컨텍스트 문제 없이 DB에 반영 | ✅ |
| INT-002 | 선착순 신청→취소→재신청 DB 검증 | AUTO_APPROVE, OPEN | 1. 신청 2. 취소 3. 재신청 → DB 확인(REGISTERED, currentCount=1) | 전체 흐름 DB 반영 정상 | ✅ |
| INT-003 | 선발제 신청→취소→재신청 DB 검증 | MANUAL_APPROVE, OPEN | 1. 신청(WAITING) 2. 취소(카운트 미변경) 3. 재신청(WAITING) → DB 확인 | WAITING 취소 시 카운트 미변경 확인 | ✅ |

### 2.2 2축 모델 — 겹침 기간 시나리오

등록 기간과 행사 기간이 겹치는 경우(`eventStartAt < registrationEndAt`) 발생하는 핵심 시나리오를 검증한다.

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-004 | OPEN+ONGOING 겹침 기간 신청 성공 | regEnd > eventStart, 행사 생성 | 1. 행사 생성(겹침 날짜 설정) 2. Lazy로 registrationStatus=OPEN 3. Lazy로 eventStatus=ONGOING 4. 신청 시도 | registrationStatus=OPEN, eventStatus=ONGOING에서 신청 성공 | ✅ |
| INT-005 | 겹침 기간 정원 마감→취소→자동 재오픈 | OPEN+ONGOING, capacity=2 | 1. 2명 신청(registrationStatus=CLOSED, CAPACITY_FULL) 2. 1명 취소 3. 자동 재오픈 확인 | registrationStatus=OPEN 복원, currentCount=1, eventStatus=ONGOING 유지 | ✅ |
| INT-006 | 겹침 기간 중 수동 마감 후 행사 계속 | OPEN+ONGOING | 1. 운영자 수동 마감 2. 상태 확인 | registrationStatus=CLOSED(MANUAL_CLOSE), eventStatus=ONGOING (행사 계속) | ✅ |

### 2.3 2축 모델 — 행사 취소/재활성화

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-007 | 행사 취소→registrationStatus=CLOSED 강제 DB 검증 | UPCOMING, registrationStatus=OPEN | 1. 행사 생성, Lazy로 OPEN 2. cancelEvent() 3. DB 확인 | registrationStatus=CLOSED, closeReason=MANUAL_CLOSE, eventStatus=CANCELED | ✅ |
| INT-008 | 행사 취소→재활성화→Lazy Evaluation | CANCELED, now < eventStart | 1. 취소 2. 재활성화 3. 상태 확인 | eventStatus=UPCOMING, registrationStatus Lazy 복원 | ✅ |
| INT-009 | CANCELED 행사에서 신청 시도 차단 | eventStatus=CANCELED | 1. 취소 2. 신청 시도 | `EventNotOpenException` (registrationStatus=CLOSED이므로) | ✅ |
| INT-010 | COMPLETED 행사에서 승인 시도 차단 | eventStatus=COMPLETED, WAITING 신청 존재 | 1. 선발제 신청(WAITING) 2. Lazy로 COMPLETED 전이 3. 승인 시도 | `EventNotEditableException` | ✅ |

### 2.4 2축 모델 — 수동 재오픈 E2E

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-011 | 수동 마감→수동 재오픈 전체 흐름 | OPEN, now <= regEnd | 1. 수동 마감 2. 수동 재오픈(reason 입력) 3. DB 상태 확인 4. 감사 이력 확인 | registrationStatus=OPEN, closeReason=null, 감사 이력 기록 | ✅ |
| INT-012 | 기한 만료 후 수동 재오픈 거부→regEnd 연장→재오픈 | now > regEnd | 1. 재오픈 시도(거부) 2. regEnd 연장(updateEvent) 3. 재오픈 성공 | 거부 후 연장 후 성공, registrationStatus=OPEN | ✅ |
| INT-013 | ONGOING 중 수동 재오픈 | eventStatus=ONGOING, registrationStatus=CLOSED, now <= regEnd | 1. ONGOING 전이 2. 수동 마감 3. 수동 재오픈 | registrationStatus=OPEN, eventStatus=ONGOING (2축 모델 핵심) | ✅ |

### 2.5 2축 모델 — 수정/상태 연동

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-014 | ONGOING 부분 수정 DB 반영 | eventStatus=ONGOING | 1. Lazy로 ONGOING 전이 2. title, description 수정 3. DB 확인 | 허용 필드만 수정됨, eventStartAt 변경 안 됨 | ✅ |
| INT-015 | CANCELED 수정→재활성화 E2E | eventStatus=CANCELED | 1. 취소 2. 날짜 수정 3. 재활성화 4. 상태 확인 | 수정된 날짜 기반 Lazy Evaluation 정상 동작 | ✅ |
| INT-016 | Lazy 두 축 동시 전이 DB 반영 | NOT_STARTED+UPCOMING, 미래 날짜 | 1. 행사 생성 2. regStart 및 eventStart 경과 후 조회 3. DB 확인 | registrationStatus=OPEN (또는 CLOSED), eventStatus=ONGOING | ✅ |

### 2.6 선발제 전체 흐름

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-017 | 선발제: 신청→승인→정원 마감→되돌리기→재오픈 | MANUAL_APPROVE, capacity=1 | 1. 신청(WAITING, count=0) 2. 승인(APPROVED, count=1, 정원 마감) 3. 되돌리기(WAITING, count=0, 재오픈) | currentCount: 0→1→0, registrationStatus: OPEN→CLOSED→OPEN | ✅ |
| INT-018 | 선발제: registrationStatus=CLOSED 후 승인 가능 | MANUAL_APPROVE, capacity=2 | 1. 2명 신청(WAITING) 2. 수동 마감(CLOSED) 3. 1명 승인 시도 | 승인 성공 (incrementCurrentCountForApproval은 registrationStatus 무관) | ✅ |

### 2.7 동시성 테스트

> **테스트 방식**: `@SpringBootTest` + `ExecutorService`를 사용한 멀티스레드 동시 접근 테스트. 원자적 UPDATE의 안전성을 실제 DB 레벨에서 검증한다.

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-019 | 동시 신청: capacity=1, 3명 동시 | AUTO_APPROVE, capacity=1 | 3개 스레드에서 동시 `registerEvent()` | 정확히 1명 성공, 2명 `EventCapacityFullException`, currentCount=1, 0 <= currentCount <= capacity | ✅ |
| INT-020 | 동시 신청: capacity=10, 15명 동시 | AUTO_APPROVE, capacity=10 | 15개 스레드에서 동시 `registerEvent()` | 정확히 10명 성공, 5명 실패, currentCount=10 | ✅ |
| INT-021 | 동시 취소+신청: 정원 경계 | capacity=10, currentCount=9 | 1명 취소 + 2명 신청 동시 실행 | 데이터 정합성 유지: 0 <= currentCount <= capacity | ✅ |
| INT-022 | 동시 승인: capacity=1, 2건 WAITING | MANUAL_APPROVE, capacity=1, 2명 WAITING | 2개 스레드에서 동시 `approveRegistration()` | 정확히 1명 승인, 1명 `EventCapacityFullException` | ✅ |

### 2.8 컨트롤러 RBAC 테스트 (MockMvc)

> **테스트 방식**: `ServiceIntegrationTestBase` + `@AutoConfigureMockMvc` 기반 MockMvc 테스트. HTTP 레벨에서 인증/인가를 검증한다.

#### 2.8.1 Event 엔드포인트

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-023 | 비인증 사용자 행사 생성 → 401 | 인증 없음 | `POST /api/v1/events` | 401 Unauthorized | ✅ |
| INT-024 | 비인증 사용자 행사 상세 조회 → 401 | 인증 없음 | `GET /api/v1/events/{id}` | 401 Unauthorized | ✅ |
| INT-025 | MEMBER가 행사 생성 → 403 | MEMBER 토큰 | `POST /api/v1/events` | 403 Forbidden (EventAccessDeniedException) | ✅ |
| INT-026 | ASSOCIATE가 행사 상세 조회 → 403 | ASSOCIATE 토큰 | `GET /api/v1/events/{id}` | 403 Forbidden (AssociateMemberNotAllowedException) | ✅ |
| INT-027 | MEMBER가 행사 취소 → 403 | MEMBER 토큰 | `POST /api/v1/events/{id}/cancel` | 403 Forbidden | ✅ |
| INT-028 | MEMBER가 행사 재활성화 → 403 | MEMBER 토큰 | `POST /api/v1/events/{id}/reactivate` | 403 Forbidden | ✅ |
| INT-029 | MEMBER가 등록 수동 재오픈 → 403 | MEMBER 토큰 | `POST /api/v1/events/{id}/reopen-registration` | 403 Forbidden | ✅ |

#### 2.8.2 EventRegistration 엔드포인트

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| INT-030 | 비인증 사용자 행사 신청 → 401 | 인증 없음 | `POST /api/v1/events/{id}/registrations` | 401 Unauthorized | ✅ |
| INT-031 | ASSOCIATE가 행사 신청 → 403 | ASSOCIATE 토큰 | `POST /api/v1/events/{id}/registrations` | 403 Forbidden (AssociateMemberNotAllowedException) | ✅ |
| INT-032 | MEMBER가 신청 승인 → 403 | MEMBER 토큰 | `POST /api/v1/registrations/{id}/approve` | 403 Forbidden (OperatorPermissionRequiredException) | ✅ |
| INT-033 | MEMBER가 신청 거절 → 403 | MEMBER 토큰 | `POST /api/v1/registrations/{id}/reject` | 403 Forbidden | ✅ |
| INT-034 | MEMBER가 되돌리기 → 403 | MEMBER 토큰 | `POST /api/v1/registrations/{id}/revert` | 403 Forbidden | ✅ |
| INT-035 | MEMBER가 신청자 목록 조회 → 403 | MEMBER 토큰 | `GET /api/v1/events/{id}/registrations` | 403 Forbidden | ✅ |

---

## 3. 검증 기준 매핑

### Event 불변조건 커버리지

| 검증 기준 | 커버 테스트 ID |
|----------|-------------|
| EVT-INV-10 (교차 축 불변조건) | INT-007, INT-009, INT-016 |
| EVT-INV-11 (CANCELED→CLOSED 강제) | INT-007, INT-009 |
| EVT-INV-12 (유효 복합 상태 조합) | INT-004, INT-006, INT-007, INT-016 |
| EVT-INV-13 (수동 재오픈 조건) | INT-011~013 |
| EVT-INV-14 (수동 재오픈 감사) | INT-011 |

### Registration 불변조건 커버리지

| 검증 기준 | 커버 테스트 ID |
|----------|-------------|
| REG-INV-01 (중복 방지) | INT-019 (동시 접근 시에도 중복 방지) |
| REG-INV-05 (registrationStatus OPEN) | INT-004 (OPEN+ONGOING 신청) |
| REG-INV-13 (CANCELED 행사 신청 차단) | INT-009 |
| REG-INV-14 (COMPLETED/CANCELED 승인/거절 차단) | INT-010 |

---

## 4. GAP 항목 커버리지

| GAP ID | 내용 | 커버 테스트 ID |
|--------|------|-------------|
| GAP-EVT-06 | 컨트롤러 RBAC | INT-023~035 |
| GAP-EVT-09 | 겹침 기간 동작 | INT-004 |
| GAP-EVT-10 | 겹침 중 정원 재오픈 | INT-005 |
| GAP-EVT-11 | 취소 시 CLOSED 강제 | INT-007 |
| GAP-EVT-12 | 재활성화 Lazy 복원 | INT-008 |
| GAP-EVT-17 | 수동 재오픈 감사 이력 | INT-011 |
| GAP-EVT-22 | CANCELED 수정→재활성화 E2E | INT-015 |
| GAP-REG-01 | APPROVED 취소 통합 | INT-017 |
| GAP-REG-02 | 선발제 승인 후 정원 마감 | INT-017 |
| GAP-REG-03 | 동시성 테스트 | INT-019~022 |
| GAP-REG-05 | 컨트롤러 RBAC (신청) | INT-030~035 |
| GAP-REG-06 | 시간 겹침 경계값 통합 | INT-005 |
| GAP-REG-09 | OPEN+ONGOING 신청 | INT-004 |
| GAP-REG-10 | CANCELED 신청/재신청 차단 | INT-009 |
| GAP-REG-11 | COMPLETED/CANCELED 승인/거절 차단 | INT-010 |
| GAP-REG-12 | 겹침 기간 정원 재오픈 연동 | INT-005 |

---

## 5. 구현 현황 요약

| 카테고리 | 전체 | ✅ |
|---------|:---:|:---:|
| 기존 통합 (DB 영속성) | 3 | 3 |
| 겹침 기간 시나리오 | 3 | 3 |
| 행사 취소/재활성화 | 4 | 4 |
| 수동 재오픈 E2E | 3 | 3 |
| 수정/상태 연동 | 3 | 3 |
| 선발제 전체 흐름 | 2 | 2 |
| 동시성 테스트 | 4 | 4 |
| 컨트롤러 RBAC — Event | 7 | 7 |
| 컨트롤러 RBAC — Registration | 6 | 6 |
| **합계** | **35** | **35** |

---

## 6. 구현된 테스트 클래스

### 6.1 EventRegistrationIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/event/integration/EventRegistrationIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` (non-transactional)
- **테스트**: INT-001~018 (18개)

### 6.2 EventConcurrencyTest
- **파일**: `backend/src/test/java/igrus/web/event/integration/EventConcurrencyTest.java`
- **기반**: `ServiceIntegrationTestBase` + `ExecutorService`
- **테스트**: INT-019~022 (4개)

### 6.3 EventControllerIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/event/integration/EventControllerIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` + `@AutoConfigureMockMvc` + MockMvc
- **테스트**: INT-023~029 (7개)

### 6.4 EventRegistrationControllerIntegrationTest
- **파일**: `backend/src/test/java/igrus/web/event/integration/EventRegistrationControllerIntegrationTest.java`
- **기반**: `ServiceIntegrationTestBase` + `@AutoConfigureMockMvc` + MockMvc
- **테스트**: INT-030~035 (6개)

---

## 7. 테스트 실행 가이드

### 전체 통합 테스트

```bash
# 전체 통합 테스트 실행
./gradlew test --tests "igrus.web.event.integration.*"
```

### 개별 실행

```bash
# E2E 통합 테스트 (INT-001~018)
./gradlew test --tests "igrus.web.event.integration.EventRegistrationIntegrationTest"

# 동시성 테스트 (INT-019~022)
./gradlew test --tests "igrus.web.event.integration.EventConcurrencyTest"

# Event 컨트롤러 RBAC (INT-023~029)
./gradlew test --tests "igrus.web.event.integration.EventControllerIntegrationTest"

# Registration 컨트롤러 RBAC (INT-030~035)
./gradlew test --tests "igrus.web.event.integration.EventRegistrationControllerIntegrationTest"
```

### 주의사항

- 통합 테스트는 `@Transactional`이 **없으므로** 각 테스트에서 `cleanupDatabase()`로 데이터를 정리해야 한다.
- 동시성 테스트는 `ExecutorService`와 `CountDownLatch`를 사용하여 스레드 동기화를 보장한다.
- 컨트롤러 테스트는 실제 JWT 토큰을 생성하여 MockMvc에 전달한다.
- INT-017/018 테스트 과정에서 `approveRegistration()`의 `clearAutomatically` 관련 영속성 컨텍스트 버그를 발견하고 수정함 (명시적 save 추가).

---

## 8. 관련 문서

- [행사 검증 기준서](../../criteria/event/event-verification-criteria.md) — EVT-INV-10~14
- [행사 신청 검증 기준서](../../criteria/event/event-registration-verification-criteria.md) — REG-INV-01~14, 동시성 제어
- [행사 도메인 테스트 케이스](./event-domain-test-cases.md) — Event 엔티티/서비스 단위 테스트
- [행사 신청 테스트 케이스](./event-registration-test-cases.md) — EventRegistration 엔티티/서비스 단위 테스트
- [기존 행사 테스트 케이스 v1.0](./event-test-cases.md) — 단일 축 FSM 기준 레거시

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-03 | - | INT-001~003 (event-test-cases.md에 미포함, 코드에만 존재) |
| 2.0 | 2026-02-21 | - | 2축 모델 기반 통합 테스트 문서 신규 작성. 겹침 기간(3), 취소/재활성화(4), 수동 재오픈(3), 수정/상태 연동(3), 선발제 흐름(2), 동시성(4), RBAC(13) 추가. 총 35건 (기존 3 + 신규 32) |
| 2.1 | 2026-02-23 | - | INT-004~035 전체 구현 완료 (32건 ⬜→✅). 테스트 클래스 4개로 구성. approveRegistration 영속성 컨텍스트 버그 수정 포함. |
