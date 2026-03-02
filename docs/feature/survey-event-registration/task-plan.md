# 설문 연동 행사 신청 (Survey-Event Registration) 작업 계획

## 개요

- **기능 설명**: 행사(Event)에 설문(Survey)을 선택적으로 연결하여, 설문 응답과 행사 신청을 하나의 통합 API로 원자적으로 처리하는 기능. 설문이 없는 행사는 기존 그대로 바로 신청 가능. 3종 API 체계 — 행사 단독 API, 설문 단독 API, 행사-설문 통합 API — 를 유지한다.
- **관련 문서**
  - 검증 기준: [`docs/criteria/event/survey-event-registration-verification-criteria.md`](../../criteria/event/survey-event-registration-verification-criteria.md)
  - 행사 검증 기준: [`docs/criteria/event/event-verification-criteria.md`](../../criteria/event/event-verification-criteria.md)
  - 행사 신청 검증 기준: [`docs/criteria/event/event-registration-verification-criteria.md`](../../criteria/event/event-registration-verification-criteria.md)
  - 설문 검증 기준: [`docs/criteria/survey/survey-criteria-v1.md`](../../criteria/survey/survey-criteria-v1.md)
- **작성일**: 2026-03-02
- **기술 스택**: Backend -- Java 21 + Spring Boot 3.5.9 + Spring Data JPA + MySQL / Frontend -- React 19 + TypeScript + Vite 7
- **설계 결정 기준선**: DECISION-01(A), DECISION-02(A), DECISION-03(A), DECISION-04(A), DECISION-05(A), DECISION-06(A), DECISION-07(B 확정)

---

## 핵심 설계 요약

### 도메인 간 결합

```
Event 도메인                      Survey 도메인
+----------------+               +----------------+
| Event          |               | Survey         |
|  surveyId: Long|---- 조회 --->  |  id: Long      |
|                |               |                |
| EventRegistra- |               | SurveyResponse |
| tionService    |---- 조회 --->  |  surveyId      |
|                |               |  userId        |
+----------------+               +----------------+
```

- **단방향 의존**: Event -> Survey (약한 참조, Long surveyId, FK 없음)
- **설문 도메인 코드 변경 없음**: 행사 도메인이 `SurveyAnswerValidator` + `SurveyResponseRepository`를 직접 호출하는 방식 (`SurveyResponseService.submitResponse()` 미사용)

### 3종 API 트랜잭션 모델

| API 계층 | 트랜잭션 | 서비스 |
|----------|:---:|------|
| 행사 단독 API (surveyId == null) | Tx-1 | `EventRegistrationService.registerEvent()` -- 기존 그대로 |
| 설문 단독 API (행사 무관) | Tx-A | `SurveyResponseService.submitResponse()` -- 기존 그대로 |
| **행사-설문 통합 API** (surveyId != null) | **Tx-C (단일)** | `EventRegistrationService.registerEventWithSurvey()` -- **신규** |

---

## 작업 목록

### 1. DB 스키마 및 엔티티 변경

#### TASK-001: Flyway 마이그레이션 -- events 테이블에 surveyId 컬럼 추가

- **작업명**: events 테이블에 `event_survey_id` 컬럼 추가 마이그레이션 스크립트 작성
- **설명**: `events` 테이블에 `event_survey_id BIGINT NULL` 컬럼을 추가한다. FK 제약조건은 설정하지 않는다 (약한 참조 채택, DECISION-02). soft delete와의 호환 및 도메인 독립성을 위해 서비스 레벨에서 참조 무결성을 검증한다.
- **관련 검증 기준**: SEVT-INV-01, SEVT-INV-02, SEVT-INV-03
- **관련 테스트 케이스**: 해당 없음 (스키마 변경)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **세부사항**:
  - 마이그레이션 파일: `V46__add_survey_id_to_events.sql`
  - `ALTER TABLE events ADD COLUMN event_survey_id BIGINT NULL;`
  - 인덱스 추가: `CREATE INDEX idx_events_survey_id ON events(event_survey_id);` (설문 기준 행사 조회 성능)

#### TASK-002: Event 엔티티에 surveyId 필드 추가

- **작업명**: Event 엔티티에 `surveyId` 필드 및 관련 메서드 추가
- **설명**: `Event` 엔티티에 `surveyId` (nullable Long) 필드를 추가한다. `create()` 팩토리 메서드에 `surveyId` 파라미터를 추가하고, `update()` 메서드에서 설문 연결/변경/해제를 지원한다. `hasSurvey()` 편의 메서드를 추가한다.
- **관련 검증 기준**: SEVT-INV-01, SEVT-INV-02, SEVT-INV-05
- **관련 테스트 케이스**: Event 도메인 단위 테스트 (SEVT-INV-01, 02, 05)
- **선행 작업**: TASK-001
- **구현 범위**: backend
- **예상 난이도**: 하
- **세부사항**:
  - `@Column(name = "event_survey_id")` -- nullable, FK 없음
  - `Event.create(...)` 시그니처에 `Long surveyId` 추가 (null 허용)
  - `Event.update(...)` 시그니처에 `Long surveyId` 추가
  - `update()` 메서드에서 SEVT-INV-05에 따라 COMPLETED 상태에서만 설문 변경 차단 (기존 update() 로직의 COMPLETED 차단과 동일)
  - `hasSurvey()`: `return surveyId != null;`

### 2. 예외 클래스 및 ErrorCode 정의

#### TASK-003: 설문 연동 관련 예외 클래스 및 ErrorCode 추가

- **작업명**: `SurveyResponseRequiredException`, `SurveyNotReadyException` 예외 및 EventErrorCode 항목 추가
- **설명**: 검증 기준서에 정의된 2개의 신규 예외 클래스를 `igrus.web.event.exception` 패키지에 추가하고, `EventErrorCode`에 대응하는 에러 코드를 추가한다.
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-10, SEC-SEVT-04
- **관련 테스트 케이스**: 예외 발생 시나리오 전체 (F1, F2)
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **세부사항**:
  - `SurveyResponseRequiredException`: 설문 응답 미존재 시 행사 신청 거부 (HTTP 400)
  - `SurveyNotReadyException`: 설문이 NOT_STARTED 상태일 때 행사 신청 거부 (HTTP 400)
  - `EventErrorCode`에 `SURVEY_RESPONSE_REQUIRED`, `SURVEY_NOT_READY` 추가
  - 기존 `SurveyNotFoundException`(설문 도메인)은 설문 삭제/휴지통 시 재활용

### 3. 행사 생성/수정 서비스 변경

#### TASK-004: EventService.createEvent()에 설문 연결 로직 추가

- **작업명**: 행사 생성 시 설문 존재 검증 및 surveyId 저장 로직 구현
- **설명**: `EventService.createEvent()`에서 `surveyId`가 제공된 경우, 해당 설문이 존재하고 활성 상태(deleted == false, trashedAt == null)인지 검증한다. 검증 통과 시 `Event.create()`에 surveyId를 전달한다. `surveyId == null`이면 기존 동작과 동일하게 처리한다.
- **관련 검증 기준**: SEVT-INV-04, SEVT-INV-01, SEVT-INV-02
- **관련 테스트 케이스**: 서비스 단위 (SEVT-INV-04), 시나리오 F4, F5
- **선행 작업**: TASK-002, TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - `EventService`에 `SurveyRepository` 의존성 추가
  - 검증 로직: `surveyRepository.findByIdAndDeletedFalse(surveyId)` -> 존재하지 않거나 `trashedAt != null`이면 `SurveyNotFoundException`
  - SEVT-INV-04: 연결 시점에 `visibility`나 `responseStatus`는 검증하지 않음 (설문이 UNPUBLISHED여도 미리 연결 가능)
  - `CreateEventRequest` DTO에 `Long surveyId` (nullable) 필드 추가

#### TASK-005: EventService.updateEvent()에 설문 변경/해제 로직 추가

- **작업명**: 행사 수정 시 설문 연결/변경/해제 로직 구현
- **설명**: `EventService.updateEvent()`에서 `surveyId` 변경을 처리한다. 새 surveyId가 non-null이면 존재 검증을 수행하고, null이면 설문 해제로 처리한다. SEVT-INV-05에 따라 COMPLETED 상태에서는 설문 변경 불가.
- **관련 검증 기준**: SEVT-INV-05, SEVT-INV-04
- **관련 테스트 케이스**: 서비스 단위 (SEVT-INV-05), 시나리오 4-3 경계값
- **선행 작업**: TASK-004
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - `UpdateEventRequest` DTO에 `Long surveyId` (nullable) 필드 추가
  - DECISION-01(A 권장): 신청자 존재 시에도 설문 변경 허용, 기존 신청 유지
  - 로그: `행사 수정 - eventId: {}, surveyId 변경: {} -> {}` (INFO)

### 4. 행사 신청 서비스 -- 통합 API 핵심 로직

#### TASK-006: 설문 상태 검증 유틸리티 메서드 구현

- **작업명**: 설문 연동 행사 신청 시 설문 상태 검증 로직 구현
- **설명**: `EventRegistrationService` 내에 설문 상태 검증을 위한 private 메서드를 구현한다. 설문의 존재, 삭제 여부, 휴지통 여부, responseStatus를 검증한다.
- **관련 검증 기준**: SEVT-INV-10, SEVT-INV-11
- **관련 테스트 케이스**: 서비스 단위 (SEVT-INV-10, 11)
- **선행 작업**: TASK-003
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - `EventRegistrationService`에 `SurveyRepository` 의존성 추가
  - `validateSurveyState(Survey survey)`:
    - `survey == null || survey.isDeleted()` -> `SurveyNotFoundException`
    - `survey.getTrashedAt() != null` -> `SurveyNotFoundException`
    - `survey.getResponseStatus() == NOT_STARTED` -> `SurveyNotReadyException`
  - DECISION-03(A): `responseStatus != NOT_STARTED`만 검증. `visibility`는 검증 대상 아님

#### TASK-007: EventRegistrationService.registerEventWithSurvey() 신규 메서드 구현

- **작업명**: 설문 응답 + 행사 신청 통합 처리 메서드 구현
- **설명**: `event.surveyId != null`인 행사에 대해 설문 응답 저장과 행사 신청을 단일 트랜잭션으로 원자적으로 처리하는 핵심 메서드를 구현한다. 설문 응답 유효성 검증은 `SurveyAnswerValidator.validate()`를, 저장은 `SurveyResponseRepository.save()`를 직접 호출한다 (`SurveyResponseService.submitResponse()`는 사용하지 않음 -- 아래 위임 방식 참조).
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-08, SEVT-INV-10, SEVT-INV-12
- **관련 테스트 케이스**: 서비스 단위 (SEVT-INV-06, 08, 10), 통합 (선착순/선발제 E2E), 시나리오 S1, F1, F6
- **선행 작업**: TASK-006
- **구현 범위**: backend
- **예상 난이도**: 상
- **세부사항**:

  **의존성 주입:**
  - `EventRegistrationService`에 `SurveyAnswerValidator` + `SurveyResponseRepository` 의존성 추가
  - `SurveyResponseService.submitResponse()`는 **사용하지 않는다**

  **`SurveyResponseService.submitResponse()` 미사용 근거:**
  - `submitResponse()`는 내부적으로 `Survey.isAcceptingResponses()`를 호출하여 `visibility == PUBLISHED && responseStatus == OPEN && trashedAt == null` 전체를 검증한다
  - 그러나 검증 기준서 SEVT-INV-10은 `responseStatus != NOT_STARTED`만 검증하고 `visibility`는 검증 대상이 아니다
  - `submitResponse()`를 그대로 위임하면, UNPUBLISHED+CLOSED 상태에서 기존 응답이 있는 사용자가 `surveyAnswers`를 포함하여 통합 API로 신청할 때 불필요하게 차단된다
  - 따라서 `SurveyAnswerValidator.validate()`로 응답 유효성만 검증하고, `SurveyResponseRepository.save()`로 직접 저장하는 방식을 채택한다

  **검증 순서 (SEVT-INV-12 권장 순서 준수):**
  1. 권한 확인 (REG-INV-04: 준회원 차단)
  2. Lazy Evaluation (행사 상태 갱신)
  3. 중복 신청 확인 (REG-INV-01) / 재신청 분기
  4. 등록 상태 확인 (REG-INV-05: OPEN)
  5. 신청 기간 확인
  6. **설문 상태 검증** (SEVT-INV-10, 11) -- TASK-006의 `validateSurveyState()` 호출
  7. **설문 응답 처리** (SEVT-INV-06) -- 아래 분기 매트릭스에 따라 처리
  8. 시간 겹침 확인 (REG-INV-06)
  9. 정원 확인 및 원자적 UPDATE

  **설문 상태별 `surveyAnswers` 처리 분기 매트릭스:**

  | # | 설문 responseStatus | surveyAnswers 포함 | 기존 응답 존재 | 처리 결과 | 근거 |
  |:-:|:---:|:---:|:---:|:---|:---|
  | 1 | OPEN | O (있음) | 무관 | 새 응답 저장 + 신청 진행 | 정상 케이스. 기존 응답 있으면 설문 INV-01(중복 방지)에 의해 저장 단계에서 예외 발생 -- 클라이언트가 기존 응답 존재 시 surveyAnswers를 생략하도록 안내 |
  | 2 | OPEN | X (없음) | O (있음) | 기존 응답으로 신청 진행 | S4(동일 설문 다중 행사), S3(이미 응답한 사용자) |
  | 3 | OPEN | X (없음) | X (없음) | **실패**: `SurveyResponseRequiredException` | F1(설문 미응답 신청 시도) |
  | 4 | CLOSED | O (있음) | X (없음) | **실패**: `SurveyResponseClosedException` 또는 적절한 예외 | CLOSED 상태에서는 새 응답 저장 불가. 설문 INV-09(PUBLISHED+OPEN에서만 응답 가능) 위반 방지 |
  | 5 | CLOSED | O (있음) | O (있음) | 기존 응답으로 신청 진행 (**제공된 surveyAnswers 무시**) | 기존 응답이 있으므로 신청 자격 충분. 새 응답 저장은 CLOSED라 불가하므로 무시 |
  | 6 | CLOSED | X (없음) | O (있음) | 기존 응답으로 신청 진행 | S3(설문 마감 후 신청) |
  | 7 | CLOSED | X (없음) | X (없음) | **실패**: `SurveyResponseRequiredException` | 응답 없고 새로 저장도 불가 |
  | 8 | NOT_STARTED | 무관 | 무관 | **실패**: `SurveyNotReadyException` | 검증 순서 6단계에서 이미 차단됨 (SEVT-INV-10) |

  **분기 구현 의사코드:**

  ```java
  // 6단계: 설문 상태 검증 (NOT_STARTED, 삭제, 휴지통 차단)
  Survey survey = surveyRepository.findById(event.getSurveyId())
      .orElseThrow(() -> new SurveyNotFoundException());
  validateSurveyState(survey); // NOT_STARTED -> SurveyNotReadyException

  // 7단계: 설문 응답 처리 -- 분기 매트릭스 적용
  boolean hasExistingResponse = surveyResponseRepository
      .existsBySurveyIdAndUserId(event.getSurveyId(), userId);

  if (surveyAnswers != null && !surveyAnswers.isEmpty()) {
      // surveyAnswers 포함된 요청
      if (survey.getResponseStatus() == ResponseStatus.OPEN) {
          // #1: OPEN + surveyAnswers 있음 -> 새 응답 저장
          surveyAnswerValidator.validate(survey, surveyAnswers);
          SurveyResponse response = SurveyResponse.create(survey, userId, surveyAnswers);
          surveyResponseRepository.save(response);
      } else if (survey.getResponseStatus() == ResponseStatus.CLOSED) {
          if (hasExistingResponse) {
              // #5: CLOSED + surveyAnswers 있음 + 기존 응답 있음 -> surveyAnswers 무시, 기존 응답으로 진행
              log.debug("설문 CLOSED 상태 - 제공된 surveyAnswers 무시, 기존 응답으로 진행 - surveyId: {}, userId: {}", survey.getId(), userId);
          } else {
              // #4: CLOSED + surveyAnswers 있음 + 기존 응답 없음 -> 실패
              throw new SurveyResponseRequiredException(); // 또는 전용 예외
          }
      }
  } else {
      // surveyAnswers 미포함된 요청
      if (!hasExistingResponse) {
          // #3, #7: 기존 응답 없음 -> 실패
          throw new SurveyResponseRequiredException();
      }
      // #2, #6: 기존 응답 있음 -> 진행
  }
  ```

  - 원자성: 정원 초과 등 신청 실패 시 설문 응답도 롤백 (동일 트랜잭션)
  - #4 케이스의 예외 선택: `SurveyResponseRequiredException`을 재사용하거나, "설문이 마감되어 새 응답을 저장할 수 없습니다" 의미의 전용 메시지를 포함. 구현 시 ErrorCode 수준에서 결정

#### TASK-008: registerEvent() 메서드에 설문 분기 로직 추가

- **작업명**: 기존 registerEvent()에서 surveyId 유무에 따른 분기 처리
- **설명**: 기존 `registerEvent()` 메서드 진입 시, `event.surveyId != null`이면 `registerEventWithSurvey()`로 위임하고, `null`이면 기존 로직 그대로 실행한다. 이를 통해 동일 엔드포인트(`POST /events/{eventId}/registrations`)에서 설문 유무에 따라 서버가 자동 분기한다.
- **관련 검증 기준**: SEVT-INV-07, SEVT-INV-08, SEVT-INV-12
- **관련 테스트 케이스**: 서비스 단위 (SEVT-INV-07), 회귀 테스트 전체
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - `registerEvent(Long eventId, Long userId, List<SubmitAnswerRequest> surveyAnswers)` 시그니처 오버로드 또는 기존 시그니처 확장
  - `event.hasSurvey()` -> `registerEventWithSurvey(event, user, surveyAnswers)` 위임
  - `!event.hasSurvey()` -> 기존 로직 그대로 (SEVT-INV-07 보존)
  - 기존 테스트가 변경 없이 통과해야 함 (회귀 테스트)

#### TASK-009: handleReRegistration()에 설문 검증 추가

- **작업명**: 재신청 로직에 설문 응답 검증 추가
- **설명**: 취소 후 재신청(`handleReRegistration`) 시에도 설문 연결 행사의 경우 설문 응답 존재 여부를 검증한다. SEVT-INV-06에 따라 신청 및 재신청 시 모두 적용.
- **관련 검증 기준**: SEVT-INV-06 (재신청 적용)
- **관련 테스트 케이스**: 서비스 단위 (재신청 + 설문), 시나리오 4-5 경계값
- **선행 작업**: TASK-007
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - 재신청 시 현재 `event.surveyId` 기준으로 검증 (SEVT-INV-06 기준 시점)
  - 설문이 해제된 경우(`surveyId == null`) 설문 검증 생략 (시나리오 4-5)

### 5. OpenAPI 스펙 변경

#### TASK-010: OpenAPI 스펙 -- 행사 생성/수정 요청에 surveyId 필드 추가

- **작업명**: OpenAPI events.yaml 스펙에 surveyId 필드 추가
- **설명**: 행사 생성(`CreateEventRequest`) 및 수정(`UpdateEventRequest`) 스키마에 `surveyId` (nullable integer) 필드를 추가한다. 행사 상세 응답에도 `surveyId` 필드를 추가한다.
- **관련 검증 기준**: SEVT-INV-01, SEVT-INV-02, SEVT-INV-04, SEVT-INV-05
- **관련 테스트 케이스**: 해당 없음 (스펙)
- **선행 작업**: 없음
- **구현 범위**: both
- **예상 난이도**: 중
- **세부사항**:
  - `openapi/paths/events.yaml` 또는 관련 스키마 파일 수정
  - `CreateEventRequest.surveyId`: `type: integer, format: int64, nullable: true`
  - `UpdateEventRequest.surveyId`: `type: integer, format: int64, nullable: true`
  - `EventDetailResponse.surveyId`: `type: integer, format: int64, nullable: true`
  - `EventCreateResponse.surveyId`: `type: integer, format: int64, nullable: true`
  - `EventListResponse.surveyId`: `type: integer, format: int64, nullable: true`

#### TASK-011: OpenAPI 스펙 -- 행사 신청 요청에 surveyAnswers 필드 추가

- **작업명**: OpenAPI 스펙에 행사 신청 요청 body의 surveyAnswers 필드 추가
- **설명**: 행사 신청 엔드포인트(`POST /events/{eventId}/registrations`)의 요청 body에 `surveyAnswers` (nullable, 설문 응답 배열) 필드를 추가한다. 기존 설문 응답 제출 스키마의 answers 형식을 재사용한다.
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-08
- **관련 테스트 케이스**: 해당 없음 (스펙)
- **선행 작업**: TASK-010
- **구현 범위**: both
- **예상 난이도**: 중
- **세부사항**:
  - 현재 `POST /events/{eventId}/registrations`는 request body가 없음 -> body 추가 필요
  - `surveyAnswers`: 기존 설문 응답 제출의 `answers` 스키마(`SubmitAnswerRequest` 배열) 재사용 또는 $ref
  - 설문 미연결 행사 신청 시 body 없이 또는 `surveyAnswers: null`로 요청 가능
  - 에러 응답 스키마: `SurveyResponseRequiredException`, `SurveyNotReadyException`에 대한 400 응답 추가

#### TASK-012: openapi-generator 재생성 및 컨트롤러 인터페이스 갱신

- **작업명**: OpenAPI 스펙 변경 후 코드 재생성 및 컨트롤러 어댑트
- **설명**: `./gradlew openApiGenerate`로 인터페이스와 모델 DTO를 재생성하고, `EventController` 및 `EventRegistrationController`가 새 인터페이스에 맞게 구현되도록 수정한다.
- **관련 검증 기준**: 해당 없음 (인프라)
- **관련 테스트 케이스**: 컴파일 통과 확인
- **선행 작업**: TASK-010, TASK-011
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - `./gradlew openApiGenerate` 실행
  - 컨트롤러가 새 인터페이스 시그니처에 맞게 `@Override` 메서드 수정
  - DTO 매핑: 생성된 모델 DTO의 `surveyId`, `surveyAnswers` 필드를 내부 DTO/서비스로 전달

### 6. 행사 상세 응답에 설문 정보 포함

#### TASK-013: EventDetailResponse에 설문 연결 정보 추가

- **작업명**: 행사 조회 응답에 surveyId 및 설문 기본 정보 포함
- **설명**: 행사 상세 조회, 목록 조회, 생성 응답 DTO에 `surveyId`를 포함한다. 프론트엔드가 행사에 연결된 설문의 존재 여부와 상태를 판단할 수 있도록 한다.
- **관련 검증 기준**: SEVT-INV-01 (설문 연결 여부 노출)
- **관련 테스트 케이스**: 해당 없음 (DTO 변경)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하
- **세부사항**:
  - `EventDetailResponse.from(Event)` 메서드에 `surveyId` 추가
  - `EventListResponse.from(Event)` 메서드에 `surveyId` 추가
  - `EventCreateResponse.from(Event)` 메서드에 `surveyId` 추가

### 7. 로깅 및 관측 가능성

#### TASK-014: 설문 연동 관련 로그 메시지 추가

- **작업명**: 설문 연동 행사 생성/수정/신청 거부 시 로그 메시지 구현
- **설명**: 검증 기준서 6-1절에 정의된 로그 메시지를 서비스 레이어에 추가한다.
- **관련 검증 기준**: 관측 가능성 6-1 전체
- **관련 테스트 케이스**: 해당 없음 (로깅)
- **선행 작업**: TASK-007, TASK-004, TASK-005
- **구현 범위**: backend
- **예상 난이도**: 하
- **세부사항**:
  - 설문 연결 행사 생성: INFO `행사 생성 요청 - userId: {}, title: {}, surveyId: {}`
  - 설문 연결 변경: INFO `행사 수정 - eventId: {}, surveyId 변경: {} -> {}`
  - 설문 응답 미존재 신청 거부: INFO
  - 설문 NOT_STARTED 신청 거부: INFO
  - 설문 삭제/휴지통 신청 거부: WARN
  - 설문 응답 확인 후 신청 진행: DEBUG

### 8. 단위 테스트

#### TASK-015: Event 도메인 단위 테스트 추가

- **작업명**: Event 엔티티의 surveyId 관련 도메인 단위 테스트 작성
- **설명**: `Event.create()` 및 `Event.update()`에서 surveyId 설정/변경/null 처리가 올바르게 동작하는지 검증한다.
- **관련 검증 기준**: SEVT-INV-01, SEVT-INV-02, SEVT-INV-05
- **관련 테스트 케이스**: 도메인 단위 (Event.create with surveyId, Event.update with surveyId 변경, Event.getSurveyId null)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-016: EventService 설문 연결 단위 테스트 추가

- **작업명**: 행사 생성/수정 시 설문 존재 검증 서비스 단위 테스트 작성
- **설명**: `EventService.createEvent()` 및 `updateEvent()`에서 설문 연결 시 존재 검증 로직을 Mockito 기반 단위 테스트로 검증한다.
- **관련 검증 기준**: SEVT-INV-04, SEVT-INV-05
- **관련 테스트 케이스**: 서비스 단위 (존재하지 않는 설문 ID, 삭제된 설문 ID, 설문 연결/해제)
- **선행 작업**: TASK-004, TASK-005
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-017: EventRegistrationService 설문 연동 단위 테스트 추가

- **작업명**: 설문 연동 행사 신청 서비스 단위 테스트 작성
- **설명**: `registerEventWithSurvey()` 및 설문 분기 로직에 대한 Mockito 기반 단위 테스트를 작성한다. 설문 미연결 행사 회귀 테스트도 포함한다.
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-07, SEVT-INV-08, SEVT-INV-10, SEVT-INV-11, SEVT-INV-12
- **관련 테스트 케이스**: 서비스 단위 테스트 전체 (7-1절 서비스 단위 테스트 목록 참조)
- **선행 작업**: TASK-008, TASK-009
- **구현 범위**: backend
- **예상 난이도**: 상
- **세부사항** (테스트 케이스 목록 -- TASK-007 분기 매트릭스 기반):
  - 설문 미연결 행사 신청 -- 기존 로직 보존 (SEVT-INV-07, 12)
  - 설문 OPEN + surveyAnswers 포함 -- 새 응답 저장 + 신청 성공 (분기 #1, SEVT-INV-06, 08, S1)
  - 설문 OPEN + surveyAnswers 미포함 + 기존 응답 있음 -- 기존 응답으로 신청 성공 (분기 #2, SEVT-INV-06, S4)
  - 설문 OPEN + surveyAnswers 미포함 + 기존 응답 없음 -- 실패 (분기 #3, SEVT-INV-06, F1)
  - 설문 CLOSED + surveyAnswers 포함 + 기존 응답 없음 -- 실패 (분기 #4, CLOSED에서 새 응답 저장 불가)
  - 설문 CLOSED + surveyAnswers 포함 + 기존 응답 있음 -- 기존 응답으로 신청 성공, surveyAnswers 무시 (분기 #5)
  - 설문 CLOSED + surveyAnswers 미포함 + 기존 응답 있음 -- 기존 응답으로 신청 성공 (분기 #6, SEVT-INV-10, S3)
  - 설문 CLOSED + surveyAnswers 미포함 + 기존 응답 없음 -- 실패 (분기 #7)
  - 설문 NOT_STARTED 행사 신청 거부 (분기 #8, SEVT-INV-10, F2)
  - 설문 휴지통 행사 신청 거부 (SEVT-INV-11, F3)
  - 설문 삭제 행사 신청 거부 (SEVT-INV-11)
  - 재신청 -- 설문 응답 존재 확인 (SEVT-INV-06)
  - 동일 설문 다중 행사 -- 응답 1회로 양쪽 신청 (SEVT-INV-03, 06)
  - 설문 응답 수정 후 기존 신청 유효 (SEVT-INV-09)

### 9. 통합 테스트

#### TASK-018: 설문 연동 행사 신청 통합 테스트 작성

- **작업명**: 설문-행사 통합 신청 E2E 통합 테스트 작성
- **설명**: `@SpringBootTest` non-transactional 환경에서 설문 응답 제출부터 행사 신청까지의 전체 흐름을 검증한다. 선착순/선발제 모두 테스트한다.
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-07, SEVT-INV-08, SEVT-INV-10, SEVT-INV-11, SEVT-INV-12
- **관련 테스트 케이스**: 통합 테스트 목록 전체 (7-1절)
- **선행 작업**: TASK-008, TASK-009, TASK-012
- **구현 범위**: backend
- **예상 난이도**: 상
- **세부사항** (테스트 시나리오):
  - 설문 응답 + 행사 신청 E2E (선착순) -- S1
  - 설문 응답 + 행사 신청 E2E (선발제) -- S1
  - 설문 응답 미제출 행사 신청 거부 -- F1
  - 설문 미연결 행사 신청 회귀 테스트 -- S2
  - 설문 CLOSED 후 기존 응답으로 신청 -- S3
  - 설문 휴지통 이동 후 신규 신청 차단, 기존 신청 유지 -- SEVT-INV-11
  - 통합 API 원자성 검증: 정원 초과 시 설문 응답 롤백 -- F6
  - 행사 생성 -> 설문 연결 -> 수정 -> 설문 변경 -> 신청 흐름 -- SEVT-INV-04, 05, 06

#### TASK-019: 권한 관련 통합 테스트 추가

- **작업명**: 설문 연동 행사의 RBAC 통합 테스트 작성
- **설명**: 역할별 접근 제어 매트릭스(5-2절)에 따른 권한 검증 통합 테스트를 작성한다.
- **관련 검증 기준**: SEC-SEVT-02, SEC-SEVT-03, SEC-SEVT-04, SEC-SEVT-05, SEC-SEVT-06
- **관련 테스트 케이스**: 권한 검증 체크리스트 전체 (5-3절)
- **선행 작업**: TASK-018
- **구현 범위**: backend
- **예상 난이도**: 중
- **세부사항**:
  - 준회원 설문 연결 행사 신청 시도 -> 403 (SEC-SEVT-02)
  - 일반 회원 설문 연결 행사 생성 시도 -> 403 (SEC-SEVT-03)
  - 설문 응답 없이 신청 시도 -> 400 (SEC-SEVT-04)
  - 비인가 접근 시 DB 변경 없음 확인 (SEC-SEVT-06)

### 10. 프론트엔드 연동

#### TASK-020: Orval API 클라이언트 재생성

- **작업명**: OpenAPI 스펙 변경 반영하여 프론트엔드 API 클라이언트 재생성
- **설명**: `pnpm api:generate`를 실행하여 변경된 행사 API 스펙(surveyId, surveyAnswers 추가)에 대한 TypeScript 타입과 API 클라이언트를 재생성한다.
- **관련 검증 기준**: 해당 없음 (인프라)
- **관련 테스트 케이스**: 해당 없음
- **선행 작업**: TASK-010, TASK-011
- **구현 범위**: frontend
- **예상 난이도**: 하

#### TASK-021: 행사 생성/수정 폼에 설문 연결 UI 추가

- **작업명**: 행사 생성/수정 화면에 설문 선택 기능 추가
- **설명**: 운영진이 행사 생성/수정 시 설문을 선택하여 연결할 수 있는 UI를 구현한다. 설문 목록 조회(기존 API) + 선택/해제 기능.
- **관련 검증 기준**: SEVT-INV-01, SEVT-INV-04, SEVT-INV-05
- **관련 테스트 케이스**: 해당 없음 (UI)
- **선행 작업**: TASK-020
- **구현 범위**: frontend
- **예상 난이도**: 중
- **세부사항**:
  - 설문 목록 드롭다운/모달에서 선택
  - 설문 해제 버튼 (surveyId = null)
  - SEC-SEVT-01: 설문 accessLevel과 행사 권한 불일치 시 경고 표시 (DECISION-04 A)

#### TASK-022: 행사 상세 페이지에 설문 연결 정보 표시

- **작업명**: 행사 상세 화면에 연결된 설문 정보 표시
- **설명**: 행사 상세 조회 응답의 `surveyId`를 기반으로 연결된 설문의 존재 여부와 기본 정보를 표시한다.
- **관련 검증 기준**: SEVT-INV-01
- **관련 테스트 케이스**: 해당 없음 (UI)
- **선행 작업**: TASK-020, TASK-013
- **구현 범위**: frontend
- **예상 난이도**: 하

#### TASK-023: 행사 신청 화면 -- 설문 응답 통합 폼 구현

- **작업명**: 설문 연동 행사 신청 시 설문 응답 + 신청을 하나의 화면에서 처리
- **설명**: 설문이 연결된 행사의 신청 화면에서 설문 응답 폼을 함께 표시하고, 한 번의 제출로 설문 응답과 행사 신청을 통합 API로 요청한다. 이미 설문에 응답한 사용자는 설문 폼 생략.
- **관련 검증 기준**: SEVT-INV-06, SEVT-INV-08
- **관련 테스트 케이스**: 시나리오 S1, S3, S4
- **선행 작업**: TASK-020, TASK-022
- **구현 범위**: frontend
- **예상 난이도**: 상
- **세부사항**:
  - 설문 연동 행사: 설문 질문 표시 -> 응답 입력 -> 신청 버튼 -> 통합 API 호출 (surveyAnswers 포함)
  - 이미 응답한 사용자: "이미 설문에 응답하셨습니다" 안내 + 바로 신청 (surveyAnswers 생략)
  - 설문 NOT_STARTED: "설문이 아직 시작되지 않았습니다" 안내 (SEVT-INV-10)
  - 에러 핸들링: SurveyResponseRequiredException, SurveyNotReadyException 에러 메시지 표시

---

## 작업 순서 및 의존성

### 의존성 그래프

```
TASK-001 (Flyway) ─────────────────> TASK-002 (Entity)
                                         |
                                         v
TASK-003 (예외) ──────────────> TASK-004 (createEvent) ──> TASK-005 (updateEvent)
     |                                   |                        |
     v                                   v                        v
TASK-006 (설문 검증) ──────> TASK-007 (통합 메서드) ──> TASK-008 (분기 로직)
                                   |                        |
                                   v                        v
                            TASK-009 (재신청)          TASK-013 (응답 DTO)
                                   |                        |
                                   v                        v
                            TASK-014 (로깅)           TASK-015 (도메인 테스트)
                                   |
                                   v
                    TASK-016, 017 (단위 테스트) ──> TASK-018, 019 (통합 테스트)

TASK-010 (스펙 생성/수정) ──> TASK-011 (스펙 신청) ──> TASK-012 (코드 재생성)
     |                             |
     v                             v
TASK-020 (Orval) ──> TASK-021 (생성/수정 UI) ──> TASK-022 (상세 UI) ──> TASK-023 (신청 UI)
```

### 권장 실행 순서 (4개 단계)

#### Phase 1: 기반 작업 (병렬 가능)
| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 1-1 | TASK-001 | Flyway 마이그레이션 | 독립 |
| 1-2 | TASK-003 | 예외/ErrorCode 정의 | 독립 |
| 1-3 | TASK-010 | OpenAPI 스펙 (생성/수정) | 독립 |

#### Phase 2: 핵심 백엔드 구현
| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 2-1 | TASK-002 | Event 엔티티 변경 | TASK-001 이후 |
| 2-2 | TASK-004 | createEvent 설문 연결 | TASK-002, 003 이후 |
| 2-3 | TASK-005 | updateEvent 설문 변경 | TASK-004 이후 |
| 2-4 | TASK-006 | 설문 상태 검증 | TASK-003 이후 |
| 2-5 | TASK-007 | 통합 API 핵심 메서드 | TASK-006 이후 |
| 2-6 | TASK-008 | 분기 로직 | TASK-007 이후 |
| 2-7 | TASK-009 | 재신청 설문 검증 | TASK-007 이후 |
| 2-8 | TASK-011 | OpenAPI 스펙 (신청) | TASK-010 이후 |
| 2-9 | TASK-012 | 코드 재생성 | TASK-011 이후 |
| 2-10 | TASK-013 | 응답 DTO 변경 | TASK-002 이후 |
| 2-11 | TASK-014 | 로깅 | TASK-007 이후 |

#### Phase 3: 테스트
| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 3-1 | TASK-015 | 도메인 단위 테스트 | TASK-002 이후 |
| 3-2 | TASK-016 | 서비스 단위 (생성/수정) | TASK-005 이후 |
| 3-3 | TASK-017 | 서비스 단위 (신청) | TASK-009 이후 |
| 3-4 | TASK-018 | 통합 테스트 | TASK-012 이후 |
| 3-5 | TASK-019 | 권한 통합 테스트 | TASK-018 이후 |

#### Phase 4: 프론트엔드
| 순서 | 작업 ID | 작업명 | 비고 |
|:---:|:---:|------|------|
| 4-1 | TASK-020 | Orval 재생성 | TASK-011 이후 |
| 4-2 | TASK-021 | 생성/수정 UI | TASK-020 이후 |
| 4-3 | TASK-022 | 상세 UI | TASK-020, 013 이후 |
| 4-4 | TASK-023 | 신청 통합 UI | TASK-022 이후 |

---

## 구현 시 주의사항

### 기술적 고려사항

1. **트랜잭션 경계**: `registerEventWithSurvey()`는 단일 `@Transactional` 메서드로 구현한다. 설문 응답 저장과 행사 신청이 모두 성공하거나 모두 롤백되어야 한다 (SEVT-INV-08).
2. **약한 참조 무결성**: `event_survey_id`에 FK 제약조건이 없으므로, 서비스 레벨에서 반드시 설문 존재를 검증해야 한다 (SEVT-INV-04, 11).
3. **동시성 제어**: 기존 `incrementCurrentCountIfAvailable()` 원자적 UPDATE 패턴을 그대로 사용한다. 설문 응답 저장은 동시성 이슈가 없음 (1인 1응답 unique 제약).
4. **`@Modifying` 쿼리와 영속성 컨텍스트**: 기존 `EventRegistrationService`의 `clearAutomatically=true` 패턴을 고려하여, 설문 응답 저장 후 영속성 컨텍스트가 초기화될 수 있음에 유의.
5. **설문 도메인 코드 변경 없음**: `SurveyService`, `SurveyResponseService`, `SurveyAnswerValidator` 등 설문 도메인 코드는 변경하지 않는다. 통합 API에서는 `SurveyAnswerValidator.validate()` + `SurveyResponseRepository.save()`를 직접 호출하며, `SurveyResponseService.submitResponse()`는 사용하지 않는다 (SEVT-INV-10과의 충돌 방지).

### 잠재적 위험 요소

1. **설문 응답 저장 로직 재사용**: `SurveyResponseService.submitResponse()`는 내부적으로 `Survey.isAcceptingResponses()`(= `PUBLISHED + OPEN + 비휴지통`)를 검증한다. 이는 SEVT-INV-10(`responseStatus != NOT_STARTED`만 검증)과 충돌한다 -> **해결 방안 (확정)**: `submitResponse()`를 사용하지 않고, `SurveyAnswerValidator.validate()` + `SurveyResponseRepository.save()`를 직접 호출한다. 설문 상태별 `surveyAnswers` 처리는 TASK-007의 분기 매트릭스를 따른다.
2. **기존 테스트 회귀**: `registerEvent()` 시그니처 변경 시 기존 테스트가 깨질 수 있음 -> 오버로드 방식으로 하위 호환 유지 또는 기존 테스트도 함께 수정.
3. **OpenAPI 스펙 변경 범위**: 행사 신청 엔드포인트에 request body가 새로 추가되면, 기존 body 없는 호출과의 호환성 확인 필요.

### 기존 코드와의 통합 포인트

| 기존 코드 | 변경 유형 | 변경 설명 |
|----------|:---:|------|
| `Event.java` | 수정 | `surveyId` 필드 추가, `create()`, `update()` 시그니처 변경 |
| `EventService.java` | 수정 | `SurveyRepository` 의존성 추가, `createEvent()`, `updateEvent()` 설문 검증 로직 추가 |
| `EventRegistrationService.java` | 수정 | `SurveyRepository`, `SurveyResponseRepository`, `SurveyAnswerValidator` 의존성 추가 (`SurveyResponseService`는 미사용), `registerEventWithSurvey()` 신규 (분기 매트릭스 포함), `registerEvent()` 분기, `handleReRegistration()` 수정 |
| `CreateEventRequest.java` | 수정 | `surveyId` 필드 추가 |
| `UpdateEventRequest.java` | 수정 | `surveyId` 필드 추가 |
| `EventDetailResponse.java` | 수정 | `surveyId` 필드 추가 |
| `EventListResponse.java` | 수정 | `surveyId` 필드 추가 |
| `EventCreateResponse.java` | 수정 | `surveyId` 필드 추가 |
| `EventErrorCode.java` | 수정 | `SURVEY_RESPONSE_REQUIRED`, `SURVEY_NOT_READY` 추가 |
| `openapi/paths/events.yaml` | 수정 | `surveyId`, `surveyAnswers` 스키마 추가 |
| `SurveyResponseService.java` | **변경 없음** | 통합 API에서 `submitResponse()`를 호출하지 않음 (`isAcceptingResponses()` 검증이 SEVT-INV-10과 충돌하므로). 대신 `SurveyAnswerValidator` + `SurveyResponseRepository`를 직접 사용 |
| `SurveyAnswerValidator.java` | **변경 없음** | 통합 API에서 검증 로직 재사용 |
| `SurveyRepository.java` | **변경 없음** | 기존 `findByIdAndDeletedFalse()` 재사용 |
| `SurveyResponseRepository.java` | **변경 없음** | 기존 `existsBySurveyIdAndUserId()` 재사용 |

### 확인이 필요한 사항

1. **DECISION-01 최종 확정**: 신청자 존재 시 설문 변경 허용 여부. 현재 권장안(A: 허용)으로 작성했으나, 최종 확정 필요.
2. **설문 응답 저장 위임 방식**: **확정** -- `SurveyResponseService.submitResponse()`는 사용하지 않고, `SurveyAnswerValidator.validate()` + `SurveyResponseRepository.save()`를 직접 호출한다. 근거: `submitResponse()`의 `isAcceptingResponses()` 검증이 SEVT-INV-10의 `responseStatus != NOT_STARTED` 조건과 충돌하기 때문 (상세: TASK-007 세부사항 참조).
3. **Flyway 버전 번호**: 현재 최신이 V45이므로 V46으로 작성했으나, 다른 작업이 먼저 머지되면 번호 충돌 가능. 머지 시점에 확인 필요.

---

## 완료 기준

### 검증 기준 충족 여부 체크리스트

| 불변조건 | 커버 작업 | 상태 |
|---------|-----------|:---:|
| SEVT-INV-01 (설문 연결 선택 사항) | TASK-002, 004, 008, 015 | [ ] |
| SEVT-INV-02 (행사당 설문 최대 1개) | TASK-001, 002, 015 | [ ] |
| SEVT-INV-03 (설문 재사용 가능) | TASK-001, 017 | [ ] |
| SEVT-INV-04 (설문 존재 검증) | TASK-004, 005, 016 | [ ] |
| SEVT-INV-05 (상태별 설문 변경) | TASK-002, 005, 016 | [ ] |
| SEVT-INV-06 (설문 응답 필수) | TASK-007, 008, 009, 017, 018 | [ ] |
| SEVT-INV-07 (미연결 행사 보존) | TASK-008, 017, 018 | [ ] |
| SEVT-INV-08 (통합 API 원자적 처리) | TASK-007, 017, 018 | [ ] |
| SEVT-INV-09 (응답 수정 무영향) | TASK-017 | [ ] |
| SEVT-INV-10 (신청 시 설문 상태 검증) | TASK-006, 017, 018 | [ ] |
| SEVT-INV-11 (설문 삭제/휴지통 정책) | TASK-006, 017, 018 | [ ] |
| SEVT-INV-12 (기존 행사 신청 불변조건 보존) | TASK-008, 017, 018 | [ ] |
| SEVT-INV-13 (기존 설문 불변조건 보존) | TASK-007 (설문 코드 변경 없음으로 보장) | [ ] |

### 권한 검증 충족 여부 체크리스트

| 검증 | 커버 작업 | 상태 |
|------|-----------|:---:|
| SEC-SEVT-01 (accessLevel 경고) | TASK-021 | [ ] |
| SEC-SEVT-02 (준회원 차단) | TASK-019 | [ ] |
| SEC-SEVT-03 (일반 회원 생성 차단) | TASK-019 | [ ] |
| SEC-SEVT-04 (설문 응답 없이 신청) | TASK-017, 019 | [ ] |
| SEC-SEVT-05 (accessLevel 부족) | TASK-017 | [ ] |
| SEC-SEVT-06 (비인가 부작용 없음) | TASK-019 | [ ] |

### 시나리오 커버리지 체크리스트

| 시나리오 | 커버 작업 | 상태 |
|---------|-----------|:---:|
| S1 (설문 연결 행사 기본 흐름) | TASK-018 | [ ] |
| S2 (설문 미연결 행사 기존 흐름) | TASK-018 | [ ] |
| S3 (설문 마감 후 신청) | TASK-018 | [ ] |
| S4 (동일 설문 다중 행사) | TASK-017 | [ ] |
| S5 (설문 연결 후 해제) | TASK-016 | [ ] |
| F1 (설문 미응답 신청 시도) | TASK-017, 018 | [ ] |
| F2 (설문 NOT_STARTED 신청) | TASK-017, 018 | [ ] |
| F3 (설문 휴지통 행사 신청) | TASK-017, 018 | [ ] |
| F4 (존재하지 않는 설문 연결) | TASK-016 | [ ] |
| F5 (삭제된 설문 연결) | TASK-016 | [ ] |
| F6 (정원 초과 시 응답 롤백) | TASK-018 | [ ] |
| F7 (accessLevel 부족) | TASK-017 | [ ] |
| E1-E6 (엣지 케이스) | TASK-017, 018 | [ ] |
