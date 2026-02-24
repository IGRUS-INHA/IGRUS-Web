# 행사 신청(EventRegistration) 도메인 테스트 케이스

**작성일**: 2026-02-21
**버전**: 2.2
**관련 스펙**: [행사 신청 검증 기준서](../../criteria/event/event-registration-verification-criteria.md)
**우선순위**: P0

> **2축 모델 기반**: 이 문서는 기존 단일 축 FSM에서 **2축 모델**(registrationStatus + eventStatus)로 재설계된 목표 사양을 기준으로 테스트 케이스를 기술한다.
> - ✅ 구현 완료 (현재 코드와 일치)

---

## 1. 개요

행사 신청(EventRegistration) 도메인의 테스트 케이스이다. 신청 생성, EventRegistrationStatus FSM(선착순/선발제 분기), 신청/취소/재신청/승인/거절/되돌리기, 시간 겹침 검증, 동시성 제어, 2축 모델 연동을 검증한다.

---

## 2. 테스트 케이스

### 2.1 신청 생성 (REG-INV-02, REG-INV-03)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-001 | 선착순 행사 신청 시 REGISTERED | AUTO_APPROVE 행사 | `EventRegistration.create(event, user)` | status=REGISTERED, isActive=true | ✅ |
| REG-002 | 선발제 행사 신청 시 WAITING | MANUAL_APPROVE 행사 | `EventRegistration.create(event, user)` | status=WAITING, isActive=false | ✅ |
| REG-003 | 신청 시 registeredAt 설정 | 유효한 행사/사용자 | `EventRegistration.create(event, user)` | registeredAt != null | ✅ |

### 2.2 상태 변경 — approve/reject/cancel

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-010 | WAITING→APPROVED 승인 | status=WAITING | `approve()` | status=APPROVED, isActive=true | ✅ |
| REG-011 | WAITING→REJECTED 거절 | status=WAITING | `reject()` | status=REJECTED, isActive=false | ✅ |
| REG-012 | REGISTERED→CANCELED 취소 | status=REGISTERED | `cancel()` | status=CANCELED, isActive=false | ✅ |
| REG-013 | APPROVED→CANCELED 취소 | status=APPROVED | `cancel()` | status=CANCELED | ✅ |
| REG-014 | WAITING→CANCELED 취소 | status=WAITING | `cancel()` | status=CANCELED | ✅ |
| REG-045 | REJECTED→CANCELED 취소 | status=REJECTED | `cancel()` | status=CANCELED | ✅ |
| REG-046 | CANCELED에서 cancel() 불가 | status=CANCELED | `cancel()` | `InvalidRegistrationStatusException` | ✅ |

### 2.3 재신청 (REG-INV-11)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-020 | 선착순 취소 후 재신청 → REGISTERED | CANCELED, AUTO_APPROVE | `reRegister()` | status=REGISTERED, isActive=true | ✅ |
| REG-021 | 선발제 취소 후 재신청 → WAITING | CANCELED, MANUAL_APPROVE | `reRegister()` | status=WAITING, isActive=false | ✅ |
| REG-022 | REGISTERED에서 재신청 불가 | status=REGISTERED | `reRegister()` | `InvalidRegistrationStatusException` | ✅ |
| REG-023 | WAITING에서 재신청 불가 | status=WAITING | `reRegister()` | `InvalidRegistrationStatusException` | ✅ |
| REG-024 | APPROVED에서 재신청 불가 | status=APPROVED | `reRegister()` | `InvalidRegistrationStatusException` | ✅ |
| REG-025 | REJECTED에서 재신청 불가 | status=REJECTED | `reRegister()` | `InvalidRegistrationStatusException` | ✅ |
| REG-026 | 재신청 시 registeredAt 갱신 | CANCELED 상태 | `reRegister()` | registeredAt 갱신됨 | ✅ |

### 2.4 되돌리기 (REG-INV-09)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-040 | APPROVED→WAITING 되돌리기 | status=APPROVED | `revertToWaiting()` | status=WAITING | ✅ |
| REG-041 | REJECTED→WAITING 되돌리기 | status=REJECTED | `revertToWaiting()` | status=WAITING | ✅ |
| REG-042 | REGISTERED에서 되돌리기 불가 | status=REGISTERED | `revertToWaiting()` | `InvalidRegistrationStatusException` | ✅ |
| REG-043 | WAITING에서 되돌리기 불가 | status=WAITING | `revertToWaiting()` | `InvalidRegistrationStatusException` | ✅ |
| REG-044 | CANCELED에서 되돌리기 불가 | status=CANCELED | `revertToWaiting()` | `InvalidRegistrationStatusException` | ✅ |

### 2.5 isActive 정의 (REG-INV-12)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-030 | REGISTERED는 isActive=true | status=REGISTERED | `isActive()` | true | ✅ |
| REG-031 | APPROVED는 isActive=true | status=APPROVED | `isActive()` | true | ✅ |
| REG-032 | WAITING은 isActive=false | status=WAITING | `isActive()` | false | ✅ |
| REG-033 | REJECTED는 isActive=false | status=REJECTED | `isActive()` | false | ✅ |
| REG-034 | CANCELED는 isActive=false | status=CANCELED | `isActive()` | false | ✅ |
| REG-035 | REJECTED는 isRejected=true | status=REJECTED | `isRejected()` | true | ✅ |

### 2.6 EventRegistrationService — 신청 (REG-INV-01~06)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-001 | 정회원 선착순 행사 신청 → REGISTERED | MEMBER, AUTO_APPROVE, OPEN | `registerEvent(eventId, userId)` | REGISTERED, incrementCurrentCountIfAvailable 호출 | ✅ |
| SVC-REG-002 | 정회원 선발제 행사 신청 → WAITING | MEMBER, MANUAL_APPROVE, OPEN | `registerEvent(eventId, userId)` | WAITING, 카운트 변경 없음 | ✅ |
| SVC-REG-003 | 준회원 신청 거부 (REG-INV-04) | ASSOCIATE | `registerEvent(...)` | `AssociateMemberNotAllowedException` | ✅ |
| SVC-REG-004 | 존재하지 않는 행사 신청 | 없는 eventId | `registerEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-REG-005 | 존재하지 않는 사용자 신청 | 없는 userId | `registerEvent(...)` | `UserNotFoundException` | ✅ |
| SVC-REG-006 | 중복 신청 거부 (REG-INV-01) | 이미 신청 존재 (비취소) | `registerEvent(...)` | `AlreadyRegisteredException` | ✅ |
| SVC-REG-007 | OPEN 아닌 행사 신청 거부 (REG-INV-05) | registrationStatus != OPEN | `registerEvent(...)` | `EventNotOpenException` | ✅ |
| SVC-REG-008 | 신청 기간 전 거부 | now < regStart | `registerEvent(...)` | `EventNotInRegistrationPeriodException` | ✅ |
| SVC-REG-009 | 신청 기간 후 거부 | now > regEnd | `registerEvent(...)` | `EventNotInRegistrationPeriodException` | ✅ |
| SVC-REG-010 | 정원 초과 선착순 신청 거부 | incrementCurrentCountIfAvailable 반환 0 | `registerEvent(...)` | `EventCapacityFullException` | ✅ |
| SVC-REG-011 | 취소된 신청 재신청 | 기존 CANCELED 신청 존재 | `registerEvent(...)` | reRegister() 호출, 상태 복원 | ✅ |

### 2.7 EventRegistrationService — 취소

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-020 | REGISTERED 취소 시 카운트 감소 | isActive=true, REGISTERED | `cancelRegistration(...)` | CANCELED, decrementCurrentCount 호출 | ✅ |
| SVC-REG-021 | WAITING 취소 시 카운트 변경 없음 | isActive=false, WAITING | `cancelRegistration(...)` | CANCELED, decrementCurrentCount 미호출 | ✅ |
| SVC-REG-022 | 이미 취소된 신청 취소 | isCanceled=true | `cancelRegistration(...)` | `AlreadyCanceledException` | ✅ |
| SVC-REG-023 | 존재하지 않는 신청 취소 | 신청 없음 | `cancelRegistration(...)` | `EventRegistrationNotFoundException` | ✅ |

### 2.8 EventRegistrationService — 승인 (REG-INV-07, REG-INV-08)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-030 | 운영진 WAITING 승인 성공 | OPERATOR, WAITING, MANUAL_APPROVE | `approveRegistration(...)` | APPROVED, incrementCurrentCountForApproval 호출 | ✅ |
| SVC-REG-030-2 | 승인 시 시간 겹침 검증 | 겹치는 확정 신청 존재 | `approveRegistration(...)` | `EventTimeOverlapException` | ✅ |
| SVC-REG-031 | 자동 승인 행사 승인 거부 (REG-INV-07) | AUTO_APPROVE | `approveRegistration(...)` | `NotManualApproveEventException` | ✅ |
| SVC-REG-032 | WAITING 아닌 상태 승인 거부 (REG-INV-08) | status=APPROVED | `approveRegistration(...)` | `InvalidRegistrationStatusException` | ✅ |
| SVC-REG-033 | 정원 초과 시 승인 거부 | incrementCurrentCountForApproval 반환 0 | `approveRegistration(...)` | `EventCapacityFullException` | ✅ |
| SVC-REG-034 | 일반 회원 승인 거부 | MEMBER | `approveRegistration(...)` | `OperatorPermissionRequiredException` | ✅ |

### 2.9 EventRegistrationService — 거절 (REG-INV-07, REG-INV-08)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-035 | 운영진 WAITING 거절 성공 | OPERATOR, WAITING, MANUAL_APPROVE | `rejectRegistration(...)` | REJECTED, 카운트 변경 없음 | ✅ |
| SVC-REG-036 | 자동 승인 행사 거절 거부 (REG-INV-07) | AUTO_APPROVE | `rejectRegistration(...)` | `NotManualApproveEventException` | ✅ |
| SVC-REG-037 | WAITING 아닌 상태 거절 거부 (REG-INV-08) | status=CANCELED | `rejectRegistration(...)` | `InvalidRegistrationStatusException` | ✅ |
| SVC-REG-038 | 일반 회원 거절 거부 | MEMBER | `rejectRegistration(...)` | `OperatorPermissionRequiredException` | ✅ |

### 2.10 EventRegistrationService — 되돌리기 (REG-INV-09, REG-INV-10)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-050 | APPROVED 되돌리기 (카운트 감소) | APPROVED, MANUAL_APPROVE | `revertRegistration(...)` | WAITING, decrementCurrentCount, saveAndFlush | ✅ |
| SVC-REG-051 | REJECTED 되돌리기 (카운트 변경 없음) | REJECTED | `revertRegistration(...)` | WAITING, decrementCurrentCount 미호출 | ✅ |
| SVC-REG-052 | 유효하지 않은 상태 되돌리기 거부 | WAITING | `revertRegistration(...)` | `InvalidRegistrationStatusException` | ✅ |
| SVC-REG-053 | 자동 승인 행사 되돌리기 거부 | AUTO_APPROVE | `revertRegistration(...)` | `NotManualApproveEventException` | ✅ |
| SVC-REG-054 | 일반 회원 되돌리기 거부 | MEMBER | `revertRegistration(...)` | `OperatorPermissionRequiredException` | ✅ |
| SVC-REG-055 | ONGOING 행사 되돌리기 거부 (REG-INV-10) | eventStatus=ONGOING | `revertRegistration(...)` | `EventNotEditableException` | ✅ |
| SVC-REG-056 | COMPLETED 행사 되돌리기 거부 (REG-INV-10) | eventStatus=COMPLETED | `revertRegistration(...)` | `EventNotEditableException` | ✅ |

### 2.11 EventRegistrationService — 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-040 | 내 신청 목록 조회 | 2건 신청 존재 | `getMyRegistrations(userId)` | 2건 반환 | ✅ |
| SVC-REG-041 | 신청 내역 없을 때 빈 목록 | 0건 | `getMyRegistrations(userId)` | 빈 목록 | ✅ |
| SVC-REG-042 | 운영진 신청자 목록 조회 | OPERATOR | `getRegistrationList(...)` | 목록 반환 | ✅ |
| SVC-REG-043 | 일반 회원 신청자 목록 조회 거부 | MEMBER | `getRegistrationList(...)` | `OperatorPermissionRequiredException` | ✅ |

### 2.12 EventRegistrationService — 시간 겹침 (REG-INV-06)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-060 | 시간 겹치는 행사 신청 거부 | existsOverlappingRegistration=true | `registerEvent(...)` | `EventTimeOverlapException` | ✅ |
| SVC-REG-061 | 시간 안 겹치는 행사 신청 성공 | existsOverlappingRegistration=false | `registerEvent(...)` | 성공 | ✅ |
| SVC-REG-062 | 재신청 시 시간 겹침 거부 | 재신청, overlap=true | `registerEvent(...)` | `EventTimeOverlapException` | ✅ |

### 2.13 2축 모델 연동 — 신규 서비스 테스트 (REG-INV-05, REG-INV-13, REG-INV-14)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-063 | registrationStatus=OPEN, eventStatus=ONGOING에서 신청 성공 | 겹침 기간 (reg=OPEN, event=ONGOING) | `registerEvent(...)` | 성공 (2축 모델 핵심) | ✅ |
| SVC-REG-064 | eventStatus=CANCELED 행사 신청 거부 (REG-INV-13) | eventStatus=CANCELED → registrationStatus=CLOSED | `registerEvent(...)` | `EventNotOpenException` | ✅ |
| SVC-REG-065 | eventStatus=CANCELED 행사 재신청 거부 (REG-INV-13) | eventStatus=CANCELED, 취소된 신청 존재 | `registerEvent(...)` | `EventNotOpenException` | ✅ |
| SVC-REG-066 | eventStatus=COMPLETED 행사 승인 거부 (REG-INV-14) | eventStatus=COMPLETED, WAITING 존재 | `approveRegistration(...)` | `EventNotEditableException` | ✅ |
| SVC-REG-067 | eventStatus=CANCELED 행사 승인 거부 (REG-INV-14) | eventStatus=CANCELED, WAITING 존재 | `approveRegistration(...)` | `EventNotEditableException` | ✅ |
| SVC-REG-068 | eventStatus=COMPLETED 행사 거절 거부 (REG-INV-14) | eventStatus=COMPLETED | `rejectRegistration(...)` | `EventNotEditableException` | ✅ |
| SVC-REG-069 | eventStatus=CANCELED 행사 거절 거부 (REG-INV-14) | eventStatus=CANCELED | `rejectRegistration(...)` | `EventNotEditableException` | ✅ |
| SVC-REG-070 | eventStatus=CANCELED 행사 되돌리기 거부 (REG-INV-10) | eventStatus=CANCELED | `revertRegistration(...)` | `EventNotEditableException` | ✅ |

### 2.14 신청자 수(currentCount) 변경 매트릭스 — 신규 서비스 테스트

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-REG-071 | 겹침 기간 정원 마감 → 취소 → 자동 재오픈 | reg=OPEN, event=ONGOING, 정원 도달 후 취소 | 신청→정원 마감→취소→재오픈 확인 | now < regEnd이면 registrationStatus=OPEN 복원 | ✅ |
| SVC-REG-072 | CANCELED 행사에서 자동 재오픈 차단 | eventStatus=CANCELED, CAPACITY_FULL, 취소 발생 | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| SVC-REG-073 | APPROVED 상태 취소 서비스 (선발제) | APPROVED, MANUAL_APPROVE | `cancelRegistration(...)` | CANCELED, decrementCurrentCount 호출 | ✅ |
| SVC-REG-074 | 선발제 재신청 서비스 (CANCELED→WAITING) | CANCELED, MANUAL_APPROVE | `registerEvent(...)` (재신청 경로) | WAITING, incrementCurrentCountIfAvailable 미호출 | ✅ |
| SVC-REG-075 | registrationStatus=CLOSED 후 선발제 승인 가능 | registrationStatus=CLOSED, eventStatus=UPCOMING, WAITING | `approveRegistration(...)` | 성공 (incrementCurrentCountForApproval은 registrationStatus 무관) | ✅ |
| SVC-REG-076 | 비인가 접근 시 DB 상태 변경 없음 | MEMBER가 승인/거절/되돌리기 시도 | 예외 후 DB 확인 | DB 변경 없음 | ✅ |
| SVC-REG-077 | incrementCurrentCountIfAvailable SQL 조건 변경 검증 | registrationStatus=CLOSED에서 선착순 신청 | 원자적 UPDATE 호출 | updated rows = 0 (registrationStatus=OPEN 조건 불충족) | ✅ |

---

## 3. 검증 기준 매핑

| 검증 기준 | 커버 테스트 ID | 상태 |
|----------|-------------|------|
| REG-INV-01 (중복 신청 방지) | SVC-REG-006 | ✅ |
| REG-INV-02 (선착순 REGISTERED) | REG-001, SVC-REG-001 | ✅ |
| REG-INV-03 (선발제 WAITING) | REG-002, SVC-REG-002 | ✅ |
| REG-INV-04 (준회원 차단) | SVC-REG-003 | ✅ |
| REG-INV-05 (registrationStatus OPEN + 기간) | SVC-REG-007~009, SVC-REG-063 | ✅ |
| REG-INV-06 (시간 겹침 방지) | SVC-REG-060~062 | ✅ |
| REG-INV-07 (선발제 전용) | SVC-REG-031, 036, 053 | ✅ |
| REG-INV-08 (WAITING 상태 전제) | SVC-REG-032, 037 | ✅ |
| REG-INV-09 (되돌리기 APPROVED/REJECTED) | REG-040~044, SVC-REG-052 | ✅ |
| REG-INV-10 (되돌리기 eventStatus=UPCOMING) | SVC-REG-055, 056, 070 | ✅ |
| REG-INV-11 (재신청 CANCELED만) | REG-022~025, SVC-REG-011 | ✅ |
| REG-INV-12 (isActive 정의) | REG-030~034 | ✅ |
| REG-INV-13 (CANCELED 행사 신청/재신청 차단) | SVC-REG-064, 065 | ✅ |
| REG-INV-14 (COMPLETED/CANCELED 승인/거절 차단) | SVC-REG-066~069 | ✅ |

---

## 4. GAP 항목 커버리지

| GAP ID | 내용 | 커버 테스트 ID |
|--------|------|-------------|
| GAP-REG-01 | APPROVED 취소 통합 테스트 | SVC-REG-073, INT-015 (통합 문서) |
| GAP-REG-02 | 선발제 승인 후 정원 마감 자동 전이 | INT-015 (통합 문서) |
| GAP-REG-03 | 동시성 테스트 | INT-017~020 (통합 문서) |
| GAP-REG-04 | 비인가 접근 부작용 없음 | SVC-REG-076 |
| GAP-REG-05 | 컨트롤러 RBAC | INT-021~025 (통합 문서) |
| GAP-REG-06 | 시간 겹침 경계값 통합 | INT-005 (통합 문서) |
| GAP-REG-07 | 선발제 재신청 서비스 | SVC-REG-074 |
| GAP-REG-08 | APPROVED 취소 서비스 | SVC-REG-073 |
| GAP-REG-09 | OPEN+ONGOING 신청 | SVC-REG-063, INT-004 (통합 문서) |
| GAP-REG-10 | CANCELED 신청/재신청 차단 | SVC-REG-064, 065, INT-008 (통합 문서) |
| GAP-REG-11 | COMPLETED/CANCELED 승인/거절 차단 | SVC-REG-066~069, INT-009 (통합 문서) |
| GAP-REG-12 | 겹침 기간 정원 재오픈 연동 | SVC-REG-071, INT-005 (통합 문서) |
| GAP-REG-13 | CANCELED 자동 재오픈 차단 | SVC-REG-072, EVT-075 (도메인 문서) |
| GAP-REG-14 | incrementCurrentCountIfAvailable SQL 조건 | SVC-REG-077 |

---

## 5. 구현 현황 요약

| 카테고리 | 전체 | ✅ |
|---------|:---:|:---:|
| 신청 생성 | 3 | 3 |
| 상태 변경 | 7 | 7 |
| 재신청 | 7 | 7 |
| 되돌리기 | 5 | 5 |
| isActive | 6 | 6 |
| 서비스 — 신청 | 11 | 11 |
| 서비스 — 취소 | 4 | 4 |
| 서비스 — 승인 | 6 | 6 |
| 서비스 — 거절 | 4 | 4 |
| 서비스 — 되돌리기 | 7 | 7 |
| 서비스 — 조회 | 4 | 4 |
| 서비스 — 시간 겹침 | 3 | 3 |
| 2축 모델 연동 | 8 | 8 |
| 카운트 매트릭스 | 7 | 7 |
| **합계** | **82** | **82** |

---

## 6. 구현된 테스트 클래스

### 6.1 EventRegistrationTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/domain/EventRegistrationTest.java`
- **범위**: EventRegistration 도메인 로직 (생성, 상태 변경, 재신청, 되돌리기, 조회)
- **현재 테스트**: REG-001~046 (30개 메서드)

### 6.2 EventRegistrationServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java`
- **범위**: EventRegistrationService 비즈니스 로직 (신청/취소/승인/거절/되돌리기/조회)
- **현재 테스트**: SVC-REG-001~077 (48개 메서드)

### 6.3 EventRegistrationIntegrationTest (통합 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/service/EventRegistrationIntegrationTest.java`
- **범위**: flush/clear 영속성 컨텍스트 문제, 연속 호출 DB 반영
- **현재 테스트**: INT-001~003 (3개)
- **확장 예정**: 통합 테스트 문서 참조

---

## 7. 관련 문서

- [행사 신청 검증 기준서](../../criteria/event/event-registration-verification-criteria.md) — REG-INV-01~14, GAP-REG-01~14
- [행사 검증 기준서](../../criteria/event/event-verification-criteria.md) — 교차 참조 (2축 모델, EVT-INV-10~12)
- [기존 행사 테스트 케이스 v1.0](./event-test-cases.md) — 단일 축 FSM 기준 레거시
- [행사 도메인 테스트 케이스](./event-domain-test-cases.md) — Event 엔티티/서비스
- [행사 통합 테스트 케이스](./event-integration-test-cases.md) — 통합/동시성/RBAC

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-03 | - | 최초 작성 (event-test-cases.md에 포함) |
| 2.0 | 2026-02-21 | - | 2축 모델 기반 별도 문서로 분리. SVC-001~062를 SVC-REG-001~062로 리매핑. 신규 테스트 17건 추가. 검증 기준서(REG-INV-01~14) 전수 커버. GAP-REG-01~14 전수 커버 |
| 2.1 | 2026-02-22 | - | 실제 코드 교차 검증: SVC-REG-007, 055, 056 🔄→✅ 상태 수정 (2축 모델 리팩토링 완료 확인). 구현 현황 요약 테이블 수정 (62→65 ✅). 🔄 카테고리 제거 |
| 2.2 | 2026-02-23 | - | 신규 구현 반영: REG-045/046 도메인 취소 검증 테스트, SVC-REG-063~070 2축 모델 통합 테스트, SVC-REG-071~077 카운트 매트릭스 테스트. approveRegistration 영속성 버그 수정. 전체 82/82 구현 완료 |
