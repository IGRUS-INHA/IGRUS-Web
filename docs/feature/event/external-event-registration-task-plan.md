# 외부인 행사 신청 (External Event Registration) 작업 계획

## 개요

- **기능 설명**: 외부인(비회원)이 행사에 신청할 수 있는 기능. `allowExternal` 플래그로 행사별 외부인 신청 허용 여부를 제어하고, 준회원(ASSOCIATE)은 `allowExternal=true`인 행사에서만 기존 엔드포인트를 통해 신청 가능. 외부인 중복 방지(studentId, phone 각각), 정원 공유, 외부인 설문 연동(별도 ExternalSurveyResponse 테이블), 관리자만 외부인 신청 취소 가능.
- **관련 문서**
  - 검증 기준: [`docs/criteria/event/external-event-registration-verification-criteria.md`](../../criteria/event/external-event-registration-verification-criteria.md)
  - 테스트 케이스: [`docs/test-case/event/external-event-registration-test-cases.md`](../../test-case/event/external-event-registration-test-cases.md)
  - 행사 신청 검증 기준: [`docs/criteria/event/event-registration-verification-criteria.md`](../../criteria/event/event-registration-verification-criteria.md) (REG-INV-04, SEC-REG-01 영향)
  - 행사 검증 기준: [`docs/criteria/event/event-verification-criteria.md`](../../criteria/event/event-verification-criteria.md)
  - 설문-행사 연동 검증 기준: [`docs/criteria/event/survey-event-registration-verification-criteria.md`](../../criteria/event/survey-event-registration-verification-criteria.md)
  - 관련 이슈: [#517 - [Backend] 외부인도 행사 신청할 수 있는 기능 구현](https://github.com/IGRUS-INHA/IGRUS-Web/issues/517)
- **작성일**: 2026-03-06
- **설계 결정 기준선**: DECISION-01(A: 단일 테이블), DECISION-02(서비스 레벨만), DECISION-03(A: 관리자만 취소), DECISION-04(B: 별도 ExternalSurveyResponse 테이블), DECISION-05(기본값 false), DECISION-06(studentId 기반 시간 겹침 검증), DECISION-07(A: 기존 승인/거절 API 재사용), DECISION-08(A: 기존 관리자 API 패턴 확장)

---

## 핵심 설계 요약

### 데이터 모델 (DECISION-01: 단일 테이블)

```
event_registrations (기존 테이블 확장)
├── event_registrations_user_id      BIGINT NULL (기존 NOT NULL -> nullable 변경)
├── event_registrations_is_external  BOOLEAN NOT NULL DEFAULT FALSE (신규)
├── event_registrations_external_name       VARCHAR(50) NULL (신규)
├── event_registrations_external_student_id VARCHAR(20) NULL (신규)
├── event_registrations_external_phone      VARCHAR(20) NULL (신규)
├── event_registrations_external_department VARCHAR(100) NULL (신규)
└── (기존 컬럼들 유지)

UNIQUE 제약 변경:
  - 기존 uk_event_registrations_event_user -> 삭제 또는 조건부 변경 (user nullable)
  - 신규 DB UNIQUE 없음 (DECISION-02: 서비스 레벨만)

external_survey_responses (신규 테이블, DECISION-04: 옵션 B)
├── external_survey_responses_id            BIGINT PK AUTO_INCREMENT
├── external_survey_responses_survey_id     BIGINT NOT NULL (FK -> surveys)
├── external_survey_responses_student_id    VARCHAR(20) NOT NULL
├── external_survey_responses_answers       JSON NOT NULL
├── external_survey_responses_created_at    DATETIME NOT NULL
└── external_survey_responses_registration_id BIGINT NOT NULL (FK -> event_registrations)
```

### 엔드포인트 구조

| 엔드포인트 | 메서드 | 대상 | 인증 | 상태 |
|-----------|--------|------|------|------|
| `POST /api/v1/events/{eventId}/registrations/external` | POST | 외부인 | `security: []` | OpenAPI 스펙 추가 완료 |
| `POST /api/v1/events/{eventId}/registrations` | POST | 회원+준회원(조건부) | Bearer | 기존 (로직 변경) |
| `POST /api/v1/registrations/{registrationId}/cancel` | POST | 관리자 | Bearer | **OpenAPI 스펙 추가 필요** (GAP-EXT-03) |
| `POST /api/v1/registrations/{registrationId}/approve` | POST | 관리자 | Bearer | 기존 재사용 (DECISION-07) |
| `POST /api/v1/registrations/{registrationId}/reject` | POST | 관리자 | Bearer | 기존 재사용 (DECISION-07) |

---

## 작업 목록

### 1. OpenAPI 스펙 변경

#### TASK-001: OpenAPI 스펙 -- 관리자 취소 엔드포인트 추가

- **작업 ID**: TASK-001
- **작업명**: OpenAPI 스펙에 `POST /api/v1/registrations/{registrationId}/cancel` 엔드포인트 추가
- **설명**: GAP-EXT-03에 명시된 관리자 취소 엔드포인트를 OpenAPI 스펙에 추가한다. DECISION-08(방안 A) 확정에 따라 기존 승인/거절/되돌리기(`/approve`, `/reject`, `/revert`)와 동일한 URL 패턴을 사용한다. 외부인 신청뿐 아니라 회원 신청에 대해서도 관리자가 취소할 수 있도록 설계한다.
  - **스펙 내용**:
    - 태그: `Event Registration` (기존 승인/거절과 동일 태그)
    - operationId: `cancelRegistrationByAdmin`
    - 인증: `security: [BearerAuthentication]`
    - 요청 본문: 없음
    - 응답: 200 OK + `RegistrationResponse`, 401, 403, 404
- **관련 검증 기준**: EXT-INV-09, DECISION-08
- **관련 테스트 케이스**: TC-019, TC-020, TC-021, TC-067, TC-068
- **선행 작업**: 없음
- **구현 범위**: both (OpenAPI 스펙 + 코드 재생성)
- **예상 난이도**: 중

#### TASK-002: openapi-generator 재생성 및 인터페이스 확인

- **작업 ID**: TASK-002
- **작업명**: OpenAPI 스펙 변경 후 백엔드/프론트엔드 코드 재생성
- **설명**: TASK-001 완료 후 `./gradlew openApiGenerate`를 실행하여 컨트롤러 인터페이스와 모델 DTO를 재생성한다. 외부인 신청 스펙이 이미 추가되어 있으므로(`ExternalRegisterEventRequest`, `registerEventExternal` operationId) 새로 생성되는 인터페이스를 확인한다. 프론트엔드 `pnpm api:generate`도 실행한다.
- **관련 검증 기준**: 해당 없음 (인프라)
- **관련 테스트 케이스**: 컴파일 통과 확인
- **선행 작업**: TASK-001
- **구현 범위**: both
- **예상 난이도**: 하

---

### 2. DB 마이그레이션

#### TASK-003: Flyway 마이그레이션 -- Event 테이블 allowExternal 컬럼 추가

- **작업 ID**: TASK-003
- **작업명**: `events` 테이블에 `event_allow_external` 컬럼 추가 마이그레이션 스크립트 작성
- **설명**: DECISION-05 확정에 따라 `events` 테이블에 `event_allow_external BOOLEAN NOT NULL DEFAULT FALSE` 컬럼을 추가한다. 기존 행사 데이터는 모두 `FALSE`로 설정된다.
  - **DDL**: `ALTER TABLE events ADD COLUMN event_allow_external BOOLEAN NOT NULL DEFAULT FALSE;`
  - 현재 Flyway 최신 버전: V47. 따라서 V48을 사용한다.
- **관련 검증 기준**: EXT-INV-06, DECISION-05
- **관련 테스트 케이스**: TC-014, TC-015
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-004: Flyway 마이그레이션 -- EventRegistration 테이블 외부인 컬럼 추가

- **작업 ID**: TASK-004
- **작업명**: `event_registrations` 테이블에 외부인 정보 컬럼 추가 및 user FK nullable 변경
- **설명**: DECISION-01(옵션 A: 단일 테이블) 확정에 따라 `event_registrations` 테이블을 확장한다.
  - **DDL**:
    1. `ALTER TABLE event_registrations MODIFY event_registrations_user_id BIGINT NULL;` -- 외부인은 user 없음
    2. `ALTER TABLE event_registrations ADD COLUMN event_registrations_is_external BOOLEAN NOT NULL DEFAULT FALSE;`
    3. `ALTER TABLE event_registrations ADD COLUMN event_registrations_external_name VARCHAR(50) NULL;`
    4. `ALTER TABLE event_registrations ADD COLUMN event_registrations_external_student_id VARCHAR(20) NULL;`
    5. `ALTER TABLE event_registrations ADD COLUMN event_registrations_external_phone VARCHAR(20) NULL;`
    6. `ALTER TABLE event_registrations ADD COLUMN event_registrations_external_department VARCHAR(100) NULL;`
    7. 기존 UNIQUE 제약(`uk_event_registrations_event_user`) 삭제 또는 변경 -- user_id가 nullable이 되므로 기존 UNIQUE가 외부인 신청(user_id=NULL)에서 문제를 일으킬 수 있음. MySQL에서 NULL은 UNIQUE 비교에서 무시되지만, 회원 신청의 UNIQUE은 유지해야 하므로 삭제하지 않는다.
  - 마이그레이션 파일: V49
  - **DECISION-02**: DB UNIQUE 제약조건 없음 (서비스 레벨만). 외부인 studentId/phone UNIQUE 인덱스는 추가하지 않는다.
- **관련 검증 기준**: EXT-INV-01~10, DECISION-01, DECISION-02
- **관련 테스트 케이스**: TC-001~TC-011, TC-031~TC-040
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-005: Flyway 마이그레이션 -- ExternalSurveyResponse 테이블 생성

- **작업 ID**: TASK-005
- **작업명**: `external_survey_responses` 테이블 신규 생성 마이그레이션 스크립트 작성
- **설명**: DECISION-04(옵션 B: 별도 테이블) 확정에 따라 외부인 설문 응답 전용 테이블을 생성한다.
  - **DDL**:
    ```sql
    CREATE TABLE external_survey_responses (
        external_survey_responses_id BIGINT NOT NULL AUTO_INCREMENT,
        external_survey_responses_survey_id BIGINT NOT NULL,
        external_survey_responses_registration_id BIGINT NOT NULL,
        external_survey_responses_student_id VARCHAR(20) NOT NULL,
        external_survey_responses_answers JSON NOT NULL,
        external_survey_responses_created_at DATETIME(6) NOT NULL,
        PRIMARY KEY (external_survey_responses_id),
        CONSTRAINT fk_ext_survey_resp_survey FOREIGN KEY (external_survey_responses_survey_id) REFERENCES surveys(survey_id),
        CONSTRAINT fk_ext_survey_resp_registration FOREIGN KEY (external_survey_responses_registration_id) REFERENCES event_registrations(event_registrations_id)
    );
    ```
  - 마이그레이션 파일: V50
- **관련 검증 기준**: EXT-INV-11, DECISION-04
- **관련 테스트 케이스**: TC-027, TC-028, TC-053, TC-054
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 3. 도메인 계층

#### TASK-006: Event 엔티티에 allowExternal 필드 추가

- **작업 ID**: TASK-006
- **작업명**: `Event` 엔티티에 `allowExternal` 필드 및 관련 메서드 추가
- **설명**: `Event` 엔티티에 `allowExternal` (Boolean, 기본값 false) 필드를 추가한다.
  - `@Column(name = "event_allow_external", nullable = false)` -- `Boolean` 타입
  - `Event.create()` 팩토리 메서드에 `Boolean allowExternal` 파라미터 추가 (null이면 false)
  - `Event.update()` 메서드에서 `allowExternal` 변경 지원
  - `isAllowExternal()` 편의 메서드 (Lombok `@Getter`로 자동 생성)
- **관련 검증 기준**: EXT-INV-06, DECISION-05
- **관련 테스트 케이스**: TC-014, TC-015
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-007: EventRegistration 엔티티 변경 -- 외부인 지원

- **작업 ID**: TASK-007
- **작업명**: `EventRegistration` 엔티티에 외부인 필드 추가 및 user nullable 변경
- **설명**: DECISION-01(단일 테이블) 확정에 따라 `EventRegistration` 엔티티를 확장한다.
  - `user` 필드: `@JoinColumn(nullable = true)` -- 외부인 신청 시 null
  - `isExternal` 필드: `@Column(name = "event_registrations_is_external", nullable = false)` -- Boolean, 기본값 false
  - `externalName` 필드: `@Column(name = "event_registrations_external_name", length = 50)` -- nullable
  - `externalStudentId` 필드: `@Column(name = "event_registrations_external_student_id", length = 20)` -- nullable
  - `externalPhone` 필드: `@Column(name = "event_registrations_external_phone", length = 20)` -- nullable
  - `externalDepartment` 필드: `@Column(name = "event_registrations_external_department", length = 100)` -- nullable
  - `createExternal(Event, String name, String studentId, String phone, String department)` 정적 팩토리 메서드 추가 -- user=null, isExternal=true
  - 기존 `create(Event, User)` 메서드는 변경 없음 (isExternal=false 유지)
  - 기존 UNIQUE 제약(`uk_event_registrations_event_user`) 처리: MySQL에서 NULL은 UNIQUE 비교에서 무시되므로 외부인 신청(user=null)은 이 제약에 걸리지 않음. 기존 회원 UNIQUE은 유지.
- **관련 검증 기준**: EXT-INV-01~10, DECISION-01
- **관련 테스트 케이스**: TC-001, TC-031~TC-036
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-008: ExternalSurveyResponse 엔티티 생성

- **작업 ID**: TASK-008
- **작업명**: `ExternalSurveyResponse` 엔티티 신규 생성
- **설명**: DECISION-04(옵션 B: 별도 테이블) 확정에 따라 외부인 설문 응답 전용 엔티티를 생성한다. `igrus.web.event.domain` 패키지에 배치한다.
  - `id`: Long, PK, AUTO_INCREMENT
  - `surveyId`: Long, NOT NULL (Survey FK)
  - `registrationId`: Long, NOT NULL (EventRegistration FK)
  - `studentId`: String(20), NOT NULL -- 외부인 식별
  - `answers`: JSON 타입 -- 설문 응답 데이터 (질문ID+답변 쌍 배열)
  - `createdAt`: Instant, NOT NULL
  - `BaseEntity`는 상속하지 않음 (createdBy가 항상 null이므로 불필요)
  - `create()` 정적 팩토리 메서드
- **관련 검증 기준**: EXT-INV-11, DECISION-04
- **관련 테스트 케이스**: TC-027
- **선행 작업**: TASK-005
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-009: 외부인 관련 예외 클래스 및 ErrorCode 추가

- **작업 ID**: TASK-009
- **작업명**: 외부인 신청 관련 예외 클래스 및 `EventErrorCode` 항목 추가
- **설명**: 검증 기준서에 정의된 신규 예외 클래스를 `igrus.web.event.exception` 패키지에 추가하고, `EventErrorCode`에 대응하는 에러 코드를 추가한다.
  - `ExternalRegistrationNotAllowedException`: allowExternal=false 행사에 외부인 신청 (HTTP 400)
  - `ExternalAlreadyRegisteredException`: 동일 studentId 또는 phone으로 중복 신청 (HTTP 409)
  - `RegisteredMemberExistsException`: 동일 studentId로 가입된 회원 존재 (HTTP 400)
  - `EventErrorCode`에 `EXTERNAL_REGISTRATION_NOT_ALLOWED`, `EXTERNAL_ALREADY_REGISTERED`, `REGISTERED_MEMBER_EXISTS` 추가
- **관련 검증 기준**: EXT-INV-01, EXT-INV-02, EXT-INV-03, EXT-INV-12
- **관련 테스트 케이스**: TC-002, TC-003, TC-005, TC-007, TC-029
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 4. Repository 계층

#### TASK-010: EventRegistrationRepository 외부인 중복 검사 쿼리 추가

- **작업 ID**: TASK-010
- **작업명**: `EventRegistrationRepository`에 외부인 중복 검사 및 시간 겹침 쿼리 추가
- **설명**: DECISION-02(서비스 레벨만) 확정에 따라 서비스에서 사용할 쿼리 메서드를 추가한다.
  - `existsByEventAndExternalStudentIdAndStatusNot(Event event, String studentId, EventRegistrationStatus status)` -- studentId 중복 검사 (CANCELED 제외)
  - `existsByEventAndExternalPhoneAndStatusNot(Event event, String phone, EventRegistrationStatus status)` -- phone 중복 검사 (CANCELED 제외)
  - `existsOverlappingExternalRegistration(String studentId, Instant eventStartAt, Instant eventEndAt, EventRegistrationStatus excludedStatus)` -- 외부인 시간 겹침 검증 (DECISION-06: studentId 기반)
- **관련 검증 기준**: EXT-INV-02, EXT-INV-03, DECISION-02, DECISION-06
- **관련 테스트 케이스**: TC-003~TC-008, TC-040
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-011: ExternalSurveyResponseRepository 생성

- **작업 ID**: TASK-011
- **작업명**: `ExternalSurveyResponseRepository` 인터페이스 생성
- **설명**: `ExternalSurveyResponse` 엔티티에 대한 Spring Data JPA Repository를 생성한다.
  - `JpaRepository<ExternalSurveyResponse, Long>` 상속
  - 필요 시 커스텀 쿼리 추가 (설문 집계 시 사용)
- **관련 검증 기준**: EXT-INV-11, DECISION-04
- **관련 테스트 케이스**: TC-027
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 5. 서비스 계층

#### TASK-012: ExternalEventRegistrationService 핵심 비즈니스 로직 구현

- **작업 ID**: TASK-012
- **작업명**: 외부인 행사 신청 서비스 핵심 로직 구현
- **설명**: `igrus.web.event.service` 패키지에 `ExternalEventRegistrationService`를 신규 생성한다. 외부인 신청의 전체 검증 및 처리 로직을 구현한다.
  - **검증 순서**:
    1. 행사 조회 + PUBLISHED 확인 (EXT-INV-08: UNPUBLISHED -> 404)
    2. `allowExternal == true` 확인 (EXT-INV-01)
    3. 동일 studentId로 가입된 회원 존재 확인 (EXT-INV-12)
    4. studentId 중복 검사 -- CANCELED 제외 (EXT-INV-02)
    5. phone 중복 검사 -- CANCELED 제외 (EXT-INV-03)
    6. `registrationStatus == OPEN` 확인 (EXT-INV-07)
    7. 신청 기간 내 확인 (EXT-INV-07)
    8. 시간 겹침 검증 -- studentId 기반 (DECISION-06)
    9. 설문 연동 처리 (EXT-INV-11) -- surveyId != null이면 설문 응답 저장
    10. 정원 확인 + 원자적 UPDATE (EXT-INV-04)
    11. `EventRegistration.createExternal()` 호출 및 저장
  - **의존성**: `EventRepository`, `EventRegistrationRepository`, `UserRepository`, `ExternalSurveyResponseRepository`, `SurveyRepository`, `SurveyAnswerValidator`
  - **트랜잭션**: `@Transactional` -- 설문 응답 + 신청이 원자적으로 처리
  - **로그**: Section 6-2의 로그 메시지 규격 준수
- **관련 검증 기준**: EXT-INV-01~12, DECISION-01~06
- **관련 테스트 케이스**: TC-001~TC-011, TC-016~TC-018, TC-027~TC-030, TC-031, TC-033, TC-074~TC-076
- **선행 작업**: TASK-007, TASK-009, TASK-010, TASK-011
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-013: EventRegistrationService 준회원 조건부 허용 로직 변경

- **작업 ID**: TASK-013
- **작업명**: `EventRegistrationService.registerEvent()`의 준회원 차단 로직을 조건부로 변경
- **설명**: 기존 `EventRegistrationService.registerEvent()` 라인 141-143의 `user.isAssociate()` 무조건 차단 로직을 `user.isAssociate() && !event.getAllowExternal()` 조건으로 변경한다. `allowExternal == true`인 행사에서는 준회원도 기존 `/registrations` 엔드포인트를 통해 신청 가능하도록 한다.
  - **변경 전**: `if (user.isAssociate()) throw new AssociateMemberNotAllowedException();`
  - **변경 후**: `if (user.isAssociate() && !event.getAllowExternal()) throw new AssociateMemberNotAllowedException();`
- **관련 검증 기준**: EXT-INV-05, REG-INV-04 변경 (Section 0-1), SEC-REG-01 변경 (Section 0-2)
- **관련 테스트 케이스**: TC-012, TC-013, TC-058, TC-061, TC-065, TC-066, TC-078, TC-079
- **선행 작업**: TASK-006
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-014: 관리자 신청 취소 서비스 구현

- **작업 ID**: TASK-014
- **작업명**: 관리자가 신청(회원/외부인)을 취소하는 서비스 메서드 구현
- **설명**: DECISION-08(방안 A) 확정에 따라 `POST /api/v1/registrations/{registrationId}/cancel` 엔드포인트의 비즈니스 로직을 구현한다. 기존 `EventRegistrationService`에 메서드를 추가하거나, 별도 서비스로 분리한다.
  - **로직**:
    1. registrationId로 `EventRegistration` 조회 (없으면 404)
    2. 요청자 권한 확인 (OPERATOR+ -- SecurityConfig에서 이미 보장되나, 서비스 레벨에서도 검증)
    3. 취소 전 상태 저장 (`previousStatus = registration.getStatus().name()`)
    4. `registration.cancel()` 호출 (이미 CANCELED면 예외)
    5. `isActive()` 상태였으면 `decrementCurrentCount` 원자적 UPDATE
    6. **감사 이력 이벤트 발행**: `eventPublisher.publishEvent(new EventStatusChanged(eventId, operatorUserId, EventChangeType.REGISTRATION_CANCELED_BY_ADMIN, previousStatus, "CANCELED", null))` — 기존 `EventStatusChanged` + `RecordEventStatusChangeService` 패턴 재사용
    7. 200 OK + `RegistrationResponse` 반환
  - 외부인 신청뿐 아니라 회원 신청에 대해서도 관리자가 취소 가능
  - **감사 이력**: `EventChangeType.REGISTRATION_CANCELED_BY_ADMIN` enum 값을 추가하고, 기존 `RecordEventStatusChangeService`가 이벤트를 수신하여 `EventStatusChangeHistory`에 기록. 기존 패턴(`EventService.cancelEvent()` 등)과 동일한 방식
  - **로그**: `관리자 행사 신청 취소 - eventId: {}, registrationId: {}, operatorId: {}`
- **관련 검증 기준**: EXT-INV-09, DECISION-03, DECISION-08
- **관련 테스트 케이스**: TC-019, TC-020, TC-021, TC-032, TC-036, TC-067, TC-068, TC-077
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-015: RegistrationListResponse 외부인 필드 포함 로직

- **작업 ID**: TASK-015
- **작업명**: 신청자 목록 조회 응답에 외부인 정보 필드 추가
- **설명**: Section 0-3에 정의된 `RegistrationListResponse` 스키마 변경을 구현한다. 기존 필드의 nullable 변경 + 신규 필드(`phone`, `isExternal`) 추가.
  - **`RegistrationListResponse.from()` 라인 42의 `registration.getUser()` NPE 방어 필수**: 외부인 신청은 `user == null`이므로, `getUser()`가 null인 경우 회원 전용 필드(`userId`, `userEmail`, `userGender`, `userGrade`)를 null로 설정하는 분기 처리가 필요하다.
  - 회원 신청: `userId != null`, `isExternal == false`, `phone == null`
  - 외부인 신청: `userId == null`, `userEmail == null`, `userGender == null`, `userGrade == null`, `isExternal == true`, `phone != null`
  - 서비스 레이어 DTO(`RegistrationListResponse`)와 컨트롤러의 자동 생성 DTO 매핑 모두 수정
- **관련 검증 기준**: Section 0-3 (스키마 변경)
- **관련 테스트 케이스**: TC-080
- **선행 작업**: TASK-007, TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-030: 기존 EventRegistrationService의 user null 방어 처리

- **작업 ID**: TASK-030
- **작업명**: 기존 `EventRegistrationService`에서 `registration.getUser()` NPE 방어 로직 추가
- **설명**: `EventRegistration.user`가 nullable로 변경됨에 따라, 기존 `EventRegistrationService`에서 `registration.getUser()`를 직접 호출하는 4개 지점에서 외부인 신청(user=null)에 대한 NPE 방어 처리를 추가한다. (`RegistrationListResponse.from()` 라인 42의 NPE 방어는 TASK-015에서 처리한다.)
  - **수정 지점 1: `approveRegistration` 라인 338 부근**
    - `validateNoTimeOverlap(registration.getUser().getId(), event)` -- 외부인 신청 승인 시 `user == null`이므로 NPE 발생
    - **수정 방안**: 외부인 여부를 먼저 확인하고, 외부인이면 `externalStudentId` 기반 시간 겹침 검증(TASK-010에서 추가한 `existsOverlappingExternalRegistration`)을 호출하고, 회원이면 기존 `validateNoTimeOverlap(userId, event)`를 호출하는 분기 처리
  - **수정 지점 2: `reRegister` 라인 634 부근**
    - `Long userId = registration.getUser().getId()` -- 외부인은 user=null이므로 NPE 발생
    - **수정 방안**: 메서드 진입 시점에 `registration.isExternal()` 체크를 추가하여, 외부인 신청이면 재신청 불가 예외를 발생시킨다. 외부인은 인증 수단이 없어 재신청(`reRegister`) 경로를 사용할 수 없으며, CANCELED 후 새 신청은 별도 `registerExternal()` 경로를 사용한다 (TC-004 참조).
  - **수정 지점 3: `reRegister` 라인 650 부근**
    - `User user = registration.getUser()` -- 수정 지점 2의 사전 차단으로 도달하지 않지만, 방어적으로 null 체크 추가
  - **수정 지점 4: `reRegister` 라인 674 부근**
    - `validateNoTimeOverlap(registration.getUser().getId(), event)` -- 수정 지점 2의 사전 차단으로 도달하지 않지만, 방어적으로 null 체크 추가
- **관련 검증 기준**: EXT-INV-09 (승인/거절 API 재사용 시 외부인 호환), DECISION-07 (기존 승인/거절 API 재사용)
- **관련 테스트 케이스**: TC-034 (외부인 승인 시 시간 겹침 검증), TC-037 (외부인 재신청 불가)
- **선행 작업**: TASK-007, TASK-010
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 6. 컨트롤러 및 보안

#### TASK-016: ExternalEventRegistrationController 구현

- **작업 ID**: TASK-016
- **작업명**: 외부인 행사 신청 컨트롤러 구현
- **설명**: OpenAPI 스펙에 이미 정의된 `Event External Registration` 태그의 `registerEventExternal` 오퍼레이션에 대한 컨트롤러를 구현한다. openapi-generator가 생성한 인터페이스를 `implements`한다.
  - `POST /api/v1/events/{eventId}/registrations/external`
  - `security: []` -- 인증 불필요
  - 컨트롤러 로그: `외부인 행사 신청 요청 - eventId: {}, name: {}, studentId: {}`
  - `ExternalEventRegistrationService.registerExternal()` 위임
- **관련 검증 기준**: EXT-INV-01~12, SEC-EXT-02
- **관련 테스트 케이스**: TC-057, TC-060, TC-063, TC-064, TC-069
- **선행 작업**: TASK-002, TASK-012
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-017: 관리자 취소 컨트롤러 구현

- **작업 ID**: TASK-017
- **작업명**: 관리자 신청 취소 컨트롤러 구현
- **설명**: TASK-001에서 추가한 OpenAPI 스펙의 `cancelRegistrationByAdmin` 오퍼레이션에 대한 컨트롤러를 구현한다.
  - `POST /api/v1/registrations/{registrationId}/cancel`
  - `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`
  - 컨트롤러 로그: `관리자 행사 신청 취소 요청 - registrationId: {}, userId: {}`
  - `cancelRegistrationByAdmin()` 서비스 위임
- **관련 검증 기준**: EXT-INV-09, SEC-EXT-05, SEC-EXT-06
- **관련 테스트 케이스**: TC-019, TC-020, TC-021, TC-067, TC-068
- **선행 작업**: TASK-002, TASK-014
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-018: SecurityConfig 외부인 엔드포인트 및 관리자 취소 경로 추가

- **작업 ID**: TASK-018
- **작업명**: `ApiSecurityConfig`에 외부인 엔드포인트 비인증 허용 + 관리자 취소 경로 권한 설정
- **설명**: Spring Security 설정을 변경한다.
  1. `POST /api/v1/events/*/registrations/external` -- `permitAll()` (인증 불필요, `security: []`)
  2. `POST /api/v1/registrations/*/cancel` -- `hasAnyRole("OPERATOR", "ADMIN")`
  - **배치 위치**: 기존 `.requestMatchers("/api/events/*/registrations").hasAnyRole("OPERATOR", "ADMIN")` 블록 근처에 추가
- **관련 검증 기준**: SEC-EXT-02, SEC-EXT-05, SEC-EXT-06
- **관련 테스트 케이스**: TC-064, TC-067, TC-068
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 7. 단위 테스트

#### TASK-019: Event 도메인 allowExternal 단위 테스트

- **작업 ID**: TASK-019
- **작업명**: `Event` 엔티티의 `allowExternal` 관련 단위 테스트 작성
- **설명**: `EventTest`에 다음 테스트를 추가한다:
  1. `Event.create()` 시 `allowExternal` 미지정 -> `false` 기본값 (TC-014)
  2. `Event.create()` 시 `allowExternal: true` 명시 설정 (TC-015)
  3. `Event.update()` 시 `allowExternal` 변경 가능
- **관련 검증 기준**: EXT-INV-06, DECISION-05
- **관련 테스트 케이스**: TC-014, TC-015
- **선행 작업**: TASK-006
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-020: EventRegistration 외부인 팩토리 단위 테스트

- **작업 ID**: TASK-020
- **작업명**: `EventRegistration.createExternal()` 단위 테스트 작성
- **설명**: `EventRegistrationTest`에 다음 테스트를 추가한다:
  1. `createExternal()` 호출 시 `isExternal == true`, `user == null`
  2. 선착순 행사: `status == REGISTERED` (TC-031)
  3. 선발제 행사: `status == WAITING` (TC-033)
  4. 외부인 정보 필드(name, studentId, phone, department) 정확히 설정됨
  5. 기존 `create(Event, User)` 호출 시 `isExternal == false` 확인 (회귀)
- **관련 검증 기준**: EXT-INV-01, EXT-INV-10, DECISION-01
- **관련 테스트 케이스**: TC-001, TC-031, TC-033
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-021: ExternalRegisterEventRequest Bean Validation 단위 테스트

- **작업 ID**: TASK-021
- **작업명**: `ExternalRegisterEventRequest` DTO의 Bean Validation 단위 테스트 작성
- **설명**: 필수 필드 검증 및 경계값 테스트를 작성한다.
  - 모든 필수 필드 유효 -> 통과 (TC-022)
  - name null -> 위반 (TC-023)
  - studentId 빈 문자열 -> 위반 (TC-024)
  - phone null -> 위반 (TC-025)
  - department null -> 위반 (TC-026)
  - name 1자/50자 경계 -> 통과 (TC-041, TC-042)
  - name 51자 -> 위반 (TC-043)
  - studentId 1자/20자 경계 -> 통과 (TC-044, TC-045)
  - studentId 21자 -> 위반 (TC-046)
  - phone 1자/20자 경계 -> 통과 (TC-047, TC-048)
  - phone 21자 -> 위반 (TC-049)
  - department 1자/100자 경계 -> 통과 (TC-050, TC-051)
  - department 101자 -> 위반 (TC-052)
- **관련 검증 기준**: EXT-INV-10, Section 4-1 BVA
- **관련 테스트 케이스**: TC-022~TC-026, TC-041~TC-052
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 8. 서비스 통합 테스트

#### TASK-022: ExternalEventRegistrationService 서비스 통합 테스트

- **작업 ID**: TASK-022
- **작업명**: 외부인 행사 신청 서비스 Mockito 기반 단위 테스트 작성
- **설명**: `ExternalEventRegistrationService`의 전체 검증 로직을 Mockito 기반으로 테스트한다. 다음 시나리오를 커버한다:
  - **정상 흐름**: allowExternal=true 행사에 외부인 신청 성공 (TC-001)
  - **allowExternal 검증**: allowExternal=false -> ExternalRegistrationNotAllowedException (TC-002)
  - **중복 방지**: studentId 중복 -> 409 (TC-003), phone 중복 -> 409 (TC-005), 둘 다 중복 -> 409 (TC-007)
  - **CANCELED 후 재신청**: CANCELED 상태 동일 studentId -> 성공 (TC-004), 동일 phone -> 성공 (TC-006)
  - **다른 행사**: 동일 정보로 다른 행사 신청 성공 (TC-008)
  - **정원 공유**: 마지막 1자리 성공 (TC-009), 가득 참 -> 400 (TC-010), 외부인만으로 가득 -> 회원도 차단 (TC-011)
  - **OPEN/기간 검증**: CLOSED -> 에러 (TC-016), 기간 외 -> 에러 (TC-017)
  - **UNPUBLISHED 차단**: 404 (TC-018)
  - **가입 회원 존재**: 동일 studentId 회원 존재 -> 400 (TC-029), 미존재 -> 성공 (TC-030)
  - **설문 연동**: 설문 응답 포함 신청 성공 (TC-027), 설문 응답 누락 -> 400 (TC-028), 설문 미연결 + null -> 성공 (TC-053), 설문 미연결 + 응답 제공 -> 무시 (TC-054)
  - **정원 경계**: 외부인만 4명 + 1명 추가 (TC-055), 회원 5명 + 외부인 -> 400 (TC-056)
  - **상태 모델**: 선착순 -> REGISTERED (TC-031), 선발제 -> WAITING (TC-033), WAITING 중 동일 studentId 중복 시도 (TC-040)
  - **로그 검증**: 성공 로그 (TC-074), 중복 거부 로그 (TC-075), allowExternal=false 로그 (TC-076)
  - **감사 추적**: createdBy null, 관리자 취소 후 updatedBy (TC-077)
- **관련 검증 기준**: EXT-INV-01~12, DECISION-01~06
- **관련 테스트 케이스**: TC-001~TC-011, TC-016~TC-018, TC-027~TC-030, TC-031, TC-033, TC-040, TC-053~TC-056, TC-074~TC-077
- **선행 작업**: TASK-012
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-023: 준회원 조건부 허용 및 기존 동작 회귀 테스트

- **작업 ID**: TASK-023
- **작업명**: `EventRegistrationService`의 준회원 조건부 허용 변경에 대한 테스트 작성
- **설명**: 기존 `EventRegistrationServiceTest`에 다음 테스트를 추가한다:
  - 준회원 + allowExternal=true -> 신청 성공 (TC-012)
  - 준회원 + allowExternal=false -> 403 (TC-013)
  - MEMBER + allowExternal=false -> 신청 성공 (TC-078, 회귀)
  - ASSOCIATE + allowExternal=false -> 403 (TC-079, 회귀)
- **관련 검증 기준**: EXT-INV-05, REG-INV-04 변경, SEC-REG-01 변경
- **관련 테스트 케이스**: TC-012, TC-013, TC-078, TC-079
- **선행 작업**: TASK-013
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-024: 관리자 취소 서비스 테스트

- **작업 ID**: TASK-024
- **작업명**: 관리자 신청 취소 서비스 단위 테스트 작성
- **설명**: 관리자 취소 로직에 대한 Mockito 기반 테스트를 작성한다:
  - OPERATOR가 외부인 REGISTERED 신청 취소 -> 성공, currentCount-- (TC-019, TC-032)
  - ADMIN이 외부인 신청 취소 -> 성공 (TC-020)
  - 선발제 APPROVED 외부인 취소 -> CANCELED, currentCount-- (TC-036)
  - 선발제 WAITING 외부인 관리자 취소 -> CANCELED, currentCount 변경 없음
  - 이미 CANCELED 상태 -> 예외
  - 존재하지 않는 registrationId -> 404
  - **감사 이력 이벤트 발행 검증**: 취소 성공 시 `EventStatusChanged(eventId, operatorUserId, REGISTRATION_CANCELED_BY_ADMIN, previousStatus, "CANCELED", null)` 이벤트가 `eventPublisher.publishEvent()`로 발행되는지 검증 (TC-077)
- **관련 검증 기준**: EXT-INV-09, DECISION-03, DECISION-08
- **관련 테스트 케이스**: TC-019, TC-020, TC-032, TC-036
- **선행 작업**: TASK-014
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 9. 컨트롤러 통합 테스트

#### TASK-025: 외부인 신청 컨트롤러 통합 테스트

- **작업 ID**: TASK-025
- **작업명**: 외부인 행사 신청 Controller 통합 테스트 (MockMvc) 작성
- **설명**: `@SpringBootTest` + MockMvc 기반 통합 테스트를 작성한다. allowExternal 동치 분할 6가지 조합(Section 4-2)과 SEC-EXT 보안 테스트를 커버한다.
  - allowExternal=true + 외부인 -> 201 (TC-057)
  - allowExternal=true + ASSOCIATE -> 201 (TC-058)
  - allowExternal=true + MEMBER -> 201 (TC-059)
  - allowExternal=false + 외부인 -> 400 (TC-060)
  - allowExternal=false + ASSOCIATE -> 403 (TC-061)
  - allowExternal=false + MEMBER -> 201 (TC-062)
  - SEC-EXT-01: 외부인 + allowExternal=false -> 400 (TC-063)
  - SEC-EXT-02: 인증 토큰 없이 접근 -> 정상 (TC-064)
  - SEC-EXT-03: ASSOCIATE + allowExternal=false -> 403 (TC-065)
  - SEC-EXT-04: ASSOCIATE + allowExternal=true -> 201 (TC-066)
  - SEC-EXT-07: allowExternal=true + UNPUBLISHED -> 404 (TC-069)
  - RegistrationListResponse 스키마 검증 (TC-080)
  - OpenAPI 스펙 응답 검증: `OpenApiValidatorUtil.matchesOpenApiSpec()` 포함
- **관련 검증 기준**: Section 4-2, SEC-EXT-01~07
- **관련 테스트 케이스**: TC-057~TC-069, TC-080
- **선행 작업**: TASK-016, TASK-015
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-026: 관리자 취소 컨트롤러 통합 테스트

- **작업 ID**: TASK-026
- **작업명**: 관리자 취소 Controller 통합 테스트 (MockMvc) 작성
- **설명**: `@SpringBootTest` + MockMvc 기반으로 관리자 취소 엔드포인트의 접근 제어를 테스트한다.
  - MEMBER가 취소 시도 -> 403 (TC-021, TC-067)
  - OPERATOR가 취소 -> 200 (TC-068)
  - 비인증 사용자가 취소 -> 401
  - OpenAPI 스펙 응답 검증 포함
- **관련 검증 기준**: EXT-INV-09, SEC-EXT-05, SEC-EXT-06
- **관련 테스트 케이스**: TC-021, TC-067, TC-068
- **선행 작업**: TASK-017
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-027: 선발제 행사 외부인 FSM 통합 테스트

- **작업 ID**: TASK-027
- **작업명**: 선발제 행사에서 외부인 신청의 FSM 전이 통합 테스트 작성
- **설명**: 선발제(MANUAL_APPROVE) 행사에서 외부인 신청의 상태 전이를 E2E로 테스트한다.
  - 외부인 신청 -> WAITING (TC-033)
  - 승인 -> APPROVED + currentCount++ (TC-034)
  - 거절 -> REJECTED (TC-035)
  - APPROVED 취소 -> CANCELED + currentCount-- (TC-036)
  - REJECTED에서 직접 approve -> 실패 (TC-038)
  - REGISTERED에서 approve -> 실패 (TC-039)
  - CANCELED에서 reRegister 불가 (TC-037)
- **관련 검증 기준**: Section 2-1, DECISION-07
- **관련 테스트 케이스**: TC-033~TC-039
- **선행 작업**: TASK-012, TASK-014
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 10. 동시성 테스트

#### TASK-028: 동시성 테스트

- **작업 ID**: TASK-028
- **작업명**: 외부인 신청 동시성 멀티스레드 테스트 작성
- **설명**: 멀티스레드 환경에서의 동시성 제어를 검증한다.
  - 회원 + 외부인 마지막 1자리 동시 신청 -> 하나만 성공 (TC-070)
  - 동일 studentId 동시 신청 -> DECISION-02 허용 범위 확인 (TC-071)
  - 동일 phone 동시 신청 -> DECISION-02 허용 범위 확인 (TC-072)
  - 외부인 신청 + 관리자 취소 동시 발생 -> 순차 처리 (TC-073)
- **관련 검증 기준**: EXT-INV-02, EXT-INV-03, EXT-INV-04, Section 3-1, Section 7-2
- **관련 테스트 케이스**: TC-070~TC-073
- **선행 작업**: TASK-012, TASK-014
- **구현 범위**: backend
- **예상 난이도**: 상

---

### 11. 문서 갱신

#### TASK-029: 기존 검증 기준서 갱신

- **작업 ID**: TASK-029
- **작업명**: 기존 검증 기준서 및 OpenAPI 스펙 갱신 (GAP-EXT-01~05)
- **설명**: 구현 완료 후 Section 9 부록에 명시된 기존 문서를 갱신한다.
  - GAP-EXT-01: `event-registration-verification-criteria.md`의 REG-INV-04 본문 갱신
  - GAP-EXT-02: `event-registration-verification-criteria.md`의 SEC-REG-01 및 5-1 매트릭스 갱신
  - GAP-EXT-04: `event-verification-criteria.md`의 Event 엔티티/DTO에 `allowExternal` 필드 추가 기술
  - **GAP-EXT-05**: DECISION-02(서비스 레벨만) 확정에 따라 DB UNIQUE 경합 테스트가 불필요해졌으므로, 검증 기준서의 GAP-EXT-05 상태를 **"해결됨 -- DECISION-02에 의해 DB UNIQUE 미사용으로 확정, 서비스 레벨 동시성 테스트(TC-071, TC-072)로 대체"**로 갱신한다. 또한 다음 항목들도 DECISION-02를 반영하여 수정한다:
    - **Section 4-3** (동시성 경계값 테이블): "동시 동일 studentId 신청 (서비스 통과 후 DB 충돌)" 및 "동시 동일 phone 신청 (서비스 통과 후 DB 충돌)" 행의 기대 결과를 "DB UNIQUE constraint violation" → "DECISION-02: 극히 드문 중복 허용 (서비스 레벨만)"으로 수정
    - **Section 6-2** (로그 메시지 규격): "DB UNIQUE 경합" WARN 로그 항목이 존재하면 제거 또는 "해당 없음 (DECISION-02: DB UNIQUE 미사용)"으로 수정
    - **Section 7-2** (동시성 테스트 시나리오): 시나리오 2~3의 "DB UNIQUE가 최종 방어선" → "서비스 레벨 검증만 수행, 극히 드문 동시 중복은 관리자가 수동 정리"로 수정
- **관련 검증 기준**: GAP-EXT-01, GAP-EXT-02, GAP-EXT-04, GAP-EXT-05
- **관련 테스트 케이스**: 해당 없음 (문서)
- **선행 작업**: TASK-025, TASK-026
- **구현 범위**: docs
- **예상 난이도**: 중 (GAP-EXT-05 반영으로 검증 기준서 Section 4-3, 6-2, 7-2 수정 범위 증가)

---

## 작업 순서 및 의존성

### 의존성 그래프

```
TASK-001 (OpenAPI 취소 스펙) ──> TASK-002 (코드 재생성)
                                       |
TASK-003 (Flyway allowExternal) ──> TASK-006 (Event 엔티티)
                                       |
TASK-004 (Flyway EventRegistration) ──> TASK-007 (EventRegistration 엔티티)
     |                                     |
     v                                     ├──> TASK-010 (Repository 쿼리)
TASK-005 (Flyway ExternalSurveyResp) ──> TASK-008 (ExternalSurveyResp 엔티티)
                                           |
                                           └──> TASK-011 (ExternalSurveyResp Repo)

TASK-009 (예외/ErrorCode) ──────┐
                                 v
TASK-007 + TASK-009 + TASK-010 + TASK-011 ──> TASK-012 (핵심 서비스)
                                                  |
TASK-006 ──> TASK-013 (준회원 조건부)              ├──> TASK-016 (외부인 컨트롤러) [+ TASK-002]
                                                  |
TASK-007 ──> TASK-014 (관리자 취소 서비스) ──> TASK-017 (관리자 취소 컨트롤러) [+ TASK-002]
                                                  |
TASK-007 + TASK-010 ──> TASK-030 (기존 서비스 user null 방어)
                                                  |
TASK-007 + TASK-002 ──> TASK-015 (RegistrationListResponse)

TASK-018 (SecurityConfig) ──(독립)

TASK-006 ──> TASK-019 (Event 단위 테스트)
TASK-007 ──> TASK-020 (EventRegistration 단위 테스트)
TASK-002 ──> TASK-021 (Bean Validation 단위 테스트)

TASK-012 ──> TASK-022 (서비스 통합 테스트)
TASK-013 ──> TASK-023 (준회원 회귀 테스트)
TASK-014 ──> TASK-024 (관리자 취소 테스트)

TASK-016 + TASK-015 ──> TASK-025 (외부인 컨트롤러 통합 테스트)
TASK-017 ──> TASK-026 (관리자 취소 통합 테스트)
TASK-012 + TASK-014 ──> TASK-027 (선발제 FSM 통합 테스트)
TASK-012 + TASK-014 ──> TASK-028 (동시성 테스트)

TASK-025 + TASK-026 ──> TASK-029 (문서 갱신)
```

### 권장 실행 순서 (6개 단계)

#### Phase 1: 기반 작업 (병렬 가능)

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 1-1 | TASK-001 | OpenAPI 스펙 -- 관리자 취소 엔드포인트 추가 | 독립 |
| 1-2 | TASK-003 | Flyway -- allowExternal 컬럼 | 독립 |
| 1-3 | TASK-004 | Flyway -- EventRegistration 외부인 컬럼 | 독립 |
| 1-4 | TASK-005 | Flyway -- ExternalSurveyResponse 테이블 | TASK-004 이후 |
| 1-5 | TASK-009 | 예외/ErrorCode 정의 | 독립 |
| 1-6 | TASK-018 | SecurityConfig 경로 추가 | 독립 |

#### Phase 2: 도메인/엔티티 (Phase 1 완료 후)

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 2-1 | TASK-002 | openapi-generator 재생성 | TASK-001 이후 |
| 2-2 | TASK-006 | Event 엔티티 allowExternal | TASK-003 이후 |
| 2-3 | TASK-007 | EventRegistration 엔티티 변경 | TASK-004 이후 |
| 2-4 | TASK-008 | ExternalSurveyResponse 엔티티 | TASK-005 이후 |

#### Phase 3: Repository + 서비스 (Phase 2 완료 후)

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 3-1 | TASK-010 | EventRegistrationRepository 쿼리 | TASK-007 이후 |
| 3-2 | TASK-011 | ExternalSurveyResponseRepository | TASK-008 이후 |
| 3-3 | TASK-012 | 외부인 신청 핵심 서비스 | TASK-007, 009, 010, 011 이후 |
| 3-4 | TASK-013 | 준회원 조건부 허용 변경 | TASK-006 이후 |
| 3-5 | TASK-014 | 관리자 취소 서비스 | TASK-007 이후 |
| 3-6 | TASK-015 | RegistrationListResponse 변경 | TASK-007, 002 이후 |
| 3-7 | TASK-030 | 기존 서비스 user null 방어 | TASK-007, 010 이후 |

#### Phase 4: 컨트롤러 (Phase 3 완료 후)

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 4-1 | TASK-016 | 외부인 신청 컨트롤러 | TASK-002, 012 이후 |
| 4-2 | TASK-017 | 관리자 취소 컨트롤러 | TASK-002, 014 이후 |

#### Phase 5: 테스트 (각 구현 완료 후, 병렬 가능)

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 5-1 | TASK-019 | Event 도메인 단위 테스트 | TASK-006 이후 |
| 5-2 | TASK-020 | EventRegistration 단위 테스트 | TASK-007 이후 |
| 5-3 | TASK-021 | Bean Validation 단위 테스트 | TASK-002 이후 |
| 5-4 | TASK-022 | 서비스 통합 테스트 | TASK-012 이후 |
| 5-5 | TASK-023 | 준회원 회귀 테스트 | TASK-013 이후 |
| 5-6 | TASK-024 | 관리자 취소 서비스 테스트 | TASK-014 이후 |
| 5-7 | TASK-025 | 외부인 컨트롤러 통합 테스트 | TASK-016, 015 이후 |
| 5-8 | TASK-026 | 관리자 취소 통합 테스트 | TASK-017 이후 |
| 5-9 | TASK-027 | 선발제 FSM 통합 테스트 | TASK-012, 014 이후 |
| 5-10 | TASK-028 | 동시성 테스트 | TASK-012, 014 이후 |

#### Phase 6: 문서 갱신

| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 6-1 | TASK-029 | 기존 검증 기준서 갱신 | 구현 + 테스트 완료 후 |

---

## 구현 시 주의사항

### 기술적 고려사항

1. **EventRegistration.user nullable 변경**: 기존 `@JoinColumn(nullable = false)` -> `nullable = true`로 변경 시, 기존 코드에서 `registration.getUser()`를 호출하는 모든 곳에서 null 체크가 필요하다. 구체적으로 다음 5개 지점에서 NPE 방어가 필수이다 (**TASK-030**, **TASK-015**에서 처리):
   - `EventRegistrationService.approveRegistration()` 라인 338: `validateNoTimeOverlap(registration.getUser().getId(), event)` -- 외부인이면 studentId 기반 검증으로 분기
   - `EventRegistrationService.reRegister()` 라인 634: `Long userId = registration.getUser().getId()` -- 외부인 사전 차단
   - `EventRegistrationService.reRegister()` 라인 650: `User user = registration.getUser()` -- 외부인 사전 차단
   - `EventRegistrationService.reRegister()` 라인 674: `validateNoTimeOverlap(registration.getUser().getId(), event)` -- 외부인 사전 차단
   - `RegistrationListResponse.from()` 라인 42: `var user = registration.getUser()` -- null 분기 처리 (TASK-015)

2. **UNIQUE 제약조건 호환성**: MySQL에서 `UNIQUE(event_id, user_id)` 제약은 user_id가 NULL인 행에 대해 적용되지 않는다 (MySQL 8에서 NULL은 고유값으로 취급되지 않음). 따라서 기존 UNIQUE을 삭제하지 않아도 외부인 신청(user=null)이 가능하다. 단, 이를 테스트로 검증해야 한다.

3. **단일 테이블 정원 공유**: DECISION-01(옵션 A) 확정으로 기존 `incrementCurrentCountIfAvailable()` 원자적 UPDATE를 그대로 사용한다. 외부인 신청 서비스에서도 동일한 Repository 쿼리를 호출하므로 회원/외부인 동시 신청 시 원자적 제어가 보장된다.

4. **ExternalSurveyResponse JSON 필드**: 설문 응답을 JSON으로 저장하되, 응답 유효성 검증은 기존 `SurveyAnswerValidator.validate()`를 재사용한다. JSON 스키마는 `[{questionId: Long, answer: String/List}]` 형태.

5. **SecurityConfig 경로 순서**: `/api/v1/events/*/registrations/external`을 `permitAll()`로 설정할 때, 기존 `/api/v1/events/*/registrations` 경로보다 **더 구체적이므로 먼저** 매칭된다. 하지만 명시적으로 `permitAll()` 규칙을 인증 필요 규칙 앞에 배치하는 것이 안전하다.

6. **외부인 시간 겹침 검증 (DECISION-06)**: studentId 기반으로 동일 학번의 다른 외부인 신청과 시간 겹침을 검사한다. 쿼리는 `WHERE er.externalStudentId = :studentId AND er.status NOT IN ('CANCELED') AND (event.eventStartAt < :endAt AND event.eventEndAt > :startAt)`와 같은 형태.

7. **createdBy/updatedBy 감사**: 외부인 신청은 비인증 요청이므로 `SecurityAuditorAware`에서 null을 반환한다. 따라서 `createdBy = null`. 관리자 취소 시에는 인증된 요청이므로 `updatedBy = operatorUserId`.

### 잠재적 위험 요소

1. **기존 테스트 깨짐**: `EventRegistration.user`가 nullable로 변경되고, `createExternal()` 메서드가 추가되면서 기존 테스트에서 null 관련 검증이 깨질 수 있다. 기존 테스트는 모두 `create(Event, User)` 팩토리를 사용하므로 영향은 제한적이지만, `RegistrationListResponse` 생성 로직 변경 시 기존 테스트가 깨질 수 있다.

2. **Orval 재생성 영향**: OpenAPI 스펙에 `ExternalRegisterEventRequest`, 관리자 취소 엔드포인트가 추가되면서 프론트엔드 타입이 변경된다. 프론트엔드에서 해당 타입을 사용하지 않으면 컴파일 에러는 없지만, `RegistrationListResponse`의 필드 변경(nullable)은 기존 프론트엔드 코드에 영향을 줄 수 있다.

3. **서비스 레벨 중복 검증 한계 (DECISION-02)**: DB UNIQUE 없이 서비스 레벨만으로 중복을 검증하므로 동시 요청 시 극히 드물게 중복 레코드가 생성될 수 있다. 이는 설계 결정으로 허용된 사항이며, 관리자가 수동 정리할 수 있다.

4. **외부인 설문 응답 집계**: DECISION-04(별도 테이블)로 인해 설문 집계 시 `SurveyResponse`와 `ExternalSurveyResponse`를 UNION하여 처리해야 한다. 현재 설문 집계 기능이 별도로 존재하지 않으면 즉시 영향은 없으나, 향후 집계 기능 구현 시 고려 필요.

### 기존 코드와의 통합 포인트

| 파일 | 변경 유형 | 설명 |
|------|:---:|------|
| `Event.java` | **수정** | `allowExternal` 필드 추가, `create()`, `update()` 시그니처 변경 |
| `EventRegistration.java` | **수정** | `user` nullable 변경, 외부인 필드 5개 추가, `createExternal()` 팩토리 추가 |
| `EventRegistrationService.java` | **수정** | 준회원 조건부 허용 로직 변경 (라인 141-143), `approveRegistration`/`reRegister` 내 `getUser()` NPE 방어 (라인 338, 634, 650, 674) |
| `EventRegistrationRepository.java` | **수정** | 외부인 중복 검사, 시간 겹침 쿼리 추가 |
| `EventChangeType.java` | **수정** | `REGISTRATION_CANCELED_BY_ADMIN` enum 값 추가 (TASK-014) |
| `EventErrorCode.java` | **수정** | 3개 에러 코드 추가 |
| `ApiSecurityConfig.java` | **수정** | 외부인 엔드포인트 `permitAll()`, 관리자 취소 경로 추가 |
| `openapi/paths/events.yaml` | **수정** | 관리자 취소 엔드포인트 추가 |
| `openapi/schemas/events.yaml` | **확인** | 외부인 신청 스키마 이미 추가됨 |
| **`ExternalEventRegistrationService.java`** | **신규** | 외부인 신청 핵심 서비스 |
| **`ExternalEventRegistrationController.java`** | **신규** | 외부인 신청 컨트롤러 |
| **`ExternalSurveyResponse.java`** | **신규** | 외부인 설문 응답 엔티티 |
| **`ExternalSurveyResponseRepository.java`** | **신규** | 외부인 설문 응답 Repository |
| **`ExternalRegistrationNotAllowedException.java`** | **신규** | 예외 클래스 |
| **`ExternalAlreadyRegisteredException.java`** | **신규** | 예외 클래스 |
| **`RegisteredMemberExistsException.java`** | **신규** | 예외 클래스 |
| **`V48__add_allow_external_to_events.sql`** | **신규** | Flyway 마이그레이션 |
| **`V49__add_external_columns_to_event_registrations.sql`** | **신규** | Flyway 마이그레이션 |
| **`V50__create_external_survey_responses_table.sql`** | **신규** | Flyway 마이그레이션 |

---

## 완료 기준

### 검증 기준 충족 여부 체크리스트

| 불변조건 | 커버 작업 | 상태 |
|---------|-----------|:---:|
| EXT-INV-01: allowExternal 검사 | TASK-012, 016, 022, 025 | [ ] |
| EXT-INV-02: studentId 중복 방지 | TASK-010, 012, 022 | [ ] |
| EXT-INV-03: phone 중복 방지 | TASK-010, 012, 022 | [ ] |
| EXT-INV-04: 정원 공유 | TASK-012, 022, 028 | [ ] |
| EXT-INV-05: 준회원 조건부 허용 | TASK-013, 023, 025 | [ ] |
| EXT-INV-06: allowExternal 기본값 | TASK-003, 006, 019 | [ ] |
| EXT-INV-07: OPEN + 기간 내 검증 | TASK-012, 022 | [ ] |
| EXT-INV-08: UNPUBLISHED 차단 | TASK-012, 022, 025 | [ ] |
| EXT-INV-09: 관리자만 취소 | TASK-014, 017, 018, 024, 026, 030 | [ ] |
| EXT-INV-10: 필수 필드 검증 | TASK-021 | [ ] |
| EXT-INV-11: 외부인 설문 연동 | TASK-005, 008, 011, 012, 022 | [ ] |
| EXT-INV-12: 동일 학번 가입 회원 존재 | TASK-012, 022 | [ ] |
| SEC-EXT-01~07: 권한/보안 | TASK-018, 025, 026 | [ ] |
| Section 0-1: REG-INV-04 변경 | TASK-013, 023, 029 | [ ] |
| Section 0-2: SEC-REG-01 변경 | TASK-013, 023, 029 | [ ] |
| Section 0-3: RegistrationListResponse 변경 | TASK-015, 025 | [ ] |

### 테스트 케이스 통과 여부 체크리스트

| 카테고리 | 테스트 케이스 | 커버 작업 | 상태 |
|---------|-------------|-----------|:---:|
| 도메인 규칙 | TC-001~TC-030 | TASK-019~024 | [ ] |
| 상태 모델 | TC-031~TC-040 | TASK-020, 022, 027 | [ ] |
| 입력 경계값 | TC-041~TC-056 | TASK-021, 022 | [ ] |
| 동치 분할 | TC-057~TC-062 | TASK-025 | [ ] |
| 권한/보안 | TC-063~TC-069 | TASK-025, 026 | [ ] |
| 동시성 | TC-070~TC-073 | TASK-028 | [ ] |
| 관측 가능성 | TC-074~TC-077 | TASK-022, 024 | [ ] |
| 회귀 테스트 | TC-078~TC-080 | TASK-023, 025 | [ ] |

### GAP 해결 체크리스트

| GAP ID | 내용 | 커버 작업 | 상태 |
|--------|------|-----------|:---:|
| GAP-EXT-01 | REG-INV-04 본문 갱신 | TASK-029 | [ ] |
| GAP-EXT-02 | SEC-REG-01 및 매트릭스 갱신 | TASK-029 | [ ] |
| GAP-EXT-03 | OpenAPI 취소 엔드포인트 추가 | TASK-001 | [ ] |
| GAP-EXT-04 | Event 엔티티/DTO allowExternal 기술 | TASK-029 | [ ] |
| GAP-EXT-05 | DB UNIQUE 경합 테스트 -> DECISION-02 반영 갱신 (Section 4-3, 6-2, 7-2 포함) | TASK-029 | [ ] |

### 확인이 필요한 사항

1. **Flyway 버전 번호**: 현재 최신이 V47이므로 V48, V49, V50으로 작성했으나, 다른 작업이 먼저 머지되면 번호 충돌 가능. 머지 시점에 확인 필요.
2. **ExternalSurveyResponse 스키마 상세**: JSON 필드에 저장할 설문 응답의 정확한 스키마(질문 유형별 답변 구조)는 기존 `SurveyAnswer` 엔티티 구조를 참조하여 결정. 구현 시 `SurveyAnswerValidator.validate()` 호출 후 유효한 응답만 JSON으로 직렬화.
3. **관리자 취소 컨트롤러 배치**: 기존 `EventRegistrationController`에 추가할지, 별도 컨트롤러로 분리할지는 OpenAPI 스펙 태그에 따라 결정. 기존 승인/거절과 동일 태그이면 동일 컨트롤러에 배치.
4. **RegistrationListResponse 외부인 필드**: OpenAPI 스펙에 이미 `isExternal`, `phone` 필드가 추가되어 있는지 확인 필요. 추가되어 있으면 TASK-002에서 자동 생성되므로 서비스 DTO 매핑만 수정.
