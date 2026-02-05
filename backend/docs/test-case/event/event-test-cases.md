# 행사(Event) 테스트 케이스

**작성일**: 2026-02-03
**버전**: 1.0
**관련 스펙**: [event-spec.md](../../spec/event/event-spec.md)
**우선순위**: P1

---

## 1. 개요

행사 기능에 대한 테스트 케이스입니다. 행사 생성/조회/수정/삭제, 신청/취소, 승인/거절 등 행사 관련 핵심 기능을 검증합니다.

---

## 2. 테스트 케이스

### 2.1 Event 도메인 - 생성

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-001 | 선착순 행사 생성 | 유효한 입력 데이터 | Event.create() 호출 (AUTO_APPROVE) | 행사 생성, status=UPCOMING, currentCount=0 | ✅ |
| EVT-002 | 선발제 행사 생성 | 유효한 입력 데이터 | Event.create() 호출 (MANUAL_APPROVE) | 행사 생성, registrationType=MANUAL_APPROVE | ✅ |
| EVT-003 | 정원 0 생성 거부 | capacity=0 | Event.create() 호출 | InvalidEventCapacityException 발생 | ✅ |
| EVT-004 | 정원 음수 생성 거부 | capacity=-1 | Event.create() 호출 | InvalidEventCapacityException 발생 | ✅ |
| EVT-005 | 정원 null 생성 거부 | capacity=null | Event.create() 호출 | InvalidEventCapacityException 발생 | ✅ |

### 2.2 Event 도메인 - 상태 전이

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-010 | UPCOMING→OPEN 전이 | status=UPCOMING | event.open() 호출 | status=OPEN | ✅ |
| EVT-011 | UPCOMING→CANCELED 전이 | status=UPCOMING | event.cancel() 호출 | status=CANCELED | ✅ |
| EVT-012 | OPEN→CLOSED 수동 마감 | status=OPEN | event.closeManually() 호출 | status=CLOSED, closeReason=MANUAL_CLOSE | ✅ |
| EVT-013 | OPEN→CLOSED 기한 만료 | status=OPEN | event.closeByDeadline() 호출 | status=CLOSED, closeReason=DEADLINE_PASSED | ✅ |
| EVT-014 | CLOSED→OPEN 재오픈 | status=CLOSED | event.open() 호출 | status=OPEN, closeReason=null | ✅ |
| EVT-015 | COMPLETED→OPEN 전이 불가 | status=COMPLETED | event.open() 호출 | InvalidEventStateTransitionException 발생 | ✅ |
| EVT-016 | CANCELED→OPEN 전이 불가 | status=CANCELED | event.open() 호출 | InvalidEventStateTransitionException 발생 | ✅ |
| EVT-017 | UPCOMING→COMPLETED 직접 전이 불가 | status=UPCOMING | event.complete() 호출 | InvalidEventStateTransitionException 발생 | ✅ |

### 2.3 Event 도메인 - 신청자 수 관리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-020 | 신청자 수 증가 | currentCount=0 | event.incrementCurrentCount() 호출 | currentCount=1 | ✅ |
| EVT-021 | 정원 초과 시 자동 마감 | capacity=2, currentCount=1 | 2회 incrementCurrentCount() 호출 | status=CLOSED, closeReason=CAPACITY_FULL | ✅ |
| EVT-022 | 신청자 수 감소 | currentCount=2 | event.decrementCurrentCount() 호출 | currentCount=1 | ✅ |
| EVT-023 | 정원 마감 후 취소 시 재오픈 | status=CLOSED, closeReason=CAPACITY_FULL | decrementCurrentCount() 호출 | status=OPEN, closeReason=null | ✅ |
| EVT-024 | 신청자 수 0 이하 방지 | currentCount=0 | event.decrementCurrentCount() 호출 | currentCount=0 (음수 안됨) | ✅ |

### 2.4 Event 도메인 - 조회 메서드

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-030 | OPEN+여유 시 신청 가능 | status=OPEN, !isFull() | event.isRegistrable() 호출 | true 반환 | ✅ |
| EVT-031 | OPEN+정원 초과 시 신청 불가 | status=OPEN, isFull() | event.isRegistrable() 호출 | false 반환 | ✅ |
| EVT-032 | UPCOMING 시 신청 불가 | status=UPCOMING | event.isRegistrable() 호출 | false 반환 | ✅ |
| EVT-033 | 남은 자리 수 계산 | capacity=30, currentCount=5 | event.getRemainingCapacity() 호출 | 25 반환 | ✅ |
| EVT-034 | 정원 초과 시 남은 자리 0 | capacity=1, currentCount=1 | event.getRemainingCapacity() 호출 | 0 반환 | ✅ |
| EVT-035 | 자동 승인 여부 확인 | registrationType=AUTO_APPROVE | event.isAutoApprove() 호출 | true 반환 | ✅ |
| EVT-036 | 수동 승인 여부 확인 | registrationType=MANUAL_APPROVE | event.isManualApprove() 호출 | true 반환 | ✅ |

### 2.5 Event 도메인 - 시간 중복 확인

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-040 | 완전히 겹치는 시간대 | 동일 시간 범위 | event.overlaps() 호출 | true 반환 | ✅ |
| EVT-041 | 부분적으로 겹치는 시간대 (앞) | 앞부분 겹침 | event.overlaps() 호출 | true 반환 | ✅ |
| EVT-042 | 부분적으로 겹치는 시간대 (뒤) | 뒷부분 겹침 | event.overlaps() 호출 | true 반환 | ✅ |
| EVT-043 | 완전히 포함되는 시간대 | 내부에 포함 | event.overlaps() 호출 | true 반환 | ✅ |
| EVT-044 | 완전히 앞에 있는 시간대 | 앞에 위치 | event.overlaps() 호출 | false 반환 | ✅ |
| EVT-045 | 완전히 뒤에 있는 시간대 | 뒤에 위치 | event.overlaps() 호출 | false 반환 | ✅ |
| EVT-046 | 경계에서 끝나는 시간대 | 정확히 시작점에서 끝남 | event.overlaps() 호출 | false 반환 | ✅ |
| EVT-047 | 경계에서 시작하는 시간대 | 정확히 종료점에서 시작 | event.overlaps() 호출 | false 반환 | ✅ |

### 2.6 Event 도메인 - 수정

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-050 | UPCOMING 상태에서 수정 | status=UPCOMING | event.update() 호출 | 정상 수정 | ✅ |
| EVT-051 | OPEN 상태에서 수정 | status=OPEN | event.update() 호출 | 정상 수정 | ✅ |
| EVT-052 | COMPLETED 상태에서 수정 불가 | status=COMPLETED | event.update() 호출 | EventNotEditableException 발생 | ✅ |
| EVT-053 | CANCELED 상태에서 수정 불가 | status=CANCELED | event.update() 호출 | EventNotEditableException 발생 | ✅ |
| EVT-054 | 정원 0으로 수정 불가 | 유효한 행사 | event.update(capacity=0) 호출 | InvalidEventCapacityException 발생 | ✅ |

---

### 2.7 EventRegistration 도메인 - 생성

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-001 | 선착순 행사 신청 | AUTO_APPROVE 행사 | EventRegistration.create() 호출 | status=REGISTERED | ✅ |
| REG-002 | 선발제 행사 신청 | MANUAL_APPROVE 행사 | EventRegistration.create() 호출 | status=WAITING | ✅ |
| REG-003 | 신청 시 registeredAt 설정 | 유효한 행사, 사용자 | EventRegistration.create() 호출 | registeredAt != null | ✅ |

### 2.8 EventRegistration 도메인 - 상태 변경

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-010 | WAITING→APPROVED 승인 | status=WAITING | registration.approve() 호출 | status=APPROVED | ✅ |
| REG-011 | WAITING→REJECTED 거절 | status=WAITING | registration.reject() 호출 | status=REJECTED | ✅ |
| REG-012 | REGISTERED→CANCELED 취소 | status=REGISTERED | registration.cancel() 호출 | status=CANCELED | ✅ |
| REG-013 | APPROVED→CANCELED 취소 | status=APPROVED | registration.cancel() 호출 | status=CANCELED | ✅ |
| REG-014 | WAITING→CANCELED 취소 | status=WAITING | registration.cancel() 호출 | status=CANCELED | ✅ |

### 2.9 EventRegistration 도메인 - 재신청

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-020 | 선착순 행사 재신청 | CANCELED 상태, AUTO_APPROVE 행사 | registration.reRegister() 호출 | status=REGISTERED | ✅ |
| REG-021 | 선발제 행사 재신청 | CANCELED 상태, MANUAL_APPROVE 행사 | registration.reRegister() 호출 | status=WAITING | ✅ |
| REG-022 | REGISTERED 상태에서 재신청 불가 | status=REGISTERED | registration.reRegister() 호출 | IllegalStateException 발생 | ✅ |
| REG-023 | WAITING 상태에서 재신청 불가 | status=WAITING | registration.reRegister() 호출 | IllegalStateException 발생 | ✅ |
| REG-024 | APPROVED 상태에서 재신청 불가 | status=APPROVED | registration.reRegister() 호출 | IllegalStateException 발생 | ✅ |
| REG-025 | REJECTED 상태에서 재신청 불가 | status=REJECTED | registration.reRegister() 호출 | IllegalStateException 발생 | ✅ |
| REG-026 | 재신청 시 registeredAt 갱신 | CANCELED 상태 | registration.reRegister() 호출 | registeredAt 갱신됨 | ✅ |

### 2.10 EventRegistration 도메인 - 조회 메서드

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| REG-030 | REGISTERED는 isActive true | status=REGISTERED | registration.isActive() 호출 | true 반환 | ✅ |
| REG-031 | APPROVED는 isActive true | status=APPROVED | registration.isActive() 호출 | true 반환 | ✅ |
| REG-032 | WAITING은 isActive false | status=WAITING | registration.isActive() 호출 | false 반환 | ✅ |
| REG-033 | REJECTED는 isActive false | status=REJECTED | registration.isActive() 호출 | false 반환 | ✅ |
| REG-034 | CANCELED는 isActive false | status=CANCELED | registration.isActive() 호출 | false 반환 | ✅ |

---

### 2.11 EventRegistrationService - 신청

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-001 | 정회원 선착순 행사 신청 | 정회원, AUTO_APPROVE 행사 | registerEvent() 호출 | REGISTERED 상태, 카운트 증가 | ✅ |
| SVC-002 | 정회원 선발제 행사 신청 | 정회원, MANUAL_APPROVE 행사 | registerEvent() 호출 | WAITING 상태, 카운트 변화 없음 | ✅ |
| SVC-003 | 준회원 신청 거부 | 준회원 | registerEvent() 호출 | AssociateMemberNotAllowedException | ✅ |
| SVC-004 | 존재하지 않는 행사 신청 | 미존재 행사 ID | registerEvent() 호출 | EventNotFoundException | ✅ |
| SVC-005 | 존재하지 않는 사용자 신청 | 미존재 사용자 ID | registerEvent() 호출 | UserNotFoundException | ✅ |
| SVC-006 | 중복 신청 거부 | 이미 신청한 사용자 | registerEvent() 호출 | AlreadyRegisteredException | ✅ |
| SVC-007 | OPEN 아닌 행사 신청 거부 | status=UPCOMING | registerEvent() 호출 | EventNotOpenException | ✅ |
| SVC-008 | 신청 기간 전 신청 거부 | 현재 < registrationStartAt | registerEvent() 호출 | EventNotInRegistrationPeriodException | ✅ |
| SVC-009 | 신청 기간 후 신청 거부 | 현재 > registrationEndAt | registerEvent() 호출 | EventNotInRegistrationPeriodException | ✅ |
| SVC-010 | 정원 초과 선착순 신청 거부 | isFull()=true, AUTO_APPROVE | registerEvent() 호출 | EventCapacityFullException | ✅ |
| SVC-011 | 취소된 신청 재신청 | CANCELED 상태 신청 존재 | registerEvent() 호출 | 재신청 처리, 상태 복원 | ✅ |

### 2.12 EventRegistrationService - 취소

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-020 | REGISTERED 상태 취소 | status=REGISTERED | cancelRegistration() 호출 | CANCELED 상태, 카운트 감소 | ✅ |
| SVC-021 | WAITING 상태 취소 | status=WAITING | cancelRegistration() 호출 | CANCELED 상태, 카운트 변화 없음 | ✅ |
| SVC-022 | 이미 취소된 신청 취소 | status=CANCELED | cancelRegistration() 호출 | AlreadyCanceledException | ✅ |
| SVC-023 | 존재하지 않는 신청 취소 | 미존재 신청 | cancelRegistration() 호출 | EventRegistrationNotFoundException | ✅ |

### 2.13 EventRegistrationService - 승인/거절

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-030 | 운영진 WAITING 승인 | 운영진, WAITING 상태 | approveRegistration() 호출 | APPROVED 상태, 카운트 증가 | ✅ |
| SVC-031 | 자동 승인 행사 승인 거부 | AUTO_APPROVE 행사 | approveRegistration() 호출 | NotManualApproveEventException | ✅ |
| SVC-032 | WAITING 아닌 상태 승인 거부 | status=APPROVED | approveRegistration() 호출 | InvalidRegistrationStatusException | ✅ |
| SVC-033 | 정원 초과 상태에서 승인 거부 | isFull()=true | approveRegistration() 호출 | EventCapacityFullException | ✅ |
| SVC-034 | 일반 회원 승인 거부 | 정회원 (비운영진) | approveRegistration() 호출 | OperatorPermissionRequiredException | ✅ |
| SVC-035 | 운영진 WAITING 거절 | 운영진, WAITING 상태 | rejectRegistration() 호출 | REJECTED 상태, 카운트 변화 없음 | ✅ |
| SVC-036 | 자동 승인 행사 거절 거부 | AUTO_APPROVE 행사 | rejectRegistration() 호출 | NotManualApproveEventException | ✅ |
| SVC-037 | WAITING 아닌 상태 거절 거부 | status=CANCELED | rejectRegistration() 호출 | InvalidRegistrationStatusException | ✅ |
| SVC-038 | 일반 회원 거절 거부 | 정회원 (비운영진) | rejectRegistration() 호출 | OperatorPermissionRequiredException | ✅ |

### 2.14 EventRegistrationService - 조회

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-040 | 내 신청 목록 조회 | 신청 내역 존재 | getMyRegistrations() 호출 | 신청 목록 반환 | ✅ |
| SVC-041 | 신청 내역 없을 때 빈 목록 | 신청 내역 없음 | getMyRegistrations() 호출 | 빈 목록 반환 | ✅ |
| SVC-042 | 운영진 신청자 목록 조회 | 운영진, 신청자 존재 | getRegistrationList() 호출 | 신청자 목록 반환 | ✅ |
| SVC-043 | 일반 회원 신청자 목록 조회 거부 | 정회원 | getRegistrationList() 호출 | OperatorPermissionRequiredException | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-E001 | 행사 상태 관리 | EVT-010 ~ EVT-017 |
| FR-E002 | 신청 방식 | EVT-001, EVT-002, REG-001, REG-002 |
| FR-E003 | 정원 관리 | EVT-003 ~ EVT-005, EVT-020 ~ EVT-024 |
| FR-E004 | 신청 기간 | SVC-008, SVC-009 |
| FR-E012 | 행사 수정 | EVT-050 ~ EVT-054 |
| FR-E020 | 신청 권한 | SVC-003 |
| FR-E021 | 중복 신청 방지 | SVC-006 |
| FR-E022 | 선착순 신청 | SVC-001, REG-001 |
| FR-E023 | 선발제 신청 | SVC-002, REG-002 |
| FR-E024 | 신청 취소 | SVC-020 ~ SVC-023, REG-012 ~ REG-014 |
| FR-E025 | 재신청 | SVC-011, REG-020 ~ REG-026 |
| FR-E030 | 승인 권한 | SVC-034, SVC-038 |
| FR-E031 | 승인 대상 | SVC-031, SVC-032, SVC-036, SVC-037 |
| FR-E032 | 승인 시 정원 확인 | SVC-033 |

---

## 4. 구현된 테스트 클래스

### 4.1 EventTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/domain/EventTest.java`
- **테스트 범위**: Event 도메인 로직 테스트
- **테스트 케이스**: EVT-001 ~ EVT-054

### 4.2 EventRegistrationTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/domain/EventRegistrationTest.java`
- **테스트 범위**: EventRegistration 도메인 로직 테스트
- **테스트 케이스**: REG-001 ~ REG-034

### 4.3 EventRegistrationServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java`
- **테스트 범위**: EventRegistrationService 비즈니스 로직 테스트
- **테스트 케이스**: SVC-001 ~ SVC-043

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-03 | - | 최초 작성 |
