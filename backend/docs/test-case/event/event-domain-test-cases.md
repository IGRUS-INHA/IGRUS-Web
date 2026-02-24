# 행사(Event) 도메인 테스트 케이스

**작성일**: 2026-02-21
**버전**: 2.2
**관련 스펙**: [행사 검증 기준서](../../criteria/event/event-verification-criteria.md)
**우선순위**: P0

> **2축 모델 기반**: 이 문서는 기존 단일 축 FSM에서 **2축 모델**(registrationStatus + eventStatus)로 재설계된 목표 사양을 기준으로 테스트 케이스를 기술한다.
> - ✅ 구현 완료 (현재 코드와 일치)
> - ⬜ 미구현 (신규 작성 필요)

---

## 1. 개요

행사(Event) 도메인의 테스트 케이스이다. 행사 엔티티의 생성, 2축 상태 모델(registrationStatus + eventStatus), Lazy Evaluation, 행사 취소/재활성화, 등록 수동 재오픈, 날짜 경계값, 수정 정책, EventService CRUD를 검증한다.

---

## 2. 테스트 케이스

### 2.1 행사 생성 (EVT-INV-04, EVT-INV-05)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-001 | 선착순 행사 생성 | 유효한 입력, AUTO_APPROVE | `Event.create()` 호출 | status=UPCOMING, currentCount=0, registrationType=AUTO_APPROVE | ✅ |
| EVT-002 | 선발제 행사 생성 | 유효한 입력, MANUAL_APPROVE | `Event.create()` 호출 | registrationType=MANUAL_APPROVE, isManualApprove()=true | ✅ |
| EVT-003 | 정원 0 생성 거부 | capacity=0 | `Event.create()` 호출 | `InvalidEventCapacityException` | ✅ |
| EVT-004 | 정원 음수 생성 거부 | capacity=-1 | `Event.create()` 호출 | `InvalidEventCapacityException` | ✅ |
| EVT-005 | 정원 null 생성 거부 | capacity=null | `Event.create()` 호출 | `InvalidEventCapacityException` | ✅ |
| EVT-062 | 생성 시 2축 모델 초기 상태 검증 | 유효한 입력 | `Event.create()` 호출 | registrationStatus=NOT_STARTED, eventStatus=UPCOMING, currentCount=0 | ✅ |
| EVT-063 | 정원 1인 행사 생성 (경계값) | capacity=1 | `Event.create()` 호출 | 성공, capacity=1 | ✅ |
| EVT-064 | 정원 Integer.MAX_VALUE 행사 생성 (경계값) | capacity=Integer.MAX_VALUE | `Event.create()` 호출 | 성공 | ✅ |

### 2.2 상태 전이 — 단일 축 (레거시, 2축 모델로 대체 완료)

> 아래 테스트는 2축 모델 도입 이전의 단일 축 FSM 기반 테스트이다. 모두 2축 모델 테스트로 대체되어 코드에서 제거되었다.

| ID | 테스트 케이스 | 대체 테스트 ID | 상태 |
|----|-------------|-------------|------|
| EVT-010 | UPCOMING→OPEN 전이 | EVT-065 (NOT_STARTED→OPEN Lazy) | ✅ 대체 |
| EVT-011 | UPCOMING→CANCELED 전이 | EVT-080 (UPCOMING→CANCELED) | ✅ 대체 |
| EVT-012 | OPEN→CLOSED 수동 마감 | EVT-069 (OPEN→CLOSED 수동) | ✅ 대체 |
| EVT-013 | OPEN→CLOSED 기한 만료 | EVT-067 (OPEN→CLOSED Lazy 기한) | ✅ 대체 |
| EVT-014 | CLOSED→OPEN 재오픈 | EVT-070 (CLOSED→OPEN 자동 재오픈) | ✅ 대체 |
| EVT-015 | COMPLETED→OPEN 전이 불가 | EVT-084 (COMPLETED 종단) | ✅ 대체 |
| EVT-016 | CANCELED→OPEN 전이 불가 | EVT-082~083 (reactivate) | ✅ 대체 |
| EVT-017 | UPCOMING→COMPLETED 직접 전이 불가 | EVT-086 | ✅ 대체 |
| EVT-018 | OPEN→ONGOING 직접 전이 불가 | N/A (2축 모델에서 불필요) | 삭제 |
| EVT-019 | ONGOING→COMPLETED 전이 | EVT-079 (ONGOING→COMPLETED Lazy) | ✅ 대체 |

### 2.3 registrationStatus 축 전이 (2축 모델)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-065 | NOT_STARTED→OPEN (Lazy, 등록 시작일 도래) | registrationStatus=NOT_STARTED, now >= regStart, eventStatus != CANCELED | `updateStatusIfNeeded(now)` | registrationStatus=OPEN | ✅ |
| EVT-066 | NOT_STARTED→OPEN 불가 (eventStatus=CANCELED) | registrationStatus=NOT_STARTED, eventStatus=CANCELED | `updateStatusIfNeeded(now)` | registrationStatus 변경 없음 | ✅ |
| EVT-067 | OPEN→CLOSED (Lazy, 기한 만료) | registrationStatus=OPEN, now > regEnd | `updateStatusIfNeeded(now)` | registrationStatus=CLOSED, closeReason=DEADLINE_PASSED | ✅ |
| EVT-068 | OPEN→CLOSED (정원 자동 마감) | registrationStatus=OPEN, currentCount >= capacity | `incrementCurrentCount()` | registrationStatus=CLOSED, closeReason=CAPACITY_FULL | ✅ |
| EVT-069 | OPEN→CLOSED (수동 마감) | registrationStatus=OPEN | `closeManually()` | registrationStatus=CLOSED, closeReason=MANUAL_CLOSE | ✅ |
| EVT-070 | CLOSED→OPEN (정원 자동 재오픈) | CLOSED, closeReason=CAPACITY_FULL, !isFull(), now < regEnd, eventStatus != CANCELED | `reopenIfCapacityAvailable()` | registrationStatus=OPEN, closeReason=null | ✅ |
| EVT-071 | 자동 재오픈 불가: 기한 만료 | closeReason=CAPACITY_FULL, !isFull(), now >= regEnd | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| EVT-072 | 자동 재오픈 불가: 여전히 만석 | closeReason=CAPACITY_FULL, isFull() | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| EVT-073 | 자동 재오픈 불가: DEADLINE_PASSED | closeReason=DEADLINE_PASSED, !isFull() | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| EVT-074 | 자동 재오픈 불가: MANUAL_CLOSE | closeReason=MANUAL_CLOSE, !isFull() | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| EVT-075 | 자동 재오픈 불가: eventStatus=CANCELED | closeReason=CAPACITY_FULL, !isFull(), now < regEnd, eventStatus=CANCELED | `reopenIfCapacityAvailable()` | CLOSED 유지 | ✅ |
| EVT-076 | NOT_STARTED→CLOSED (행사 취소 강제) | registrationStatus=NOT_STARTED | `cancel()` | registrationStatus=CLOSED, closeReason=MANUAL_CLOSE | ✅ |
| EVT-077 | 자동 재오픈 경계값: now == regEnd (exclusive) | closeReason=CAPACITY_FULL, !isFull(), now == regEnd | `reopenIfCapacityAvailable()` | CLOSED 유지 (now < regEnd는 exclusive) | ✅ |

### 2.4 eventStatus 축 전이 (2축 모델)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-078 | UPCOMING→ONGOING (Lazy, 행사 시작일 도래) | eventStatus=UPCOMING, now >= eventStartAt | `updateStatusIfNeeded(now)` | eventStatus=ONGOING | ✅ |
| EVT-079 | ONGOING→COMPLETED (Lazy, 행사 종료일 경과) | eventStatus=ONGOING, now > eventEndAt | `updateStatusIfNeeded(now)` | eventStatus=COMPLETED, registrationStatus=CLOSED 강제 | ✅ |
| EVT-080 | UPCOMING→CANCELED (수동 취소) | eventStatus=UPCOMING, OPERATOR+ | `cancel()` | eventStatus=CANCELED, registrationStatus=CLOSED | ✅ |
| EVT-081 | ONGOING→CANCELED (수동 취소) | eventStatus=ONGOING, OPERATOR+ | `cancel()` | eventStatus=CANCELED, registrationStatus=CLOSED | ✅ |
| EVT-082 | CANCELED→UPCOMING 재활성화 | eventStatus=CANCELED, now < eventStartAt | `reactivate()` 후 Lazy Evaluation | eventStatus=UPCOMING | ✅ |
| EVT-083 | CANCELED→ONGOING 재활성화 | eventStatus=CANCELED, eventStartAt <= now < eventEndAt | `reactivate()` 후 Lazy Evaluation | eventStatus=ONGOING | ✅ |
| EVT-084 | COMPLETED→어떤 상태든 전이 불가 (종단) | eventStatus=COMPLETED | 모든 전이 시도 | `InvalidEventStateTransitionException` | ✅ |
| EVT-085 | CANCELED→COMPLETED 전이 불가 | eventStatus=CANCELED | COMPLETED로 직접 전이 | `InvalidEventStateTransitionException` | ✅ |
| EVT-086 | UPCOMING→COMPLETED 직접 전이 불가 | eventStatus=UPCOMING | COMPLETED로 직접 전이 | `InvalidEventStateTransitionException` | ✅ |
| EVT-087 | ONGOING→UPCOMING 역방향 전이 불가 | eventStatus=ONGOING | UPCOMING으로 전이 | `InvalidEventStateTransitionException` | ✅ |

### 2.5 Lazy Evaluation — 통합 2축

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-060 | CLOSED에서 ONGOING 자동 전환 | status=CLOSED, now > eventStart | `updateStatusIfNeeded(now)` | status=ONGOING | ✅ |
| EVT-061 | ONGOING에서 COMPLETED 자동 전환 | status=ONGOING, now > eventEnd | `updateStatusIfNeeded(now)` | status=COMPLETED | ✅ |
| EVT-088 | NOT_STARTED→OPEN 자동 전이 (Lazy 단독) | registrationStatus=NOT_STARTED, now >= regStart | `updateStatusIfNeeded(now)` | registrationStatus=OPEN | ✅ |
| EVT-089 | 겹침 기간: reg=OPEN + event=ONGOING 유지 | regEnd > eventStart, eventStart 도래 | `updateStatusIfNeeded(now)` | registrationStatus=OPEN, eventStatus=ONGOING | ✅ |
| EVT-090 | 한 번의 Lazy 호출로 두 축 동시 전이 | NOT_STARTED+UPCOMING, now > eventStart && now > regStart | `updateStatusIfNeeded(now)` | registrationStatus=OPEN, eventStatus=ONGOING | ✅ |
| EVT-091 | COMPLETED 전이 시 registrationStatus=CLOSED 강제 | eventStatus=ONGOING, registrationStatus=OPEN, now > eventEnd | `updateStatusIfNeeded(now)` | eventStatus=COMPLETED, registrationStatus=CLOSED | ✅ |
| EVT-092 | regStart == regEnd 시 Lazy 동작 | regStart == regEnd, now > regEnd | `updateStatusIfNeeded(now)` | registrationStatus=CLOSED, closeReason=DEADLINE_PASSED | ✅ |

### 2.6 교차 축 불변조건 (EVT-INV-10, EVT-INV-11, EVT-INV-12)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-093 | COMPLETED이면 registrationStatus=CLOSED | eventStatus=COMPLETED | 교차 조건 확인 | registrationStatus == CLOSED | ✅ |
| EVT-094 | CANCELED이면 registrationStatus=CLOSED | eventStatus=CANCELED | 교차 조건 확인 | registrationStatus == CLOSED | ✅ |
| EVT-095 | NOT_STARTED이면 eventStatus=UPCOMING | registrationStatus=NOT_STARTED | 교차 조건 확인 | eventStatus == UPCOMING | ✅ |
| EVT-096 | 무효 조합: NOT_STARTED + ONGOING 도달 불가 | regStart < eventStart 제약 | 날짜 제약으로 인해 | 무효 조합 도달 불가 확인 | ✅ |
| EVT-097 | 무효 조합: OPEN + COMPLETED 도달 불가 | regEnd <= eventEnd 제약 | eventEnd 경과 시 regEnd도 경과 | 무효 조합 도달 불가 확인 | ✅ |
| EVT-098 | 무효 조합: OPEN + CANCELED 도달 불가 | cancel() 시 CLOSED 강제 | cancel() 호출 | registrationStatus=CLOSED 확인 | ✅ |
| EVT-099 | 유효 7가지 복합 상태 조합 검증 | 각 유효 조합 설정 | 7개 유효 조합 도달 확인 | 모두 도달 가능, 무효 5개는 도달 불가 | ✅ |

### 2.7 행사 수정 (EVT-INV-07)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-050 | UPCOMING 상태에서 전체 필드 수정 성공 | eventStatus=UPCOMING | `event.update(...)` | 모든 필드 수정됨 | ✅ |
| EVT-051 | OPEN 상태(registrationStatus)에서 수정 성공 | registrationStatus=OPEN, eventStatus=UPCOMING | `event.update(...)` | 수정 성공 | ✅ |
| EVT-052 | COMPLETED 상태에서 수정 불가 | eventStatus=COMPLETED | `event.update(...)` | `EventNotEditableException` | ✅ |
| EVT-053 | ONGOING 상태에서 부분 수정 허용 | eventStatus=ONGOING | `event.update(...)` | 허용 필드만 수정 가능 | ✅ (EVT-100~108로 커버) |
| EVT-054 | 정원 0으로 수정 불가 | 유효한 행사 | `event.update(..., capacity=0)` | `InvalidEventCapacityException` | ✅ |
| EVT-100 | ONGOING 허용 필드: title 수정 | eventStatus=ONGOING | title만 변경 | 수정 성공 | ✅ |
| EVT-101 | ONGOING 허용 필드: description 수정 | eventStatus=ONGOING | description만 변경 | 수정 성공 | ✅ |
| EVT-102 | ONGOING 허용 필드: location 수정 | eventStatus=ONGOING | location만 변경 | 수정 성공 | ✅ |
| EVT-103 | ONGOING 허용 필드: eventEndAt 수정 | eventStatus=ONGOING | eventEndAt만 변경 | 수정 성공 | ✅ |
| EVT-104 | ONGOING 허용 필드: registrationEndAt 수정 | eventStatus=ONGOING | registrationEndAt만 변경 | 수정 성공 | ✅ |
| EVT-105 | ONGOING 허용 필드: capacity 수정 (capacity >= currentCount) | eventStatus=ONGOING, currentCount=5 | capacity=10 | 수정 성공 | ✅ |
| EVT-106 | ONGOING 금지 필드: eventStartAt 변경 | eventStatus=ONGOING | eventStartAt 변경 | `EventNotEditableException` | ✅ |
| EVT-107 | ONGOING 금지 필드: registrationStartAt 변경 | eventStatus=ONGOING | registrationStartAt 변경 | `EventNotEditableException` | ✅ |
| EVT-108 | ONGOING capacity 감소: capacity < currentCount 거부 | eventStatus=ONGOING, currentCount=5 | capacity=3 | 예외 발생 (EVT-INV-01 위반 방지) | ✅ |
| EVT-109 | CANCELED 상태에서 전체 필드 수정 성공 | eventStatus=CANCELED | `event.update(...)` 모든 필드 변경 | 수정 성공, eventStatus=CANCELED 유지 | ✅ |
| EVT-110 | COMPLETED 상태에서 수정 시도 거부 (2축) | eventStatus=COMPLETED | `event.update(...)` | `EventNotEditableException` | ✅ |

### 2.8 행사 취소/재활성화 (EVT-INV-06, EVT-INV-11)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-111 | UPCOMING 행사 취소 시 registrationStatus=CLOSED 강제 | eventStatus=UPCOMING, registrationStatus=OPEN | `cancel()` | eventStatus=CANCELED, registrationStatus=CLOSED, closeReason=MANUAL_CLOSE | ✅ |
| EVT-112 | ONGOING 행사 취소 시 registrationStatus=CLOSED 강제 | eventStatus=ONGOING, registrationStatus=OPEN | `cancel()` | eventStatus=CANCELED, registrationStatus=CLOSED | ✅ |
| EVT-113 | COMPLETED 행사 취소 불가 (종단) | eventStatus=COMPLETED | `cancel()` | `InvalidEventStateTransitionException` | ✅ |
| EVT-114 | CANCELED 재활성화: 현재 시간 < eventStart → UPCOMING | eventStatus=CANCELED, now < eventStartAt | `reactivate()` | eventStatus=UPCOMING, Lazy로 registrationStatus 복원 | ✅ |
| EVT-115 | CANCELED 재활성화: eventStart <= now < eventEnd → ONGOING | eventStatus=CANCELED, eventStart <= now < eventEnd | `reactivate()` | eventStatus=ONGOING | ✅ |
| EVT-116 | CANCELED 재활성화: now > eventEnd → COMPLETED (즉시) | eventStatus=CANCELED, now > eventEndAt | `reactivate()` 후 Lazy | eventStatus=COMPLETED | ✅ |
| EVT-117 | 이미 활성 상태 재활성화 불가 | eventStatus=UPCOMING | `reactivate()` | 거부 예외 | ✅ |

### 2.9 수동 재오픈 (EVT-INV-13, EVT-INV-14)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-118 | 모든 조건 충족 시 수동 재오픈 성공 | OPERATOR, eventStatus=UPCOMING, now <= regEnd, !isFull(), reason 입력 | `reopenRegistration(reason)` | registrationStatus=OPEN, closeReason=null | ✅ |
| EVT-119 | 수동 재오픈 거부: eventStatus=COMPLETED | eventStatus=COMPLETED | `reopenRegistration(reason)` | 거부 예외 | ⬜ |
| EVT-120 | 수동 재오픈 거부: eventStatus=CANCELED | eventStatus=CANCELED | `reopenRegistration(reason)` | 거부 예외 | ⬜ |
| EVT-121 | 수동 재오픈 거부: 기한 만료 (now > regEnd) | now > registrationEndAt | `reopenRegistration(reason)` | 거부 예외 | ⬜ |
| EVT-122 | 수동 재오픈 거부: 정원 만석 | isFull() == true | `reopenRegistration(reason)` | 거부 예외 | ⬜ |
| EVT-123 | 수동 재오픈 거부: reason이 null | reason=null | `reopenRegistration(null)` | 거부 예외 | ⬜ |
| EVT-124 | 수동 재오픈 거부: reason이 빈 문자열 | reason="" | `reopenRegistration("")` | 거부 예외 | ⬜ |
| EVT-125 | 수동 재오픈 후 감사 이력 기록 확인 | 성공적 재오픈 | 재오픈 후 감사 이력 조회 | reason, 시각, 운영자 ID 기록됨 | ✅ |
| EVT-126 | closeReason=CAPACITY_FULL에서 수동 재오픈 | CAPACITY_FULL, !isFull(), now <= regEnd | `reopenRegistration(reason)` | 성공 | ✅ |
| EVT-127 | closeReason=DEADLINE_PASSED에서 수동 재오픈 | DEADLINE_PASSED, now <= regEnd (기한 연장 후) | `reopenRegistration(reason)` | 성공 | ✅ |
| EVT-128 | closeReason=MANUAL_CLOSE에서 수동 재오픈 | MANUAL_CLOSE, now <= regEnd | `reopenRegistration(reason)` | 성공 | ✅ |
| EVT-129 | ONGOING 중 수동 재오픈 성공 | eventStatus=ONGOING, now <= regEnd, !isFull() | `reopenRegistration(reason)` | registrationStatus=OPEN (2축 모델 핵심) | ✅ |

### 2.10 날짜 경계값 (EVT-INV-02, EVT-INV-03)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-130 | regStart == eventStart (무효) | regStart == eventStart | 행사 생성 | `InvalidEventDateException` | ✅ |
| EVT-131 | regStart 1ms before eventStart (유효) | regStart = eventStart - 1ms | 행사 생성 | 성공 | ✅ |
| EVT-132 | regEnd == eventStart (유효, 2축 모델) | regEnd == eventStart | 행사 생성 | 성공 (기존 무효 → 유효로 변경) | ✅ |
| EVT-133 | regEnd == eventEnd (유효) | regEnd == eventEnd | 행사 생성 | 성공 | ✅ |
| EVT-134 | regEnd > eventEnd (무효) | regEnd = eventEnd + 1ms | 행사 생성 | `InvalidEventDateException` | ⬜ |
| EVT-135 | regStart > regEnd (무효) | regStart > regEnd | 행사 생성 | `InvalidEventDateException` | ✅ |
| EVT-136 | eventStart > eventEnd (무효) | eventStart > eventEnd | 행사 생성 | `InvalidEventDateException` | ⬜ |
| EVT-137 | eventStart == eventEnd (유효) | eventStart == eventEnd | 행사 생성 | 성공 | ✅ |
| EVT-138 | regStart == regEnd (유효, 즉시 CLOSED) | regStart == regEnd | 행사 생성 | 성공, Lazy 시 즉시 CLOSED 전이 | ✅ |

### 2.11 신청자 수 관리 (EVT-INV-01)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-020 | 신청자 수 증가 | currentCount=0, OPEN | `incrementCurrentCount()` | currentCount=1 | ✅ |
| EVT-021 | 정원 초과 시 자동 마감 | capacity=2, currentCount=1 | 2회 `incrementCurrentCount()` | currentCount=2, CLOSED, CAPACITY_FULL | ✅ |
| EVT-022 | 신청자 수 감소 | currentCount=2 | `decrementCurrentCount()` | currentCount=1 | ✅ |
| EVT-023 | 정원 마감 후 취소 시 자동 재오픈 | CLOSED, CAPACITY_FULL | `decrementCurrentCount()` | OPEN, closeReason=null | ✅ |
| EVT-024 | 신청자 수 0 이하 방지 | currentCount=0 | `decrementCurrentCount()` | currentCount=0 (음수 안됨) | ✅ |

### 2.12 조회 메서드

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-030 | OPEN+여유 시 isRegistrable=true | OPEN, !isFull() | `isRegistrable()` | true | ✅ |
| EVT-031 | OPEN+만석 시 isRegistrable=false | OPEN, isFull() | `isRegistrable()` | false | ✅ |
| EVT-032 | NOT_STARTED 시 isRegistrable=false | NOT_STARTED | `isRegistrable()` | false | ✅ |
| EVT-033 | 남은 자리 수 계산 | capacity=30, currentCount=5 | `getRemainingCapacity()` | 25 | ✅ |
| EVT-034 | 정원 만석 시 남은 자리 0 | capacity=1, currentCount=1 | `getRemainingCapacity()` | 0 | ✅ |
| EVT-035 | 자동 승인 여부 확인 | AUTO_APPROVE | `isAutoApprove()` | true | ✅ |
| EVT-036 | 수동 승인 여부 확인 | MANUAL_APPROVE | `isManualApprove()` | true | ✅ |

### 2.13 시간 중복 확인

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-040 | 완전히 겹치는 시간대 | 동일 시간 | `overlaps()` | true | ✅ |
| EVT-041 | 부분 겹침 (앞) | 앞부분 겹침 | `overlaps()` | true | ✅ |
| EVT-042 | 부분 겹침 (뒤) | 뒷부분 겹침 | `overlaps()` | true | ✅ |
| EVT-043 | 완전히 포함 | 완전 포함 | `overlaps()` | true | ✅ |
| EVT-044 | 완전히 앞에 | 겹침 없음 | `overlaps()` | false | ✅ |
| EVT-045 | 완전히 뒤에 | 겹침 없음 | `overlaps()` | false | ✅ |
| EVT-046 | 경계에서 끝남 | otherEnd == eventStart | `overlaps()` | false | ✅ |
| EVT-047 | 경계에서 시작 | otherStart == eventEnd | `overlaps()` | false | ✅ |

### 2.14 Soft Delete / closeReason 정합성 (EVT-INV-08, EVT-INV-09)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| EVT-139 | Soft delete 후 일반 조회 필터링 | 삭제된 행사 | `findById()`, `findAll()` | 결과에 포함되지 않음 | ✅ |
| EVT-140 | Soft delete 시 삭제자/시각 기록 | 삭제 실행 | `delete(userId)` | deletedAt, deletedBy 설정 | ✅ |
| EVT-141 | registrationStatus=CLOSED 시 closeReason != null | CLOSED 상태 | closeReason 확인 | null이 아님 | ✅ |
| EVT-142 | registrationStatus=OPEN 시 closeReason == null | OPEN 상태 | closeReason 확인 | null | ✅ |
| EVT-143 | registrationStatus=NOT_STARTED 시 closeReason == null | NOT_STARTED 상태 | closeReason 확인 | null | ✅ |

### 2.15 EventService 테스트

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-EVT-001 | 운영진 유효한 요청으로 행사 생성 | OPERATOR, 유효한 요청 | `createEvent(request, operatorId)` | 성공, ID 반환 | ✅ |
| SVC-EVT-002 | 일반 회원 행사 생성 거부 | MEMBER | `createEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-003 | 존재하지 않는 사용자 생성 | 없는 userId | `createEvent(...)` | `UserNotFoundException` | ✅ |
| SVC-EVT-004 | 행사 날짜 역전 시 생성 거부 | eventEnd < eventStart | `createEvent(...)` | `InvalidEventDateException` | ✅ |
| SVC-EVT-005 | 등록 기간-행사 기간 날짜 검증 (2축 새 제약) | regEnd > eventEnd 또는 regStart >= eventStart | `createEvent(...)` | `InvalidEventDateException` | ✅ (SVC-EVT-033, 034로 커버) |
| SVC-EVT-006 | 정회원 행사 단건 조회 | MEMBER, 유효한 eventId | `getEvent(eventId, memberId)` | 성공, Lazy Evaluation 호출 | ✅ |
| SVC-EVT-007 | 삭제된 행사 단건 조회 | 삭제된 행사 | `getEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-008 | 준회원 행사 조회 거부 | ASSOCIATE | `getEvent(...)` | `AssociateMemberNotAllowedException` | ✅ |
| SVC-EVT-009 | 운영진 조회 시 canEdit=true | OPERATOR | `getEvent(...)` | canEdit=true | ✅ |
| SVC-EVT-010 | 상태 필터 없이 목록 조회 | null 필터 | `getEventList(null)` | 전체 목록, Lazy Evaluation 호출 | ✅ |
| SVC-EVT-011 | OPEN 필터로 목록 조회 | status=OPEN | `getEventList(OPEN)` | OPEN 행사만 반환 | ✅ |
| SVC-EVT-012 | 행사 없을 때 빈 목록 | 0건 | `getEventList(null)` | 빈 목록 | ✅ |
| SVC-EVT-013 | Lazy 갱신 후 필터 제외 | OPEN→CLOSED 전이 | `getEventList(OPEN)` | 전이된 행사 제외 | ✅ |
| SVC-EVT-014 | 운영진 행사 수정 성공 | OPERATOR | `updateEvent(...)` | 수정 성공 | ✅ |
| SVC-EVT-015 | 일반 회원 행사 수정 거부 | MEMBER | `updateEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-016 | 삭제된 행사 수정 거부 | 삭제된 행사 | `updateEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-017 | 운영진 행사 삭제 (soft delete) | OPERATOR | `deleteEvent(...)` | soft delete 수행 | ✅ |
| SVC-EVT-018 | 일반 회원 행사 삭제 거부 | MEMBER | `deleteEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-019 | 이미 삭제된 행사 삭제 | 삭제된 행사 | `deleteEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-020 | 존재하지 않는 사용자 삭제 | 없는 userId | `deleteEvent(...)` | `UserNotFoundException` | ✅ |
| SVC-EVT-021 | 운영진 행사 수동 마감 | OPERATOR, OPEN | `closeEvent(...)` | 마감 성공, closeManually() 호출 | ✅ |
| SVC-EVT-022 | 일반 회원 행사 마감 거부 | MEMBER | `closeEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-023 | 삭제된 행사 마감 거부 | 삭제된 행사 | `closeEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-024 | 운영진 행사 취소 성공 | OPERATOR, eventStatus=UPCOMING | `cancelEvent(...)` | eventStatus=CANCELED, registrationStatus=CLOSED | ✅ |
| SVC-EVT-025 | 일반 회원 행사 취소 거부 | MEMBER | `cancelEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-026 | COMPLETED 행사 취소 거부 | eventStatus=COMPLETED | `cancelEvent(...)` | `InvalidEventStateTransitionException` | ✅ |
| SVC-EVT-026-2 | 삭제된 행사 취소 거부 | 삭제된 행사 | `cancelEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-027 | 운영진 행사 재활성화 성공 | OPERATOR, CANCELED | `reactivateEvent(...)` | 적절한 상태로 복원 | ✅ |
| SVC-EVT-028 | 일반 회원 행사 재활성화 거부 | MEMBER | `reactivateEvent(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-029 | 이미 활성 상태 재활성화 거부 | eventStatus=UPCOMING | `reactivateEvent(...)` | 거부 예외 | ✅ |
| SVC-EVT-029-2 | 삭제된 행사 재활성화 거부 | 삭제된 행사 | `reactivateEvent(...)` | `EventNotFoundException` | ✅ |
| SVC-EVT-030 | 운영진 수동 재오픈 성공 | OPERATOR, 5가지 조건 충족 | `reopenRegistration(eventId, operatorId, reason)` | registrationStatus=OPEN | ✅ |
| SVC-EVT-031 | 일반 회원 수동 재오픈 거부 | MEMBER | `reopenRegistration(...)` | `EventAccessDeniedException` | ✅ |
| SVC-EVT-032 | 생성 시 registrationStartAt 미래 제약 | regStart < now | `createEvent(...)` | `InvalidEventDateException` | ✅ |
| SVC-EVT-033 | regStart < eventStart 새 제약 검증 | regStart >= eventStart | `createEvent(...)` | `InvalidEventDateException` | ✅ |
| SVC-EVT-034 | regEnd <= eventEnd 새 제약 검증 | regEnd > eventEnd | `createEvent(...)` | `InvalidEventDateException` | ✅ |
| SVC-EVT-035 | 비인가 접근 시 DB 상태 변경 없음 | MEMBER가 생성/수정/삭제 시도 | 예외 발생 후 DB 확인 | DB 변경 없음 | ⬜ |

---

## 3. 검증 기준 매핑

| 검증 기준 | 커버 테스트 ID | 상태 |
|----------|-------------|------|
| EVT-INV-01 (신청자 수 범위) | EVT-020~024, EVT-108 | ✅ |
| EVT-INV-02 (날짜 순서, 2축 모델) | EVT-130~138, SVC-EVT-004,005,033,034 | ✅ (서비스) + ✅ (도메인 경계값 대부분, EVT-134/136 ⬜) |
| EVT-INV-03 (생성 시 미래 제약) | SVC-EVT-032 | ✅ |
| EVT-INV-04 (정원 최소값) | EVT-003~005, EVT-054, EVT-063,064 | ✅ |
| EVT-INV-05 (초기 상태 2축) | EVT-062 | ✅ |
| EVT-INV-06 (COMPLETED 종단) | EVT-084, EVT-086, EVT-087, EVT-113 | ✅ |
| EVT-INV-07 (상태별 수정 정책) | EVT-050, 052, 054, EVT-100~110 | ✅ |
| EVT-INV-08 (closeReason 정합성) | EVT-141~143 | ✅ |
| EVT-INV-09 (soft delete 필터링) | EVT-139,140, SVC-EVT-007 | ✅ |
| EVT-INV-10 (교차 축 불변조건) | EVT-093~095 | ✅ |
| EVT-INV-11 (CANCELED→CLOSED 강제) | EVT-076, EVT-111,112, SVC-EVT-024 | ✅ |
| EVT-INV-12 (유효 복합 상태 조합) | EVT-096~099 | ✅ |
| EVT-INV-13 (수동 재오픈 조건) | EVT-118, 125~129, SVC-EVT-030,031 | ✅ (부분: 119~124 ⬜) |
| EVT-INV-14 (수동 재오픈 감사) | EVT-125 | ✅ |

---

## 4. 구현 현황 요약

| 카테고리 | 전체 | ✅ | ⬜ |
|---------|:---:|:---:|:---:|
| 행사 생성 | 8 | 8 | 0 |
| 상태 전이 (단일 축) | 10 | 10 (대체) | 0 |
| registrationStatus 축 | 13 | 13 | 0 |
| eventStatus 축 | 10 | 10 | 0 |
| Lazy Evaluation | 7 | 7 | 0 |
| 교차 축 불변조건 | 7 | 7 | 0 |
| 행사 수정 | 16 | 16 | 0 |
| 취소/재활성화 | 7 | 7 | 0 |
| 수동 재오픈 | 12 | 6 | 6 |
| 날짜 경계값 | 9 | 7 | 2 |
| 신청자 수 관리 | 5 | 5 | 0 |
| 조회 메서드 | 7 | 7 | 0 |
| 시간 중복 | 8 | 8 | 0 |
| Soft Delete/closeReason | 5 | 5 | 0 |
| EventService | 37 | 36 | 1 |
| **합계** | **161** | **152** | **9** |

---

## 5. 구현된 테스트 클래스

### 5.1 EventTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/domain/EventTest.java`
- **범위**: Event 도메인 로직 (생성, 2축 상태 전이, 신청자 수, 조회, 시간 겹침, 수정, 취소/재활성화, 수동 재오픈, closeReason)
- **현재 테스트**: EVT-001~129 범위에서 구현된 메서드 약 80개
- **확장 예정**: 날짜 경계값(9), 수동 재오픈 거부(8), 교차 축(2), Lazy(1), 수정(1) — 총 21개

### 5.2 EventServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/event/service/EventServiceTest.java`
- **범위**: EventService 비즈니스 로직 (CRUD, 상태 관리, 취소/재활성화, 수동 재오픈)
- **현재 테스트**: SVC-EVT-001~034 범위에서 약 33개
- **확장 예정**: SVC-EVT-026, 029, 032, 035 — 총 4개

---

## 6. 관련 문서

- [행사 검증 기준서](../../criteria/event/event-verification-criteria.md) — EVT-INV-01~14, GAP-EVT-01~23
- [행사 신청 검증 기준서](../../criteria/event/event-registration-verification-criteria.md) — 교차 참조
- [기존 행사 테스트 케이스 v1.0](./event-test-cases.md) — 단일 축 FSM 기준 레거시
- [IGRUS_WEB_PRD_V2.md](../../feature/common/IGRUS_WEB_PRD_V2.md) — PRD 행사 섹션

---

## 7. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-03 | - | 최초 작성 (단일 축 FSM, event-test-cases.md) |
| 2.0 | 2026-02-21 | - | 2축 모델 기반 전면 재작성. 기존 테스트 매핑 + 신규 테스트 91건 추가. 검증 기준서(EVT-INV-01~14) 전수 커버. GAP-EVT-01~23 전수 커버 |
| 2.1 | 2026-02-22 | - | 구현 상태 전면 검증. 68개 ⬜→✅ 업데이트. 단일 축(2.2) 대체 완료 표기. SVC-EVT-026/029 ID 재할당(기존→026-2/029-2). 요약 테이블 실제 기준으로 수정. 총 135/161 구현 완료 |
| 2.2 | 2026-02-23 | - | 신규 구현 반영: EVT-051/092/096/097/127 도메인 테스트, SVC-EVT-026/029/032 서비스 테스트, EVT-125/130~133/135/137/138 날짜 경계값 테스트. approveRegistration 영속성 버그 수정. 총 152/161 구현 완료 |
