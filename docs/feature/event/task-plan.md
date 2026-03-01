# 행사 Visibility(공개/비공개) 기능 작업 계획

## 개요

- **기능 설명**: 행사(Event) 도메인에 3번째 상태 축 `visibility`(PUBLISHED/UNPUBLISHED)를 추가하고, 관리자 전용 API(`/api/v1/admin/events/**`)를 신규 구현한다. 공개 API에서는 PUBLISHED 행사만 접근 가능하며, 관리자 API에서는 모든 행사를 조회/관리할 수 있다. unpublish 시 등록 상태가 OPEN이면 자동 마감 처리된다.
- **관련 문서**
  - 검증 기준서: [`docs/criteria/event/event-verification-criteria.md`](../../criteria/event/event-verification-criteria.md)
  - 관련 이슈: [#483 행사 Visibility(공개/비공개) 추가 및 관리자 전용 API](https://github.com/IGRUS-INHA/IGRUS-Web/issues/483)
  - 참조 패턴: [`SurveyVisibility.java`](../../../backend/src/main/java/igrus/web/survey/domain/SurveyVisibility.java), [`Survey.java`](../../../backend/src/main/java/igrus/web/survey/domain/Survey.java)
- **작성일**: 2026-02-27
- **3축 모델**: visibility(PUBLISHED/UNPUBLISHED) + registrationStatus(NOT_STARTED/OPEN/CLOSED) + eventStatus(UPCOMING/ONGOING/COMPLETED/CANCELED)

---

## 작업 목록

### 1. DB 마이그레이션

#### TASK-001: Flyway 마이그레이션 -- event_visibility 컬럼 추가

- **작업 ID**: TASK-001
- **작업명**: `events` 테이블에 `event_visibility` 컬럼 추가 마이그레이션 스크립트 작성
- **설명**: Flyway 마이그레이션 스크립트 `V46__add_event_visibility_column.sql`을 작성한다. `events` 테이블에 `event_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'` 컬럼을 추가한다. DB 기본값이 `PUBLISHED`인 이유는 기존 행사 데이터가 이미 공개된 상태이므로 마이그레이션 후에도 동일하게 공개 상태를 유지해야 하기 때문이다 (EVT-INV-23). 신규 행사 생성 시에는 JPA 엔티티의 `Event.create()`에서 `UNPUBLISHED`를 명시적으로 설정하여 DB 기본값을 덮어쓴다 (EVT-INV-05).
  - **DDL 스펙**:
    - 컬럼명: `event_visibility`
    - 타입: `VARCHAR(20)`
    - 제약: `NOT NULL`
    - 기본값: `'PUBLISHED'`
  - 현재 Flyway 최신 버전은 V45이므로 V46을 사용한다.
- **관련 검증 기준**: EVT-INV-23 (DB 마이그레이션 기존 데이터 정합성)
- **관련 테스트 케이스**: GAP-EVT-41
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 2. 도메인 계층

#### TASK-002: EventVisibility enum 구현

- **작업 ID**: TASK-002
- **작업명**: `EventVisibility` enum 구현 (SurveyVisibility 패턴 준수)
- **설명**: `igrus.web.event.domain` 패키지에 `EventVisibility` enum을 생성한다. `SurveyVisibility`와 동일한 구조로 `UNPUBLISHED("비공개", "일반 사용자에게 노출되지 않음")`, `PUBLISHED("공개", "일반 사용자에게 노출")` 두 값을 정의한다. `canTransitionTo(EventVisibility target)` 메서드를 구현하여 `this != target`일 때만 전이를 허용한다 (양방향 전이, 동일 상태 거부).
- **관련 검증 기준**: EVT-INV-17 (Visibility 양방향 전이)
- **관련 테스트 케이스**: GAP-EVT-26
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-003: Event 엔티티에 visibility 필드 및 publish/unpublish 메서드 추가

- **작업 ID**: TASK-003
- **작업명**: Event 엔티티 visibility 필드, `publish()`, `unpublish()` 메서드 구현
- **설명**: `Event` 엔티티에 다음을 추가한다:
  1. `visibility` 필드: `@Enumerated(EnumType.STRING) @Column(name = "event_visibility", nullable = false, length = 20)` -- `EventVisibility` 타입
  2. `Event.create()` 메서드: `visibility = EventVisibility.UNPUBLISHED` 설정 추가 (EVT-INV-05)
  3. `publish()` 메서드: `UNPUBLISHED -> PUBLISHED` 전이. `canTransitionTo()` 검증 후 전이. 실패 시 `InvalidEventStateTransitionException` (또는 동등 예외) 발생. registrationStatus/eventStatus 변경 없음
  4. `unpublish()` 메서드: `PUBLISHED -> UNPUBLISHED` 전이. `canTransitionTo()` 검증 후 전이. `registrationStatus == OPEN`이면 `registrationStatus = CLOSED`, `closeReason = MANUAL_CLOSE`로 자동 마감 (EVT-INV-20). NOT_STARTED/CLOSED이면 변경 없음. Survey.unpublish() 패턴 참조
  5. `Event.update()` 메서드에서 visibility 필드를 **미변경** 상태로 유지 (EVT-INV-16, 기존 코드 변경 불필요)
- **관련 검증 기준**: EVT-INV-05 (초기 상태), EVT-INV-16 (Visibility 축 독립성), EVT-INV-17 (양방향 전이), EVT-INV-20 (unpublish 시 등록 마감 연동)
- **관련 테스트 케이스**: GAP-EVT-25, GAP-EVT-26, GAP-EVT-31, GAP-EVT-32, GAP-EVT-33, GAP-EVT-34, GAP-EVT-35, GAP-EVT-42, GAP-EVT-43
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-004: EventChangeType에 EVENT_PUBLISHED, EVENT_UNPUBLISHED 추가

- **작업 ID**: TASK-004
- **작업명**: `EventChangeType` enum에 visibility 변경 유형 2개 추가
- **설명**: `EventChangeType` enum에 `EVENT_PUBLISHED`(행사 공개), `EVENT_UNPUBLISHED`(행사 비공개) 두 값을 추가한다. 기존 4개 값(`EVENT_CANCELED`, `EVENT_REACTIVATED`, `REGISTRATION_CLOSED_MANUAL`, `REGISTRATION_REOPENED`)과 함께 총 6개 값이 된다.
- **관련 검증 기준**: EVT-INV-14 (행사 상태 변경 감사 이력)
- **관련 테스트 케이스**: GAP-EVT-44
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 3. DTO 계층

#### TASK-005: EventDetailResponse에 visibility 필드 추가

- **작업 ID**: TASK-005
- **작업명**: `EventDetailResponse` record에 `visibility` 필드 추가
- **설명**: `EventDetailResponse` record에 `EventVisibility visibility` 필드를 추가한다. `from(Event, boolean, boolean)` 정적 팩토리 메서드에서 `event.getVisibility()`를 매핑한다. Javadoc의 `@param` 설명도 추가한다. 공개 API에서는 항상 `PUBLISHED`만 반환되지만(EVT-INV-18), 관리자 API에서는 `UNPUBLISHED`도 반환될 수 있다.
- **관련 검증 기준**: EVT-INV-22 (Visibility DTO 필드 포함)
- **관련 테스트 케이스**: GAP-EVT-38
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-006: EventListResponse에 visibility 필드 추가

- **작업 ID**: TASK-006
- **작업명**: `EventListResponse` record에 `visibility` 필드 추가
- **설명**: `EventListResponse` record에 `EventVisibility visibility` 필드를 추가한다. `from(Event)` 정적 팩토리 메서드에서 `event.getVisibility()`를 매핑한다. Javadoc의 `@param` 설명도 추가한다.
- **관련 검증 기준**: EVT-INV-22 (Visibility DTO 필드 포함)
- **관련 테스트 케이스**: GAP-EVT-38
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 4. Repository 계층

#### TASK-007: EventRepository에 visibility 기반 쿼리 추가

- **작업 ID**: TASK-007
- **작업명**: `EventRepository`에 visibility 기반 쿼리 메서드 추가
- **설명**: `EventRepository`에 공개 API용 및 관리자 API용 쿼리 메서드를 추가한다:
  1. 공개 목록 조회: 기존 목록 조회 쿼리에 `AND e.visibility = 'PUBLISHED'` 조건 추가 (또는 별도 메서드)
  2. 공개 단건 조회: `visibility = PUBLISHED` 필터가 적용된 조회 메서드 (또는 서비스 레벨에서 필터링)
  3. 관리자 목록 조회: **visibility 선택적 필터 파라미터** 지원. `EventVisibility visibility` 파라미터가 null이면 전체 반환 (PUBLISHED + UNPUBLISHED), non-null이면 해당 visibility만 필터링. eventStatus/registrationStatus 필터도 함께 지원
  - 구현 방식은 서비스 레벨에서 visibility 검증과 Repository 메서드 중 선택 가능. Repository에서 처리하는 것이 불필요한 데이터 로딩을 방지하므로 권장
  - 관리자 목록 조회 쿼리 예시: `WHERE (:visibility IS NULL OR e.visibility = :visibility) AND (:eventStatus IS NULL OR e.eventStatus = :eventStatus) AND ...`
- **관련 검증 기준**: EVT-INV-18 (공개 API UNPUBLISHED 차단), EVT-INV-21 (관리자 API visibility 무관 접근)
- **관련 테스트 케이스**: GAP-EVT-27, GAP-EVT-29
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 5. 서비스 계층

#### TASK-008: EventService 공개 API 메서드에 visibility 필터 적용

- **작업 ID**: TASK-008
- **작업명**: 기존 공개 API 조회 메서드에 PUBLISHED 필터 적용
- **설명**: `EventService`의 기존 공개 API 조회 메서드를 수정한다:
  1. `getEventList()`: PUBLISHED 행사만 반환하도록 Repository 쿼리 또는 서비스 필터 변경
  2. `getEvent()`: UNPUBLISHED 행사 접근 시 `EventNotFoundException` 반환 (404). 기존 `findById()` 후 visibility 검증 추가 또는 Repository에서 필터링
  - **주의**: UNPUBLISHED 행사가 존재하지만 404를 반환하는 것은 정보 은폐(information hiding)이며 의도된 동작
  - Lazy Evaluation(`updateStatusIfNeeded`)은 visibility에 영향을 주지 않음 (EVT-INV-16, 섹션 2-4)
- **관련 검증 기준**: EVT-INV-18 (공개 API UNPUBLISHED 차단)
- **관련 테스트 케이스**: GAP-EVT-27, GAP-EVT-28
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-009: EventService에 publishEvent, unpublishEvent 메서드 추가

- **작업 ID**: TASK-009
- **작업명**: `EventService`에 행사 공개/비공개 비즈니스 로직 구현
- **설명**: `EventService`에 다음 메서드를 추가한다:
  1. `publishEvent(Long eventId, Long userId)`:
     - 행사 조회 (findById)
     - `event.publish()` 호출 (UNPUBLISHED -> PUBLISHED)
     - 감사 이력 이벤트 발행: `EventStatusChangeEvent` (changeType=EVENT_PUBLISHED, previousValue=UNPUBLISHED, newValue=PUBLISHED, reason=null)
     - `EventDetailResponse` 반환
  2. `unpublishEvent(Long eventId, Long userId)`:
     - 행사 조회 (findById)
     - `event.unpublish()` 호출 (PUBLISHED -> UNPUBLISHED + registrationStatus 연동)
     - 감사 이력 이벤트 발행: `EventStatusChangeEvent` (changeType=EVENT_UNPUBLISHED, previousValue=PUBLISHED, newValue=UNPUBLISHED, reason=null)
     - `EventDetailResponse` 반환
  - **권한 검증**: SecurityConfig URL 규칙(`/api/v1/admin/events/**` -> OPERATOR+)으로 이미 보장되므로 서비스 레벨 추가 검증 불필요 (검증 기준 4-3 참조)
  - **사유(reason)**: 불필요. body 없이 호출 가능 (EVT-INV-19). reason=null로 이력 기록
- **관련 검증 기준**: EVT-INV-14 (감사 이력), EVT-INV-16 (Visibility 축 독립성), EVT-INV-17 (양방향 전이), EVT-INV-19 (사유 불필요), EVT-INV-20 (unpublish 시 등록 마감 연동)
- **관련 테스트 케이스**: GAP-EVT-26, GAP-EVT-31, GAP-EVT-32, GAP-EVT-33, GAP-EVT-34, GAP-EVT-39, GAP-EVT-42, GAP-EVT-43, GAP-EVT-44
- **선행 작업**: TASK-003, TASK-004
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-010: EventService에 관리자용 조회 메서드 추가

- **작업 ID**: TASK-010
- **작업명**: `EventService`에 관리자 전용 행사 목록/상세 조회 메서드 추가
- **설명**: `EventService`에 관리자 전용 조회 메서드를 추가한다 (또는 별도 `AdminEventService` 분리):
  1. `getAdminEventList(EventVisibility visibility, EventStatus eventStatus, RegistrationStatus registrationStatus, Pageable pageable)`:
     - **`visibility` 파라미터 추가**: null이면 모든 행사 반환 (PUBLISHED + UNPUBLISHED), non-null이면 해당 visibility만 필터링. 관리자가 PUBLISHED/UNPUBLISHED 행사를 구분하여 조회할 수 있도록 지원
     - eventStatus/registrationStatus 필터 지원 (기존 공개 API와 동일 패턴)
     - Lazy Evaluation 적용 (기존과 동일)
     - `Page<EventListResponse>` 반환
  2. `getAdminEvent(Long eventId, Long userId)`:
     - visibility 값과 무관하게 행사 조회 (UNPUBLISHED도 정상 반환)
     - Lazy Evaluation 적용
     - `EventDetailResponse` 반환
  - **준회원(ASSOCIATE) 차단**: 관리자 API는 SecurityConfig에서 OPERATOR+ 권한이 보장되므로 서비스 레벨 ASSOCIATE 체크 불필요. 단, EventDetailResponse 생성 시 canEdit/isRegistered 등의 사용자 컨텍스트 처리 방법 결정 필요
- **관련 검증 기준**: EVT-INV-21 (관리자 API visibility 무관 접근)
- **관련 테스트 케이스**: GAP-EVT-29, GAP-EVT-30, GAP-EVT-40
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 6. 컨트롤러 계층

#### TASK-011: AdminEventController 신규 생성

- **작업 ID**: TASK-011
- **작업명**: 관리자 전용 행사 컨트롤러 `AdminEventController` 구현
- **설명**: `igrus.web.event.controller` (또는 `igrus.web.admin.controller`) 패키지에 `AdminEventController`를 신규 생성한다:
  1. `GET /api/v1/admin/events` -- 관리자 행사 목록 조회. **`visibility` 쿼리 파라미터 추가** (`@RequestParam(required = false) EventVisibility visibility`). eventStatus/registrationStatus 필터와 함께 visibility 필터도 지원. visibility 미지정 시 전체 반환
  2. `GET /api/v1/admin/events/{eventId}` -- 관리자 행사 상세 조회 (visibility 무관)
  3. `POST /api/v1/admin/events/{eventId}/publish` -- 행사 공개 (body 없음)
  4. `POST /api/v1/admin/events/{eventId}/unpublish` -- 행사 비공개 (body 없음)
  - 모든 엔드포인트에 Swagger 어노테이션 필수 (`@Operation`, `@ApiResponse`, `@SecurityRequirement`)
  - SurveyController의 publish/unpublish 패턴 참조: `@PostMapping("/{eventId}/publish")`, `@AuthenticationPrincipal AuthenticatedUser user`
  - 로그 메시지 규격 (검증 기준 5-1 참조):
    - `[관리자] 행사 목록 조회 요청 - userId: {}, visibility: {}, eventStatus: {}, registrationStatus: {}`
    - `[관리자] 행사 상세 조회 요청 - eventId: {}, userId: {}`
    - `행사 공개 요청 - eventId: {}, userId: {}`
    - `행사 비공개 요청 - eventId: {}, userId: {}`
- **관련 검증 기준**: EVT-INV-19 (사유 불필요), EVT-INV-21 (관리자 API visibility 무관 접근), 섹션 4-1 (권한 매트릭스), 섹션 5-1 (로그 메시지)
- **관련 테스트 케이스**: GAP-EVT-29, GAP-EVT-30, GAP-EVT-36, GAP-EVT-37, GAP-EVT-39, GAP-EVT-40
- **선행 작업**: TASK-009, TASK-010
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 7. 보안 설정

#### TASK-012: SecurityConfig에 관리자 행사 API 경로 추가

- **작업 ID**: TASK-012
- **작업명**: `ApiSecurityConfig`에 `/api/v1/admin/events/**` OPERATOR+ 권한 규칙 추가
- **설명**: `ApiSecurityConfig`에서 `/api/v1/admin/events/**` 경로를 OPERATOR+ 역할(OPERATOR, ADMIN)로 접근 가능하도록 설정한다.
  - **배치 위치**: 기존 `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` 규칙 **앞에** 배치해야 함 (더 구체적인 경로 우선). 현재 코드에서 `.requestMatchers("/api/v1/admin/dashboard", "/api/v1/admin/users/**", "/api/events/*/registrations", "/api/v1/admin/comment-reports/**").hasAnyRole("OPERATOR", "ADMIN")` 블록에 `/api/v1/admin/events/**`를 추가하는 것이 가장 깔끔함
  - **검증**: OPERATOR 역할로 `/api/v1/admin/events` 접근 시 200 OK, MEMBER 역할로 접근 시 403 확인
- **관련 검증 기준**: 섹션 4-4 (SecurityConfig 변경 사항), SEC-EVT-11~17
- **관련 테스트 케이스**: GAP-EVT-36, GAP-EVT-37
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 8. 테스트

#### TASK-013: EventVisibility 도메인 단위 테스트

- **작업 ID**: TASK-013
- **작업명**: `EventVisibilityTest` 단위 테스트 작성
- **설명**: `EventVisibility` enum의 `canTransitionTo()` 메서드에 대한 단위 테스트를 작성한다. `SurveyVisibilityTest` 패턴 참조.
  - UNPUBLISHED -> PUBLISHED: 허용 (true)
  - PUBLISHED -> UNPUBLISHED: 허용 (true)
  - PUBLISHED -> PUBLISHED: 거부 (false)
  - UNPUBLISHED -> UNPUBLISHED: 거부 (false)
- **관련 검증 기준**: EVT-INV-17 (Visibility 양방향 전이)
- **관련 테스트 케이스**: GAP-EVT-26
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-014: Event 엔티티 visibility 관련 단위 테스트

- **작업 ID**: TASK-014
- **작업명**: `EventTest`에 visibility 관련 테스트 추가
- **설명**: `EventTest` 클래스에 visibility 관련 테스트를 추가한다 (중첩 클래스 패턴 유지):
  1. 생성 시 `visibility = UNPUBLISHED` 초기값 검증 (EVT-INV-05)
  2. `publish()` 성공: UNPUBLISHED -> PUBLISHED
  3. `publish()` 실패: PUBLISHED -> PUBLISHED (동일 상태 전이 거부)
  4. `unpublish()` 성공: PUBLISHED -> UNPUBLISHED
  5. `unpublish()` 실패: UNPUBLISHED -> UNPUBLISHED (동일 상태 전이 거부)
  6. `unpublish()` + registrationStatus=OPEN 연동: CLOSED(MANUAL_CLOSE)로 자동 마감 (EVT-INV-20)
  7. `unpublish()` + registrationStatus=NOT_STARTED: 변경 없음 (EVT-INV-20)
  8. `unpublish()` + registrationStatus=CLOSED: 기존 closeReason 유지 (EVT-INV-20)
  9. `publish()`/`unpublish()` 시 eventStatus 변경 없음 확인 (EVT-INV-16 독립성)
  10. `update()` 호출 전후 visibility 변경 없음 확인 (EVT-INV-16)
  11. COMPLETED 행사 unpublish: registrationStatus 변경 없음 (이미 CLOSED)
  12. CANCELED 행사 publish: eventStatus 변경 없음 (독립성)
- **관련 검증 기준**: EVT-INV-05, EVT-INV-16, EVT-INV-17, EVT-INV-20
- **관련 테스트 케이스**: GAP-EVT-25, GAP-EVT-26, GAP-EVT-31, GAP-EVT-32, GAP-EVT-33, GAP-EVT-34, GAP-EVT-35, GAP-EVT-42, GAP-EVT-43
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-015: EventService visibility 관련 단위 테스트

- **작업 ID**: TASK-015
- **작업명**: `EventServiceTest`에 publishEvent, unpublishEvent 메서드 테스트 추가
- **설명**: `EventServiceTest` 클래스에 publish/unpublish 서비스 메서드 테스트를 추가한다:
  1. `publishEvent` 정상 동작 (UNPUBLISHED -> PUBLISHED)
  2. `publishEvent` 이미 공개 상태 시 예외
  3. `unpublishEvent` 정상 동작 (PUBLISHED -> UNPUBLISHED)
  4. `unpublishEvent` 이미 비공개 상태 시 예외
  5. `unpublishEvent` + OPEN 상태: 등록 자동 마감
  6. 감사 이력 이벤트 발행 검증 (ApplicationEventPublisher mock)
  7. 존재하지 않는 행사 ID로 호출 시 EventNotFoundException
- **관련 검증 기준**: EVT-INV-14, EVT-INV-17, EVT-INV-19, EVT-INV-20
- **관련 테스트 케이스**: GAP-EVT-26, GAP-EVT-31, GAP-EVT-34, GAP-EVT-39, GAP-EVT-44
- **선행 작업**: TASK-009
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-016: EventService 공개 API visibility 필터 테스트

- **작업 ID**: TASK-016
- **작업명**: `EventServiceTest`에 공개 API visibility 필터링 테스트 추가
- **설명**: 기존 공개 API 조회 메서드에 대한 visibility 필터링 테스트를 추가한다:
  1. `getEventList()`: UNPUBLISHED 행사가 목록에 포함되지 않음
  2. `getEventList()`: PUBLISHED 행사만 정상 반환
  3. `getEvent()`: UNPUBLISHED 행사 접근 시 EventNotFoundException (404)
  4. `getEvent()`: PUBLISHED 행사 정상 반환
- **관련 검증 기준**: EVT-INV-18 (공개 API UNPUBLISHED 차단)
- **관련 테스트 케이스**: GAP-EVT-27, GAP-EVT-28
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-017: EventService 관리자 API 조회 테스트

- **작업 ID**: TASK-017
- **작업명**: `EventServiceTest`에 관리자 API 조회 메서드 테스트 추가
- **설명**: 관리자 전용 조회 메서드 테스트를 추가한다:
  1. `getAdminEventList(visibility=null)`: PUBLISHED + UNPUBLISHED 행사 모두 반환
  2. `getAdminEventList(visibility=PUBLISHED)`: PUBLISHED 행사만 반환
  3. `getAdminEventList(visibility=UNPUBLISHED)`: UNPUBLISHED 행사만 반환
  4. `getAdminEventList()`: eventStatus/registrationStatus 필터 동작 확인
  5. `getAdminEventList()`: visibility + eventStatus 복합 필터 동작 확인
  6. `getAdminEvent()`: UNPUBLISHED 행사 정상 반환 (404 아님)
  7. `getAdminEvent()`: Lazy Evaluation 정상 적용 확인
- **관련 검증 기준**: EVT-INV-21 (관리자 API visibility 무관 접근)
- **관련 테스트 케이스**: GAP-EVT-29, GAP-EVT-30, GAP-EVT-40
- **선행 작업**: TASK-010
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-018: SecurityConfig 관리자 API 접근 제어 테스트

- **작업 ID**: TASK-018
- **작업명**: SecurityConfig `/api/v1/admin/events/**` 접근 제어 통합 테스트
- **설명**: MockMvc 기반 통합 테스트로 SecurityConfig의 관리자 API 접근 제어를 검증한다:
  1. 비인증 사용자: 401 Unauthorized (SEC-EVT-11)
  2. ASSOCIATE 역할: 403 Forbidden (SEC-EVT-12)
  3. MEMBER 역할: 403 Forbidden (SEC-EVT-13, SEC-EVT-14, SEC-EVT-15)
  4. OPERATOR 역할: 200 OK -- 목록 조회, 상세 조회, publish, unpublish (SEC-EVT-16, SEC-EVT-17)
  5. ADMIN 역할: 200 OK
- **관련 검증 기준**: 섹션 4-1 (권한 매트릭스), 섹션 4-4 (SecurityConfig 변경 사항)
- **관련 테스트 케이스**: GAP-EVT-36, GAP-EVT-37
- **선행 작업**: TASK-011, TASK-012
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-019: DTO visibility 필드 포함 검증 테스트

- **작업 ID**: TASK-019
- **작업명**: EventDetailResponse, EventListResponse visibility 필드 포함 테스트
- **설명**: DTO의 `from()` 팩토리 메서드가 visibility 필드를 올바르게 매핑하는지 검증한다:
  1. `EventDetailResponse.from()`: visibility 값 정확히 매핑
  2. `EventListResponse.from()`: visibility 값 정확히 매핑
  3. 공개 API 응답에서 항상 PUBLISHED 값만 반환 확인 (서비스 테스트에서 함께 검증 가능)
- **관련 검증 기준**: EVT-INV-22 (Visibility DTO 필드 포함)
- **관련 테스트 케이스**: GAP-EVT-38
- **선행 작업**: TASK-005, TASK-006
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 9. 행사 신청 서비스 visibility 연동

#### TASK-020: EventRegistrationService에 UNPUBLISHED 행사 신청 차단 로직 추가

- **작업 ID**: TASK-020
- **작업명**: `EventRegistrationService.registerEvent()`에서 UNPUBLISHED 행사 신청 거부
- **설명**: 현재 `EventRegistrationService.registerEvent()`는 `eventRepository.findByIdAndNotDeleted(eventId)`로 행사를 조회하므로, UNPUBLISHED 행사에 대한 신청이 차단되지 않는 보안 취약점이 존재한다. 다음을 구현한다:
  1. `registerEvent()` 메서드에서 행사 조회 후 `event.getVisibility() == EventVisibility.UNPUBLISHED`이면 `EventNotFoundException`을 throw한다 (정보 은폐 원칙 -- UNPUBLISHED 행사는 존재하지 않는 것처럼 처리)
  2. 기존 `findByIdAndNotDeleted()` 조회 로직은 유지하되, 조회 직후 visibility 검증을 추가하는 방식으로 구현한다
  3. 대안: `findByIdAndNotDeleted()` 대신 `findByIdAndNotDeletedAndVisibility(eventId, PUBLISHED)` 쿼리를 사용하는 방식도 가능하나, 기존 메서드 시그니처 변경 범위를 최소화하려면 서비스 레벨 검증이 적절함
  - **주의**: `cancelRegistration()`, `getRegistrationList()`, `approveRegistration()`, `revertRegistration()` 등 다른 메서드에서의 visibility 검증은 불필요. 이미 신청이 완료된 건에 대한 취소/관리 작업은 visibility와 무관하게 수행 가능해야 함 (운영진이 unpublish 후에도 기존 신청을 관리할 수 있어야 함)
- **관련 검증 기준**: EVT-INV-18 (공개 API UNPUBLISHED 차단 -- 행사 신청도 공개 API)
- **관련 테스트 케이스**: GAP-EVT-45 (신규)
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-021: EventRegistrationService UNPUBLISHED 행사 신청 차단 테스트

- **작업 ID**: TASK-021
- **작업명**: `EventRegistrationServiceTest`에 UNPUBLISHED 행사 신청 거부 테스트 추가
- **설명**: `EventRegistrationServiceTest`에 visibility 관련 테스트를 추가한다:
  1. UNPUBLISHED 행사에 대한 `registerEvent()` 호출 시 `EventNotFoundException` 발생 확인
  2. PUBLISHED 행사에 대한 `registerEvent()` 호출 시 정상 신청 진행 확인
  3. PUBLISHED 행사에서 신청 후 unpublish한 경우, 기존 신청에 대한 `cancelRegistration()` 정상 동작 확인 (visibility와 무관)
- **관련 검증 기준**: EVT-INV-18 (공개 API UNPUBLISHED 차단)
- **관련 테스트 케이스**: GAP-EVT-45 (신규)
- **선행 작업**: TASK-020
- **구현 범위**: backend
- **예상 난이도**: 중

---

## 작업 순서 및 의존성

### 의존성 그래프

```
TASK-001 (Flyway 마이그레이션)
  +--(독립)

TASK-002 (EventVisibility enum)
  +--(독립)
  +-- TASK-003 (Event 엔티티 visibility)
  |     +-- TASK-005 (EventDetailResponse visibility)
  |     +-- TASK-006 (EventListResponse visibility)
  |     +-- TASK-007 (EventRepository visibility 쿼리)
  |     |     +-- TASK-008 (EventService 공개 API 필터)
  |     |     |     +-- TASK-016 (공개 API 필터 테스트)
  |     |     +-- TASK-010 (EventService 관리자 조회)
  |     |           +-- TASK-017 (관리자 API 조회 테스트)
  |     +-- TASK-014 (Event 엔티티 테스트)
  |     +-- TASK-020 (EventRegistrationService visibility 차단)
  |           +-- TASK-021 (신청 차단 테스트)
  +-- TASK-013 (EventVisibility 테스트)

TASK-004 (EventChangeType 추가)
  +--(독립)
  +-- TASK-009 (EventService publish/unpublish) [+ TASK-003]
  |     +-- TASK-015 (publish/unpublish 서비스 테스트)
  |     +-- TASK-011 (AdminEventController) [+ TASK-010]
  |           +-- TASK-018 (SecurityConfig 테스트) [+ TASK-012]

TASK-012 (SecurityConfig)
  +--(독립)

TASK-005, TASK-006 --> TASK-019 (DTO 테스트)
```

### 권장 실행 순서

**Phase 1 -- 기반 구조 (병렬 가능)**
1. TASK-001: Flyway 마이그레이션
2. TASK-002: EventVisibility enum
3. TASK-004: EventChangeType 확장
4. TASK-012: SecurityConfig 경로 추가

**Phase 2 -- 도메인/DTO (TASK-002 완료 후)**
5. TASK-003: Event 엔티티 visibility 필드 및 메서드
6. TASK-005: EventDetailResponse visibility (TASK-003 완료 후)
7. TASK-006: EventListResponse visibility (TASK-003 완료 후)

**Phase 3 -- Repository/Service (TASK-003 완료 후, 병렬 가능)**
8. TASK-007: EventRepository visibility 쿼리
9. TASK-009: EventService publish/unpublish (TASK-003 + TASK-004 완료 후)

**Phase 4 -- Service 조회 로직 + 신청 연동 (TASK-007, TASK-003 완료 후, 병렬 가능)**
10. TASK-008: EventService 공개 API 필터
11. TASK-010: EventService 관리자 조회 (visibility 필터 파라미터 포함)
12. TASK-020: EventRegistrationService UNPUBLISHED 신청 차단

**Phase 5 -- Controller (TASK-009 + TASK-010 완료 후)**
13. TASK-011: AdminEventController (visibility 쿼리 파라미터 포함)

**Phase 6 -- 테스트 (각 구현 완료 후, 병렬 가능)**
14. TASK-013: EventVisibility 단위 테스트
15. TASK-014: Event 엔티티 visibility 테스트
16. TASK-015: EventService publish/unpublish 테스트
17. TASK-016: 공개 API visibility 필터 테스트
18. TASK-017: 관리자 API 조회 테스트 (visibility 필터 포함)
19. TASK-018: SecurityConfig 통합 테스트
20. TASK-019: DTO visibility 필드 테스트
21. TASK-021: EventRegistrationService UNPUBLISHED 신청 차단 테스트

---

## 구현 시 주의사항

### 기술적 고려사항

1. **SurveyVisibility 패턴 준수**: `EventVisibility`, `Event.publish()`, `Event.unpublish()` 구현 시 Survey 도메인의 동일 패턴을 최대한 따른다. 차이점은 Event의 `unpublish()`에서 `closeReason = MANUAL_CLOSE` 설정이 추가되는 점 (Survey에는 closeReason 개념 없음)
2. **DB 기본값 vs JPA 기본값 이원화**: DB 마이그레이션에서는 `DEFAULT 'PUBLISHED'` (기존 데이터 보호), JPA `Event.create()`에서는 `UNPUBLISHED` 설정 (신규 생성). 이 이원화가 혼동을 줄 수 있으므로 Javadoc에 명확히 문서화
3. **SecurityConfig 경로 순서**: `/api/v1/admin/events/**`를 `/api/v1/admin/**` 규칙 **앞에** 배치해야 한다. Spring Security는 첫 번째 매칭 규칙을 적용하므로, 순서를 잘못 배치하면 OPERATOR가 접근 차단됨
4. **감사 이력 reason=null 허용**: `EventStatusChangeHistory.reason`이 nullable인지 확인. 현재 DDL에서 NOT NULL 제약이 없으므로 DDL 변경 불필요. 서비스 레벨에서 publish/unpublish 이력 기록 시 `reason = null`로 전달
5. **Lazy Evaluation 비영향**: `updateStatusIfNeeded()` 메서드에서 visibility 축은 변경하지 않는다. 기존 Lazy Evaluation 코드 수정 불필요

### 잠재적 위험 요소

1. **기존 테스트 깨짐**: `EventDetailResponse`, `EventListResponse`에 필드가 추가되면 기존 테스트에서 record 생성자 호출이 깨질 수 있음. `from()` 팩토리 메서드를 사용하는 곳은 영향 없으나, 직접 생성자를 호출하는 테스트가 있다면 수정 필요
2. **Orval 재생성**: DTO 변경으로 인해 프론트엔드의 Orval 자동 생성 코드가 변경됨. 프론트엔드에서 `visibility` 필드를 처리하지 않으면 타입 에러 발생 가능. 프론트엔드 작업과 동기화 필요
3. **공개 API 목록 조회 성능**: visibility 필터 추가로 인한 쿼리 성능 변화 확인. `event_visibility` 컬럼에 인덱스가 필요한지 검토 (대부분 PUBLISHED이므로 인덱스 효과 제한적일 수 있음)
4. **행사 신청(Registration) 연동**: `EventRegistrationService.registerEvent()`에서 `findByIdAndNotDeleted()`로 행사를 조회하므로 UNPUBLISHED 행사에 대한 신청이 차단되지 않는 문제가 있음. **TASK-020에서 visibility 검증 로직을 추가하여 해결**. `cancelRegistration()` 등 기존 신청 관리 메서드는 unpublish 후에도 정상 동작해야 하므로 visibility 검증을 추가하지 않음

### 기존 코드와의 통합 포인트

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `Event.java` | **수정** | visibility 필드, publish(), unpublish() 추가 |
| `EventChangeType.java` | **수정** | EVENT_PUBLISHED, EVENT_UNPUBLISHED 추가 |
| `EventDetailResponse.java` | **수정** | visibility 필드 추가, from() 메서드 수정 |
| `EventListResponse.java` | **수정** | visibility 필드 추가, from() 메서드 수정 |
| `EventRepository.java` | **수정** | visibility 기반 쿼리 추가 |
| `EventService.java` | **수정** | publishEvent(), unpublishEvent(), getAdminEventList(visibility 파라미터 포함), getAdminEvent() 추가, 기존 getEventList()/getEvent() 수정 |
| `EventRegistrationService.java` | **수정** | registerEvent()에 UNPUBLISHED 행사 신청 차단 visibility 검증 추가 |
| `ApiSecurityConfig.java` | **수정** | `/api/v1/admin/events/**` OPERATOR+ 규칙 추가 |
| `EventVisibility.java` | **신규** | enum 신규 생성 |
| `AdminEventController.java` | **신규** | 관리자 전용 컨트롤러 신규 생성 |
| `V46__add_event_visibility_column.sql` | **신규** | Flyway 마이그레이션 |

---

## 완료 기준

### 검증 기준 충족 체크리스트

- [ ] EVT-INV-05: 행사 생성 시 `visibility = UNPUBLISHED` 초기 상태 설정
- [ ] EVT-INV-14: `EventChangeType`에 EVENT_PUBLISHED, EVENT_UNPUBLISHED 추가, 감사 이력 정상 기록 (reason=null)
- [ ] EVT-INV-16: visibility 축이 eventStatus/registrationStatus와 독립적으로 동작, `Event.update()`에서 visibility 미변경
- [ ] EVT-INV-17: UNPUBLISHED <-> PUBLISHED 양방향 전이 가능, 동일 상태 전이 거부
- [ ] EVT-INV-18: 공개 API 목록에서 UNPUBLISHED 행사 제외, 공개 API 단건에서 UNPUBLISHED 행사 404 반환
- [ ] EVT-INV-19: publish/unpublish body 없이 호출 가능 (사유 불필요)
- [ ] EVT-INV-20: unpublish 시 registrationStatus=OPEN이면 CLOSED(MANUAL_CLOSE) 자동 마감, NOT_STARTED/CLOSED이면 변경 없음
- [ ] EVT-INV-21: 관리자 API에서 PUBLISHED + UNPUBLISHED 행사 모두 조회 가능
- [ ] EVT-INV-22: EventDetailResponse, EventListResponse에 visibility 필드 포함
- [ ] EVT-INV-23: Flyway 마이그레이션 후 기존 행사 데이터 `event_visibility = 'PUBLISHED'`

### 테스트 통과 체크리스트

- [ ] GAP-EVT-25: 행사 생성 시 visibility=UNPUBLISHED 초기값 테스트
- [ ] GAP-EVT-26: EventVisibility 양방향 전이 + 동일 상태 거부 테스트
- [ ] GAP-EVT-27: 공개 API 목록 조회에서 UNPUBLISHED 행사 제외 테스트
- [ ] GAP-EVT-28: 공개 API 단건 조회에서 UNPUBLISHED 행사 404 테스트
- [ ] GAP-EVT-29: 관리자 API 목록 조회에서 전체 행사 반환 테스트
- [ ] GAP-EVT-30: 관리자 API 단건 조회에서 UNPUBLISHED 행사 정상 반환 테스트
- [ ] GAP-EVT-31: unpublish + registrationStatus=OPEN 연동 테스트
- [ ] GAP-EVT-32: unpublish + registrationStatus=NOT_STARTED 변경 없음 테스트
- [ ] GAP-EVT-33: unpublish + registrationStatus=CLOSED closeReason 유지 테스트
- [ ] GAP-EVT-34: publish/unpublish 시 eventStatus 변경 없음 테스트
- [ ] GAP-EVT-35: Event.update() 호출 전후 visibility 불변 테스트
- [ ] GAP-EVT-36: SecurityConfig OPERATOR+ 접근 허용 테스트
- [ ] GAP-EVT-37: SecurityConfig MEMBER/ASSOCIATE 접근 차단 테스트
- [ ] GAP-EVT-38: DTO visibility 필드 포함 테스트
- [ ] GAP-EVT-39: publish/unpublish body 없이 호출 성공 테스트
- [ ] GAP-EVT-40: 관리자 API 목록 조회 필터 동작 테스트
- [ ] GAP-EVT-41: DB 마이그레이션 후 기존 데이터 정합성 확인
- [ ] GAP-EVT-42: COMPLETED 행사 unpublish registrationStatus 변경 없음 테스트
- [ ] GAP-EVT-43: CANCELED 행사 publish eventStatus 변경 없음 테스트
- [ ] GAP-EVT-44: publish/unpublish 감사 이력 기록 테스트 (reason=null)
- [ ] GAP-EVT-45: UNPUBLISHED 행사에 대한 registerEvent() 신청 거부 테스트 (신규)

### 확인이 필요한 사항

1. **AdminEventService 분리 여부**: 관리자 전용 조회 로직을 기존 `EventService`에 추가할지, 별도 `AdminEventService`로 분리할지 팀 논의 필요. SRP 관점에서 분리가 바람직하나, 기존 Event 도메인 로직(Lazy Evaluation 등)을 공유해야 하므로 코드 중복 우려
2. ~~**행사 신청 서비스 연동**~~ -> **TASK-020으로 해결됨**. `EventRegistrationService.registerEvent()`에서 UNPUBLISHED 행사 신청을 차단하는 visibility 검증 로직을 추가한다.
3. **프론트엔드 동기화 시점**: 백엔드 API 변경(DTO visibility 추가, 관리자 API 신규) 완료 후 프론트엔드 Orval 재생성 및 UI 작업 시점 조율 필요
