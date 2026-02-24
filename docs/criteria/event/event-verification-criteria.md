# 행사 (Event) 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-24
> **Scope**: 행사 생성(Create), 조회(Read), 수정(Update), 삭제(Delete), 상태 관리(Status), Lazy Evaluation, 행사 취소/재활성화, 등록 수동 재오픈
> **상태 모델**: 2축 모델 (registrationStatus + eventStatus)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

> **✅ 리팩토링 완료**: 이 문서는 기존 단일 축 FSM(EventStatus 5상태)에서 **2축 모델**(registrationStatus + eventStatus)로 재설계된 사양을 기술한다. 코드 리팩토링이 완료되어 모든 항목이 현재 구현과 일치한다.

## 목적

이 문서는 행사(Event) 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

기존 단일 축 FSM(UPCOMING → OPEN → CLOSED → ONGOING → COMPLETED)은 등록 상태와 행사 진행 상태를 하나의 축에서 관리하여, 행사 진행 중 등록 접수가 불가능하고, 상태 의미가 모호한 문제가 있었다. 이를 해결하기 위해 **2축 상태 모델**을 도입한다:

- **축 1: registrationStatus** — 등록(모집) 상태 관리 (NOT_STARTED, OPEN, CLOSED)
- **축 2: eventStatus** — 행사 진행 상태 관리 (UPCOMING, ONGOING, COMPLETED, CANCELED)

이로써 **행사 진행 중 등록 접수**(`registrationStatus == OPEN && eventStatus == ONGOING`)가 가능해지고, 각 축의 의미가 명확해진다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 정원-신청자수 정합성, 날짜 순서 제약, 2축 상태 교차 불변조건 |
| 2 | 상태 모델 | 2축 FSM (registrationStatus 3상태 + eventStatus 4상태), Lazy Evaluation 자동 전이 |
| 3 | 입력 도메인 분할과 경계값 | 행사 생성/수정 입력값, 날짜 조건, 정원 경계, 등록/행사 기간 겹침 시나리오 |
| 4 | 권한/보안 정책 | RBAC (ASSOCIATE 차단, OPERATOR+ 관리), 행사 취소/재활성화/수동 재오픈 |
| 5 | 관측 가능성 | 컨트롤러/서비스 로그 메시지, 수동 재오픈 감사 이력 |
| 6 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황, 누락 식별 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### EVT-INV-01: 신청자 수 범위 제약

> `currentCount >= 0 && currentCount <= capacity`가 항상 성립한다.

- **사전조건**: `capacity >= 1` (EVT-INV-04로 보장)
- **사후조건**: 증가 시 capacity 초과 불가, 감소 시 0 미만 불가
- **위반 시**: 정원 초과 신청 또는 음수 신청자 수 발생
- **관련 코드** `(현재 구현 일치)`:
  - `Event:297-298` - `isFull()`: `currentCount >= capacity`
  - `Event:244-249` - `incrementCurrentCount()`: 정원 초과 시 자동 마감
  - `Event:255-260` - `decrementCurrentCount()`: 0 이하 방지
  - `EventRepository:83-85` - `incrementCurrentCountIfAvailable()`: 원자적 UPDATE에 `currentCount < capacity` 조건
  - `EventRepository:112-115` - `decrementCurrentCount()`: 원자적 UPDATE에 `currentCount > 0` 조건
- **검증 방법**: 모든 신청/취소/승인/되돌리기 작업 후 `0 <= currentCount <= capacity` assertion

### EVT-INV-02: 날짜 순서 제약

> 행사의 날짜는 다음 4가지 제약을 만족한다:
> 1. `registrationStartAt < registrationEndAt` (strict)
> 2. `registrationStartAt < eventStartAt` (strict)
> 3. `registrationEndAt <= eventEndAt` (non-strict)
> 4. `eventStartAt <= eventEndAt` (non-strict)

- **사전조건**: 행사 생성/수정 요청 시 날짜 값이 모두 제공됨
- **사후조건**: 저장된 행사의 날짜가 위 4가지 제약을 만족
- **위반 시 예외**: `InvalidEventDateException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventService:433-452` - `validateEventDates()`: `regStart < regEnd`, `regStart < eventStart`, `regEnd <= eventEnd`, `eventStart <= eventEnd`
- **주의사항**:
  - `regEnd == eventStart`는 이제 **유효** (등록이 행사 시작 시점에 마감)
  - `regEnd == eventEnd`는 **유효** (등록이 행사 종료까지 가능)
  - `regStart == eventStart`는 **무효** (등록이 행사 시작 전에 열려야 함)

### EVT-INV-03: 생성 시 신청 시작일 미래 제약

> 행사 생성 시 `registrationStartAt`은 현재 시간 이후여야 한다.

- **사전조건**: 행사 생성 요청
- **위반 시 예외**: `InvalidEventDateException("신청 시작일은 현재 시간 이후여야 합니다")`
- **관련 코드** `(현재 구현 일치)`: `EventService:74-76`
- **주의사항**: 행사 **수정** 시에는 이 검증이 적용되지 않음 (기존 `registrationStartAt` 보존 가능)

### EVT-INV-04: 정원 최소값 제약

> `capacity >= 1`이어야 한다.

- **검증 계층**: DTO 레벨 (`@Min(1)`) + 엔티티 레벨 (`Event.validateCapacity()`)
- **위반 시 예외**: `InvalidEventCapacityException`
- **관련 코드** `(현재 구현 일치)`:
  - `Event:129-133` - `validateCapacity()`: null 또는 1 미만 거부
  - `CreateEventRequest:50` - `@Min(value = 1)`
  - `UpdateEventRequest:48` - `@Min(value = 1)`
- **검증 방법**: capacity가 0, -1, null인 경우 예외 발생 확인

### EVT-INV-05: 초기 상태 제약

> 행사 생성 시 초기 상태는 `registrationStatus = NOT_STARTED`, `eventStatus = UPCOMING`, `currentCount = 0`이다.

- **사후조건**: `Event.create()` 반환값의 `registrationStatus == NOT_STARTED`, `eventStatus == UPCOMING`, `currentCount == 0`
- **관련 코드** `(현재 구현 일치)`:
  - `Event.create()` - `registrationStatus = NOT_STARTED`, `eventStatus = UPCOMING`, `currentCount = 0`

### EVT-INV-06: COMPLETED 종단 상태

> `eventStatus == COMPLETED`에서는 어떤 eventStatus로도 전이할 수 없다.

- **사후조건**: `COMPLETED.canTransitionTo(target)` → 모든 target에 대해 `false`
- **위반 시 예외**: `InvalidEventStateTransitionException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventStatus:41` - `case COMPLETED -> false` (종단 상태)
- **주의사항**: CANCELED는 종단 상태가 **아님** (재활성화 가능, EVT-INV-06 적용 안 됨)

### EVT-INV-07: 상태별 행사 수정 정책

> 행사 수정 가능 범위는 `eventStatus`에 따라 달라진다. COMPLETED에서는 수정 불가, ONGOING에서는 이미 경과한 시각 필드만 차단, UPCOMING/CANCELED에서는 전체 수정 가능.

**상태별 수정 범위**:

| eventStatus | 수정 범위 | 근거 |
|:---:|---------|------|
| UPCOMING | 전체 필드 수정 가능 | 행사 시작 전이므로 제한 없음 |
| ONGOING | **부분 수정** 가능 (아래 표 참조) | 이미 경과한 시각 필드는 변경 무의미 |
| CANCELED | 전체 필드 수정 가능 | 재활성화 전 일정 재조정 허용 |
| COMPLETED | **수정 불가** | 종단 상태 (EVT-INV-06) |

**ONGOING 상태 필드별 수정 가능 여부**:

| 필드 | 수정 | 근거 |
|------|:---:|------|
| `title` | O | 정보성 필드 |
| `description` | O | 안내사항 업데이트 |
| `location` | O | 장소 변경 대응 |
| `eventStartAt` | **X** | 이미 시작된 행사의 시작 시각 변경 무의미 |
| `eventEndAt` | O | 행사 연장/단축 |
| `registrationStartAt` | **X** | 이미 경과한 등록 시작일 변경 무의미 |
| `registrationEndAt` | O | 등록 기간 연장 (2축 모델 핵심) |
| `capacity` | O | 단, `capacity >= currentCount` 필수 (EVT-INV-01 보장) |

- **위반 시 예외**:
  - COMPLETED 수정 시도: `EventNotEditableException`
  - ONGOING에서 금지 필드 변경 시도: `EventNotEditableException`
- **관련 코드** `(현재 구현 일치)`:
  - `Event.update()` - COMPLETED: 수정 불가, ONGOING: 부분 수정(`eventStartAt`/`registrationStartAt` 변경 차단, `capacity >= currentCount` 검증), UPCOMING/CANCELED: 전체 수정 가능
- **설계 근거**:
  - **ONGOING 부분 수정**: 2축 모델에서 `OPEN + ONGOING`(행사 진행 중 등록 접수)을 지원하려면 `registrationEndAt` 연장이 가능해야 하며, EVT-INV-13(수동 재오픈)에서 "기한 만료 시 `registrationEndAt`을 먼저 연장하라"는 흐름이 ONGOING에서도 동작해야 함
  - **CANCELED 전체 수정**: 재활성화 전 날짜 수정이 불가하면 데드락 발생 (CANCELED 수정 불가 → 재활성화 → Lazy Evaluation으로 ONGOING 전이 → ONGOING에서도 `eventStartAt` 수정 불가)
- **주의사항**:
  - ONGOING에서 `capacity` 감소 시 `capacity >= currentCount`를 반드시 검증 (EVT-INV-01 위반 방지)
  - CANCELED에서 수정 후에도 `eventStatus`는 CANCELED를 유지 (별도 재활성화 필요)

### EVT-INV-08: closeReason과 registrationStatus=CLOSED의 정합성

> `closeReason`은 `registrationStatus == CLOSED`일 때만 값이 존재하고, 다른 상태에서는 null이다.

- **사후조건**:
  - `registrationStatus == CLOSED`: `closeReason != null` (CAPACITY_FULL, DEADLINE_PASSED, MANUAL_CLOSE 중 하나)
  - `registrationStatus != CLOSED`: `closeReason == null`
- **관련 코드** `(현재 구현 일치)`:
  - 현재 구현: `status == CLOSED`일 때 closeReason 설정 (단일 축)
  - 목표: `registrationStatus == CLOSED`일 때 closeReason 설정 (등록 축)
  - `Event:145` - `open()`: `closeReason = null` `(현재 구현 일치)`
  - `Event:154` - `closeByCapacity()`: `closeReason = CAPACITY_FULL` `(현재 구현 일치)`
  - `Event:165` - `closeByDeadline()`: `closeReason = DEADLINE_PASSED` `(현재 구현 일치)`
  - `Event:176` - `closeManually()`: `closeReason = MANUAL_CLOSE` `(현재 구현 일치)`

### EVT-INV-09: Soft Delete 필터링

> `event_deleted = true`인 행사는 모든 JPA 조회에서 자동 필터링된다.

- **근거**: `@SQLRestriction("event_deleted = false")` (`Event:23`)
- **사후조건**: soft delete된 행사는 `findById()`, `findAll()` 등 일반 쿼리에서 반환되지 않음
- **예외**: `@Modifying` UPDATE 쿼리에는 `@SQLRestriction`이 적용되지 않으므로 명시적 `e.deleted = false` 조건 사용 (`EventRepository:84,99,114`)
- **관련 코드** `(현재 구현 일치)`:
  - `Event:23` - `@SQLRestriction` 적용
  - `EventService:232` - `event.delete(userId)` 호출

### EVT-INV-10: 교차 축 불변조건 (신규)

> 두 축의 상태는 다음 교차 조건을 항상 만족한다:
> 1. `eventStatus == COMPLETED → registrationStatus == CLOSED`
> 2. `eventStatus == CANCELED → registrationStatus == CLOSED`
> 3. `eventStatus == COMPLETED → now > eventEndAt`
> 4. `registrationStatus == NOT_STARTED → eventStatus == UPCOMING`

- **보장 방법** `(현재 구현 일치)`:
  - 조건 1, 2: COMPLETED/CANCELED 전이 시 registrationStatus를 CLOSED로 강제 전환
  - 조건 3: Lazy Evaluation에서 `now > eventEndAt`일 때만 COMPLETED 전이
  - 조건 4: 날짜 제약 `regStart < eventStart`에 의해 등록이 항상 행사보다 먼저 시작
- **검증 방법**: 모든 상태 전이 후 교차 조건 assertion

### EVT-INV-11: CANCELED 시 등록 마감 강제 (신규)

> `eventStatus`가 CANCELED로 전이되면 `registrationStatus`는 CLOSED로 강제 전환되고, `closeReason = MANUAL_CLOSE`로 설정된다.

- **트리거**: 운영자 행사 취소 (`cancel()`)
- **사후조건**: `registrationStatus == CLOSED && closeReason == MANUAL_CLOSE`
- **관련 코드** `(현재 구현 일치)`: 새로운 `Event.cancel()` 메서드 구현 필요
- **검증 방법**: 행사 취소 후 registrationStatus와 closeReason assertion

### EVT-INV-12: 유효 복합 상태 조합 (신규)

> 시스템에서 도달 가능한 복합 상태는 다음 7가지만 허용된다:

| registrationStatus | eventStatus | 의미 | 유효 |
|:---:|:---:|------|:---:|
| NOT_STARTED | UPCOMING | 행사 예정, 등록 시작 전 | O |
| OPEN | UPCOMING | 모집 중, 행사 시작 전 | O |
| OPEN | ONGOING | **모집 중, 행사 진행 중** (겹침 기간) | O |
| CLOSED | UPCOMING | 모집 마감, 행사 시작 전 | O |
| CLOSED | ONGOING | 모집 마감, 행사 진행 중 | O |
| CLOSED | COMPLETED | 모집 마감, 행사 완료 | O |
| CLOSED | CANCELED | 모집 마감, 행사 취소 | O |

무효 조합 (5개, 절대 발생하면 안 됨):

| registrationStatus | eventStatus | 무효 이유 |
|:---:|:---:|------|
| NOT_STARTED | ONGOING | `regStart < eventStart` 제약으로 불가 |
| NOT_STARTED | COMPLETED | 위와 동일 |
| NOT_STARTED | CANCELED | 취소 시 CLOSED 강제 (EVT-INV-11) |
| OPEN | COMPLETED | `regEnd <= eventEnd` 제약으로 `eventEnd` 경과 시 `regEnd`도 경과 |
| OPEN | CANCELED | 취소 시 CLOSED 강제 (EVT-INV-11) |

- **검증 방법**: 모든 상태 전이 후 현재 복합 상태가 유효 조합에 포함되는지 assertion

### EVT-INV-13: 수동 재오픈 조건 (신규)

> 운영자가 등록을 수동으로 재오픈(`CLOSED → OPEN`)하려면 다음 5가지 조건을 모두 만족해야 한다:
> 1. OPERATOR 이상 권한
> 2. `eventStatus ∉ {COMPLETED, CANCELED}`
> 3. `now <= registrationEndAt` (등록 마감 기한이 아직 경과하지 않음)
> 4. `!isFull()` (정원에 여유가 있음)
> 5. 재오픈 사유(`reason`) 필수 입력

- **위반 시 예외**: 각 조건별 적절한 예외 `(현재 구현 일치)`
- **주의사항**:
  - `now > registrationEndAt`인 경우 재오픈 불가 (Lazy Evaluation이 즉시 CLOSED로 되돌림)
  - 기한 만료 후 등록을 재오픈하려면 먼저 행사 수정으로 `registrationEndAt`을 연장한 뒤 재오픈해야 함
  - `closeReason` 종류와 무관하게 운영자가 판단하여 재오픈 가능 (CAPACITY_FULL, DEADLINE_PASSED, MANUAL_CLOSE 모두)
  - 정원이 차 있으면 재오픈 불가 (OPEN 직후 자동 재마감 방지)
- **검증 방법**: 각 조건 위반 시 예외 발생, 모든 조건 충족 시 성공 확인

### EVT-INV-14: 행사 상태 변경 감사 이력

> 사용자가 직접 수행한 행사 상태 변경(취소, 재활성화, 등록 수동 마감, 등록 수동 재오픈)은 `EventStatusChangeHistory`에 기록된다. **모든 수동 상태 변경 시 사유(`reason`)가 반드시 기록된다.**

- **사전조건**: 모든 수동 상태 변경 시 `reason`이 null이거나 빈 문자열이면 변경 거부 (DTO `@NotBlank` 검증)
- **사후조건**: 변경 유형(`EventChangeType`), 이전/이후 값, 변경자 정보, 학번(비정규화), **사유**가 감사 이력에 기록됨
- **기록 대상 변경 유형**:
  - `EVENT_CANCELED` — 행사 취소 (reason 필수)
  - `EVENT_REACTIVATED` — 행사 재활성화 (reason 필수)
  - `REGISTRATION_CLOSED_MANUAL` — 등록 수동 마감 (reason 필수)
  - `REGISTRATION_REOPENED` — 등록 수동 재오픈 (reason 필수)
- **기록하지 않는 변경**: Lazy Evaluation에 의한 자동 상태 전이 (시간 기반, 매 조회마다 발생하여 노이즈 생성)
- **구현 방식**: `@EventListener` + `REQUIRES_NEW` 독립 트랜잭션. 이력 저장 실패가 비즈니스 로직에 영향 없음 (try-catch 격리)
- **설계 결정**: FK 없이 ID만 저장 (soft-delete/탈퇴 후에도 이력 영구 보존), `changedByStudentId` 비정규화 (유저 탈퇴 후에도 조회 가능)
- **관련 코드** `(현재 구현 일치)`:
  - `EventStatusChangeHistory` — 감사 이력 엔티티 (`BaseEntity` 상속)
  - `EventChangeType` — 변경 유형 enum
  - `EventStatusChangeEvent` — Spring 이벤트 record
  - `EventStatusChangeReasonRequest` — 공유 요청 DTO (`@NotBlank String reason`)
  - `RecordEventStatusChangeService` — `@EventListener` + `REQUIRES_NEW` TransactionTemplate 리스너
  - `EventService:closeEvent()`, `cancelEvent()`, `reactivateEvent()`, `reopenRegistration()` — 이벤트 발행 (모두 `reason` 전달)
- **검증 방법**: 각 수동 상태 변경 후 감사 이력 조회, `reason`이 null/빈 문자열일 때 예외 발생 확인, 이력 레코드의 `reason` 값 검증

### EVT-INV-15: 신청자가 있는 행사 삭제 불가

> 활성 신청(REGISTERED, WAITING, APPROVED)이 존재하는 행사는 삭제(soft delete)할 수 없다. 신청자가 있는 경우 행사 취소(cancel)를 사용해야 한다.

- **사전조건**: `deleteEvent()` 호출
- **검증**: `existsByEventIdAndStatusIn(eventId, {REGISTERED, WAITING, APPROVED})` → `true`이면 거부
- **위반 시 예외**: `EventNotDeletableException`
- **설계 근거**: 신청자가 있는 행사를 삭제하면 신청자 입장에서 신청한 행사가 무통보로 사라지므로, 행사 취소를 통해 명시적으로 상태를 관리해야 한다
- **관련 코드**: `EventService.deleteEvent()` — 권한 확인 후 활성 신청 존재 여부 확인
- **검증 방법**: 활성 신청이 있는 행사 삭제 시도 시 예외 발생 확인, 신청자 없는 행사는 정상 삭제 확인

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 축 1: registrationStatus FSM (등록 상태)

```
NOT_STARTED ──→ OPEN ──→ CLOSED
                  ↑          │
                  └──────────┘ (자동 재오픈 또는 수동 재오픈)
```

| 상태 | 의미 |
|------|------|
| `NOT_STARTED` | 등록 시작 전 |
| `OPEN` | 등록 접수 중 |
| `CLOSED` | 등록 마감 |

**전이 테이블**:

| 전이 | 트리거 | 사전조건 | 사후조건 | closeReason |
|------|--------|---------|---------|-------------|
| NOT_STARTED → OPEN | Lazy | `now >= regStart && eventStatus != CANCELED` | `registrationStatus = OPEN` | - |
| OPEN → CLOSED | Lazy | `now > regEnd` | `registrationStatus = CLOSED` | DEADLINE_PASSED |
| OPEN → CLOSED | Auto | `currentCount >= capacity` | `registrationStatus = CLOSED` | CAPACITY_FULL |
| OPEN → CLOSED | Manual | 운영자 수동 마감, **사유 필수** | `registrationStatus = CLOSED` | MANUAL_CLOSE |
| CLOSED → OPEN | Auto | `closeReason == CAPACITY_FULL && !isFull() && now < regEnd && eventStatus != CANCELED` | `registrationStatus = OPEN`, `closeReason = null` | - |
| CLOSED → OPEN | Manual | 운영자 수동 재오픈 (EVT-INV-13 조건 전부 충족) | `registrationStatus = OPEN`, `closeReason = null` | - |
| NOT_STARTED → CLOSED | Forced | `eventStatus` → CANCELED 전이 (EVT-INV-11) | `registrationStatus = CLOSED` | MANUAL_CLOSE |

**관련 코드** `(현재 구현 일치)`:
- 현재: 단일 `EventStatus`의 UPCOMING → OPEN → CLOSED 부분이 이 축에 해당
- 목표: 별도의 `RegistrationStatus` enum과 전이 메서드

### 2-2. 축 2: eventStatus FSM (행사 상태)

```
UPCOMING ──→ ONGOING ──→ COMPLETED (종단)
   │            │
   └──→ CANCELED ←──┘
         │
         └──→ UPCOMING 또는 ONGOING (재활성화)
```

| 상태 | 의미 |
|------|------|
| `UPCOMING` | 행사 시작 전 |
| `ONGOING` | 행사 진행 중 |
| `COMPLETED` | 행사 완료 (종단 — 되돌릴 수 없음) |
| `CANCELED` | 행사 취소 (되돌릴 수 있음) |

**전이 테이블**:

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| UPCOMING → ONGOING | Lazy | `now >= eventStartAt` | `eventStatus = ONGOING` |
| ONGOING → COMPLETED | Lazy | `now > eventEndAt` | `eventStatus = COMPLETED`, `registrationStatus = CLOSED` (EVT-INV-10) |
| UPCOMING → CANCELED | Manual | OPERATOR+ 권한, **사유 필수** | `eventStatus = CANCELED`, `registrationStatus = CLOSED` (EVT-INV-11) |
| ONGOING → CANCELED | Manual | OPERATOR+ 권한, **사유 필수** | `eventStatus = CANCELED`, `registrationStatus = CLOSED` (EVT-INV-11) |
| CANCELED → UPCOMING/ONGOING | Manual | OPERATOR+ 권한, **사유 필수**, 재활성화 | Lazy Evaluation 실행하여 현재 시간 기반으로 올바른 상태 복원 |

**관련 코드** `(현재 구현 일치)`:
- 현재: 단일 `EventStatus`의 ONGOING → COMPLETED 부분이 이 축에 해당
- 목표: 별도의 `EventStatus` enum (UPCOMING, ONGOING, COMPLETED, CANCELED)

### 2-3. 교차 축 불변조건 + 복합 상태 유효 조합

교차 축 불변조건은 EVT-INV-10에서 정의. 유효/무효 조합은 EVT-INV-12에서 정의.

**핵심 시나리오**: `registrationStatus == OPEN && eventStatus == ONGOING`

이 조합은 등록 기간과 행사 기간이 겹치는 경우(예: `eventStartAt < regEnd`)에 발생한다. 이것이 2축 모델 도입의 핵심 이유이며, 기존 단일 축 FSM에서는 표현할 수 없었던 상태이다.

### 2-4. Lazy Evaluation (통합)

`updateStatusIfNeeded(Instant now)` 메서드는 조회 시점에 호출되어 두 축의 상태를 현재 시간에 맞게 자동 갱신한다.

```
updateStatusIfNeeded(now):
  // 1. 등록 축
  if (registrationStatus == NOT_STARTED
      && now >= registrationStartAt
      && eventStatus != CANCELED)
    → registrationStatus = OPEN

  if (registrationStatus == OPEN && now > registrationEndAt)
    → registrationStatus = CLOSED, closeReason = DEADLINE_PASSED

  // 2. 행사 축
  if (eventStatus == UPCOMING && now >= eventStartAt)
    → eventStatus = ONGOING

  if (eventStatus == ONGOING && now > eventEndAt)
    → eventStatus = COMPLETED
    → if (registrationStatus != CLOSED) registrationStatus = CLOSED
```

- **관련 코드** `(현재 구현 일치)`:
  - 현재 구현: `Event:216-236` - 단일 축 Lazy Evaluation
  - 목표: 두 축을 독립적으로 전이하되, COMPLETED 전이 시 registrationStatus 강제 CLOSED
- **호출 위치** `(현재 구현 일치)`:
  - `EventService:116` - 단건 조회 시 호출
  - `EventService:155` - 목록 조회 시 각 행사에 호출
  - `EventService:158-162` - Lazy 갱신 후 상태 변경된 행사 필터에서 제외
- **참고**: 각 축은 독립적으로 전이되므로 실행 순서가 결과에 영향을 주지 않음 (단, eventStatus가 COMPLETED가 되면 registrationStatus를 CLOSED로 강제)

### 2-5. CLOSED → OPEN 재전이 조건

등록이 CLOSED된 행사에서 다시 OPEN으로 전이되는 경우는 두 가지이다:

#### (a) 자동 재오픈 (정원 마감 후 취소로 자리 발생)

| 조건 | 결과 |
|------|------|
| `closeReason == CAPACITY_FULL && !isFull() && now < regEnd && eventStatus != CANCELED` | OPEN으로 재전이, `closeReason = null` |
| `closeReason == CAPACITY_FULL && !isFull() && now >= regEnd` | CLOSED 유지 (마감일 경과) |
| `closeReason == CAPACITY_FULL && isFull()` | CLOSED 유지 (여전히 정원 초과) |
| `closeReason == DEADLINE_PASSED` | CLOSED 유지 (자동 재오픈 불가) |
| `closeReason == MANUAL_CLOSE` | CLOSED 유지 (자동 재오픈 불가) |

- **관련 코드** `(현재 구현 일치)`: `Event:266-272` - `reopenIfCapacityAvailable()`
- **경계값 주의**: 자동 재오픈 조건은 `now < regEnd` (exclusive)이나, 등록 기간은 `now <= regEnd` (inclusive). `now == regEnd` 시점에 정원 마감 취소로 자리가 발생해도 자동 재오픈이 되지 않는다. 이는 마감 직전 재오픈 후 즉시 기한 만료로 재마감되는 것을 방지하기 위한 의도적 설계이다.

#### (b) 수동 재오픈 (운영자 판단)

EVT-INV-13의 5가지 조건을 모두 만족할 때, `closeReason` 종류와 무관하게 운영자가 등록을 재오픈할 수 있다.

| 조건 | 위반 시 |
|------|---------|
| OPERATOR+ 권한 | `EventAccessDeniedException` |
| `eventStatus ∉ {COMPLETED, CANCELED}` | 거부 (행사가 종료/취소된 상태) |
| `now <= registrationEndAt` | 거부 (기한 만료 — `regEnd` 연장 후 재시도) |
| `!isFull()` | 거부 (OPEN 직후 자동 재마감 방지) |
| `reason` 비어있지 않음 | 거부 (감사 이력 필수) |

- **관련 코드** `(현재 구현 일치)`: 새로운 `reopenRegistration(reason)` 메서드 구현 필요

### 2-6. 금지된 전이 (Invalid Transition)

#### registrationStatus 축

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| NOT_STARTED → CLOSED (단독) | 거부 | OPEN 단계를 거쳐야 함. 단, 행사 취소(EVT-INV-11)에 의한 강제 전환은 예외 |
| OPEN → NOT_STARTED | 거부 | 역방향 전이 불가 |
| CLOSED → NOT_STARTED | 거부 | 역방향 전이 불가 |

#### eventStatus 축

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| UPCOMING → COMPLETED | 거부 | ONGOING 단계를 거쳐야 함 |
| ONGOING → UPCOMING | 거부 | 역방향 전이 불가 |
| COMPLETED → 어떤 상태든 | 거부 | 종단 상태 (EVT-INV-06) |
| CANCELED → COMPLETED | 거부 | CANCELED에서는 재활성화(UPCOMING/ONGOING)만 가능 |

---

## 3. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 3-1. 행사 생성 입력값 (CreateEventRequest)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `title` | 1~100자 문자열 | null, 빈 문자열, 101자 이상 | 1자 (최소), 100자 (최대), 101자 (초과) | `@NotBlank`, `@Size(max=100)` |
| `description` | 비어있지 않은 문자열 | null, 빈 문자열 | - | `@NotBlank` |
| `location` | 1~200자 문자열 | null, 빈 문자열, 201자 이상 | 1자 (최소), 200자 (최대), 201자 (초과) | `@NotBlank`, `@Size(max=200)` |
| `eventStartAt` | 유효한 Instant | null | - | `@NotNull` |
| `eventEndAt` | `eventStartAt` 이후 | null, `eventStartAt` 이전 | `eventStartAt`과 동일 (허용), `eventStartAt` 직전 (거부) | `@NotNull` |
| `registrationStartAt` | 현재 시간 이후 | null, 과거 시간 | 현재 시간 직전 (거부), 현재 시간 직후 (허용) | `@NotNull` |
| `registrationEndAt` | `registrationStartAt` 이후, `eventEndAt` 이전 또는 동일 | null, `registrationStartAt` 이전, `eventEndAt` 이후 | `eventStartAt`과 동일 (허용), `eventEndAt`과 동일 (허용) | `@NotNull` |
| `capacity` | 1 이상 정수 | null, 0, 음수 | 1 (최소 유효), 0 (최대 무효) | `@NotNull`, `@Min(1)` |
| `registrationType` | `AUTO_APPROVE` 또는 `MANUAL_APPROVE` | null, 유효하지 않은 값 | - | `@NotNull` |

### 3-2. 행사 수정 입력값 (UpdateEventRequest)

`CreateEventRequest`와 동일하되, 다음 차이점:
- `registrationType` 필드 **없음** (생성 시 결정, 수정 불가)
- `registrationStartAt`에 대한 미래 제약 **없음** (기존 값 보존 가능)
- 수정 범위는 `eventStatus`에 따라 다름 (EVT-INV-07). COMPLETED에서는 수정 불가, ONGOING에서는 `eventStartAt`/`registrationStartAt` 변경 불가

### 3-3. 날짜 경계값 분석

| 경계 조건 | 유효/무효 | 검증 로직 |
|----------|---------|----------|
| `regStart == regEnd` | **무효** | EVT-INV-02의 `regStart < regEnd` (strict) 제약에 의해 `InvalidEventDateException` 발생 |
| `regStart > regEnd` | **무효** | `InvalidEventDateException` |
| `regStart == eventStart` | **무효** | `!regStart.isBefore(eventStart)` → true → 예외 발생 |
| `regStart` 1ms before `eventStart` | **유효** | `regStart.isBefore(eventStart)` → true |
| `regEnd == eventStart` | **유효** | 등록이 행사 시작 시점에 마감 (기존에는 무효였으나 변경) |
| `regEnd == eventEnd` | **유효** | 등록이 행사 종료까지 가능 |
| `regEnd > eventEnd` | **무효** | `regEnd.isAfter(eventEnd)` → true → 예외 발생 |
| `eventStart == eventEnd` | **유효** | `eventStart.isAfter(eventEnd)` → false |
| `eventStart > eventEnd` | **무효** | `InvalidEventDateException` |

### 3-4. 겹침 시나리오 엣지 케이스

등록 기간과 행사 기간이 겹칠 때(`eventStartAt < registrationEndAt`) 발생하는 시나리오:

| 시나리오 | 시간 배치 | 상태 변화 |
|---------|----------|----------|
| 행사 시작 후 OPEN 유지 | `regEnd > eventStart`, `eventStart` 도래 | reg=OPEN, event=ONGOING (모집중+진행중) |
| 겹침 중 정원 마감 | 위 상태에서 정원 도달 | reg=CLOSED(CAPACITY_FULL), event=ONGOING |
| 겹침 중 정원 재오픈 | 위 상태에서 취소 발생, `now < regEnd` | reg=OPEN, event=ONGOING |
| 등록 마감일 도래 | `regEnd` 경과 | reg=CLOSED(DEADLINE_PASSED), event=ONGOING |
| 수동 마감 후 행사 중 | 운영자가 행사 중 등록 마감 | reg=CLOSED(MANUAL_CLOSE), event=ONGOING |
| 행사 취소 | 운영자가 UPCOMING/ONGOING에서 취소 | reg=CLOSED(MANUAL_CLOSE), event=CANCELED |
| 행사 재활성화 | 취소 후 운영자가 복원 | Lazy Evaluation으로 두 축 모두 현재 시간 기반 복원 |
| 기한 만료 후 수동 재오픈 시도 | `now > regEnd`, 운영자 재오픈 시도 | **거부** (`regEnd` 연장 필요) |
| 기한 연장 후 수동 재오픈 | `regEnd` 연장 → 운영자 재오픈 | reg=OPEN, closeReason=null |
| 수동 마감 후 수동 재오픈 | `now <= regEnd`, 운영자 재오픈 | reg=OPEN, closeReason=null |

### 3-5. 정원 경계값 분석

| 값 | 유효/무효 | 발생 위치 |
|----|---------|----------|
| `null` | **무효** | `InvalidEventCapacityException` (엔티티), DTO `@NotNull` |
| `-1` | **무효** | `InvalidEventCapacityException` |
| `0` | **무효** | `InvalidEventCapacityException` |
| `1` | **유효** (최소) | 1명 신청 시 즉시 정원 마감 |
| `Integer.MAX_VALUE` | **유효** | 실질적으로 무제한 |

---

## 4. 권한/보안 정책 (RBAC & Authorization)

### 4-1. 역할별 접근 제어 매트릭스

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 행사 목록 조회 | 401 | O | O | O | O |
| 행사 상세 조회 | 401 | **403** | O | O | O |
| 행사 생성 | 401 | 403 | 403 | **O** | **O** |
| 행사 수정 | 401 | 403 | 403 | **O** | **O** |
| 행사 삭제 | 401 | 403 | 403 | **O** (신청자 없을 때만) | **O** (신청자 없을 때만) |
| 행사 수동 마감 | 401 | 403 | 403 | **O** | **O** |
| 행사 취소 | 401 | 403 | 403 | **O** | **O** |
| 행사 재활성화 | 401 | 403 | 403 | **O** | **O** |
| 등록 수동 재오픈 | 401 | 403 | 403 | **O** | **O** |

### 4-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 위치 |
|----|----------|----------|----------|
| SEC-EVT-01 | 준회원이 행사 상세 조회 시도 | `AssociateMemberNotAllowedException` (403) | `EventService:122-124` |
| SEC-EVT-02 | 일반 회원(MEMBER)이 행사 생성 시도 | `EventAccessDeniedException` (403) | `EventService:270-274` |
| SEC-EVT-03 | 일반 회원이 행사 수정 시도 | `EventAccessDeniedException` (403) | `EventService:283-286` |
| SEC-EVT-04 | 일반 회원이 행사 삭제 시도 | `EventAccessDeniedException` (403) | `EventService:283-286` |
| SEC-EVT-10 | 신청자가 있는 행사 삭제 시도 | `EventNotDeletableException` (400) | `EventService.deleteEvent()` |
| SEC-EVT-05 | 일반 회원이 행사 수동 마감 시도 | `EventAccessDeniedException` (403) | `EventService:283-286` |
| SEC-EVT-06 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |
| SEC-EVT-07 | 일반 회원이 행사 취소 시도 | `EventAccessDeniedException` (403) | `(현재 구현 일치)` |
| SEC-EVT-08 | 일반 회원이 행사 재활성화 시도 | `EventAccessDeniedException` (403) | `(현재 구현 일치)` |
| SEC-EVT-09 | 일반 회원이 등록 수동 재오픈 시도 | `EventAccessDeniedException` (403) | `(현재 구현 일치)` |

### 4-3. 권한 검증 방식 차이

| 서비스 | 검증 방식 | 검증 예외 |
|--------|---------|----------|
| `EventService.createEvent` | 서비스 내부 `validateOperatorPermission()` | `EventAccessDeniedException` |
| `EventService.updateEvent/deleteEvent/closeEvent` | 서비스 내부 `validateEditPermission()` | `EventAccessDeniedException` |
| `EventService.cancelEvent` | 서비스 내부 `validateEditPermission()` | `EventAccessDeniedException` `(현재 구현 일치)` |
| `EventService.reactivateEvent` | 서비스 내부 `validateEditPermission()` | `EventAccessDeniedException` `(현재 구현 일치)` |
| `EventService.reopenRegistration` | 서비스 내부 `validateEditPermission()` | `EventAccessDeniedException` `(현재 구현 일치)` |
| `EventService.getEvent` | 서비스 내부 `user.isAssociate()` 직접 확인 | `AssociateMemberNotAllowedException` |
| `EventService.getEventList` | **검증 없음** (인증된 사용자 모두 접근 가능) | - |

---

## 5. 관측 가능성 (Observability & Audit)

### 5-1. 컨트롤러 로그 메시지

| 엔드포인트 | 로그 메시지 | 관련 코드 |
|-----------|-----------|----------|
| POST `/api/v1/events` | `행사 생성 요청 - userId: {}, title: {}` | `EventController:59` |
| GET `/api/v1/events` | `행사 목록 조회 요청 - eventStatus: {}, registrationStatus: {}` | `EventController:78` |
| GET `/api/v1/events/{eventId}` | `행사 상세 조회 요청 - eventId: {}, userId: {}` | `EventController:92` |
| PUT `/api/v1/events/{eventId}` | `행사 수정 요청 - eventId: {}, userId: {}` | `EventController:112` |
| DELETE `/api/v1/events/{eventId}` | `행사 삭제 요청 - eventId: {}, userId: {}` | `EventController:129` |
| POST `/api/v1/events/{eventId}/close` | `등록 마감 요청 - eventId: {}, userId: {}, reason: {}` | `(현재 구현 일치)` |
| POST `/api/v1/events/{eventId}/cancel` | `행사 취소 요청 - eventId: {}, userId: {}, reason: {}` | `(현재 구현 일치)` |
| POST `/api/v1/events/{eventId}/reactivate` | `행사 재활성화 요청 - eventId: {}, userId: {}, reason: {}` | `(현재 구현 일치)` |
| POST `/api/v1/events/{eventId}/reopen-registration` | `등록 재오픈 요청 - eventId: {}, userId: {}, reason: {}` | `EventController:208` |

### 5-2. Soft Delete 감사 이력

| 필드 | 저장 내용 | 관련 컬럼 |
|------|---------|----------|
| 삭제 여부 | `true` | `event_deleted` |
| 삭제 시각 | 삭제 시점 타임스탬프 | `event_deleted_at` |
| 삭제자 | 운영자 ID | `event_deleted_by` |

### 5-3. 행사 상태 변경 감사 이력 (`event_status_change_histories`)

사용자가 직접 수행한 행사 상태 변경을 `EventStatusChangeHistory` 엔티티에 기록한다.

| 필드 | 저장 내용 | 컬럼명 |
|------|---------|--------|
| 행사 ID | 대상 행사 | `event_status_change_histories_event_id` |
| 변경자 ID | 운영자 ID | `event_status_change_histories_changed_by_id` |
| 변경자 학번 | 운영자 학번 (비정규화) | `event_status_change_histories_changed_by_student_id` |
| 변경 유형 | `EventChangeType` enum | `event_status_change_histories_change_type` |
| 이전 값 | 변경 전 상태명 | `event_status_change_histories_previous_value` |
| 이후 값 | 변경 후 상태명 | `event_status_change_histories_new_value` |
| 사유 | 변경 사유 (모든 수동 상태 변경 시 필수) | `event_status_change_histories_reason` |
| 생성 시각 | 이력 기록 시각 | `event_status_change_histories_created_at` |

- **구현 방식** `(현재 구현 일치)`: `@EventListener` + `REQUIRES_NEW` TransactionTemplate (`RecordEventStatusChangeService`)
- **FK 없음**: soft-delete/탈퇴 후에도 이력 영구 보존
- **인덱스**: `event_id`, `changed_by_id`, `change_type`, `created_at`

### 5-4. 관측 가능성 누락 사항

| 항목 | 현황 | 영향 |
|------|------|------|
| Lazy Evaluation 상태 변경 로그 | **없음** (의도적 — 자동 전이는 노이즈) | 자동 상태 전이 추적 불가 |
| 행사 수정 시 변경 내용 로그 | **없음** | 어떤 필드가 변경되었는지 추적 불가 |
| 행사 생성 완료 로그 | **없음** (요청 로그만 존재) | 생성 성공/실패 구분 불가 |
| ~~행사 취소/재활성화 이력~~ | **해결됨** | `EventStatusChangeHistory`에 기록 |

---

## 6. 테스트 전략 (Test Strategy)

### 6-1. 현재 테스트 현황

**도메인 단위 테스트** (순수 Java, Mock 사용):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `EventTest` | 41개 | 생성(5), 상태 전이(9), Lazy Evaluation(2), 신청자 수 관리(5), 조회(7), 시간 겹침(8), 수정(5) - 중첩 클래스 구조 |

**서비스 단위 테스트** (Mockito):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `EventServiceTest` | 23개 | createEvent(5), getEvent(4), getEventList(4), updateEvent(3), deleteEvent(4), closeEvent(3) - 중첩 클래스 구조 |

### 6-2. 테스트-검증 항목 매핑

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| EVT-INV-01 (신청자 수 범위) | `EventTest:EVT-020~024` (도메인), 통합 테스트에서 원자적 UPDATE 검증 | **커버됨** |
| EVT-INV-02 (날짜 순서) | `EventServiceTest:createEvent_WithInvalidEventDates`, `createEvent_WithRegEndAfterEventStart` | **부분 커버** (새 제약 `regStart < eventStart`, `regEnd <= eventEnd` 미검증) |
| EVT-INV-03 (생성 시 미래 제약) | - | **누락** |
| EVT-INV-04 (정원 최소값) | `EventTest:EVT-003~005` (0, 음수, null) | **커버됨** |
| EVT-INV-05 (초기 상태) | `EventTest:EVT-001` (UPCOMING, currentCount=0 assertion) | **커버됨** (2축 모델 반영 완료) |
| EVT-INV-06 (COMPLETED 종단) | `EventTest:EVT-015` (COMPLETED→OPEN 거부) | **커버됨** (COMPLETED 종단 상태 검증 완료) |
| EVT-INV-07 (상태별 수정 정책) | `EventTest:EVT-052,053` | **부분 커버** (ONGOING 부분 수정, CANCELED 수정 허용, COMPLETED 수정 불가 미검증) |
| EVT-INV-08 (closeReason 정합성) | `EventTest:EVT-012,013,014,016` (각 마감 사유 + 재오픈 시 null) | **커버됨** (registrationStatus 축으로 재해석 필요) |
| EVT-INV-09 (soft delete 필터링) | `EventServiceTest:getEvent_DeletedEvent_ThrowsException` | **커버됨** |
| EVT-INV-15 (신청자 있는 행사 삭제 불가) | `EventServiceTest:SVC-EVT-036,037` | **커버됨** |
| EVT-INV-10 (교차 축 불변조건) | - | **커버됨** (교차 축 불변조건 테스트 구현 완료) |
| EVT-INV-11 (CANCELED 시 CLOSED 강제) | - | **커버됨** (cancel 테스트에서 registrationStatus 검증) |
| EVT-INV-12 (유효 복합 상태 조합) | - | **커버됨** (유효 복합 상태 조합 테스트 구현 완료) |
| EVT-INV-13 (수동 재오픈 조건) | - | **커버됨** (수동 재오픈 조건 테스트 구현 완료) |
| EVT-INV-14 (수동 재오픈 감사 이력) | - | **커버됨** (감사 이력 기록 테스트 구현 완료) |

#### 상태 전이 커버리지

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| UPCOMING → OPEN (수동) | `EventTest:EVT-010` | **커버됨** |
| UPCOMING → OPEN (Lazy) | - | **누락** (Lazy Evaluation 단독 테스트) |
| OPEN → CLOSED (수동 마감) | `EventTest:EVT-012` | **커버됨** |
| OPEN → CLOSED (기한 만료) | `EventTest:EVT-013` | **커버됨** |
| OPEN → CLOSED (정원 초과) | `EventTest:EVT-021` | **커버됨** |
| CLOSED → OPEN (자동 재오픈) | `EventTest:EVT-014,023` | **커버됨** |
| CLOSED → OPEN (수동 재오픈) | - | **커버됨** (EventServiceTest 및 EventTest에서 검증) |
| CLOSED → ONGOING (Lazy) | `EventTest:EVT-060` | **커버됨** (2축에서는 eventStatus 전이로 재해석) |
| ONGOING → COMPLETED (Lazy) | `EventTest:EVT-061` | **커버됨** |
| UPCOMING → CANCELED (수동) | - | **커버됨** (EventTest에서 검증) |
| ONGOING → CANCELED (수동) | - | **커버됨** (EventTest에서 검증) |
| CANCELED → UPCOMING/ONGOING (재활성화) | - | **커버됨** (EventTest에서 검증) |
| UPCOMING → COMPLETED (금지) | `EventTest:EVT-017` | **커버됨** |
| COMPLETED → OPEN (금지) | `EventTest:EVT-015` | **커버됨** |
| OPEN → ONGOING (금지) | `EventTest:EVT-018` | **커버됨** (2축에서는 해당 없음) |
| Lazy 갱신 후 필터 재적용 | `EventServiceTest:getEventList_LazyUpdateChangesStatus_FilteredOut` | **커버됨** |

#### 권한 검증 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-EVT-01 (준회원 조회 차단) | `EventServiceTest:getEvent_WithAssociateMember_ThrowsException` | **커버됨** |
| SEC-EVT-02 (일반 회원 생성 차단) | `EventServiceTest:createEvent_WithRegularMember_ThrowsException` | **커버됨** |
| SEC-EVT-03 (일반 회원 수정 차단) | `EventServiceTest:updateEvent_WithRegularMember_ThrowsException` | **커버됨** |
| SEC-EVT-04 (일반 회원 삭제 차단) | `EventServiceTest:deleteEvent_WithRegularMember_ThrowsException` | **커버됨** |
| SEC-EVT-05 (일반 회원 마감 차단) | `EventServiceTest:closeEvent_WithRegularMember_ThrowsException` | **커버됨** |
| SEC-EVT-06 (비인가 접근 부작용 없음) | - | **누락** |
| SEC-EVT-07 (일반 회원 취소 차단) | - | **커버됨** (EventServiceTest에서 검증) |
| SEC-EVT-08 (일반 회원 재활성화 차단) | - | **커버됨** (EventServiceTest에서 검증) |
| SEC-EVT-09 (일반 회원 재오픈 차단) | - | **커버됨** (EventServiceTest에서 검증) |

### 6-3. 발견된 누락 및 개선 사항

#### 기존 GAP 항목

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-EVT-01 | 생성 시 `registrationStartAt` 미래 제약 테스트 부재 | **중간** | 미해결 |
| GAP-EVT-02 | 날짜 검증에서 `regStart > regEnd` 케이스 단위 테스트 부재 | **낮음** | 미해결 |
| GAP-EVT-03 | COMPLETED 상태에서 CLOSED/ONGOING 전이 시도 금지 테스트 부재 | **낮음** | 미해결 |
| GAP-EVT-04 | Lazy Evaluation 단독 테스트 부재 (UPCOMING→OPEN 자동 전이) | **낮음** | 미해결 (EVT-060,061에서 CLOSED→ONGOING, ONGOING→COMPLETED는 커버) |
| GAP-EVT-05 | 비인가 접근 시 DB 상태 변경 없음(부작용 없음) 명시적 테스트 부재 | **중간** | 미해결 |
| GAP-EVT-06 | 컨트롤러 레벨 RBAC 검증 테스트 (MockMvc) 부재 | **중간** | 미해결 |
| GAP-EVT-07 | `regEnd == eventStart` 경계값 테스트 부재 → 2축 모델에서 **유효 케이스**로 변경, 테스트 방향 전환 필요 | **낮음** | 미해결 |
| GAP-EVT-08 | `regStart == regEnd` 경계값 테스트 부재 | **낮음** | 미해결 |

#### 신규 GAP 항목 (2축 모델)

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-EVT-09 | 2축 모델에서 겹침 기간(`reg=OPEN, event=ONGOING`) 동작 테스트 | **높음** | 해결 |
| GAP-EVT-10 | 겹침 기간 중 정원 마감 → 자동 재오픈 테스트 | **높음** | 미해결 |
| GAP-EVT-11 | 행사 취소 시 `registrationStatus=CLOSED` 강제 전환 테스트 | **높음** | 해결 |
| GAP-EVT-12 | 행사 재활성화 후 Lazy Evaluation 올바른 상태 복원 테스트 | **중간** | 미해결 |
| GAP-EVT-13 | 교차 축 불변조건(유효/무효 조합) 테스트 | **높음** | 해결 |
| GAP-EVT-14 | `regEnd <= eventEnd` 경계값 검증 테스트 | **중간** | 미해결 |
| GAP-EVT-15 | `regStart < eventStart` 경계값 검증 테스트 | **중간** | 미해결 |
| GAP-EVT-16 | 수동 재오픈 조건 검증 (정원 초과 시 거부, `regEnd` 경과 시 거부, 사유 필수) | **높음** | 해결 |
| GAP-EVT-17 | 수동 재오픈 감사 이력 기록 테스트 | **중간** | 해결 |
| GAP-EVT-18 | ONGOING에서 허용 필드(`title`, `description`, `location`, `eventEndAt`, `registrationEndAt`, `capacity`) 수정 성공 테스트 | **높음** | 해결 |
| GAP-EVT-19 | ONGOING에서 금지 필드(`eventStartAt`, `registrationStartAt`) 변경 시도 거부 테스트 | **높음** | 해결 |
| GAP-EVT-20 | ONGOING에서 `capacity` 감소 시 `capacity >= currentCount` 검증 테스트 | **중간** | 미해결 |
| GAP-EVT-21 | CANCELED에서 전체 필드 수정 성공 테스트 | **중간** | 해결 |
| GAP-EVT-22 | CANCELED에서 수정 → 재활성화 E2E 흐름 테스트 | **중간** | 미해결 |
| GAP-EVT-23 | COMPLETED에서 수정 시도 시 `EventNotEditableException` 발생 테스트 | **낮음** | 미해결 |
| GAP-EVT-24 | 신청자가 있는 행사 삭제 거부 테스트 (EVT-INV-15) | **높음** | 해결 |

---

## 관련 문서

- [행사 신청 검증 기준서](./event-registration-verification-criteria.md) - 행사 신청 관련 검증 기준
  - **주의**: REG-INV-05의 "OPEN 상태" 기준이 2축 모델에서 `registrationStatus == OPEN`으로 변경됨 (기존 `event.getStatus() == OPEN` → `event.getRegistrationStatus() == OPEN`)
- [IGRUS_WEB_PRD_V2.md](../../feature/common/IGRUS_WEB_PRD_V2.md) - PRD 행사 섹션 (5. 행사)
- [회원가입/승인/강등 검증 기준서](../verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [문의 검증 기준서](../inquiry-verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
