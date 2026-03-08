# 외부인 행사 신청 (External Event Registration) 검증 기준서

> **Status**: Complete
> **Last Updated**: 2026-03-06
> **Scope**: 외부인(비회원) 행사 신청, allowExternal 플래그, 준회원(ASSOCIATE) 조건부 허용, 외부인 중복 방지, 정원 공유, 외부인 설문 연동
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 이슈**: [#517 - [Backend] 외부인도 행사 신청할 수 있는 기능 구현](https://github.com/IGRUS-INHA/IGRUS-Web/issues/517)
> **관련 문서**:
> - [행사 신청 검증 기준서](./event-registration-verification-criteria.md) — 기존 회원 신청 규칙 (REG-INV-04, SEC-REG-01 영향받음)
> - [행사 검증 기준서](./event-verification-criteria.md) — 행사 3축 상태 모델
> - [설문-행사 연동 검증 기준서](./survey-event-registration-verification-criteria.md) — 설문 연동 신청 규칙

## 목적

이 문서는 **외부인(비회원) 행사 신청** 기능에서 반드시 지켜져야 하는 규칙을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 7개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | allowExternal 불변조건, 외부인 중복 방지, 정원 공유, 준회원 조건부 허용, 기존 INV 변경 |
| 2 | 상태 모델 | 외부인 신청의 EventRegistrationStatus FSM, User 없는 레코드의 상태 전이 |
| 3 | 시스템 경계와 책임 분리 | 데이터 모델 설계 결정(DECISION), 원자적 UPDATE 동시성 제어 공유 |
| 4 | 입력 도메인 분할과 경계값 | ExternalRegisterEventRequest 필드별 BVA, allowExternal 동치 분할 |
| 5 | 권한/보안 정책 | 외부인 엔드포인트 인증 불필요(security: []), 준회원 조건부 RBAC 변경 |
| 6 | 관측 가능성 | 외부인 신청 로그, 중복 방지 실패 로그 |
| 7 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황 |

---

## 0. 기존 문서 영향 분석

이 기능의 구현으로 인해 기존 [행사 신청 검증 기준서](./event-registration-verification-criteria.md)의 다음 항목이 영향을 받는다.

### 0-1. REG-INV-04 변경: "준회원(ASSOCIATE) 신청 불가" → 조건부 허용

**기존 정의**:
> `UserRole.ASSOCIATE` 사용자는 행사에 신청할 수 없다.

**변경 후 정의**:
> `UserRole.ASSOCIATE` 사용자는 `allowExternal == false`인 행사에 신청할 수 없다. `allowExternal == true`인 행사에서는 준회원도 기존 `/registrations` 엔드포인트를 통해 신청할 수 있다.

- **변경 이유**: 외부인 허용 행사에서는 준회원도 동아리 활동에 참여할 수 있어야 한다
- **변경 범위**: `EventRegistrationService.registerEvent()` line 141-142의 `user.isAssociate()` 검사를 `allowExternal` 설정에 따라 조건부로 변경
- **[RESOLVED]**: `event-registration-verification-criteria.md`의 REG-INV-04 본문 갱신 완료 (GAP-EXT-01 해결)

### 0-2. SEC-REG-01 변경: "준회원이 행사 신청 시도 → 403" → 조건부

**기존 정의**:
> 준회원이 행사 신청 시도 → `AssociateMemberNotAllowedException` (403)

**변경 후 정의**:
> 준회원이 `allowExternal == false` 행사에 신청 시도 → `AssociateMemberNotAllowedException` (403)
> 준회원이 `allowExternal == true` 행사에 신청 시도 → 201 Created (정상 신청)

- **[RESOLVED]**: `event-registration-verification-criteria.md`의 SEC-REG-01, 5-1 역할별 접근 제어 매트릭스, 5-3 권한 검증 방식 갱신 완료 (GAP-EXT-02 해결)

### 0-3. RegistrationListResponse 스키마 변경

기존 RegistrationListResponse에 다음 변경이 적용된다:

| 필드 | 기존 | 변경 후 | 비고 |
|------|------|---------|------|
| userId | integer (int64) | integer \| null | 외부인 신청의 경우 null |
| userEmail | string | string \| null | 외부인 신청의 경우 null |
| userGender | string | string \| null | 외부인 신청의 경우 null |
| userGrade | integer (int32) | integer \| null | 외부인 신청의 경우 null |
| phone | (없음) | string \| null **(신규)** | 외부인의 경우에만 표시 |
| isExternal | (없음) | boolean **(신규)** | 외부인 신청 여부 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

> **모든 DECISION 확정 완료**: DECISION-01(옵션 A: 단일 테이블), DECISION-02(서비스 레벨만), DECISION-03(옵션 A: 관리자만 취소), DECISION-04(옵션 B: 별도 테이블), DECISION-05(기본값 false), DECISION-06(studentId 기반 검증), DECISION-07(옵션 A: 기존 API 재사용), DECISION-08(방안 A: 기존 관리자 API 확장)이 모두 확정되었다.

### EXT-INV-01: 외부인 신청은 allowExternal == true 행사에서만 가능

> 외부인(비회원)은 `event.allowExternal == true`인 행사에서만 신청할 수 있다.

- **사전조건**: `event.getAllowExternal() == true`
- **위반 시**: 400 Bad Request — `ExternalRegistrationNotAllowedException` (비즈니스 예외)
- **관련 코드**: **(신규 구현 필요)**
  - `ExternalEventRegistrationService` (또는 기존 서비스 확장)에서 `event.getAllowExternal()` 검증
- **교차 참조**: OpenAPI 스펙 `POST /api/v1/events/{eventId}/registrations/external` — 400 응답

### EXT-INV-02: 외부인 중복 신청 방지 — studentId 기준

> 동일 행사에서 동일 `studentId`로 2건 이상의 활성 신청(CANCELED 제외)이 존재할 수 없다.

- **검증 계층**: 서비스 레벨 검증만 (DECISION-02 확정: DB UNIQUE 제약조건 없음)
- **위반 시**: 409 Conflict (이미 신청함) — `ExternalAlreadyRegisteredException` (비즈니스 예외)
- **동시성 주의**: DB UNIQUE 제약조건이 없으므로 동시 신청 시 서비스 레벨 검증을 모두 통과할 수 있다. 이 경우 중복 레코드가 생성될 수 있으나, 발생 빈도가 낮고 관리자가 수동 정리 가능하므로 허용한다.
- **관련 코드**: **(신규 구현 필요)** — 서비스에서 `SELECT` 후 활성 상태(CANCELED 제외) 신청 존재 여부 확인
- **비고**: OpenAPI 스펙에 "학번과 전화번호 **각각**으로 중복 신청을 방지" 명시

### EXT-INV-03: 외부인 중복 신청 방지 — phone 기준

> 동일 행사에서 동일 `phone`으로 2건 이상의 활성 신청(CANCELED 제외)이 존재할 수 없다.

- **검증 계층**: 서비스 레벨 검증만 (DECISION-02 확정: DB UNIQUE 제약조건 없음)
- **위반 시**: 409 Conflict (이미 신청함) — `ExternalAlreadyRegisteredException` (비즈니스 예외)
- **동시성 주의**: EXT-INV-02와 동일. DB UNIQUE 없으므로 극히 드문 중복 허용.
- **관련 코드**: **(신규 구현 필요)** — 서비스에서 `SELECT` 후 활성 상태(CANCELED 제외) 신청 존재 여부 확인
- **비고**: studentId와 phone은 각각 독립적으로 중복 검사된다. 즉, 같은 학번 또는 같은 전화번호로 동일 행사에 2회 신청 불가

### EXT-INV-04: 정원 공유 — 회원과 외부인 동일 capacity/currentCount

> 회원 신청과 외부인 신청은 동일한 `capacity`와 `currentCount`를 공유한다. 별도의 외부인 정원은 존재하지 않는다. (DECISION-01 확정: 옵션 A — 단일 테이블)

- **불변조건**: `event.currentCount == count(회원 활성 신청) + count(외부인 활성 신청)`
- **관련 코드**: **(신규 구현 필요)**
  - 외부인 신청 시에도 기존 `EventRepository.incrementCurrentCountIfAvailable()` 원자적 UPDATE 사용 (DECISION-01 확정: 단일 테이블이므로 기존 쿼리 그대로 사용)
- **동시성**: 회원과 외부인이 동시에 마지막 1자리에 신청하는 경우, 원자적 UPDATE로 하나만 성공
- **교차 참조**: REG-INV-05 (registrationStatus == OPEN 조건), 기존 Section 3-1 원자적 UPDATE

### EXT-INV-05: 준회원(ASSOCIATE) 조건부 허용

> 준회원은 `allowExternal == true`인 행사에서만 신청할 수 있다. `allowExternal == false`이면 기존과 동일하게 차단된다.

- **사전조건**: `!(user.isAssociate() && !event.getAllowExternal())`
- **위반 시 예외**: `AssociateMemberNotAllowedException` (403)
- **관련 코드**:
  - `EventRegistrationService:141-143` — 현재 `user.isAssociate()` 무조건 차단 `(변경 필요)`
  - 변경 후: `user.isAssociate() && !event.getAllowExternal()` 조건으로 차단
- **비고**: 준회원은 로그인된 사용자이므로 **기존 `/registrations` 엔드포인트**를 사용한다 (외부인 엔드포인트가 아님)
- **교차 참조**: REG-INV-04 변경 (Section 0-1)

### EXT-INV-06: allowExternal 기본값

> `allowExternal` 필드의 기본값은 `false`이다. 명시적으로 `true`로 설정하지 않으면 외부인 신청이 불가능하다.

- **관련 코드**: **(신규 구현 필요)**
  - `Event.java`에 `allowExternal` 필드 추가, 기본값 `false`
  - DB 마이그레이션: `ALTER TABLE events ADD COLUMN event_allow_external BOOLEAN NOT NULL DEFAULT FALSE`
- **교차 참조**: DECISION-05

### EXT-INV-07: 외부인 신청도 registrationStatus == OPEN + 기간 내 필수

> 외부인 신청도 회원 신청과 동일하게 `registrationStatus == OPEN`이고 신청 기간 내여야 한다.

- **사전조건**: 기존 REG-INV-05와 동일
- **위반 시 예외**: `EventNotOpenException`, `EventNotInRegistrationPeriodException`
- **관련 코드**: **(신규 구현 필요)** — 기존 `validateEventIsOpen()`, `validateRegistrationPeriod()` 재사용

### EXT-INV-08: UNPUBLISHED 행사에서 외부인 신청 차단

> `visibility == UNPUBLISHED`인 행사에서는 외부인 신청이 불가능하다. 행사가 존재하지 않는 것처럼 처리한다.

- **사전조건**: `event.getVisibility() == PUBLISHED`
- **위반 시**: 404 Not Found (정보 은폐)
- **관련 코드**: 기존 `EventRegistrationService:132-134`와 동일한 패턴 적용 **(신규 구현 필요)**
- **교차 참조**: EVT-INV-18 (공개 API에서 UNPUBLISHED 행사 접근 차단) — 외부인 엔드포인트는 공개 API이므로 동일 정책 적용

### EXT-INV-09: 외부인 신청 취소는 관리자만 가능

> 외부인은 인증 수단이 없으므로, 외부인 신청의 취소는 운영진(OPERATOR) 이상만 수행할 수 있다. (DECISION-03 확정: 옵션 A — 관리자만 취소)

- **사전조건**: 요청자가 `OPERATOR+` 권한 보유
- **위반 시 예외**: `AccessDeniedException` (403)
- **취소 엔드포인트**: `POST /api/v1/registrations/{registrationId}/cancel` (DECISION-08 확정: 방안 A — 기존 관리자 API 확장, Section 3-3-1 참조)
- **관련 코드**: **(신규 구현 필요)**
- **비고**: OpenAPI 스펙에 "외부인 신청 취소는 관리자만 가능합니다" 명시
- **교차 참조**: DECISION-03, DECISION-08

### EXT-INV-10: 외부인 신청 정보 필수 필드

> 외부인 신청 시 `name`, `studentId`, `phone`, `department`는 모두 필수이다.

- **사전조건**: 4개 필드 모두 non-null, non-empty, 길이 제약 충족
- **위반 시**: 400 Bad Request (Bean Validation)
- **관련 코드**: **(신규 구현 필요)** — `ExternalRegisterEventRequest` DTO + `@Valid`
- **교차 참조**: Section 4-1 (입력 도메인 BVA)

### EXT-INV-12: 외부인 신청 시 동일 학번 가입 회원 존재 검증

> 외부인 신청 시 입력된 `studentId`로 이미 가입된 회원(User)이 존재하면 외부인 신청을 거부한다. 해당 학번의 회원이 있다면 로그인하여 일반 신청 경로를 이용해야 한다.

- **사전조건**: 동일 `studentId`로 가입된 User가 존재하지 않아야 한다
- **위반 시**: 400 Bad Request — 해당 학번으로 가입된 회원이 존재하므로 로그인 후 신청하라는 메시지
- **관련 코드**: **(신규 구현 필요)** — `UserRepository`에서 studentId로 User 조회, 존재 시 예외 발생
- **비고**: 타인의 학번으로 허위 신청하는 것을 방지하는 효과도 있음

### EXT-INV-11: 외부인도 설문 연결 행사에 신청 가능

> `event.surveyId != null`인 행사에서 외부인도 `surveyAnswers`를 포함하여 신청할 수 있다. 설문 응답과 행사 신청은 단일 트랜잭션으로 원자적으로 처리된다. (DECISION-04 확정: 옵션 B — 별도 `ExternalSurveyResponse` 테이블에 저장)

- **관련 코드**: **(신규 구현 필요)**
  - `ExternalSurveyResponse` 엔티티 생성 (Survey FK + 외부인 식별 정보 + 응답 데이터)
  - 외부인 신청 시 설문 응답을 `ExternalSurveyResponse`에 저장
- **교차 참조**: SEVT-INV-06~11 (설문-행사 연동 규칙), DECISION-04
- **비고**: 설문 집계 시 기존 `SurveyResponse`와 `ExternalSurveyResponse`를 UNION하여 처리 필요

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 외부인 신청의 EventRegistrationStatus FSM

외부인 신청의 상태 전이는 회원 신청과 **동일한 FSM**을 따른다. 단, 외부인은 본인 취소가 불가능하므로 `cancel()` 트리거 주체가 다르다. (DECISION-01 확정: 옵션 A — 단일 테이블이므로 동일 `EventRegistration` 엔티티의 FSM 사용, DECISION-03 확정: 옵션 A — 관리자만 취소)

#### 선착순(AUTO_APPROVE) 행사

```
┌────────────┐     cancel() (관리자만)    ┌──────────┐
│ REGISTERED │ ────────────────────────> │ CANCELED │
└────────────┘                           └──────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| (생성) → REGISTERED | 외부인 신청 API | allowExternal==true, OPEN, 기간 내, 정원 여유 | `status = REGISTERED`, `currentCount++` |
| REGISTERED → CANCELED | 관리자 취소 | OPERATOR+ 권한 (DECISION-03 확정) | `status = CANCELED`, `currentCount--` |

**회원 신청과의 차이점**:
- 외부인 신청에는 **재신청(reRegister) 경로가 없다** — 외부인은 인증 수단이 없으므로 취소 후 재신청 불가 (DECISION-03 확정: 관리자만 취소). 단, 관리자 취소 후 동일 학번/전화로 새로운 외부인 신청은 가능 (DECISION-02 확정: 서비스 레벨만, CANCELED 제외 중복 검증)
- `cancel()` 트리거: 회원 = 본인 요청, 외부인 = 관리자만 (DECISION-03 확정)
- **시간 겹침 검증**: 외부인은 studentId 기반으로 시간 겹침 검증 수행 (DECISION-06 확정: studentId 기반 검증)

**금지된 전이 (선착순)**:

| 현재 상태 | 시도 전이 | 결과 | 근거 |
|-----------|----------|------|------|
| REGISTERED | reRegister() | 불가 — 이미 활성 신청 존재 | EXT-INV-02/03 (중복 방지) |
| CANCELED | reRegister() | 불가 — 외부인은 재신청 불가 | DECISION-03 확정 (인증 수단 부재) |
| CANCELED | (새 외부인 신청) | 가능 — 서비스 레벨에서 CANCELED 제외 중복 검증 | DECISION-02 확정: 취소 후 재신청 가능 |

#### 선발제(MANUAL_APPROVE) 행사

```
                      approve() (관리자)
                 ┌──────────────> ┌──────────┐
┌─────────┐     │                │ APPROVED │ ───┐
│ WAITING │ ────┤                └──────────┘    │ cancel() (관리자)
└─────────┘     │                                ▼
               │  reject() (관리자)         ┌──────────┐
               └──────────────> ┌──────────┐│ CANCELED │
                                │ REJECTED ││──────────┘
                                └──────────┘     ▲
                                     │           │ cancel() (관리자)
                                     └───────────┘
```

- 승인/거절/되돌리기는 기존 REG-INV-07~10과 동일한 규칙을 따른다
- 외부인 신청도 동일한 `EventRegistration` 상태 전이 메서드를 사용한다 (DECISION-01 확정: 단일 테이블)

**금지된 전이 (선발제)**:

| 현재 상태 | 시도 전이 | 결과 | 근거 |
|-----------|----------|------|------|
| WAITING | reRegister() | 불가 — 이미 활성 신청 존재 | EXT-INV-02/03 |
| WAITING | cancel() (외부인 본인) | 불가 — 인증 수단 부재 | DECISION-03 확정 |
| APPROVED | reRegister() | 불가 — 이미 활성 신청 존재 | EXT-INV-02/03 |
| APPROVED | approve() | 불가 — 이미 승인됨 | REG-INV-08 |
| REJECTED | reRegister() | 불가 — 외부인은 재신청 불가 | DECISION-03 확정 |
| REJECTED | approve() | 불가 — 되돌리기 후 재승인 필요 | REG-INV-09 |
| CANCELED | reRegister() | 불가 — 외부인은 재신청 불가 | DECISION-03 |
| CANCELED | (새 외부인 신청) | 가능 — 서비스 레벨에서 CANCELED 제외 중복 검증 | DECISION-02 확정: 취소 후 재신청 가능 |

### 2-2. 외부인 신청의 currentCount 변경 매트릭스

> 기존 회원 신청 매트릭스(event-registration-verification-criteria.md Section 2-3)와 **동일한 규칙**을 따른다.

| 작업 | 선착순 | 선발제 | 비고 |
|------|--------|--------|------|
| 외부인 신청(register) | `++` (원자적 UPDATE) | 변경 없음 | EXT-INV-04 (정원 공유) |
| 외부인 취소(cancel, 관리자) | `--` (isActive=true일 때) | `--` (APPROVED만) | 관리자만 취소 가능 (DECISION-03 확정) |
| 외부인 승인(approve) | N/A | `++` (원자적 UPDATE) | 기존 로직 재사용 |
| 외부인 거절(reject) | N/A | 변경 없음 | 기존 로직 재사용 |

---

## 3. 시스템 경계와 책임 분리 (System Boundary & SoC)

### 3-1. 원자적 UPDATE 동시성 제어 — 외부인과 공유

외부인 신청도 기존 원자적 UPDATE 쿼리를 **그대로 사용**한다 (DECISION-01 확정: 옵션 A — 단일 테이블이므로 기존 쿼리 재사용):

| 쿼리 | 조건 | 용도 |
|------|------|------|
| `incrementCurrentCountIfAvailable` | `currentCount < capacity AND registrationStatus = 'OPEN' AND deleted = false` | 외부인 선착순 신청 |
| `incrementCurrentCountForApproval` | `currentCount < capacity AND deleted = false` | 외부인 선발제 승인 |
| `decrementCurrentCount` | `currentCount > 0 AND deleted = false` | 외부인 취소/되돌리기 |

- **동시성 시나리오**: 회원 1명 + 외부인 1명이 마지막 1자리에 동시 신청 → 원자적 UPDATE로 하나만 성공, 나머지는 `EventCapacityFullException`

### 3-2. 데이터 모델 설계 결정

외부인 신청의 데이터 모델은 기존 `EventRegistration` 엔티티와의 관계에 따라 구현 방식이 달라진다. Section 8 DECISION 테이블의 DECISION-01 참조.

### 3-3. 외부인 엔드포인트 분리

| 엔드포인트 | 메서드 | 대상 | 인증 | 비고 |
|-----------|--------|------|------|------|
| `/api/v1/events/{eventId}/registrations` | POST | 회원 (MEMBER+), 준회원 (allowExternal=true일 때) | `security: [BearerAuthentication]` | 기존 |
| `/api/v1/events/{eventId}/registrations/external` | POST | 외부인 (비회원) | `security: []` (인증 불필요) | **(신규)** |
| `/api/v1/registrations/{registrationId}/cancel` | POST | 관리자 (OPERATOR+)가 외부인 신청 취소 | `security: [BearerAuthentication]` | **(신규)** — DECISION-08 확정, 아래 상세 참조 |

- **설계 이유**: 외부인은 인증 토큰이 없으므로 별도 엔드포인트로 분리. 요청 본문도 다르다 (name, studentId, phone, department 필수).
- **컨트롤러 분리**: 기존 `EventRegistrationController`에 추가하거나, 별도 `ExternalEventRegistrationController`로 분리 (DECISION 아님 — OpenAPI 스펙에 이미 `Event External Registration` 태그로 분리됨)

#### 3-3-1. 외부인 신청 취소 엔드포인트 (DECISION-08 확정: 방안 A — 기존 관리자 API 확장)

외부인 신청 취소를 위한 관리자 엔드포인트가 필요하다 (EXT-INV-09). 기존 관리자 API 패턴을 확장하여 다음과 같이 구현한다:

| 항목 | 내용 |
|------|------|
| **엔드포인트** | `POST /api/v1/registrations/{registrationId}/cancel` |
| **메서드** | POST |
| **인증** | `security: [BearerAuthentication]`, OPERATOR+ 권한 필요 |
| **요청 본문** | 없음 (registrationId로 대상 식별) |
| **응답** | 200 OK + `RegistrationResponse` |
| **URL 패턴 근거** | 기존 승인(`POST /registrations/{registrationId}/approve`), 거절(`POST /registrations/{registrationId}/reject`), 되돌리기(`POST /registrations/{registrationId}/revert`)와 동일한 패턴 |
| **적용 대상** | 외부인 신청뿐 아니라, 관리자가 회원 신청을 취소하는 경우에도 사용 가능 (기존 `DELETE /api/v1/events/{eventId}/registrations`는 본인 취소 용도로 유지) |

- **[RESOLVED]**: OpenAPI 스펙에 외부인 취소 엔드포인트 추가 완료 (GAP-EXT-03 해결, TASK-001)

---

## 4. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 4-1. ExternalRegisterEventRequest 필드별 경계값

#### name (1-50자, 필수)

| 입력값 | 유효/무효 | 예상 결과 |
|--------|---------|----------|
| null | **무효** | 400 Bad Request |
| `""` (빈 문자열) | **무효** | 400 Bad Request |
| `"김"` (1자) | **유효** | 정상 신청 |
| `"가"*50` (50자) | **유효** | 정상 신청 |
| `"가"*51` (51자) | **무효** | 400 Bad Request |

#### studentId (1-20자, 필수)

| 입력값 | 유효/무효 | 예상 결과 |
|--------|---------|----------|
| null | **무효** | 400 Bad Request |
| `""` (빈 문자열) | **무효** | 400 Bad Request |
| `"1"` (1자) | **유효** | 정상 신청 |
| `"12345678901234567890"` (20자) | **유효** | 정상 신청 |
| `"123456789012345678901"` (21자) | **무효** | 400 Bad Request |

#### phone (1-20자, 필수)

| 입력값 | 유효/무효 | 예상 결과 |
|--------|---------|----------|
| null | **무효** | 400 Bad Request |
| `""` (빈 문자열) | **무효** | 400 Bad Request |
| `"1"` (1자) | **유효** | 정상 신청 |
| `"01012345678901234567"` (20자) | **유효** | 정상 신청 |
| `"010123456789012345678"` (21자) | **무효** | 400 Bad Request |

#### department (1-100자, 필수)

| 입력값 | 유효/무효 | 예상 결과 |
|--------|---------|----------|
| null | **무효** | 400 Bad Request |
| `""` (빈 문자열) | **무효** | 400 Bad Request |
| `"공"` (1자) | **유효** | 정상 신청 |
| `"가"*100` (100자) | **유효** | 정상 신청 |
| `"가"*101` (101자) | **무효** | 400 Bad Request |

#### surveyAnswers (optional)

| 입력값 | 행사 상태 | 유효/무효 | 예상 결과 |
|--------|----------|---------|----------|
| null | 설문 미연결 | **유효** | 정상 신청 |
| null | 설문 연결 + OPEN | **무효** | 400 Bad Request — 설문 응답 필수 (DECISION-04 확정: 옵션 B) |
| `[{...}]` (유효한 응답) | 설문 연결 + OPEN | **유효** | `ExternalSurveyResponse`에 저장 + 신청 (DECISION-04 확정: 옵션 B, 원자적 트랜잭션) |
| `[{...}]` (유효한 응답) | 설문 미연결 | **유효** | surveyAnswers 무시, 정상 신청 |

### 4-2. allowExternal 동치 분할

| allowExternal | 신청자 유형 | 사용 엔드포인트 | 예상 결과 |
|:---:|:---:|:---:|:---:|
| true | 외부인(비회원) | `/registrations/external` | 201 Created |
| true | 준회원(ASSOCIATE) | `/registrations` | 201 Created (EXT-INV-05) |
| true | 정회원(MEMBER+) | `/registrations` | 201 Created (기존과 동일) |
| false | 외부인(비회원) | `/registrations/external` | 400 Bad Request (EXT-INV-01) |
| false | 준회원(ASSOCIATE) | `/registrations` | 403 Forbidden (EXT-INV-05) |
| false | 정회원(MEMBER+) | `/registrations` | 201 Created (기존과 동일) |

### 4-3. 외부인 중복 신청 경계값

| 상황 | 예상 결과 | 근거 |
|------|----------|------|
| 동일 행사 + 동일 studentId + 다른 phone | 409 Conflict | EXT-INV-02 (studentId 중복) |
| 동일 행사 + 다른 studentId + 동일 phone | 409 Conflict | EXT-INV-03 (phone 중복) |
| 동일 행사 + 동일 studentId + 동일 phone | 409 Conflict | EXT-INV-02 + EXT-INV-03 |
| 다른 행사 + 동일 studentId + 동일 phone | 201 Created | 행사별 독립적 중복 검사 |
| 동일 행사 + 다른 studentId + 다른 phone | 201 Created | 중복 아님 |
| 이전 신청이 CANCELED + 동일 studentId | 201 Created | DECISION-02 확정: CANCELED 제외 중복 검증, 취소 후 재신청 가능 |
| 동시 동일 studentId 신청 (서비스 레벨 동시 통과) | DECISION-02: 극히 드문 중복 허용 (서비스 레벨만, DB UNIQUE 미사용) | EXT-INV-02 — 관리자가 수동 정리 |
| 동시 동일 phone 신청 (서비스 레벨 동시 통과) | DECISION-02: 극히 드문 중복 허용 (서비스 레벨만, DB UNIQUE 미사용) | EXT-INV-03 — 관리자가 수동 정리 |

### 4-4. 정원 경계값 — 회원+외부인 혼합

| 회원 신청 | 외부인 신청 | capacity | 새 외부인 신청 결과 |
|:---:|:---:|:---:|:---:|
| 3 | 1 | 5 | 201 Created (currentCount: 4→5, CLOSED) |
| 3 | 2 | 5 | 400 `EventCapacityFullException` |
| 0 | 4 | 5 | 201 Created (currentCount: 4→5, CLOSED) |
| 5 | 0 | 5 | 400 `EventCapacityFullException` |

---

## 5. 권한/보안 정책 (RBAC & Authorization)

### 5-1. 역할별 접근 제어 매트릭스 — 외부인 관련

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 외부인 신청 (`/external`) | **O** (인증 불필요) | N/A | N/A | N/A | N/A |
| 행사 신청 (`/registrations`, allowExternal=true) | 401 | **O** | **O** | **O** | **O** |
| 행사 신청 (`/registrations`, allowExternal=false) | 401 | **403** | **O** | **O** | **O** |
| 외부인 신청 취소 (`/registrations/{id}/cancel`) | 401 | 403 | 403 | **O** | **O** |
| 신청자 목록 조회 (외부인 포함) | 401 | 403 | 403 | **O** | **O** |
| 외부인 신청 승인 (선발제) | 401 | 403 | 403 | **O** | **O** |
| 외부인 신청 거절 (선발제) | 401 | 403 | 403 | **O** | **O** |

### 5-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 위치 |
|----|----------|----------|----------|
| SEC-EXT-01 | 외부인이 allowExternal=false 행사에 `/external`로 신청 시도 | 400 Bad Request | 서비스 레벨 (EXT-INV-01) |
| SEC-EXT-02 | 외부인 엔드포인트에 인증 토큰 없이 접근 | 정상 처리 (인증 불필요) | SecurityConfig (`security: []`) |
| SEC-EXT-03 | 준회원이 allowExternal=false 행사에 `/registrations`로 신청 시도 | 403 Forbidden | 서비스 레벨 (EXT-INV-05) |
| SEC-EXT-04 | 준회원이 allowExternal=true 행사에 `/registrations`로 신청 시도 | 201 Created | 서비스 레벨 (EXT-INV-05) |
| SEC-EXT-05 | 일반 회원(MEMBER)이 외부인 신청 취소 시도 | 403 Forbidden | 서비스 레벨 (EXT-INV-09) |
| SEC-EXT-06 | OPERATOR가 외부인 신청 취소 시도 | 정상 취소 | 서비스 레벨 (EXT-INV-09) |
| SEC-EXT-07 | 외부인이 `allowExternal=true`이지만 `UNPUBLISHED` 행사에 신청 시도 | 404 Not Found (정보 은폐) | 서비스 레벨 (EXT-INV-08, EVT-INV-18) |

### 5-3. 외부인 엔드포인트 보안 고려사항

외부인 엔드포인트(`POST /api/v1/events/{eventId}/registrations/external`)는 인증이 불필요하므로 다음 보안 위협에 대한 대응이 필요하다:

| 위협 | 대응 방안 | 비고 |
|------|---------|------|
| 무차별 대량 신청 (DoS) | Rate Limiting (IP 기반) | Spring Security 또는 Nginx 레벨 |
| 스크립트를 이용한 자동 신청 | 향후 CAPTCHA 도입 검토 | 현 시점에서는 미구현 |
| 개인정보 노출 (신청자 목록) | 신청자 목록은 OPERATOR+ 권한 필요 (기존 정책 유지) | SEC-REG-02 |
| 타인 정보로 허위 신청 | 학번+전화번호 중복 방지로 부분 완화 | 완전한 본인인증은 범위 밖 |

---

## 6. 관측 가능성 (Observability & Audit)

### 6-1. 외부인 신청 컨트롤러 로그 메시지

| 엔드포인트 | 로그 메시지 | 비고 |
|-----------|-----------|------|
| `POST /api/v1/events/{eventId}/registrations/external` | `외부인 행사 신청 요청 - eventId: {}, name: {}, studentId: {}` | **(신규 구현 필요)** |
| 외부인 신청 취소 엔드포인트 (Section 3-3-1) | `외부인 행사 신청 취소 요청 - registrationId: {}` | **(신규 구현 필요)** |

### 6-2. 외부인 신청 서비스 로그

| 상황 | 로그 레벨 | 로그 메시지 | 비고 |
|------|---------|-----------|------|
| 외부인 신청 성공 | INFO | `외부인 행사 신청 완료 - eventId: {}, studentId: {}, registrationId: {}` | **(신규 구현 필요)** |
| 외부인 중복 신청 (studentId) | INFO | `외부인 행사 신청 거부 - eventId: {}, studentId: {}, 사유: 학번 중복` | **(신규 구현 필요)** |
| 외부인 중복 신청 (phone) | INFO | `외부인 행사 신청 거부 - eventId: {}, phone: {}, 사유: 전화번호 중복` | **(신규 구현 필요)** |
| ~~외부인 중복 신청 (DB UNIQUE 경합)~~ | ~~WARN~~ | ~~해당 없음~~ | DECISION-02: DB UNIQUE 미사용으로 확정. 서비스 레벨 검증만 수행하며 극히 드문 동시 중복은 관리자가 수동 정리 |
| allowExternal=false 행사에 외부인 신청 | INFO | `외부인 행사 신청 거부 - eventId: {}, 사유: 외부인 신청 비허용 행사` | **(신규 구현 필요)** |
| 관리자 외부인 신청 취소 | INFO | `외부인 행사 신청 취소 - eventId: {}, registrationId: {}, operatorId: {}` | **(신규 구현 필요)** |

### 6-3. 감사 추적 (Audit Trail)

- 외부인 신청 레코드는 `BaseEntity`의 `createdAt`, `updatedAt` 필드로 생성/수정 시각 추적
- `createdBy`, `updatedBy`는 외부인 신청의 경우 null (비인증 요청이므로 `SecurityAuditorAware`에서 null 반환)
- 관리자 취소 시 `updatedBy`에 취소한 운영진의 userId가 기록됨

---

## 7. 테스트 전략 (Test Strategy & Coverage)

### 7-1. 테스트-검증 항목 매핑

| 검증 항목 | 테스트 유형 | 현재 커버리지 | 비고 |
|----------|-----------|:---:|------|
| EXT-INV-01: allowExternal 검사 | Unit + Integration | 미작성 | **(신규)** |
| EXT-INV-02: studentId 중복 방지 | Unit + Integration | 미작성 | **(신규)** DB UNIQUE 경합 테스트 포함 |
| EXT-INV-03: phone 중복 방지 | Unit + Integration | 미작성 | **(신규)** DB UNIQUE 경합 테스트 포함 |
| EXT-INV-04: 정원 공유 | Integration | 부분 커버 (외부인 미검증) | 기존 정원 테스트 확장 필요 |
| EXT-INV-05: 준회원 조건부 허용 | Unit + Integration | 미작성 | **(신규)** 기존 SEC-REG-01 테스트 분기 추가 |
| EXT-INV-06: allowExternal 기본값 | Unit | 미작성 | **(신규)** |
| EXT-INV-07: OPEN + 기간 내 검증 | Integration | 부분 커버 (외부인 미검증) | 기존 테스트 재사용 가능 |
| EXT-INV-08: UNPUBLISHED 차단 | Integration | 부분 커버 (외부인 미검증) | 기존 패턴 확장 |
| EXT-INV-09: 관리자만 취소 가능 | Integration | 미작성 | **(신규)** 취소 엔드포인트 테스트 포함 |
| EXT-INV-10: 필수 필드 검증 | Unit (Bean Validation) | 미작성 | **(신규)** |
| EXT-INV-11: 외부인 설문 연동 | Integration | 미작성 | **(신규)** DECISION-04 확정 후 |
| SEC-EXT-01~07 | Integration | 미작성 | **(신규)** |
| 4-2: allowExternal 동치 분할 | Integration | 미작성 | 6가지 조합 테스트 |
| 4-3: 외부인 중복 경계값 | Integration | 미작성 | 8가지 시나리오 (DB 경합 포함) |
| 4-4: 혼합 정원 경계값 | Integration | 미작성 | 4가지 시나리오 |

### 7-2. 동시성 테스트 시나리오

| 시나리오 | 예상 결과 | 테스트 방법 |
|---------|----------|-----------|
| 회원 + 외부인이 마지막 1자리에 동시 신청 | 하나만 성공, 나머지 `EventCapacityFullException` | 멀티스레드 테스트 |
| 2명의 외부인이 동일 studentId로 동시 신청 | 서비스 레벨 검증만 수행. 극히 드문 동시 중복은 관리자가 수동 정리 (DECISION-02: DB UNIQUE 미사용) | 멀티스레드 테스트 (서비스 레벨 검증 확인) |
| 2명의 외부인이 동일 phone으로 동시 신청 | 서비스 레벨 검증만 수행. 극히 드문 동시 중복은 관리자가 수동 정리 (DECISION-02: DB UNIQUE 미사용) | 멀티스레드 테스트 (서비스 레벨 검증 확인) |
| 외부인 신청 + 관리자 취소 동시 발생 | 순차 처리 (트랜잭션 격리) | 멀티스레드 테스트 |

### 7-3. GAP 테이블 (미해결 테스트 갭)

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-EXT-01 | `event-registration-verification-criteria.md`의 REG-INV-04 본문 갱신 (Section 0-1) | **높음** | **해결됨** — REG-INV-04를 조건부 제한으로 갱신 완료 (TASK-029) |
| GAP-EXT-02 | `event-registration-verification-criteria.md`의 SEC-REG-01 및 5-1 매트릭스 갱신 (Section 0-2) | **높음** | **해결됨** — SEC-REG-01, 5-1 매트릭스, 5-3 권한 검증 방식 갱신 완료 (TASK-029) |
| GAP-EXT-03 | OpenAPI 스펙에 외부인 취소 엔드포인트 추가: `POST /api/v1/registrations/{registrationId}/cancel` (DECISION-08 확정, Section 3-3-1) | **높음** | **해결됨** — TASK-001에서 스펙 추가 완료 |
| GAP-EXT-04 | `event-verification-criteria.md`의 Event 엔티티/DTO에 `allowExternal` 필드 추가 기술 (Section 9 참조) | **중간** | **해결됨** — EVT-INV-24 추가, CreateEventRequest/UpdateEventRequest/응답 DTO 갱신 완료 (TASK-029) |
| GAP-EXT-05 | ~~EXT-INV-02/03 DB UNIQUE 경합 시 예외 변환 통합 테스트~~ | ~~높음~~ | **해결됨** — DECISION-02에 의해 DB UNIQUE 미사용으로 확정. 서비스 레벨 동시성 테스트(TC-071, TC-072)로 대체. Section 4-3, 6-2, 7-2도 갱신 완료 |
| GAP-EXT-06 | 모든 DECISION 확정 완료. 관련 불변조건 및 Section 본문 갱신 완료. | **해결됨** | 해결 |

---

## 8. DECISION 테이블 (미결정 설계 항목)

구현자가 결정해야 하는 설계 항목이다. 각 DECISION의 선택에 따라 영향받는 Section이 명시되어 있다.

> **모든 DECISION 확정 완료**: DECISION-01(옵션 A), DECISION-02(서비스 레벨만), DECISION-03(옵션 A), DECISION-04(옵션 B: 별도 테이블), DECISION-05(기본값 false), DECISION-06(studentId 기반 검증), DECISION-07(옵션 A: 기존 API 재사용), DECISION-08(방안 A)이 모두 확정되었다.

### DECISION-01: 외부인 신청 데이터 모델

| 항목 | 내용 |
|------|------|
| **질문** | 외부인 신청을 기존 `EventRegistration` 테이블에 저장할 것인가, 별도 테이블을 만들 것인가? |
| **옵션 A** | `EventRegistration.user`를 nullable로 변경 + 외부인 정보 컬럼 추가 (`externalName`, `externalStudentId`, `externalPhone`, `externalDepartment`, `isExternal`) |
| **옵션 B** | 별도 `ExternalEventRegistration` 테이블 생성 (Event FK + 외부인 정보 컬럼) |
| **옵션 A 장점** | 단일 테이블로 정원 관리 단순화, 기존 원자적 UPDATE 쿼리 그대로 사용 가능, 신청자 목록 조회 쿼리 단순 |
| **옵션 A 단점** | User FK nullable 변경으로 기존 UNIQUE 제약조건(`uk_event_registrations_event_user`) 변경 필요, null user 처리 코드 분산 |
| **옵션 B 장점** | 기존 `EventRegistration` 코드 변경 최소화, 관심사 분리 명확 |
| **옵션 B 단점** | 정원 관리를 두 테이블 조인으로 처리해야 함 (원자적 UPDATE 복잡도 증가), 신청자 목록 UNION 쿼리 필요 |
| **결정** | **옵션 A** — 정원 공유(EXT-INV-04)를 단일 테이블에서 처리하는 것이 동시성 제어에 유리하고, OpenAPI 스펙의 RegistrationListResponse가 회원/외부인 통합 응답으로 설계됨 |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-01~04, Section 2 (FSM), Section 3-1 (원자적 UPDATE), Section 4-3 (중복 방지), DECISION-02 |

### DECISION-02: 외부인 중복 방지 DB 제약조건 방식

| 항목 | 내용 |
|------|------|
| **질문** | 외부인 중복 방지를 DB 레벨에서 어떻게 보장할 것인가? |
| **전제** | OpenAPI 스펙: "학번과 전화번호 **각각**으로 중복 신청을 방지합니다" — 즉, studentId, phone 각각 개별 UNIQUE |
| **옵션 A (DECISION-01 옵션 A 선택 시)** | `EventRegistration` 테이블에 2개 UNIQUE 제약조건 추가: `UNIQUE(event_id, external_student_id)`, `UNIQUE(event_id, external_phone)`. 단, 회원 신청은 이 컬럼이 null이므로 MySQL의 UNIQUE null 허용 특성상 자동 무시됨 |
| **옵션 B (DECISION-01 옵션 B 선택 시)** | `ExternalEventRegistration` 테이블에 `UNIQUE(event_id, student_id)`, `UNIQUE(event_id, phone)` |
| **취소된 신청의 처리** | 취소된 신청(CANCELED)이 UNIQUE 제약에 포함되면 동일 학번/전화번호로 재신청 불가. MySQL 8 Generated Column 패턴 사용: `student_id_unique_key GENERATED ALWAYS AS (IF(status != 'CANCELED', student_id, NULL)) STORED` + UNIQUE on generated column |
| **결정** | **서비스 레벨만** — DB UNIQUE 제약조건 없이 서비스 레벨에서 활성 신청(CANCELED 제외) 중복 검증. 동시성으로 인한 극히 드문 중복은 허용하며 관리자가 수동 정리. 취소 후 동일 학번/전화로 재신청 가능. |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-02, EXT-INV-03, Section 4-3 (중복 경계값), DB 마이그레이션 |

### DECISION-03: 외부인 신청 취소 정책

| 항목 | 내용 |
|------|------|
| **질문** | 외부인 신청 취소를 어떤 방식으로 제공할 것인가? |
| **옵션 A** | 관리자(OPERATOR+)만 취소 가능 — 외부인은 관리자에게 취소 요청 |
| **옵션 B** | 신청 시 취소 토큰(UUID)을 발급하여 외부인이 직접 취소 가능 |
| **옵션 A 장점** | 구현 단순, 무단 취소 방지 |
| **옵션 A 단점** | 외부인 UX 불편 (관리자에게 연락해야 함) |
| **옵션 B 장점** | 외부인이 자율적으로 취소 가능, UX 향상 |
| **옵션 B 단점** | 취소 토큰 관리 필요 (DB 컬럼, 토큰 노출 시 악용 가능) |
| **결정** | **옵션 A** — OpenAPI 스펙에 "외부인 신청 취소는 관리자만 가능합니다" 명시, 초기 구현은 단순하게 |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-09, Section 2 (FSM — 재신청 경로 유무, 금지된 전이 표), Section 2-2 (currentCount 매트릭스), Section 3-3-1 (취소 엔드포인트), Section 5-1 (외부인 신청 취소 행), SEC-EXT-05~06, DECISION-08 |

### DECISION-04: 외부인 설문 응답 저장 방식

| 항목 | 내용 |
|------|------|
| **질문** | 외부인의 설문 응답을 어떻게 저장할 것인가? `SurveyResponse` 엔티티는 `User` FK가 필수(nullable=false)이다. |
| **옵션 A** | `SurveyResponse.user`를 nullable로 변경 + 외부인 식별 정보(studentId 등) 추가 |
| **옵션 B** | 외부인 설문 응답 전용 테이블 `ExternalSurveyResponse` 생성 |
| **옵션 C** | 외부인 신청 시 설문 응답을 EventRegistration 레코드에 JSON으로 저장 (설문 도메인 침범 회피) |
| **옵션 A 장점** | 기존 설문 분석/집계 로직 재사용 가능 |
| **옵션 A 단점** | 설문 도메인 코드 전체에서 null user 처리 필요, 기존 UNIQUE 제약(`uk_survey_responses_survey_user`) 변경 필요 |
| **옵션 B 장점** | 기존 설문 코드 변경 없음 |
| **옵션 B 단점** | 설문 집계 시 두 테이블 UNION 필요 |
| **옵션 C 장점** | 설문 도메인 완전 독립, 가장 단순한 구현 |
| **옵션 C 단점** | 설문 응답 분석/집계에서 외부인 응답 제외, JSON 스키마 관리 부담 |
| **결정** | **옵션 B** — 외부인 설문 응답 전용 `ExternalSurveyResponse` 테이블 생성. 기존 설문 코드 변경 없음. 설문 집계 시 두 테이블 UNION 필요. |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-11, Section 4-1 (surveyAnswers 입력 도메인), 설문 도메인 전반 |

### DECISION-05: allowExternal 기본값 및 기존 데이터 처리

| 항목 | 내용 |
|------|------|
| **질문** | `allowExternal`의 기본값은 무엇이며, 기존 행사 데이터에는 어떤 값을 적용할 것인가? |
| **결정** | 기본값 `false` (외부인 신청 비허용이 기본), 기존 데이터도 `FALSE`로 마이그레이션 |
| **DB 마이그레이션** | `ALTER TABLE events ADD COLUMN event_allow_external BOOLEAN NOT NULL DEFAULT FALSE` |
| **비고** | 기존 Event 생성 API의 `CreateEventRequest`에서 `allowExternal`은 optional (미지정 시 false) |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-06, DB 마이그레이션, Event 엔티티 |

### DECISION-06: 외부인 시간 겹침 검증 여부

| 항목 | 내용 |
|------|------|
| **질문** | 외부인 신청 시 기존 신청과의 시간 겹침을 검증할 것인가? |
| **옵션 A** | 검증하지 않음 — 외부인은 User가 없으므로 기존 `existsOverlappingRegistration(userId, ...)` 쿼리 사용 불가. studentId 기반 검증은 타교생 동명이인 등 신뢰성 부족 |
| **옵션 B** | studentId 기반으로 시간 겹침 검증 — 동일 studentId의 다른 외부인 신청과 시간 겹침 검사 |
| **결정** | **옵션 B** — studentId 기반으로 동일 studentId의 다른 외부인 신청과 시간 겹침 검사 수행. 동일 학번으로 시간이 겹치는 행사에 중복 신청 방지. |
| **상태** | **확정** |
| **영향 범위** | Section 2 (FSM 사전조건에서 시간 겹침 검증 제외), Section 4 (시간 겹침 BVA 외부인 미적용) |

### DECISION-07: 선발제 행사에서 외부인 승인/거절 동작

| 항목 | 내용 |
|------|------|
| **질문** | 선발제(MANUAL_APPROVE) 행사에서 외부인 신청의 승인/거절은 어떻게 처리할 것인가? |
| **옵션 A** | 기존 `approveRegistration`, `rejectRegistration` API를 외부인 신청에도 그대로 사용 — registrationId 기반이므로 회원/외부인 구분 불필요 |
| **옵션 B** | 외부인 전용 승인/거절 API 생성 |
| **결정** | **옵션 A** — 기존 API는 registrationId 기반으로 동작하므로 회원/외부인 구분 없이 동일하게 처리 가능. 외부인 승인 시 시간 겹침 검증은 studentId 기반으로 수행 (DECISION-06 확정). |
| **상태** | **확정** |
| **영향 범위** | Section 2 (선발제 FSM), REG-INV-06 (시간 겹침 — 외부인 예외), 기존 승인/거절 서비스 코드 |

### DECISION-08: 외부인 신청 취소 엔드포인트 방식

| 항목 | 내용 |
|------|------|
| **질문** | 외부인 신청 취소를 위한 관리자 엔드포인트를 어떻게 설계할 것인가? |
| **옵션 A** | 기존 관리자 API 패턴 확장 — `POST /api/v1/registrations/{registrationId}/cancel` 신규 생성. 기존 승인/거절/되돌리기와 동일한 URL 패턴 |
| **옵션 B** | 기존 회원 취소 API(`DELETE /api/v1/events/{eventId}/registrations`)를 확장하여 registrationId 파라미터 추가 |
| **옵션 A 장점** | 기존 승인(`/approve`), 거절(`/reject`), 되돌리기(`/revert`)와 URL 패턴 일관성 유지. registrationId 기반으로 회원/외부인 구분 불필요 |
| **옵션 A 단점** | 신규 엔드포인트 추가 필요 |
| **옵션 B 장점** | 기존 엔드포인트 재사용 |
| **옵션 B 단점** | 기존 회원 취소는 userId 기반 조회이므로 registrationId 기반과 혼재, 인터페이스 복잡도 증가 |
| **결정** | **옵션 A** — 기존 관리자 API 패턴과 일관성 유지 |
| **상태** | **확정** |
| **영향 범위** | EXT-INV-09, Section 3-3 (엔드포인트 테이블), Section 3-3-1 (취소 엔드포인트 상세), Section 5-1 (접근 제어 매트릭스), SEC-EXT-05~06, GAP-EXT-03 |

---

## 9. 부록: 기존 검증 기준서 갱신 체크리스트

구현 완료 후 다음 문서의 해당 항목을 갱신해야 한다:

| 대상 문서 | 갱신 항목 | 갱신 내용 | GAP 참조 |
|----------|----------|----------|----------|
| `event-registration-verification-criteria.md` | REG-INV-04 | "준회원 신청 불가" → "준회원은 allowExternal=true 행사에서만 신청 가능" | GAP-EXT-01 |
| `event-registration-verification-criteria.md` | SEC-REG-01 | "준회원 신청 시 403" → "allowExternal=false일 때만 403" | GAP-EXT-02 |
| `event-registration-verification-criteria.md` | Section 5-1 | 역할별 접근 제어 매트릭스에 allowExternal 조건부 행 추가 | GAP-EXT-02 |
| `event-registration-verification-criteria.md` | Section 5-3 | `registerEvent` 검증 방식 설명에 allowExternal 조건 추가 | GAP-EXT-02 |
| `event-verification-criteria.md` | Event 엔티티 필드 | `allowExternal` 필드 추가 기술 | GAP-EXT-04 |
| `event-verification-criteria.md` | CreateEventRequest / UpdateEventRequest | `allowExternal` 필드 추가 기술 | GAP-EXT-04 |
| `event-verification-criteria.md` | 응답 DTO | EventDetailResponse, EventListResponse 등에 `allowExternal` 필드 추가 기술 | GAP-EXT-04 |
| OpenAPI 스펙 (`events.yaml`) | 외부인 취소 엔드포인트 | Section 3-3-1에 기술된 관리자 취소 엔드포인트 추가 | GAP-EXT-03 |
