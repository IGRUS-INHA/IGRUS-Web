# 행사 그룹 (EventGroup) 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-03-03
> **Scope**: 행사 그룹 생성(Create), 조회(Read), 수정(Update), 삭제(Delete), 그룹-행사 연결(Association) 관리
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 문서**: [행사 검증 기준서](./event-verification-criteria.md)

## 목적

이 문서는 행사 그룹(EventGroup) 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

행사 그룹은 여러 행사(Event)를 하나의 그룹으로 묶어 관리하는 기능이다. MT, 정기 세미나 시리즈, 신입생 OT 프로그램 등 동아리 활동에서 관련 행사를 논리적으로 그룹핑하여 관리 편의성을 높인다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 그룹-행사 연결 정합성, 그룹명 제약, 소속 관계 제약 |
| 2 | 상태 모델 | EventGroup 자체는 단순 CRUD이므로 별도 FSM 없음. 단, 소속 행사의 상태 모델과의 교차 동작을 정의 |
| 3 | 입력 도메인 분할과 경계값 | 그룹 생성/수정 입력값, 행사 추가/제거 입력값 경계 |
| 4 | 권한/보안 정책 | RBAC (OPERATOR+ 그룹 관리, 일반 사용자 조회), 관리자 전용 API 접근 제어 |
| 5 | 관측 가능성 | 컨트롤러/서비스 로그 메시지, 그룹 변경 감사 추적 |
| 6 | 테스트 전략 | 테스트-검증 항목 매핑, 커버리지 현황 |

---

## 설계 결정 (DECISION)

> 이 기능은 아직 구현되지 않은 신규 기능이므로, 구현 전 결정이 필요한 사항을 명시한다. 각 DECISION은 검증 기준 전반에 영향을 미치며, 결정 후 관련 섹션을 확정해야 한다.

| ID | 질문 | 선택지 | 권장안 | 영향 범위 |
|----|------|--------|--------|----------|
| DECISION-01 | EventGroup과 Event의 관계 모델 | (A) Event에 `groupId` FK 추가 (N:1), (B) 중간 테이블 `event_group_events` (M:N), (C) Event에 nullable `groupId` 약한 참조 (Long, FK 없음) | **(C)**: 기존 Event-Survey 패턴(약한 참조)과 일관. FK 없이 soft delete 호환. | 1절 전체, 3-1, 부록 A DB 마이그레이션 |
| DECISION-02 | 하나의 행사가 여러 그룹에 소속 가능한가 | (A) 1:N -- 행사는 최대 1개 그룹에만 소속, (B) M:N -- 행사가 여러 그룹에 소속 가능 | **(A)**: MT 1차/2차를 하나의 "MT 시리즈" 그룹에 묶는 용도이므로 1:N이면 충분. M:N은 과도한 복잡도. DECISION-01에서 (C) 선택 시 자연스럽게 1:N이 됨. | EGRP-INV-03, 3-3 |
| DECISION-03 | 그룹 삭제 시 소속 행사 처리 정책 | (A) 소속 행사의 `groupId`를 null로 변경 (연결 해제), (B) 소속 행사가 있으면 삭제 거부, (C) 그룹만 soft delete하고 행사의 `groupId`는 유지 (orphan 허용) | **(A)**: 그룹 삭제 시 소속 행사는 독립 행사로 복원. 깔끔한 정합성 유지. | EGRP-INV-08, EGRP-INV-09, 3-6 |
| DECISION-04 | 그룹 조회 API 배치 (공개/관리자) | (A) 공개 API만 (`/api/v1/event-groups`), (B) 관리자 API만 (`/api/v1/admin/event-groups`), (C) 공개 + 관리자 양쪽 | **(C)**: 일반 사용자도 그룹 단위로 행사 목록을 볼 수 있어야 함 (PUBLISHED 행사만). 관리자는 모든 행사 포함. | 4절 전체, 5절 로그, 부록 B |
| DECISION-05 | 그룹 내 행사 정렬 기준 | (A) 행사 시작일(`eventStartAt`) 오름차순, (B) 그룹 추가 순서, (C) 별도 `displayOrder` 필드 | **(A)**: 시간순 정렬이 가장 직관적. 별도 정렬 필드는 관리 복잡도 증가. | EGRP-INV-07 |
| DECISION-06 | 그룹에 소속되지 않은 행사도 그룹 목록 API에서 "미분류"로 표시하는가 | (A) 미분류 행사는 별도 API/필터로 조회, (B) 그룹 목록 응답에 "미분류" 가상 그룹 포함 | **(A)**: 그룹 API는 그룹만 반환. 미분류 행사는 기존 행사 목록 API로 조회 (groupId가 null인 행사). | 3-4, 부록 B API 설계 |
| DECISION-07 | 이미 같은 그룹에 소속된 행사를 다시 추가할 때의 동작 | (A) 멱등적 성공 (200 OK, 변경 없음), (B) 충돌 거부 (409 Conflict) | **(A)**: 멱등적 성공이 클라이언트 구현 편의성과 재시도 안전성에 유리. 기존 Storage Confirm API 멱등성 패턴(COMPLETED 재요청 시 200)과 일관. | EGRP-INV-03, 3-3, GAP-EGRP-03 |
| DECISION-08 | 그룹에 소속되지 않은 행사를 제거 시도할 때의 동작 | (A) 404 Not Found (해당 그룹에 소속된 행사가 아님), (B) 400 Bad Request (잘못된 요청) | **(A)**: 그룹-행사 연결을 리소스로 보면 "존재하지 않는 연결"이므로 404가 RESTful 의미론에 부합. | 3-4, GAP-EGRP-19 |
| DECISION-09 | 그룹 수정 API의 HTTP 메서드 (PUT vs PATCH) | (A) PUT (전체 수정), (B) PATCH (부분 수정) | **(A)**: 그룹의 수정 가능 필드가 `name`과 `description` 2개뿐이므로, 항상 모든 필드를 전달하는 전체 수정(PUT)이 적합. `backend/CLAUDE.md`의 "PUT: 전체 수정, PATCH: 부분 수정" 원칙에 따름. | 부록 B API, 5절 로그 메시지 |
| DECISION-10 | 그룹 목록 조회의 페이지네이션 방식 | (A) `Page<T>` (총 개수 포함), (B) `Slice<T>` (다음 페이지 존재 여부만), (C) 페이지네이션 없음 (전체 반환) | **(C)**: 동아리 규모상 그룹 수가 수십 개 이내로 예상되므로, MVP에서는 전체 반환. 그룹 수가 증가하면 기존 `EventRegistration` 패턴(`Page<T>`)으로 전환 가능. | 3-5, 부록 B API |
| DECISION-11 | 공개 그룹 목록/상세 조회 API의 인증 요구 수준 | (A) 비인증 허용 (`permitAll()`), (B) 인증 필수 (`isAuthenticated()`) | **(B)**: 기존 행사 상세 조회가 인증 필수인 것과 일관성 유지. 행사 목록만 `GET /api/v1/events`에서 `permitAll()`이며, 행사 상세/그룹 조회 등 상세 정보는 인증 사용자에게만 제공. `ApiSecurityConfig`의 `.anyRequest().authenticated()` 기본 규칙 적용. | 4절 전체, SEC-EGRP-01~02 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### EGRP-INV-01: 그룹명 고유 제약

> 활성(deleted = false) 상태의 `EventGroup.name`은 중복될 수 없다. 수정 시에는 자기 자신을 제외하고 중복 검사한다.

- **사전조건**: 그룹 생성/수정 요청
- **사후조건**: 동일한 `name`을 가진 활성 그룹이 2개 이상 존재하지 않음
- **위반 시 예외**: `EventGroupNameDuplicateException` (409 Conflict)
- **검증 계층 (이중 검증)**:
  - **서비스 레벨**: `EventGroupRepository.existsByNameAndDeletedFalse(name)` (생성 시) / `existsByNameAndIdNotAndDeletedFalse(name, id)` (수정 시, 자기 자신 제외)
  - **DB 레벨**: `event_group_name`에 대한 조건부 유니크 제약 -- MySQL 8의 Generated Column을 활용하여 `deleted = false`인 행만 유니크 제약을 적용 (부록 A DDL 참조)
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.createEventGroup()` -- 서비스 레벨 중복 검증 후 생성
  - `EventGroupService.updateEventGroup()` -- 자기 자신(id)을 제외하고 변경된 이름 중복 검증
  - `EventGroupRepository.existsByNameAndDeletedFalse(name)` -- 생성 시 중복 확인
  - `EventGroupRepository.existsByNameAndIdNotAndDeletedFalse(name, id)` -- 수정 시 중복 확인 (자기 자신 제외)
- **주의사항**:
  - Soft delete된 그룹과 이름이 동일한 새 그룹 생성은 **허용** (deleted = true인 그룹은 DB 유니크 제약 대상에서 제외)
  - 대소문자 구분 여부는 MySQL 기본 collation(utf8mb4_0900_ai_ci, case-insensitive)에 따름
  - **동시성 보호**: 서비스 레벨 `existsBy...` 검증만으로는 동시 요청 시 race condition이 발생할 수 있으므로, DB 레벨 유니크 제약이 **반드시** 병행되어야 함. 동시 중복 삽입 시 `DataIntegrityViolationException`을 catch하여 409로 변환할 것
  - **수정 시 자기 자신 제외**: `existsByNameAndIdNotAndDeletedFalse(name, id)`를 사용하여, 이름을 변경하지 않고 설명만 수정하는 경우에도 자기 자신의 이름으로 인해 중복 예외가 발생하지 않도록 함
- **검증 방법**: 동일 이름 그룹 생성 시 예외 발생 확인, soft delete 후 동일 이름 재생성 성공 확인, 수정 시 자기 자신 이름 유지 성공 확인, 동시 생성 요청 시 하나만 성공 확인

### EGRP-INV-02: 그룹명 길이 제약

> `EventGroup.name`은 1자 이상 100자 이하여야 한다.

- **검증 계층**: DTO 레벨 (`@NotBlank`, `@Size(max=100)`) + 엔티티 레벨 컬럼 제약
- **위반 시**: `MethodArgumentNotValidException` (400 Bad Request)
- **관련 코드**: **(신규 구현 필요)**
  - `CreateEventGroupRequest.name` -- `@NotBlank @Size(max=100)`
  - `UpdateEventGroupRequest.name` -- `@NotBlank @Size(max=100)`
  - DB 컬럼: `event_group_name VARCHAR(100) NOT NULL`
- **검증 방법**: null, 빈 문자열, 101자 이상 입력 시 400 반환 확인

### EGRP-INV-03: 행사의 단일 그룹 소속 제약

> 하나의 행사(Event)는 최대 1개의 그룹에만 소속될 수 있다. (DECISION-02에 따라 다름)

- **사전조건**: 행사를 그룹에 추가하는 요청
- **사후조건**: `Event.groupId`가 null이 아닌 행사는 다른 그룹에 추가할 수 없음
- **위반 시 예외**: `EventAlreadyInGroupException` (409 Conflict)
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.addEventToGroup()` -- `event.groupId != null`이면 동작 분기
  - `Event.groupId` -- nullable Long 필드 (DECISION-01에 따라 다름)
- **주의사항**:
  - 이미 **같은 그룹**에 소속된 행사를 다시 추가 시도하면 **DECISION-07에 따라 다름**: 권장안 (A) 멱등적 성공 (200 OK, 변경 없음)
  - 이미 **다른 그룹**에 소속된 행사를 추가하려면 먼저 기존 그룹에서 제거해야 함 (409 Conflict 반환)
- **검증 방법**: 그룹 A에 소속된 행사를 그룹 B에 추가 시도 시 예외 발생 확인, 같은 그룹에 재추가 시 DECISION-07 정책에 따른 결과 확인

### EGRP-INV-04: Soft Delete 필터링

> `event_group_deleted = false`인 그룹은 모든 JPA 조회에서 자동 필터링된다.

- **근거**: `@SQLRestriction("event_group_deleted = false")` (Event 패턴과 동일)
- **`@SQLRestriction` 컬럼명**: `event_group_deleted` -- `SoftDeletableEntity.deleted` 필드가 `@AttributeOverride`로 `event_group_deleted` 컬럼에 매핑됨
- **사후조건**: soft delete된 그룹은 `findById()`, `findAll()` 등 일반 쿼리에서 반환되지 않음
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroup` 엔티티 -- `@SQLRestriction("event_group_deleted = false")` 적용
  - `SoftDeletableEntity` 상속
- **검증 방법**: soft delete 후 `findById()`, 목록 조회에서 해당 그룹 미반환 확인

### EGRP-INV-05: 초기 상태 제약

> 그룹 생성 시 초기 상태: `deleted = false`, 소속 행사 0개.

- **사후조건**: `EventGroup.create()` 반환값의 `deleted == false`, 소속 행사 목록이 비어있음
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroup.create()` -- 정적 팩토리 메서드
- **감사 필드 자동 설정**: `SoftDeletableEntity` -> `BaseEntity` 상속 체인에 의해 다음 필드가 JPA Auditing(`@EntityListeners(AuditingEntityListener.class)`)으로 자동 관리됨:
  - `createdAt` (Instant) -- `@CreatedDate`, 생성 시 자동 설정
  - `updatedAt` (Instant) -- `@LastModifiedDate`, 수정 시 자동 갱신
  - `createdBy` (Long) -- `@CreatedBy`, 생성자 ID 자동 설정
  - `updatedBy` (Long) -- `@LastModifiedBy`, 수정자 ID 자동 갱신
  - `deleted` (boolean) -- 기본값 `false`
  - `deletedAt` (Instant) -- 기본값 `null`
  - `deletedBy` (Long) -- 기본값 `null`
- **검증 방법**: 생성 직후 상태 assertion (감사 필드 포함)

### EGRP-INV-06: 소속 행사 존재 검증

> 그룹에 행사를 추가할 때, 해당 행사가 존재하고 활성(deleted = false) 상태여야 한다.

- **사전조건**: 행사 추가 요청의 `eventId`가 유효
- **위반 시 예외**: `EventNotFoundException` (404)
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.addEventToGroup()` -- `eventRepository.findById(eventId)` 결과 확인
- **주의사항**: soft delete된 행사는 `@SQLRestriction("event_deleted = false")`에 의해 조회되지 않으므로 자동으로 404 반환
- **검증 방법**: 존재하지 않는 eventId, soft delete된 eventId로 추가 시도 시 404 확인

### EGRP-INV-07: 그룹 내 행사 정렬 규칙

> 그룹에 소속된 행사 목록은 `eventStartAt` 오름차순으로 정렬된다. (DECISION-05에 따라 다름)

- **사후조건**: 그룹 상세 조회 응답의 행사 목록이 `eventStartAt` 오름차순
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.getEventGroup()` 또는 `EventRepository.findByGroupIdOrderByEventStartAtAsc()`
- **검증 방법**: 여러 행사를 그룹에 추가한 후 조회 시 정렬 순서 확인

### EGRP-INV-08: 그룹 삭제 시 소속 행사 연결 해제

> 그룹을 삭제(soft delete)하면, 소속 행사의 `groupId`가 null로 변경된다. (DECISION-03에 따라 다름)

- **트리거**: 운영자 그룹 삭제 (`deleteEventGroup()`)
- **사후조건**:
  - `EventGroup.deleted == true`
  - 기존 소속 행사의 `groupId == null`
  - 행사 자체의 상태(visibility, registrationStatus, eventStatus)에는 영향 없음
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.deleteEventGroup()` -- 소속 행사의 `groupId = null` 일괄 갱신 후 soft delete
  - `EventRepository.clearGroupId(groupId)` -- `@Modifying` 일괄 UPDATE 쿼리
- **주의사항**:
  - 행사의 다른 상태에는 절대 영향을 주지 않음 (행사 도메인 독립성 보장)
  - 트랜잭션 내에서 연결 해제와 soft delete가 원자적으로 실행되어야 함
  - `@Modifying` 쿼리 사용 시 `flushAutomatically = true, clearAutomatically = true` 설정 필요 -- 같은 트랜잭션 내에서 soft delete 전에 `clearGroupId()`의 pending write가 flush되어야 하며, persistence context 정합성을 위해 clear 필요 (프로젝트 `@Modifying` 패턴 참조)
- **검증 방법**: 그룹 삭제 후 소속 행사의 `groupId == null` 확인, 행사 상태 변경 없음 확인

### EGRP-INV-09: 행사 삭제 시 그룹 연결 해제

> 행사가 삭제(soft delete)되면, 해당 행사의 `groupId`는 삭제 전 값을 유지하되 그룹 조회 시 해당 행사는 포함되지 않는다.

- **근거**: Event의 `@SQLRestriction("event_deleted = false")`에 의해 삭제된 행사는 그룹 소속 행사 조회에서 자동 제외
- **사후조건**:
  - 삭제된 행사는 그룹 상세 조회의 행사 목록에 포함되지 않음
  - 그룹의 소속 행사 수 카운트에서 제외됨
- **관련 코드**: `Event.@SQLRestriction("event_deleted = false")` (기존 구현 활용)
- **검증 방법**: 그룹에 소속된 행사를 삭제한 후, 그룹 상세 조회에서 해당 행사 미포함 확인

### EGRP-INV-10: 그룹 설명 길이 제약

> `EventGroup.description`은 null 허용(선택 입력)이며, 입력 시 최대 500자이다.

- **검증 계층**: DTO 레벨 (`@Size(max=500)`) + 엔티티 레벨 컬럼 제약
- **관련 코드**: **(신규 구현 필요)**
  - `CreateEventGroupRequest.description` -- `@Size(max=500)` (nullable)
  - `UpdateEventGroupRequest.description` -- `@Size(max=500)` (nullable)
  - DB 컬럼: `event_group_description VARCHAR(500) NULL`
- **검증 방법**: null 입력 허용, 501자 이상 입력 시 400 반환 확인

### EGRP-INV-11: 그룹 소속 행사 수 제약 없음

> 하나의 그룹에 소속될 수 있는 행사 수에 상한이 없다.

- **근거**: 정기 세미나 시리즈 등에서 수십 개의 행사가 하나의 그룹에 소속될 수 있음
- **검증 방법**: 다수(10개 이상) 행사를 하나의 그룹에 추가 성공 확인

### EGRP-INV-12: 공개 API에서 그룹 내 UNPUBLISHED 행사 필터링

> 공개 API로 그룹 조회 시, 소속 행사 중 `visibility == PUBLISHED`인 행사만 포함된다.

- **근거**: 기존 EVT-INV-18(공개 API에서 UNPUBLISHED 행사 차단)과 일관성 유지
- **사후조건**:
  - 공개 그룹 상세 조회 응답에서 UNPUBLISHED 행사는 목록에 포함되지 않음
  - 관리자 그룹 상세 조회에서는 모든 행사 포함
- **관련 코드**: **(신규 구현 필요)**
  - `EventGroupService.getEventGroup()` -- PUBLISHED 필터 적용
  - `EventGroupService.getAdminEventGroup()` -- 필터 미적용
- **검증 방법**: PUBLISHED + UNPUBLISHED 행사가 혼재된 그룹을 공개 API로 조회 시 PUBLISHED 행사만 반환 확인

### EGRP-INV-13: 빈 그룹 허용

> 소속 행사가 0개인 그룹은 유효하며, 조회/수정/삭제가 정상 동작한다.

- **근거**: 그룹을 먼저 생성한 후 행사를 나중에 추가하는 워크플로우 지원
- **검증 방법**: 빈 그룹 생성 후 조회, 수정, 삭제 모두 정상 동작 확인

### EGRP-INV-14: eventCount 계산 기준

> 그룹 목록 조회 응답의 `eventCount`는 소프트 삭제되지 않은 활성 행사만 카운트한다.

- **근거**: `Event.@SQLRestriction("event_deleted = false")`에 의해 소프트 삭제된 행사는 JPA 조회에서 제외되므로, `eventCount`도 동일한 기준을 따름
- **추가 분기 (공개 API vs 관리자 API)**:
  - **공개 API**: `visibility == PUBLISHED`이면서 `deleted = false`인 행사만 카운트
  - **관리자 API**: `deleted = false`인 모든 행사 카운트 (UNPUBLISHED 포함)
- **관련 코드**: **(신규 구현 필요)**
- **검증 방법**: 소프트 삭제된 행사가 포함된 그룹의 `eventCount`가 활성 행사만 반영하는지 확인, 공개/관리자 API 각각에서 UNPUBLISHED 행사 카운트 차이 확인

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. EventGroup 상태

EventGroup은 별도의 FSM을 갖지 않는다. 활성/삭제의 2가지 상태만 존재하며, `SoftDeletableEntity`의 `deleted` 플래그로 관리된다.

```
ACTIVE (deleted=false) --> DELETED (deleted=true)
```

- ACTIVE: 기본 상태. CRUD 및 행사 연결/해제 가능.
- DELETED: soft delete 상태. 모든 JPA 조회에서 자동 필터링 (EGRP-INV-04).

### 2-2. 소속 행사 상태와의 교차 동작

EventGroup은 소속 행사의 상태(visibility, registrationStatus, eventStatus)를 **변경하지 않는다**. 그룹핑은 순수한 논리적 묶음이며, 행사의 라이프사이클에 영향을 주지 않는다.

| 행사 상태 변경 | 그룹에 대한 영향 |
|:---:|------|
| 행사 visibility 변경 (publish/unpublish) | 그룹 변경 없음. 단, 공개 API 조회 시 해당 행사의 노출 여부 변경 (EGRP-INV-12) |
| 행사 registrationStatus 변경 | 그룹 변경 없음 |
| 행사 eventStatus 변경 | 그룹 변경 없음 |
| 행사 soft delete | 그룹 변경 없음. 그룹 조회 시 해당 행사 자동 제외 (EGRP-INV-09) |
| 행사 수정 (title, dates 등) | 그룹 변경 없음. 그룹 내 정렬 순서가 변경될 수 있음 (EGRP-INV-07) |

---

## 3. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 3-1. 그룹 생성 입력값 (CreateEventGroupRequest)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `name` | 1~100자 문자열 | null, 빈 문자열, 공백만, 101자 이상 | 1자 (최소), 100자 (최대), 101자 (초과) | `@NotBlank`, `@Size(max=100)` |
| `description` | null, 1~500자 문자열 | 501자 이상 | null (허용), 500자 (최대), 501자 (초과) | `@Size(max=500)` (nullable) |

**생성 응답 HTTP 상태코드**: 201 Created (RESTful 원칙에 따라 리소스 생성 시 201 반환)

### 3-2. 그룹 수정 입력값 (UpdateEventGroupRequest)

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 | DTO 검증 |
|------|-----------|-----------|--------|---------|
| `name` | 1~100자 문자열 | null, 빈 문자열, 공백만, 101자 이상 | 1자 (최소), 100자 (최대), 101자 (초과) | `@NotBlank`, `@Size(max=100)` |
| `description` | null, 1~500자 문자열 | 501자 이상 | null (허용), 500자 (최대), 501자 (초과) | `@Size(max=500)` (nullable) |

**name 중복 검증**: 수정 시 자기 자신은 제외하고 중복 검증 (EGRP-INV-01). `existsByNameAndIdNotAndDeletedFalse(name, id)` 사용.

**HTTP 메서드**: PUT (DECISION-09에 따라 다름). `name`과 `description` 모든 필드를 전달하는 전체 수정 방식.

### 3-3. 그룹에 행사 추가 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `eventId` | 존재하는 활성 행사 ID | null, 존재하지 않는 ID, soft delete된 행사 ID | - |
| `groupId` (path) | 존재하는 활성 그룹 ID | 존재하지 않는 ID, soft delete된 그룹 ID | - |

**교차 검증**:

| 시나리오 | eventId 상태 | groupId 상태 | 기대 결과 |
|---------|:---:|:---:|------|
| 정상 추가 | 활성, groupId=null | 활성 | 성공 (200) |
| 이미 같은 그룹 소속 | 활성, groupId=해당그룹 | 활성 | DECISION-07에 따라 다름: 권장안 (A) 멱등적 성공 (200) |
| 다른 그룹 소속 | 활성, groupId=다른그룹 | 활성 | 거부 (409, EGRP-INV-03) |
| 행사 미존재 | 미존재 | 활성 | 거부 (404) |
| 그룹 미존재 | 활성 | 미존재 | 거부 (404) |
| 행사 삭제됨 | 삭제됨 | 활성 | 거부 (404) |
| 그룹 삭제됨 | 활성 | 삭제됨 | 거부 (404) |

### 3-4. 그룹에서 행사 제거 입력값

| 시나리오 | 기대 결과 |
|---------|----------|
| 그룹에 소속된 행사 제거 | 성공 (200), 행사의 `groupId = null` |
| 그룹에 소속되지 않은 행사 제거 시도 | DECISION-08에 따라 다름: 권장안 (A) 거부 (404 Not Found) |
| 존재하지 않는 행사 ID로 제거 시도 | 거부 (404) |
| 존재하지 않는 그룹 ID에서 제거 시도 | 거부 (404) |

### 3-5. 그룹 목록 조회 경계 시나리오

| 시나리오 | 기대 결과 |
|---------|----------|
| 그룹이 0개인 경우 목록 조회 | 빈 배열 반환 (200) |
| 그룹이 1개인 경우 | 1개 그룹 반환 |
| 그룹이 다수(50개)인 경우 | DECISION-10에 따라 다름: 권장안 (C) 전체 반환 (페이지네이션 없음) |
| 모든 그룹이 soft delete된 경우 | 빈 배열 반환 |
| 소속 행사가 모두 UNPUBLISHED인 그룹 (공개 API) | 그룹은 반환되지만 행사 목록은 비어있음, `eventCount = 0` |

### 3-6. 그룹 삭제 엣지 케이스

| 시나리오 | 초기 상태 | 작업 | 기대 결과 |
|---------|----------|------|----------|
| 빈 그룹 삭제 | 소속 행사 0개 | 삭제 | 성공 (EGRP-INV-13) |
| 행사 있는 그룹 삭제 | 소속 행사 3개 | 삭제 | 성공 + 행사 3개의 `groupId = null` (EGRP-INV-08, DECISION-03에 따라 다름) |
| 이미 삭제된 그룹 재삭제 | deleted=true | 삭제 | 거부 (404, EGRP-INV-04에 의해 조회 불가) |
| 행사가 다른 그룹에도 소속된 경우 | DECISION-02 (A)에서는 불가 | - | 해당 없음 |

---

## 4. 권한/보안 정책 (RBAC & Authorization)

### 4-1. 역할별 접근 제어 매트릭스

#### 공개 API (`/api/v1/event-groups/**`) -- DECISION-04, DECISION-11에 따라 다름

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 그룹 목록 조회 | 401 | O | O | O | O |
| 그룹 상세 조회 (PUBLISHED 행사만) | 401 | O | O | O | O |

**비인증 사용자 401 근거 (DECISION-11)**: `ApiSecurityConfig`의 `.anyRequest().authenticated()` 기본 규칙이 적용됨. 기존 행사 목록(`GET /api/v1/events`)만 `permitAll()`로 예외 설정되어 있으며, 그룹 조회는 상세 정보를 포함하므로 인증 필수 정책을 따름. 별도의 `permitAll()` 설정이 없으면 자동으로 인증 필수.

#### 관리자 API (`/api/v1/admin/event-groups/**`)

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 관리자 그룹 목록 조회 | 401 | 403 | 403 | **O** | **O** |
| 관리자 그룹 상세 조회 (모든 행사) | 401 | 403 | 403 | **O** | **O** |
| 그룹 생성 | 401 | 403 | 403 | **O** | **O** |
| 그룹 수정 (PUT) | 401 | 403 | 403 | **O** | **O** |
| 그룹 삭제 | 401 | 403 | 403 | **O** | **O** |
| 그룹에 행사 추가 | 401 | 403 | 403 | **O** | **O** |
| 그룹에서 행사 제거 | 401 | 403 | 403 | **O** | **O** |

### 4-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 | 검증 위치 |
|----|----------|----------|----------|
| SEC-EGRP-01 | 비인증 사용자가 공개 그룹 목록 조회 시도 | 401 Unauthorized | SecurityConfig `.anyRequest().authenticated()` |
| SEC-EGRP-02 | ASSOCIATE가 공개 그룹 목록 조회 | 200 OK (인증된 사용자이므로 허용) | SecurityConfig |
| SEC-EGRP-03 | 비인증 사용자가 관리자 그룹 목록 조회 시도 | 401 Unauthorized | SecurityConfig |
| SEC-EGRP-04 | ASSOCIATE가 관리자 그룹 API 접근 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-05 | MEMBER가 관리자 그룹 API 접근 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-06 | MEMBER가 그룹 생성 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-07 | MEMBER가 그룹 수정 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-08 | MEMBER가 그룹 삭제 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-09 | MEMBER가 그룹에 행사 추가 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-10 | MEMBER가 그룹에서 행사 제거 시도 | 403 Forbidden | SecurityConfig URL 규칙 |
| SEC-EGRP-11 | OPERATOR가 그룹 생성 수행 | 201 Created | SecurityConfig URL 규칙 |
| SEC-EGRP-12 | OPERATOR가 그룹 수정/삭제 수행 | 200 OK / 204 No Content | SecurityConfig URL 규칙 |
| SEC-EGRP-13 | OPERATOR가 그룹에 행사 추가/제거 | 200 OK | SecurityConfig URL 규칙 |
| SEC-EGRP-14 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 | 트랜잭션 롤백 확인 |

### 4-3. 권한 검증 방식

| API 경로 | 검증 방식 | 근거 |
|----------|---------|------|
| `/api/v1/event-groups/**` (공개) | SecurityConfig `.anyRequest().authenticated()` 기본 규칙 (DECISION-11) | 인증된 사용자 모두 조회 가능. 별도 `permitAll()` 설정 없음. |
| `/api/v1/admin/event-groups/**` (관리자) | SecurityConfig URL 규칙 `hasAnyRole("OPERATOR", "ADMIN")` | 기존 `/api/v1/admin/events/**` 패턴과 동일 |

### 4-4. SecurityConfig 변경 사항

> `/api/v1/admin/event-groups/**` 경로에 대해 OPERATOR+ 권한 규칙이 추가되어야 한다.

```java
// ApiSecurityConfig.java -- 기존 admin events 규칙과 동일 위치에 추가
.requestMatchers(
        "/api/v1/admin/dashboard",
        "/api/v1/admin/users/**",
        "/api/v1/admin/events/**",
        "/api/v1/admin/event-groups/**",  // <-- 추가
        "/api/events/*/registrations",
        "/api/v1/admin/comment-reports/**"
).hasAnyRole("OPERATOR", "ADMIN")
```

**주의사항**:
- 기존 `/api/v1/admin/**` ADMIN 전용 규칙보다 **앞에** 배치해야 함 (더 구체적인 경로 우선)
- 기존 `/api/v1/admin/events/**` 규칙과 동일한 `.requestMatchers()` 블록에 추가하는 것이 가장 안전

---

## 5. 관측 가능성 (Observability & Audit)

### 5-1. 컨트롤러 로그 메시지

#### 공개 API (EventGroupController) -- 신규

| 엔드포인트 | 로그 메시지 (예상) |
|-----------|------------------|
| `GET /api/v1/event-groups` | `그룹 목록 조회 요청` |
| `GET /api/v1/event-groups/{groupId}` | `그룹 상세 조회 요청 - groupId: {}` |

#### 관리자 API (AdminEventGroupController) -- 신규

| 엔드포인트 | 로그 메시지 (예상) |
|-----------|------------------|
| `GET /api/v1/admin/event-groups` | `[관리자] 그룹 목록 조회 요청 - userId: {}` |
| `GET /api/v1/admin/event-groups/{groupId}` | `[관리자] 그룹 상세 조회 요청 - groupId: {}, userId: {}` |
| `POST /api/v1/admin/event-groups` | `그룹 생성 요청 - userId: {}, name: {}` |
| `PUT /api/v1/admin/event-groups/{groupId}` | `그룹 수정 요청 - groupId: {}, userId: {}` (DECISION-09) |
| `DELETE /api/v1/admin/event-groups/{groupId}` | `그룹 삭제 요청 - groupId: {}, userId: {}` |
| `POST /api/v1/admin/event-groups/{groupId}/events/{eventId}` | `그룹에 행사 추가 요청 - groupId: {}, eventId: {}, userId: {}` |
| `DELETE /api/v1/admin/event-groups/{groupId}/events/{eventId}` | `그룹에서 행사 제거 요청 - groupId: {}, eventId: {}, userId: {}` |

### 5-2. Soft Delete 감사 이력

`SoftDeletableEntity` 상속에 의해 다음 감사 필드가 자동 관리된다:

| 구분 | 필드 | 저장 내용 | 관련 컬럼 |
|------|------|---------|----------|
| 생성 | `createdAt` | 생성 시점 타임스탬프 | `event_group_created_at` |
| 생성 | `createdBy` | 생성자 ID | `event_group_created_by` |
| 수정 | `updatedAt` | 최종 수정 시점 타임스탬프 | `event_group_updated_at` |
| 수정 | `updatedBy` | 최종 수정자 ID | `event_group_updated_by` |
| 삭제 | `deleted` | `true` | `event_group_deleted` |
| 삭제 | `deletedAt` | 삭제 시점 타임스탬프 | `event_group_deleted_at` |
| 삭제 | `deletedBy` | 운영자 ID | `event_group_deleted_by` |

### 5-3. 관측 가능성 누락 사항 (예상)

| 항목 | 현황 | 영향 |
|------|------|------|
| 그룹 변경 이력 (전용 History 테이블) | **없음** (MVP 범위 외 가능) | 그룹 이름/설명 변경 이력 추적 불가 |
| 행사 추가/제거 이력 | **없음** | 어떤 행사가 언제 그룹에 추가/제거되었는지 추적 불가 |
| 그룹 생성 완료 로그 | 요청 로그만 존재 (예상) | 생성 성공/실패 구분은 HTTP 상태 코드로 판단 |

---

## 6. 테스트 전략 (Test Strategy)

### 6-1. 현재 테스트 현황

EventGroup은 신규 기능이므로 현재 테스트가 존재하지 않는다.

### 6-2. 권장 테스트 구조

**도메인 단위 테스트** (순수 Java):

| 테스트 클래스 | 범위 |
|-------------|------|
| `EventGroupTest` | 생성, 수정, 삭제, 입력값 검증 |

**서비스 단위 테스트** (Mockito):

| 테스트 클래스 | 범위 |
|-------------|------|
| `EventGroupServiceTest` | CRUD, 행사 추가/제거, 권한 검증, 중복 검증, 엣지 케이스 |

**컨트롤러 통합 테스트** (MockMvc):

| 테스트 클래스 | 범위 |
|-------------|------|
| `EventGroupControllerTest` | 공개 API RBAC, 응답 형식, OpenAPI 스펙 검증 |
| `AdminEventGroupControllerTest` | 관리자 API RBAC, CRUD 엔드포인트, OpenAPI 스펙 검증 |

### 6-3. 테스트-검증 항목 매핑 (초기 상태)

#### 불변조건 커버리지

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| EGRP-INV-01 (그룹명 고유 -- DB 유니크 포함) | - | **누락** (신규) |
| EGRP-INV-02 (그룹명 길이) | - | **누락** (신규) |
| EGRP-INV-03 (단일 그룹 소속) | - | **누락** (신규) |
| EGRP-INV-04 (Soft Delete 필터링) | - | **누락** (신규) |
| EGRP-INV-05 (초기 상태 -- 감사 필드 포함) | - | **누락** (신규) |
| EGRP-INV-06 (행사 존재 검증) | - | **누락** (신규) |
| EGRP-INV-07 (정렬 규칙) | - | **누락** (신규) |
| EGRP-INV-08 (삭제 시 연결 해제) | - | **누락** (신규) |
| EGRP-INV-09 (행사 삭제 시 그룹 제외) | - | **누락** (신규) |
| EGRP-INV-10 (설명 길이) | - | **누락** (신규) |
| EGRP-INV-11 (소속 행사 수 무제한) | - | **누락** (신규) |
| EGRP-INV-12 (공개 API UNPUBLISHED 필터) | - | **누락** (신규) |
| EGRP-INV-13 (빈 그룹 허용) | - | **누락** (신규) |
| EGRP-INV-14 (eventCount 계산 기준) | - | **누락** (신규) |

#### 권한 검증 커버리지

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-EGRP-01~02 (공개 API 인증) | - | **누락** (신규) |
| SEC-EGRP-03~10 (관리자 API 권한 차단) | - | **누락** (신규) |
| SEC-EGRP-11~13 (OPERATOR 성공) | - | **누락** (신규) |
| SEC-EGRP-14 (부작용 없음) | - | **누락** (신규) |

### 6-4. 필수 GAP 항목

| ID | 내용 | 심각도 | 상태 |
|----|------|--------|------|
| GAP-EGRP-01 | 그룹명 중복 검증 테스트 -- 서비스 레벨 + DB 유니크 제약 (EGRP-INV-01) | **높음** | 미해결 |
| GAP-EGRP-02 | 그룹명/설명 경계값 테스트 (EGRP-INV-02, EGRP-INV-10) | **중간** | 미해결 |
| GAP-EGRP-03 | 행사의 단일 그룹 소속 제약 테스트 -- DECISION-07 정책 포함 (EGRP-INV-03) | **높음** | 미해결 |
| GAP-EGRP-04 | Soft delete 필터링 테스트 (EGRP-INV-04) | **높음** | 미해결 |
| GAP-EGRP-05 | 초기 상태 테스트 -- 감사 필드 자동 설정 포함 (EGRP-INV-05) | **중간** | 미해결 |
| GAP-EGRP-06 | 존재하지 않는/삭제된 행사 추가 시도 테스트 (EGRP-INV-06) | **높음** | 미해결 |
| GAP-EGRP-07 | 그룹 내 행사 정렬 순서 테스트 (EGRP-INV-07) | **중간** | 미해결 |
| GAP-EGRP-08 | 그룹 삭제 시 소속 행사 연결 해제 테스트 -- `@Modifying` flush/clear 포함 (EGRP-INV-08) | **높음** | 미해결 |
| GAP-EGRP-09 | 행사 삭제 후 그룹 조회에서 제외 테스트 (EGRP-INV-09) | **중간** | 미해결 |
| GAP-EGRP-10 | 공개 API에서 UNPUBLISHED 행사 필터링 테스트 (EGRP-INV-12) | **높음** | 미해결 |
| GAP-EGRP-11 | 빈 그룹 CRUD 테스트 (EGRP-INV-13) | **중간** | 미해결 |
| GAP-EGRP-12 | SecurityConfig OPERATOR+ 접근 허용 테스트 (SEC-EGRP-11~13) | **높음** | 미해결 |
| GAP-EGRP-13 | SecurityConfig MEMBER/ASSOCIATE 접근 차단 테스트 (SEC-EGRP-04~10) | **높음** | 미해결 |
| GAP-EGRP-14 | 그룹 수정 시 자기 자신 제외 이름 중복 검증 테스트 (EGRP-INV-01) | **중간** | 미해결 |
| GAP-EGRP-15 | 그룹 삭제 시 행사 상태(visibility/registrationStatus/eventStatus) 변경 없음 확인 | **높음** | 미해결 |
| GAP-EGRP-16 | 동시 요청으로 같은 이름의 그룹 생성 시 DB 유니크 제약에 의해 하나만 성공 확인 (동시성, EGRP-INV-01) | **중간** | 미해결 |
| GAP-EGRP-17 | 관리자 그룹 상세 조회에서 UNPUBLISHED 행사 포함 확인 | **중간** | 미해결 |
| GAP-EGRP-18 | OpenAPI 스펙 응답 검증 (`matchesOpenApiSpec()`) | **높음** | 미해결 |
| GAP-EGRP-19 | 그룹에 소속되지 않은 행사 제거 시 DECISION-08 정책에 따른 응답 확인 | **중간** | 미해결 |
| GAP-EGRP-20 | 그룹 생성 응답 201 Created 확인 | **중간** | 미해결 |
| GAP-EGRP-21 | eventCount 계산 시 소프트 삭제된 행사 제외 확인 (EGRP-INV-14) | **중간** | 미해결 |
| GAP-EGRP-22 | DB 유니크 제약 위반 시 `DataIntegrityViolationException` -> 409 변환 확인 (EGRP-INV-01) | **높음** | 미해결 |

---

## 부록 A: 엔티티 설계 (예상)

> 이 섹션은 구현 전 예상 설계이다. 실제 구현 시 DECISION 결정에 따라 변경될 수 있다.

### EventGroup 엔티티

```java
@Entity
@Table(name = "event_groups")
@SQLRestriction("event_group_deleted = false")
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "event_group_created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "event_group_updated_at", nullable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "event_group_created_by", updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "event_group_updated_by")),
    @AttributeOverride(name = "deleted", column = @Column(name = "event_group_deleted", nullable = false)),
    @AttributeOverride(name = "deletedAt", column = @Column(name = "event_group_deleted_at")),
    @AttributeOverride(name = "deletedBy", column = @Column(name = "event_group_deleted_by"))
})
public class EventGroup extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_group_id")
    private Long id;

    @Column(name = "event_group_name", nullable = false, length = 100)
    private String name;

    @Column(name = "event_group_description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_group_user_id", nullable = false)
    private User user;  // 그룹 생성자
}
```

### Event 엔티티 변경 (DECISION-01 (C) 기준)

```java
// Event.java에 필드 추가
@Column(name = "event_group_id")
private Long groupId;  // nullable, FK 없음 (약한 참조)
```

### DB 마이그레이션 DDL (예상)

```sql
-- event_groups 테이블 생성
CREATE TABLE event_groups (
    event_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_group_name VARCHAR(100) NOT NULL,
    event_group_description VARCHAR(500),
    event_group_user_id BIGINT NOT NULL,
    event_group_created_at TIMESTAMP(6) NOT NULL,
    event_group_updated_at TIMESTAMP(6) NOT NULL,
    event_group_created_by BIGINT,
    event_group_updated_by BIGINT,
    event_group_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    event_group_deleted_at TIMESTAMP(6),
    event_group_deleted_by BIGINT,

    -- Soft Delete 호환 조건부 유니크 제약:
    -- MySQL 8의 Generated Column을 사용하여 deleted=false인 행만 유니크 제약 적용.
    -- deleted=true인 행은 NULL이 되어 UNIQUE 제약에서 제외됨 (MySQL은 NULL 중복 허용).
    event_group_name_unique_key VARCHAR(100) GENERATED ALWAYS AS (
        IF(event_group_deleted = FALSE, event_group_name, NULL)
    ) STORED,

    CONSTRAINT fk_event_group_user FOREIGN KEY (event_group_user_id)
        REFERENCES users(users_id),
    CONSTRAINT uk_event_group_name UNIQUE (event_group_name_unique_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- events 테이블에 group_id 컬럼 추가 (nullable, FK 없음)
ALTER TABLE events ADD COLUMN event_group_id BIGINT NULL;

-- 인덱스
CREATE INDEX idx_event_groups_name ON event_groups(event_group_name);
CREATE INDEX idx_events_group_id ON events(event_group_id);
```

**DDL 설계 근거**:
- `uk_event_group_name`: Generated Column(`event_group_name_unique_key`)을 사용한 조건부 유니크 제약. `deleted = false`인 행만 실제 이름값을 가지고 유니크 체크에 참여하며, `deleted = true`인 행은 NULL이 되어 유니크 제약에서 자동 제외됨. 이를 통해 EGRP-INV-01의 "Soft delete된 그룹과 이름이 동일한 새 그룹 생성 허용" 요건과 DB 레벨 동시성 보호를 모두 충족.
- `fk_event_group_user`: 그룹 생성자 참조. User 테이블과의 FK 관계 유지.
- `event_group_id` (events 테이블): FK 없는 약한 참조 (DECISION-01 (C)). `Event.surveyId` 패턴과 동일.

---

## 부록 B: API 엔드포인트 설계 (예상)

> 이 섹션은 구현 전 예상 API 설계이다.

### 공개 API

| 메서드 | 경로 | 설명 | 인증 | 응답 코드 |
|--------|------|------|------|----------|
| `GET` | `/api/v1/event-groups` | 그룹 목록 조회 (DECISION-10: 전체 반환) | 인증 필요 (DECISION-11) | 200 OK |
| `GET` | `/api/v1/event-groups/{groupId}` | 그룹 상세 조회 (PUBLISHED 행사만) | 인증 필요 (DECISION-11) | 200 OK |

### 관리자 API

| 메서드 | 경로 | 설명 | 권한 | 응답 코드 |
|--------|------|------|------|----------|
| `GET` | `/api/v1/admin/event-groups` | 관리자 그룹 목록 조회 | OPERATOR+ | 200 OK |
| `GET` | `/api/v1/admin/event-groups/{groupId}` | 관리자 그룹 상세 조회 (모든 행사) | OPERATOR+ | 200 OK |
| `POST` | `/api/v1/admin/event-groups` | 그룹 생성 | OPERATOR+ | **201 Created** |
| `PUT` | `/api/v1/admin/event-groups/{groupId}` | 그룹 수정 (전체 수정, DECISION-09) | OPERATOR+ | 200 OK |
| `DELETE` | `/api/v1/admin/event-groups/{groupId}` | 그룹 삭제 | OPERATOR+ | 204 No Content |
| `POST` | `/api/v1/admin/event-groups/{groupId}/events/{eventId}` | 그룹에 행사 추가 | OPERATOR+ | 200 OK |
| `DELETE` | `/api/v1/admin/event-groups/{groupId}/events/{eventId}` | 그룹에서 행사 제거 | OPERATOR+ | 200 OK |

### 응답 DTO (예상)

**EventGroupListResponse**:
- `id` (Long)
- `name` (String)
- `description` (String, nullable)
- `eventCount` (int) -- 소속 활성 행사 수 (EGRP-INV-14: 소프트 삭제된 행사 제외. 공개 API에서는 PUBLISHED 행사만 카운트, 관리자 API에서는 모든 활성 행사 카운트)
- `createdAt` (Instant)

**EventGroupDetailResponse**:
- `id` (Long)
- `name` (String)
- `description` (String, nullable)
- `authorName` (String)
- `events` (List<EventListResponse>) -- 소속 행사 목록 (정렬: eventStartAt 오름차순, DECISION-05)
- `createdAt` (Instant)
- `updatedAt` (Instant)
