# 행사(Event) 기능 명세서

**작성일**: 2026-02-03
**버전**: 1.0
**우선순위**: P1

---

## 1. 개요

### 1.1 목적
동아리 행사의 생성, 관리, 신청 기능을 제공합니다. 운영진은 행사를 생성하고 관리하며, 회원들은 행사에 신청할 수 있습니다.

### 1.2 범위
- 행사 CRUD (생성, 조회, 수정, 삭제)
- 행사 신청/취소
- 신청 승인/거절 (선발제)
- 행사 상태 관리

---

## 2. 사용자 스토리

| ID | 역할 | 스토리 | 우선순위 |
|----|------|--------|---------|
| US-E01 | 운영진 | 새로운 행사를 등록할 수 있다 | P1 |
| US-E02 | 운영진 | 등록한 행사 정보를 수정할 수 있다 | P1 |
| US-E03 | 운영진 | 행사를 취소/마감할 수 있다 | P1 |
| US-E04 | 운영진 | 행사 신청자 목록을 조회할 수 있다 | P1 |
| US-E05 | 운영진 | 선발제 행사의 신청을 승인/거절할 수 있다 | P1 |
| US-E06 | 정회원 | 모집 중인 행사에 신청할 수 있다 | P1 |
| US-E07 | 정회원 | 신청한 행사를 취소할 수 있다 | P1 |
| US-E08 | 정회원 | 내 신청 목록을 조회할 수 있다 | P2 |
| US-E09 | 회원 | 행사 목록과 상세 정보를 조회할 수 있다 | P1 |

---

## 3. 기능 요구사항 (Functional Requirements)

### 3.1 행사 도메인

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-E001 | 행사 상태 관리 | UPCOMING → OPEN → CLOSED → COMPLETED, CANCELED 상태 전이 |
| FR-E002 | 신청 방식 | AUTO_APPROVE(선착순), MANUAL_APPROVE(선발제) 두 가지 방식 |
| FR-E003 | 정원 관리 | 정원은 1명 이상, 선착순은 정원 초과 시 자동 마감 |
| FR-E004 | 신청 기간 | registrationStartAt ~ registrationEndAt 기간 내에만 신청 가능 |
| FR-E005 | 행사 시간 | eventStartAt ~ eventEndAt으로 행사 시간 관리 |
| FR-E006 | 마감 사유 | CAPACITY_FULL, DEADLINE_PASSED, MANUAL_CLOSE 세 가지 |

### 3.2 행사 CRUD

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-E010 | 행사 생성 | 운영진 이상만 행사 생성 가능 |
| FR-E011 | 행사 조회 | 모든 회원이 행사 목록/상세 조회 가능 |
| FR-E012 | 행사 수정 | UPCOMING, OPEN, CLOSED 상태에서만 수정 가능 |
| FR-E013 | 행사 삭제 | 작성자 또는 관리자만 삭제 가능 |
| FR-E014 | 상태 변경 | 운영진이 수동으로 OPEN, CLOSED, CANCELED 상태 변경 가능 |

### 3.3 행사 신청

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-E020 | 신청 권한 | 정회원(MEMBER) 이상만 신청 가능, 준회원 불가 |
| FR-E021 | 중복 신청 방지 | 동일 행사에 중복 신청 불가 |
| FR-E022 | 선착순 신청 | AUTO_APPROVE 행사는 신청 즉시 REGISTERED 상태 |
| FR-E023 | 선발제 신청 | MANUAL_APPROVE 행사는 신청 시 WAITING 상태 |
| FR-E024 | 신청 취소 | 본인의 신청만 취소 가능 |
| FR-E025 | 재신청 | 취소한 신청은 다시 신청 가능 |
| FR-E026 | 동시성 제어 | 비관적 락으로 정원 초과 방지 |

### 3.4 신청 승인/거절 (선발제)

| ID | 요구사항 | 설명 |
|----|---------|------|
| FR-E030 | 승인 권한 | 운영진 이상만 승인/거절 가능 |
| FR-E031 | 승인 대상 | WAITING 상태의 신청만 승인/거절 가능 |
| FR-E032 | 승인 시 정원 확인 | 정원 초과 시 승인 불가 |
| FR-E033 | 승인 효과 | 승인 시 APPROVED 상태, 정원 카운트 증가 |
| FR-E034 | 거절 효과 | 거절 시 REJECTED 상태, 정원 카운트 변화 없음 |

---

## 4. 비기능 요구사항 (Non-Functional Requirements)

| ID | 요구사항 | 설명 |
|----|---------|------|
| NFR-E001 | 동시성 | 동시 신청 시 정원 초과 방지 (비관적 락) |
| NFR-E002 | 응답 시간 | 신청 API 응답 시간 500ms 이내 |
| NFR-E003 | 데이터 무결성 | 신청자 수와 실제 신청 레코드 일치 보장 |

---

## 5. 데이터 모델

### 5.1 Event 엔티티

| 필드 | 타입 | 설명 | 제약조건 |
|------|-----|------|---------|
| id | Long | 행사 ID | PK, Auto Increment |
| user | User | 작성자(운영진) | FK, NOT NULL |
| title | String | 행사 제목 | NOT NULL, 최대 100자 |
| description | String | 행사 설명 | NOT NULL, TEXT |
| location | String | 행사 장소 | NOT NULL, 최대 200자 |
| eventStartAt | Instant | 행사 시작 일시 | NOT NULL |
| eventEndAt | Instant | 행사 종료 일시 | NOT NULL |
| registrationStartAt | Instant | 신청 시작일 | NOT NULL |
| registrationEndAt | Instant | 신청 마감일 | NOT NULL |
| capacity | Integer | 정원 | NOT NULL, >= 1 |
| currentCount | int | 현재 신청자 수 | NOT NULL, default 0 |
| status | EventStatus | 행사 상태 | NOT NULL, ENUM |
| closeReason | EventCloseReason | 마감 사유 | NULLABLE, ENUM |
| registrationType | EventRegistrationType | 신청 방식 | NOT NULL, ENUM |

### 5.2 EventRegistration 엔티티

| 필드 | 타입 | 설명 | 제약조건 |
|------|-----|------|---------|
| id | Long | 신청 ID | PK, Auto Increment |
| event | Event | 행사 | FK, NOT NULL |
| user | User | 신청자 | FK, NOT NULL |
| registeredAt | Instant | 신청 시각 | NOT NULL |
| status | EventRegistrationStatus | 신청 상태 | NOT NULL, ENUM |

### 5.3 Enum 정의

**EventStatus (행사 상태)**
| 값 | 설명 | 신청 가능 | 수정 가능 |
|----|------|---------|---------|
| UPCOMING | 예정 | No | Yes |
| OPEN | 모집 중 | Yes | Yes |
| CLOSED | 마감 | No | Yes |
| COMPLETED | 완료 | No | No |
| CANCELED | 취소 | No | No |

**EventRegistrationType (신청 방식)**
| 값 | 설명 | 신청 시 상태 |
|----|------|------------|
| AUTO_APPROVE | 자동 승인 (선착순) | REGISTERED |
| MANUAL_APPROVE | 수동 승인 (선발제) | WAITING |

**EventRegistrationStatus (신청 상태)**
| 값 | 설명 | isActive |
|----|------|---------|
| REGISTERED | 신청 완료 (선착순) | Yes |
| WAITING | 승인 대기 (선발제) | No |
| APPROVED | 승인됨 (선발제) | Yes |
| REJECTED | 거절됨 | No |
| CANCELED | 취소됨 | No |

**EventCloseReason (마감 사유)**
| 값 | 설명 |
|----|------|
| CAPACITY_FULL | 정원 초과 |
| DEADLINE_PASSED | 기한 만료 |
| MANUAL_CLOSE | 수동 마감 |

---

## 6. 상태 전이 규칙

### 6.1 EventStatus 상태 전이

```
UPCOMING → OPEN (모집 시작)
UPCOMING → CANCELED (행사 취소)
OPEN → CLOSED (마감)
OPEN → COMPLETED (완료)
OPEN → CANCELED (행사 취소)
CLOSED → OPEN (재오픈 - 정원 여유 시)
CLOSED → COMPLETED (완료)
CLOSED → CANCELED (행사 취소)
```

### 6.2 EventRegistrationStatus 상태 전이

```
REGISTERED → CANCELED (본인 취소)
WAITING → APPROVED (운영진 승인)
WAITING → REJECTED (운영진 거절)
WAITING → CANCELED (본인 취소)
APPROVED → CANCELED (본인 취소)
CANCELED → REGISTERED (재신청 - 선착순)
CANCELED → WAITING (재신청 - 선발제)
```

---

## 7. API 엔드포인트

### 7.1 행사 API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|-----|
| POST | /api/v1/events | 행사 생성 | OPERATOR+ |
| GET | /api/v1/events | 행사 목록 조회 | ALL |
| GET | /api/v1/events/{id} | 행사 상세 조회 | ALL |
| PUT | /api/v1/events/{id} | 행사 수정 | OPERATOR+ |
| DELETE | /api/v1/events/{id} | 행사 삭제 | OPERATOR+ |
| PATCH | /api/v1/events/{id}/status | 상태 변경 | OPERATOR+ |

### 7.2 신청 API

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|-----|
| POST | /api/v1/events/{id}/registrations | 행사 신청 | MEMBER+ |
| DELETE | /api/v1/events/{id}/registrations | 신청 취소 | MEMBER+ |
| GET | /api/v1/events/{id}/registrations | 신청자 목록 | OPERATOR+ |
| GET | /api/v1/my/event-registrations | 내 신청 목록 | MEMBER+ |
| POST | /api/v1/registrations/{id}/approve | 승인 | OPERATOR+ |
| POST | /api/v1/registrations/{id}/reject | 거절 | OPERATOR+ |

---

## 8. 예외 처리

| 예외 | ErrorCode | HTTP 상태 | 발생 조건 |
|------|----------|----------|----------|
| EventNotFoundException | EVENT_NOT_FOUND | 404 | 행사 미존재 |
| EventRegistrationNotFoundException | EVENT_REGISTRATION_NOT_FOUND | 404 | 신청 미존재 |
| EventCapacityFullException | EVENT_CAPACITY_FULL | 400 | 정원 초과 |
| AlreadyRegisteredException | EVENT_ALREADY_REGISTERED | 400 | 중복 신청 |
| AlreadyCanceledException | EVENT_ALREADY_CANCELED | 400 | 이미 취소된 신청 |
| AssociateMemberNotAllowedException | EVENT_ASSOCIATE_NOT_ALLOWED | 403 | 준회원 신청 시도 |
| OperatorPermissionRequiredException | EVENT_OPERATOR_REQUIRED | 403 | 운영진 권한 필요 |
| EventNotOpenException | EVENT_NOT_OPEN | 400 | OPEN 상태가 아님 |
| EventNotInRegistrationPeriodException | EVENT_NOT_IN_REGISTRATION_PERIOD | 400 | 신청 기간 아님 |
| NotManualApproveEventException | EVENT_NOT_MANUAL_APPROVE | 400 | 선발제 행사가 아님 |
| InvalidRegistrationStatusException | EVENT_INVALID_REGISTRATION_STATUS | 400 | 유효하지 않은 신청 상태 |
| InvalidEventStateTransitionException | EVENT_INVALID_STATE_TRANSITION | 400 | 유효하지 않은 상태 전이 |
| EventNotEditableException | EVENT_NOT_EDITABLE | 400 | 수정 불가 상태 |
| InvalidEventCapacityException | EVENT_INVALID_CAPACITY | 400 | 유효하지 않은 정원 |

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-03 | - | 최초 작성 |
