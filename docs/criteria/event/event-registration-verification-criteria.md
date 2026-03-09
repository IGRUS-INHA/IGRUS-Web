# 행사 신청 (Event Registration) 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-03-06
> **Scope**: 행사 신청(Register), 취소(Cancel), 재신청(Re-Register), 승인(Approve), 거절(Reject), 되돌리기(Revert), 동시성 제어(Concurrency), 2축 모델 연동
> **상태 모델**: 2축 모델 연동 (registrationStatus + eventStatus) — [행사 검증 기준서](./event-verification-criteria.md) 참조
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)

> **✅ 리팩토링 완료**: 이 문서는 기존 단일 축 FSM(EventStatus 5상태)에서 **2축 모델**(registrationStatus + eventStatus)로 재설계된 사양을 기술한다. 코드 리팩토링이 완료되어 모든 항목이 현재 구현과 일치한다.

## 목적

이 문서는 행사 신청(Event Registration) 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 7개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 중복 신청 방지, 신청 방식별 초기 상태, 권한 제약, 2축 모델 교차 불변조건 |
| 2 | 상태 모델 | EventRegistrationStatus FSM (선착순/선발제 분기), 행사 2축 상태와의 연동 |
| 3 | 시스템 경계와 책임 분리 | 원자적 UPDATE 동시성 제어, flush/clear 영속성 컨텍스트 관리 |
| 4 | 입력 도메인 분할과 경계값 | 신청 시점, 정원 경계 |
| 5 | 권한/보안 정책 | RBAC (ASSOCIATE 차단, OPERATOR+ 관리), 본인 취소 |
| 6 | 관측 가능성 | 컨트롤러/서비스 로그, 원자적 UPDATE 실패 로그 |
| 7 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황, 누락 식별 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### REG-INV-01: 동일 사용자의 동일 행사 중복 신청 방지

> 동일 사용자가 동일 행사에 대한 신청 레코드는 최대 1건만 존재한다.

- **검증 계층**: DB UNIQUE 제약조건 (`uk_event_registrations_event_user`) + 서비스 레벨 검증
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistration:19-22` - `@UniqueConstraint(columnNames = {"event_registrations_event_id", "event_registrations_user_id"})`
  - `EventRegistrationService:108-111` - 기존 신청 존재 시 재신청 또는 중복 예외 분기
- **위반 시 예외**: `AlreadyRegisteredException` (서비스), `DataIntegrityViolationException` (DB)
- **특이사항**: 취소된 신청(CANCELED)은 삭제되지 않고 상태만 변경되므로, 재신청 시 기존 레코드를 재활용 (`reRegister()`)

### REG-INV-02: 선착순(AUTO_APPROVE) 신청 시 즉시 REGISTERED

> 선착순 행사에 신청하면 초기 상태가 `REGISTERED`이다.

- **사후조건**: `registration.getStatus() == REGISTERED`
- **관련 코드** `(현재 구현 일치)`: `EventRegistration:70-71` - `event.isAutoApprove() ? REGISTERED : WAITING`
- **검증 방법**: 선착순 행사 신청 후 상태 assertion

### REG-INV-03: 선발제(MANUAL_APPROVE) 신청 시 WAITING

> 선발제 행사에 신청하면 초기 상태가 `WAITING`이다.

- **사후조건**: `registration.getStatus() == WAITING`
- **관련 코드** `(현재 구현 일치)`: `EventRegistration:70-72`
- **검증 방법**: 선발제 행사 신청 후 상태 assertion

### REG-INV-04: 준회원(ASSOCIATE) 조건부 신청 제한

> `UserRole.ASSOCIATE` 사용자는 `allowExternal == false`인 행사에 신청할 수 없다. `allowExternal == true`인 행사에서는 준회원도 기존 `/registrations` 엔드포인트를 통해 신청할 수 있다.

- **사전조건**: `!(user.isAssociate() && !event.getAllowExternal())`
- **위반 시 예외**: `AssociateMemberNotAllowedException`
- **관련 코드** `(현재 구현 일치)`: `EventRegistrationService` — `user.isAssociate() && !event.getAllowExternal()` 조건부 차단
- **변경 이력**: 외부인 행사 신청 기능 도입으로 무조건 차단에서 조건부 차단으로 변경 ([외부인 행사 신청 검증 기준서](./external-event-registration-verification-criteria.md) Section 0-1, EXT-INV-05 참조)

### REG-INV-05: registrationStatus == OPEN + 신청 기간 내에서만 신청 가능

> 행사의 `registrationStatus == OPEN`이고 현재 시간이 `registrationStartAt`~`registrationEndAt` 범위 내일 때만 신청 가능하다.

- **사전조건**: `event.getRegistrationStatus() == OPEN && registrationStartAt <= now <= registrationEndAt`
- **위반 시 예외**:
  - `EventNotOpenException` (registrationStatus != OPEN인 경우)
  - `EventNotInRegistrationPeriodException` (기간 외)
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistrationService:452-455` - `validateEventIsOpen()`: `event.getRegistrationStatus() != RegistrationStatus.OPEN`
  - `EventRegistrationService:464-468` - `validateRegistrationPeriod()`
- **2축 모델 참고**: `registrationStatus == OPEN && eventStatus == ONGOING` (행사 진행 중 등록)도 유효한 신청 상태이다 (EVT-INV-12 참조). 2축 모델에서 `registrationStatus`만으로 신청 가능 여부를 판단하므로, `eventStatus`와 무관하게 `registrationStatus == OPEN`이면 신청 가능하다.
- **주의사항**: 선발제 **승인**은 신청 기간 종료 후에도 가능 (별도 정책)

### ~~REG-INV-06: 시간 겹침 행사 동시 신청 불가~~ (제거됨)

> ~~사용자의 확정된 신청(REGISTERED, APPROVED) 중 신청하려는 행사의 진행 시간과 겹치는 신청이 있으면 신청할 수 없다.~~

- **상태**: 제거됨 — 행사 시간이 겹쳐도 자유롭게 신청할 수 있도록 제약이 완화되었다.
- **제거된 코드**: `validateNoTimeOverlap()`, `validateNoExternalTimeOverlap()`, `existsOverlappingRegistration()`, `existsOverlappingExternalRegistration()`, `EventTimeOverlapException`

### REG-INV-07: 승인/거절은 선발제(MANUAL_APPROVE)에서만 가능

> 승인(`approve`), 거절(`reject`), 되돌리기(`revert`)는 선발제 행사에서만 수행 가능하다.

- **사전조건**: `event.isManualApprove() == true`
- **위반 시 예외**: `NotManualApproveEventException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistrationService:265-267` - approve 시 검증
  - `EventRegistrationService:323-325` - reject 시 검증
  - `EventRegistrationService:377-379` - revert 시 검증

### REG-INV-08: 승인/거절은 WAITING 상태에서만 가능

> 승인(`approve`)과 거절(`reject`)은 `WAITING` 상태의 신청에서만 수행 가능하다.

- **사전조건**: `registration.getStatus() == WAITING`
- **위반 시 예외**: `InvalidRegistrationStatusException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistrationService:270-272` - approve 시 상태 확인
  - `EventRegistrationService:328-330` - reject 시 상태 확인

### REG-INV-09: 되돌리기는 APPROVED/REJECTED에서만 가능

> 되돌리기(`revertToWaiting`)는 `APPROVED` 또는 `REJECTED` 상태에서만 수행 가능하다.

- **사전조건**: `registration.isApproved() || registration.isRejected()`
- **위반 시 예외**: `InvalidRegistrationStatusException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistration:104-108` - `revertToWaiting()` 내부 검증
  - `EventRegistrationService:382-384` - 서비스 레벨 이중 검증

### REG-INV-10: 되돌리기는 eventStatus == UPCOMING에서만 가능

> `eventStatus`가 `ONGOING`, `COMPLETED`, `CANCELED`인 행사에서는 되돌리기를 수행할 수 없다.

- **사전조건**: `event.getEventStatus() == UPCOMING`
- **위반 시 예외**: `EventNotEditableException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistrationService:355-357` - `event.getEventStatus() != EventStatus.UPCOMING` 검증
- **기존 대비 변경점**:
  - CANCELED 상태를 새로운 차단 조건으로 추가 (기존에는 CANCELED 상태 자체가 없었음)
  - 단일 축의 UPCOMING/OPEN/CLOSED(editable) → 2축에서는 모두 `eventStatus == UPCOMING`에 해당

### REG-INV-11: 재신청은 CANCELED 상태에서만 가능

> 재신청(`reRegister`)은 `CANCELED` 상태의 신청에서만 수행 가능하다.

- **사전조건**: `registration.isCanceled() == true`
- **위반 시 예외**: `InvalidRegistrationStatusException` (엔티티), `AlreadyRegisteredException` (서비스)
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistration:126-128` - `reRegister()` 내부 검증
  - `EventRegistrationService:437-439` - 서비스 레벨 검증

### REG-INV-12: isActive 정의

> `isActive() == true`는 `REGISTERED` 또는 `APPROVED` 상태에서만 성립한다.

- **사후조건**: `isActive() ↔ (status == REGISTERED || status == APPROVED)`
- **관련 코드** `(현재 구현 일치)`: `EventRegistration:168-171`
- **사용처**: 취소 시 신청자 수 감소 판단, 행사 상세 조회 시 신청 여부 표시

### REG-INV-13: CANCELED 행사에서 신청/재신청 불가 (신규)

> `eventStatus == CANCELED`인 행사에서는 신청, 재신청이 불가능하다.

- **사전조건**: `event.getEventStatus() != CANCELED`
- **보장 방식**: `eventStatus == CANCELED` 시 `registrationStatus == CLOSED`가 강제 보장되므로 (EVT-INV-11), REG-INV-05에 의해 자동 차단된다. 그러나 명시적으로 기술한다.
- **위반 시**: `EventNotOpenException` (`registrationStatus == CLOSED`이므로)
- **관련 코드** `(현재 구현 일치)`: `Event.cancel()` 에서 `registrationStatus = CLOSED` 강제 전환 (EVT-INV-11)
- **교차 참조**: EVT-INV-11, EVT-INV-12

### REG-INV-14: COMPLETED/CANCELED 행사에서 승인/거절 불가 (신규)

> `eventStatus`가 `COMPLETED` 또는 `CANCELED`인 행사에서는 신청 승인(`approve`)과 거절(`reject`)을 수행할 수 없다.

- **사전조건**: `event.getEventStatus() ∉ {COMPLETED, CANCELED}`
- **위반 시 예외**: `EventNotEditableException`
- **관련 코드** `(현재 구현 일치)`:
  - `EventRegistrationService:247-249` - approve 시 `eventStatus == COMPLETED || CANCELED` 검증
  - `EventRegistrationService:305-307` - reject 시 `eventStatus == COMPLETED || CANCELED` 검증
- **주의사항**:
  - 선발제 승인은 `registrationStatus == CLOSED` 후에도 가능 (Section 3-1 설계 이유 참조)하나, `eventStatus`가 COMPLETED/CANCELED이면 불가
  - 되돌리기는 REG-INV-10에서 이미 차단됨 (`eventStatus == UPCOMING`만 허용)
  - `eventStatus == CANCELED`에서 행사 필드 수정은 가능(EVT-INV-07)하나, 신청 관리(승인/거절)는 불가 — 취소된 행사에서 신청을 관리할 실익이 없으며, 재활성화 후 관리해야 함
- **교차 참조**: EVT-INV-06 (COMPLETED 종단), EVT-INV-07 (상태별 행사 수정 정책)

---

## 2. 상태 모델 (State Machine & Transitions)

> **2축 모델 참고**: 이 섹션의 `EventRegistrationStatus` FSM은 행사 자체의 상태 모델과 독립적이다. 행사 상태는 2축 모델(`registrationStatus` + `eventStatus`)로 관리되며, 행사의 `registrationStatus`가 `OPEN`인지를 신청 전제조건으로 사용한다. 상세는 [행사 검증 기준서](./event-verification-criteria.md) Section 2 참조.

### 2-1. 선착순(AUTO_APPROVE) 행사의 EventRegistrationStatus FSM

```
┌────────────┐         cancel()         ┌──────────┐
│ REGISTERED │ ──────────────────────> │ CANCELED │
└────────────┘ <────────────────────── └──────────┘
                     reRegister()
```

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| (생성) → REGISTERED | `EventRegistration.create()` | 선착순 행사 | `status = REGISTERED`, `registeredAt = now` | `EventRegistration:70-71` |
| REGISTERED → CANCELED | `registration.cancel()` | 본인 요청 | `status = CANCELED`, 행사 `currentCount--` | `EventRegistration:95-97`, `EventRegistrationService:168-181` |
| CANCELED → REGISTERED | `registration.reRegister()` | `registrationStatus == OPEN`, 기간 내, 정원 여유 | `status = REGISTERED`, `registeredAt` 갱신 | `EventRegistration:125-133` |

### 2-2. 선발제(MANUAL_APPROVE) 행사의 EventRegistrationStatus FSM

```
                      approve()
                 ┌──────────────> ┌──────────┐
┌─────────┐     │                │ APPROVED │ ───┐
│ WAITING │ ────┤                └────┬─────┘    │ cancel()
└─────────┘     │                     │          ▼
     ▲          │  reject()      revertToWaiting()  ┌──────────┐
     │          └──────────────> ┌──────────┐       │ CANCELED │
     │                          │ REJECTED │ ───┘  └──────────┘
     │                          └────┬─────┘        ▲
     │                               │               │ cancel()
     │          revertToWaiting()    │               │
     └───────────────────────────────┘               │
     └───────────────────────────────────────────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| (생성) → WAITING | `EventRegistration.create()` | 선발제 행사 | `status = WAITING`, `registeredAt = now` | `EventRegistration:70-72` |
| WAITING → APPROVED | `registration.approve()` | OPERATOR+ 권한, 선발제, 정원 여유, `eventStatus ∉ {COMPLETED, CANCELED}` `(현재 구현 일치)` | `status = APPROVED`, 행사 `currentCount++` | `EventRegistration:81-83`, `EventRegistrationService:285-288` |
| WAITING → REJECTED | `registration.reject()` | OPERATOR+ 권한, 선발제, `eventStatus ∉ {COMPLETED, CANCELED}` `(현재 구현 일치)` | `status = REJECTED` | `EventRegistration:88-90`, `EventRegistrationService:333` |
| APPROVED → WAITING | `registration.revertToWaiting()` | OPERATOR+ 권한, 선발제, `eventStatus == UPCOMING` `(현재 구현 일치)` | `status = WAITING`, 행사 `currentCount--` | `EventRegistration:104-108`, `EventRegistrationService:390-399` |
| REJECTED → WAITING | `registration.revertToWaiting()` | OPERATOR+ 권한, 선발제, `eventStatus == UPCOMING` `(현재 구현 일치)` | `status = WAITING` (카운트 변경 없음) | `EventRegistration:104-108`, `EventRegistrationService:390-400` |
| WAITING → CANCELED | `registration.cancel()` | 본인 요청 | `status = CANCELED` (카운트 변경 없음, WAITING은 isActive=false) | `EventRegistration:95-97` |
| APPROVED → CANCELED | `registration.cancel()` | 본인 요청 | `status = CANCELED`, 행사 `currentCount--` | `EventRegistration:95-97`, `EventRegistrationService:168-181` |
| REJECTED → CANCELED | `registration.cancel()` | 본인 요청 | `status = CANCELED` (카운트 변경 없음, REJECTED는 isActive=false) | `EventRegistration:95-97` |
| CANCELED → WAITING | `registration.reRegister()` | `registrationStatus == OPEN`, 기간 내, 정원 여유 | `status = WAITING`, `registeredAt` 갱신 | `EventRegistration:125-133` |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| REGISTERED → WAITING | `InvalidRegistrationStatusException` | `revertToWaiting()`: APPROVED/REJECTED에서만 가능 |
| WAITING → WAITING | `InvalidRegistrationStatusException` | `revertToWaiting()`: WAITING에서 불가 |
| CANCELED → WAITING (revert) | `InvalidRegistrationStatusException` | `revertToWaiting()`: CANCELED에서 불가 |
| REGISTERED → REGISTERED (reRegister) | `InvalidRegistrationStatusException` | `reRegister()`: CANCELED에서만 가능 |
| WAITING → WAITING (reRegister) | `InvalidRegistrationStatusException` | `reRegister()`: CANCELED에서만 가능 |
| APPROVED → APPROVED (reRegister) | `InvalidRegistrationStatusException` | `reRegister()`: CANCELED에서만 가능 |
| REJECTED → REJECTED (reRegister) | `InvalidRegistrationStatusException` | `reRegister()`: CANCELED에서만 가능 |
| CANCELED → CANCELED (cancel) | `InvalidRegistrationStatusException` | `cancel()`: CANCELED에서 불가 (이미 취소됨) |

### 2-3. 신청자 수(currentCount) 변경 매트릭스

| 작업 | 선착순 | 선발제 | 관련 코드 |
|------|--------|--------|----------|
| 신청(register) | `++` (원자적 UPDATE) | 변경 없음 | `EventRegistrationService:123-130` |
| 취소(cancel, isActive=true) | `--` (원자적 UPDATE) | `--` (APPROVED만) | `EventRegistrationService:174-181` |
| 취소(cancel, isActive=false) | N/A | 변경 없음 (WAITING) | `EventRegistrationService:168` |
| 승인(approve) | N/A | `++` (원자적 UPDATE) | `EventRegistrationService:279-282` |
| 거절(reject) | N/A | 변경 없음 | - |
| 되돌리기(revert, APPROVED) | N/A | `--` (원자적 UPDATE) | `EventRegistrationService:394-399` |
| 되돌리기(revert, REJECTED) | N/A | 변경 없음 | `EventRegistrationService:394` |
| 재신청(reRegister) | `++` (원자적 UPDATE) | 변경 없음 | `EventRegistrationService:451-458` |

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. 원자적 UPDATE 동시성 제어

행사 신청에서 정원 관리는 **낙관적 락(@Version)** 대신 **원자적 SQL UPDATE**를 사용하여 동시성을 제어한다.

| 쿼리 | 조건 | 용도 | 관련 코드 |
|------|------|------|----------|
| `incrementCurrentCountIfAvailable` | `currentCount < capacity AND registrationStatus = 'OPEN' AND deleted = false` `(현재 구현 일치)` | 선착순 신청 | `EventRepository:83-85` |
| `incrementCurrentCountForApproval` | `currentCount < capacity AND deleted = false` | 선발제 승인 (registrationStatus 무관) | `EventRepository:97-100` |
| `decrementCurrentCount` | `currentCount > 0 AND deleted = false` | 취소/되돌리기 | `EventRepository:112-114` |

- **`incrementCurrentCountIfAvailable` 리팩토링**: 현재 `e.status = 'OPEN'` (단일 축) → 목표 `e.registrationStatus = 'OPEN'` (등록 축)
- **설계 이유**: 선발제 승인(`incrementCurrentCountForApproval`)은 `registrationStatus = 'OPEN'` 조건을 **포함하지 않음**. 선발제 승인은 `registrationStatus == CLOSED` 후에도 가능해야 하기 때문. 단, `eventStatus ∉ {COMPLETED, CANCELED}` 검증은 서비스 레벨에서 수행 (REG-INV-14).

### 3-2. flush/clear 영속성 컨텍스트 관리

모든 `@Modifying` 쿼리에 `flushAutomatically = true, clearAutomatically = true`가 설정되어 있다.

| 설정 | 효과 | 필요 이유 |
|------|------|----------|
| `flushAutomatically = true` | UPDATE 실행 전 영속성 컨텍스트의 대기 중인 변경을 DB에 반영 | `cancel()` 등 상태 변경이 `decrementCurrentCount` 이전에 DB에 반영되어야 함 |
| `clearAutomatically = true` | UPDATE 실행 후 영속성 컨텍스트를 초기화 | 원자적 UPDATE 결과를 이후 조회에서 정확히 읽기 위해 |

**주의사항**:
- `clearAutomatically` 이후 모든 기존 엔티티는 **detached** 상태가 됨
- 되돌리기(`revert`)에서는 `saveAndFlush(registration)`으로 먼저 상태 변경을 저장한 후 `decrementCurrentCount`를 호출하고, 이후 `findById`로 다시 조회 (`EventRegistrationService:391,403-404`)

### 3-3. 상태 갱신 후 행사 등록 상태 변경

신청자 수 변경 후 행사의 `registrationStatus`가 자동으로 조정된다:

| 시나리오 | 행사 등록 상태 변경 | 관련 코드 |
|---------|:---:|----------|
| 신청/승인 후 정원 초과 | `registrationStatus`: OPEN → CLOSED (CAPACITY_FULL) `(현재 구현 일치)` | `EventRegistrationService:518-527` |
| 취소/되돌리기 후 자리 발생 | `registrationStatus`: CLOSED → OPEN (정원 마감 + 마감일 이전 + `eventStatus ∉ {CANCELED}` 일 때만) `(현재 구현 일치)` | `EventRegistrationService:535-542` |

- **기존 대비 변경점**: 자동 재오픈 시 `eventStatus != CANCELED` 조건 추가 (CANCELED 행사에서는 재오픈 불가)
- **교차 참조**: EVT-INV-11 (CANCELED 시 CLOSED 강제), [행사 검증 기준서 Section 2-5a](./event-verification-criteria.md)

---

## 4. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 4-1. 신청 시점 경계값

| 시점 | 유효/무효 | 검증 로직 |
|------|---------|----------|
| `now < registrationStartAt` | **무효** | `EventNotInRegistrationPeriodException` |
| `now == registrationStartAt` | **유효** | `now.isBefore(registrationStartAt)` = false → 통과 |
| `registrationStartAt < now < registrationEndAt` | **유효** | 정상 범위 |
| `now == registrationEndAt` | **유효** | `now.isAfter(registrationEndAt)` = false → 통과 |
| `now > registrationEndAt` | **무효** | `EventNotInRegistrationPeriodException` |

### 4-2. 정원 경계값 (선착순)

| 상태 | currentCount | 신청 결과 | 행사 등록 상태 변경 |
|------|:---:|---------|:---:|
| 여유 | `capacity - 2` | 성공, REGISTERED | 없음 |
| 마지막 1자리 | `capacity - 1` | 성공, REGISTERED | `registrationStatus`: OPEN → CLOSED (CAPACITY_FULL) |
| 정원 초과 | `capacity` | `EventCapacityFullException` | 없음 (`registrationStatus == CLOSED` 또는 `incrementCurrentCountIfAvailable` 실패) |

### ~~4-3. 시간 겹침 경계값~~ (제거됨)

> 시간 겹침 검증이 제거되어 더 이상 적용되지 않는다.

### 4-4. 취소 시 카운트 변경 분기

| 취소 전 상태 | isActive | 카운트 감소 | 행사 재오픈 가능 |
|:---:|:---:|:---:|:---:|
| REGISTERED | true | **예** | 예 (`registrationStatus == CLOSED(CAPACITY_FULL)` + 마감일 미경과 + `eventStatus ∉ {CANCELED}`) |
| APPROVED | true | **예** | 예 (위와 동일 조건) |
| WAITING | false | **아니오** | 아니오 |
| REJECTED | false | **아니오** | 아니오 |

---

## 5. 권한/보안 정책 (RBAC & Authorization)

### 5-1. 역할별 접근 제어 매트릭스

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 행사 신청 (allowExternal=false) | 401 | **403** | **O** | **O** | **O** |
| 행사 신청 (allowExternal=true) | 401 | **O** | **O** | **O** | **O** |
| 외부인 신청 (`/external`) | **O** (인증 불필요) | N/A | N/A | N/A | N/A |
| 신청 취소 (본인) | 401 | O | O | O | O |
| 신청 취소 (관리자, `/cancel`) | 401 | 403 | 403 | **O** | **O** |
| 내 신청 목록 조회 | 401 | O | O | O | O |
| 신청자 목록 조회 | 401 | 403 | 403 | **O** | **O** |
| 신청 승인 (선발제) | 401 | 403 | 403 | **O** | **O** |
| 신청 거절 (선발제) | 401 | 403 | 403 | **O** | **O** |
| 승인/거절 되돌리기 | 401 | 403 | 403 | **O** | **O** |

> **변경 이력**: 외부인 행사 신청 기능 도입으로 행사 신청 행이 allowExternal 조건별로 분리되었고, 외부인 신청 행과 관리자 취소 행이 추가됨 ([외부인 행사 신청 검증 기준서](./external-event-registration-verification-criteria.md) Section 5-1 참조)

### 5-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 위치 |
|----|----------|----------|----------|
| SEC-REG-01 | 준회원이 allowExternal=false 행사에 신청 시도 | `AssociateMemberNotAllowedException` (403) | `EventRegistrationService` — `user.isAssociate() && !event.getAllowExternal()` |
| SEC-REG-01a | 준회원이 allowExternal=true 행사에 신청 시도 | 201 Created (정상 신청) | `EventRegistrationService` — 조건부 허용 |
| SEC-REG-02 | 일반 회원이 신청자 목록 조회 시도 | `OperatorPermissionRequiredException` (403) | `EventRegistrationService:223-224` |
| SEC-REG-03 | 일반 회원이 신청 승인 시도 | `OperatorPermissionRequiredException` (403) | `EventRegistrationService:261-263` |
| SEC-REG-04 | 일반 회원이 신청 거절 시도 | `OperatorPermissionRequiredException` (403) | `EventRegistrationService:319-321` |
| SEC-REG-05 | 일반 회원이 되돌리기 시도 | `OperatorPermissionRequiredException` (403) | `EventRegistrationService:368-370` |
| SEC-REG-06 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |

### 5-3. 권한 검증 방식

| 서비스 메서드 | 검증 방식 | 검증 예외 |
|-------------|---------|----------|
| `registerEvent` | `user.isAssociate() && !event.getAllowExternal()` 조건부 확인 | `AssociateMemberNotAllowedException` |
| `cancelRegistration` | **검증 없음** (인증된 사용자 본인의 신청만 조회 가능) | - |
| `getMyRegistrations` | **검증 없음** (인증된 사용자 본인 데이터) | - |
| `getRegistrationList` | `validateOperatorPermission(user)` | `OperatorPermissionRequiredException` |
| `approveRegistration` | `validateOperatorPermission(user)` | `OperatorPermissionRequiredException` |
| `rejectRegistration` | `validateOperatorPermission(user)` | `OperatorPermissionRequiredException` |
| `revertRegistration` | `validateOperatorPermission(user)` | `OperatorPermissionRequiredException` |

---

## 6. 관측 가능성 (Observability & Audit)

### 6-1. 컨트롤러 로그 메시지

| 엔드포인트 | 로그 메시지 | 관련 코드 |
|-----------|-----------|----------|
| POST `/api/v1/events/{eventId}/registrations` | `행사 신청 요청 - eventId: {}, userId: {}` | `EventRegistrationController:62` |
| DELETE `/api/v1/events/{eventId}/registrations` | `신청 취소 요청 - eventId: {}, userId: {}` | `EventRegistrationController:79` |
| GET `/api/v1/my/registrations` | `내 신청 목록 조회 요청 - userId: {}` | `EventRegistrationController:93` |
| GET `/api/v1/events/{eventId}/registrations` | `신청자 목록 조회 요청 - eventId: {}, userId: {}` | `EventRegistrationController:114` |
| POST `/api/v1/registrations/{registrationId}/approve` | `신청 승인 요청 - registrationId: {}, userId: {}` | `EventRegistrationController:132` |
| POST `/api/v1/registrations/{registrationId}/reject` | `신청 거절 요청 - registrationId: {}, userId: {}` | `EventRegistrationController:150` |
| POST `/api/v1/registrations/{registrationId}/revert` | `승인/거절 되돌리기 요청 - registrationId: {}, userId: {}` | `EventRegistrationController:170` |

### 6-2. 서비스 에러 로그

| 이벤트 | 로그 레벨 | 로그 메시지 | 관련 코드 |
|--------|---------|-----------|----------|
| 취소 시 카운트 감소 실패 | `error` | `신청자 수 감소 실패 (이미 0): eventId={}` | `EventRegistrationService:177` |
| 되돌리기 시 카운트 감소 실패 | `error` | `신청자 수 감소 실패 (이미 0): eventId={}, registrationId={}` | `EventRegistrationService:397` |
| 원자적 UPDATE 후 행사 조회 실패 | `warn` | `행사 상태 갱신 실패: 원자적 UPDATE 이후 행사를 찾을 수 없음. eventId={}` | `EventRegistrationService:521,537` |

### 6-3. 관측 가능성 누락 사항

| 항목 | 현황 | 영향 |
|------|------|------|
| 신청 완료 로그 | **없음** (요청 로그만 존재) | 성공/실패 구분 불가 |
| 승인/거절 완료 로그 | **없음** (요청 로그만 존재) | 운영 감사 추적 불가 |
| 정원 마감 자동 전이 로그 | **없음** | 정원 마감 시점 추적 불가 |
| ~~시간 겹침으로 인한 거부 로그~~ | 제거됨 (시간 겹침 검증 제거) | - |

---

## 7. 테스트 전략 (Test Strategy)

### 7-1. 현재 테스트 현황

**도메인 단위 테스트** (순수 Java, Mock 사용):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `EventRegistrationTest` | 28개 | 생성(3), 상태 변경(5), 재신청(7), 되돌리기(5), 조회(8) - 중첩 클래스 구조 |

**서비스 단위 테스트** (Mockito):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `EventRegistrationServiceTest` | 29개 | registerEvent(11), cancelRegistration(4), getMyRegistrations(2), getRegistrationList(2), approveRegistration(5), rejectRegistration(4), revertRegistration(7) - 중첩 클래스 구조 |

**통합 테스트** (`@SpringBootTest`, non-transactional):

| 테스트 클래스 | 테스트 수 | 범위 |
|-------------|---------|------|
| `EventRegistrationIntegrationTest` | 3개 | 선착순 신청-취소(INT-001), 선착순 신청-취소-재신청(INT-002), 선발제 신청-취소-재신청(INT-003) |

### 7-2. 테스트-검증 항목 매핑

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| REG-INV-01 (중복 신청 방지) | `EventRegistrationServiceTest:SVC-006` | **커버됨** |
| REG-INV-02 (선착순 REGISTERED) | `EventRegistrationTest:REG-001`, `EventRegistrationServiceTest:SVC-001` | **커버됨** |
| REG-INV-03 (선발제 WAITING) | `EventRegistrationTest:REG-002`, `EventRegistrationServiceTest:SVC-002` | **커버됨** |
| REG-INV-04 (준회원 차단) | `EventRegistrationServiceTest:SVC-003` | **커버됨** |
| REG-INV-05 (registrationStatus OPEN + 기간 내) | `EventRegistrationServiceTest:SVC-007,008,009` | **커버됨** (현재 단일 축 기준, 2축 모델 리팩토링 후 검증 방식 업데이트 필요) |
| ~~REG-INV-06 (시간 겹침)~~ | 제거됨 (시간 겹침 검증 제거) | - |
| REG-INV-07 (선발제 전용) | `EventRegistrationServiceTest:SVC-031,036,053` | **커버됨** |
| REG-INV-08 (WAITING 상태) | `EventRegistrationServiceTest:SVC-032,037` | **커버됨** |
| REG-INV-09 (APPROVED/REJECTED 되돌리기) | `EventRegistrationTest:REG-040~044`, `EventRegistrationServiceTest:SVC-052` | **커버됨** |
| REG-INV-10 (eventStatus == UPCOMING) | `EventRegistrationServiceTest:SVC-055,056` | **커버됨** (현재 단일 축 `isEditable()` 기준, 2축 모델 리팩토링 후 CANCELED 차단 케이스 추가 필요) |
| REG-INV-11 (CANCELED 재신청) | `EventRegistrationTest:REG-022~025`, `EventRegistrationServiceTest:SVC-011` | **커버됨** |
| REG-INV-12 (isActive 정의) | `EventRegistrationTest:REG-030~034` | **커버됨** |
| REG-INV-13 (CANCELED 행사 신청/재신청 차단) | - | **커버됨** (EVT-INV-11에 의해 간접 보장, cancel 테스트에서 검증) |
| REG-INV-14 (COMPLETED/CANCELED 행사 승인/거절 차단) | - | **커버됨** (EventRegistrationServiceTest에서 검증) |

#### 상태 전이 커버리지

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| (생성) → REGISTERED | `EventRegistrationTest:REG-001` | **커버됨** |
| (생성) → WAITING | `EventRegistrationTest:REG-002` | **커버됨** |
| WAITING → APPROVED | `EventRegistrationTest:REG-010`, `EventRegistrationServiceTest:SVC-030` | **커버됨** |
| WAITING → REJECTED | `EventRegistrationTest:REG-011`, `EventRegistrationServiceTest:SVC-035` | **커버됨** |
| REGISTERED → CANCELED | `EventRegistrationTest:REG-012`, `EventRegistrationServiceTest:SVC-020` | **커버됨** |
| APPROVED → CANCELED | `EventRegistrationTest:REG-013` | **커버됨** (도메인만) |
| WAITING → CANCELED | `EventRegistrationTest:REG-014`, `EventRegistrationServiceTest:SVC-021` | **커버됨** |
| APPROVED → WAITING (revert) | `EventRegistrationTest:REG-040`, `EventRegistrationServiceTest:SVC-050` | **커버됨** |
| REJECTED → WAITING (revert) | `EventRegistrationTest:REG-041`, `EventRegistrationServiceTest:SVC-051` | **커버됨** |
| CANCELED → REGISTERED (reRegister) | `EventRegistrationTest:REG-020`, `EventRegistrationServiceTest:SVC-011` | **커버됨** |
| CANCELED → WAITING (reRegister) | `EventRegistrationTest:REG-021` | **커버됨** (도메인만) |
| REGISTERED → WAITING (금지) | `EventRegistrationTest:REG-042` | **커버됨** |
| WAITING → WAITING (금지, revert) | `EventRegistrationTest:REG-043` | **커버됨** |
| CANCELED → WAITING (금지, revert) | `EventRegistrationTest:REG-044` | **커버됨** |
| REGISTERED → REGISTERED (금지, reRegister) | `EventRegistrationTest:REG-022` | **커버됨** |
| WAITING → WAITING (금지, reRegister) | `EventRegistrationTest:REG-023` | **커버됨** |
| APPROVED → APPROVED (금지, reRegister) | `EventRegistrationTest:REG-024` | **커버됨** |
| REJECTED → REJECTED (금지, reRegister) | `EventRegistrationTest:REG-025` | **커버됨** |

#### 통합 테스트 커버리지

| 시나리오 | 커버 테스트 | 검증 대상 |
|---------|-----------|----------|
| 선착순 신청 → 취소 (DB 반영) | `EventRegistrationIntegrationTest:INT-001` | flush/clear 영속성 컨텍스트 문제 |
| 선착순 신청 → 취소 → 재신청 (DB 반영) | `EventRegistrationIntegrationTest:INT-002` | cancel + reRegister 연속 호출 |
| 선발제 신청 → 취소 → 재신청 (DB 반영) | `EventRegistrationIntegrationTest:INT-003` | WAITING 취소 시 카운트 미변경 |

#### 권한 검증 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-REG-01 (준회원 신청 차단) | `EventRegistrationServiceTest:SVC-003` | **커버됨** |
| SEC-REG-02 (일반 회원 목록 조회 차단) | `EventRegistrationServiceTest:SVC-043` | **커버됨** |
| SEC-REG-03 (일반 회원 승인 차단) | `EventRegistrationServiceTest:SVC-034` | **커버됨** |
| SEC-REG-04 (일반 회원 거절 차단) | `EventRegistrationServiceTest:SVC-038` | **커버됨** |
| SEC-REG-05 (일반 회원 되돌리기 차단) | `EventRegistrationServiceTest:SVC-054` | **커버됨** |
| SEC-REG-06 (비인가 접근 부작용 없음) | - | **누락** |

### 7-3. 발견된 누락 및 개선 사항

#### 기존 GAP 항목

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-REG-01 | APPROVED 상태에서 취소 시 카운트 감소 통합 테스트 부재 (선발제) | **중간** | 미해결 |
| GAP-REG-02 | 선발제 승인 후 정원 마감 자동 전이 통합 테스트 부재 | **중간** | 미해결 |
| GAP-REG-03 | 동시성 테스트 부재 (여러 사용자 동시 신청 시 정원 초과 방지) | **높음** | 미해결 |
| GAP-REG-04 | 비인가 접근 시 DB 상태 변경 없음 명시적 테스트 부재 | **중간** | 미해결 |
| GAP-REG-05 | 컨트롤러 레벨 RBAC 검증 테스트 (MockMvc) 부재 | **중간** | 미해결 |
| ~~GAP-REG-06~~ | ~~시간 겹침 경계값~~ 제거됨 (시간 겹침 검증 제거) | - | 해당 없음 |
| GAP-REG-07 | 선발제 재신청(CANCELED → WAITING) 서비스 레벨 테스트 부재 | **낮음** | 미해결 (도메인 테스트 REG-021은 존재) |
| GAP-REG-08 | APPROVED 상태 취소 서비스 레벨 테스트 부재 | **낮음** | 미해결 (도메인 테스트 REG-013은 존재) |

#### 신규 GAP 항목 (2축 모델)

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-REG-09 | 2축 모델에서 `registrationStatus == OPEN && eventStatus == ONGOING` 겹침 기간 신청 가능 테스트 | **높음** | 해결 |
| GAP-REG-10 | `eventStatus == CANCELED` 시 신청/재신청 불가 테스트 (`registrationStatus = CLOSED` 강제 전환 포함) | **높음** | 해결 |
| GAP-REG-11 | `eventStatus == CANCELED/COMPLETED` 시 승인/거절 불가 테스트 | **높음** | 해결 |
| GAP-REG-12 | 겹침 기간 중 정원 마감 → 취소 → 자동 재오픈 연동 테스트 | **높음** | 미해결 |
| GAP-REG-13 | CANCELED 상태에서 자동 재오픈 차단 테스트 (`eventStatus == CANCELED`이면 `registrationStatus: CLOSED → OPEN` 불가) | **중간** | 해결 |
| GAP-REG-14 | `incrementCurrentCountIfAvailable`의 SQL 조건이 `registrationStatus = 'OPEN'`으로 변경되었는지 검증하는 통합 테스트 | **높음** | 해결 |

---

## 관련 문서

- [행사 검증 기준서](./event-verification-criteria.md) - 행사 관리(CRUD, 상태), **2축 상태 모델 정의** (EVT-INV-10~14, 유효 복합 상태 조합)
  - **주의**: REG-INV-05의 "OPEN 상태" 기준이 2축 모델에서 `registrationStatus == OPEN`으로 변경됨 (기존 `event.getStatus() == OPEN` → `event.getRegistrationStatus() == OPEN`)
- [IGRUS_WEB_PRD_V2.md](../../feature/common/IGRUS_WEB_PRD_V2.md) - PRD 행사 섹션 (5. 행사)
- [회원가입/승인/강등 검증 기준서](../verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [문의 검증 기준서](../inquiry-verification-criteria.md) - 동일 형식의 기존 검증 기준서
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
