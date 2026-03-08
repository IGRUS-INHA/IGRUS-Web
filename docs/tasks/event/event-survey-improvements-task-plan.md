# 행사/설문 기능 보완 작업 계획

## 개요

- **기능 설명**: 행사(Event)와 설문(Survey) 기능의 코드 리뷰 결과 발견된 버그, 기능 부재, 코드 품질 이슈를 보완한다. 가장 높은 우선순위는 `allowExternal` 필드가 OpenAPI 스펙에 정의되어 있지만 Java DTO/컨트롤러에서 매핑되지 않는 버그 수정이다.
- **관련 문서**
  - 외부인 행사 신청 기능 명세: [`docs/feature/event/external-event-registration.md`](../../feature/event/external-event-registration.md)
  - 설문-행사 연동 작업 계획: [`docs/feature/survey-event-registration/task-plan.md`](../../feature/survey-event-registration/task-plan.md)
  - OpenAPI 행사 스키마: [`openapi/schemas/events.yaml`](../../../openapi/schemas/events.yaml)
- **작성일**: 2026-03-08
- **범위**: 버그 수정, 설문 응답 관리 기능 보완, 코드 중복 제거
- **제외 항목**: 행사 이미지 통합(별도 문서 존재), 행사 그룹(별도 문서 존재)

---

## 작업 목록

### Phase 1: allowExternal 필드 매핑 버그 수정 (HIGH)

> **문제**: OpenAPI 스펙(`openapi/schemas/events.yaml`)에 `allowExternal: boolean` 필드가 `CreateEventRequest`, `UpdateEventRequest`, `EventDetailResponse`, `EventCreateResponse`, `EventListResponse`, 관리자 DTO 총 7곳에 정의되어 있으나, Java DTO에는 해당 필드가 없고 컨트롤러에서도 매핑하지 않는다. 도메인 엔티티(`Event.java`)는 `allowExternal` 필드를 이미 지원하며 `create()`/`update()` 오버로드도 존재하므로, DTO-컨트롤러 레이어만 수정하면 된다.

#### TASK-001: CreateEventRequest DTO에 allowExternal 필드 추가

- **작업 ID**: TASK-001
- **작업명**: `CreateEventRequest` record에 `allowExternal` 필드 추가
- **설명**: `igrus.web.event.dto.request.CreateEventRequest`에 `Boolean allowExternal` 필드를 추가한다. OpenAPI 스펙에서 필수 필드가 아니므로 `@NotNull` 검증은 불필요하며, null일 경우 서비스에서 기본값 `false` 처리한다.
- **수정 파일**: `backend/src/main/java/igrus/web/event/dto/request/CreateEventRequest.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-002: UpdateEventRequest DTO에 allowExternal 필드 추가

- **작업 ID**: TASK-002
- **작업명**: `UpdateEventRequest` record에 `allowExternal` 필드 추가
- **설명**: `igrus.web.event.dto.request.UpdateEventRequest`에 `Boolean allowExternal` 필드를 추가한다.
- **수정 파일**: `backend/src/main/java/igrus/web/event/dto/request/UpdateEventRequest.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-003: EventDetailResponse DTO에 allowExternal 필드 추가

- **작업 ID**: TASK-003
- **작업명**: `EventDetailResponse` record에 `allowExternal` 필드 추가 및 `from()` 매핑 수정
- **설명**: `EventDetailResponse`에 `Boolean allowExternal` 필드를 추가한다. `from(Event, boolean, boolean)` 정적 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다.
- **수정 파일**: `backend/src/main/java/igrus/web/event/dto/response/EventDetailResponse.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-004: EventCreateResponse DTO에 allowExternal 필드 추가

- **작업 ID**: TASK-004
- **작업명**: `EventCreateResponse` record에 `allowExternal` 필드 추가 및 `from()` 매핑 수정
- **설명**: `EventCreateResponse`에 `Boolean allowExternal` 필드를 추가한다. `from(Event)` 정적 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다.
- **수정 파일**: `backend/src/main/java/igrus/web/event/dto/response/EventCreateResponse.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-005: EventListResponse DTO에 allowExternal 필드 추가

- **작업 ID**: TASK-005
- **작업명**: `EventListResponse` record에 `allowExternal` 필드 추가 및 `from()` 매핑 수정
- **설명**: `EventListResponse`에 `Boolean allowExternal` 필드를 추가한다. `from(Event)` 정적 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다. OpenAPI 스펙의 `EventListResponse`에도 `allowExternal`이 정의되어 있는지 확인 필요 — 없으면 OpenAPI 스펙에도 추가한다.
- **수정 파일**: `backend/src/main/java/igrus/web/event/dto/response/EventListResponse.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-006: EventService에서 allowExternal 전달

- **작업 ID**: TASK-006
- **작업명**: `EventService.createEvent()` 및 `updateEvent()`에서 `allowExternal` 전달
- **설명**:
  1. `createEvent()`: `Event.create()` 호출 시 `request.allowExternal()` (null이면 `false`)을 전달하도록 수정. `Event.create()`에 `allowExternal`을 받는 12-param 오버로드가 이미 존재.
  2. `updateEvent()`: `event.update()` 호출 시 `request.allowExternal()` (null이면 기존 값 유지)을 전달하도록 수정. `Event.update()`에 `allowExternal`을 받는 오버로드가 이미 존재.
- **수정 파일**: `backend/src/main/java/igrus/web/event/service/EventService.java`
- **선행 작업**: TASK-001, TASK-002
- **예상 난이도**: 하

---

#### TASK-007: EventController에서 allowExternal 매핑

- **작업 ID**: TASK-007
- **작업명**: `EventController`의 생성/수정/조회 매핑에 `allowExternal` 추가
- **설명**:
  1. `createEvent()`: Generated `CreateEventRequest`에서 `getAllowExternal()`을 가져와 내부 `CreateEventRequest`에 전달. 응답 매핑에서 `allowExternal()` 포함.
  2. `updateEvent()`: Generated `UpdateEventRequest`에서 `getAllowExternal()`을 가져와 내부 `UpdateEventRequest`에 전달.
  3. `mapToGetEvent200Response()`: `r.allowExternal()` → `.allowExternal(r.allowExternal())` 매핑 추가.
  4. `mapToEventListResponseInner()`: `r.allowExternal()` → `.allowExternal(r.allowExternal())` 매핑 추가.
- **수정 파일**: `backend/src/main/java/igrus/web/event/controller/EventController.java`
- **선행 작업**: TASK-003, TASK-004, TASK-005, TASK-006
- **예상 난이도**: 중

---

#### TASK-008: AdminEventController에서 allowExternal 매핑

- **작업 ID**: TASK-008
- **작업명**: `AdminEventController`의 관리자 응답 매핑에 `allowExternal` 추가
- **설명**:
  1. `mapToAdminEventDetailResponse()`: `r.allowExternal()` 매핑 추가. Generated `GetAdminEvent200Response`에 `allowExternal` setter가 있는지 확인 후 매핑.
  2. `mapToAdminEventListResponse()`: `r.allowExternal()` 매핑 추가. Generated `GetAdminEventList200ResponseInner`에 `allowExternal` setter가 있는지 확인 후 매핑.
- **수정 파일**: `backend/src/main/java/igrus/web/event/controller/AdminEventController.java`
- **선행 작업**: TASK-003, TASK-005
- **예상 난이도**: 하

---

#### TASK-009: allowExternal 버그 수정 검증

- **작업 ID**: TASK-009
- **작업명**: allowExternal 매핑 전체 검증 (빌드 + 기존 테스트 통과)
- **설명**: `cd backend && ./gradlew build`로 전체 빌드 및 테스트 통과를 확인한다. Generated 코드와 Java DTO 간 `allowExternal` 필드가 정상 매핑되는지 검증한다. OpenAPI 스펙과 Java DTO의 필드 목록이 일치하는지 대조한다.
- **선행 작업**: TASK-007, TASK-008
- **예상 난이도**: 하

---

### Phase 2: 설문 응답 수 표시 (MEDIUM)

> **문제**: 설문 목록 및 상세 조회 시 해당 설문에 제출된 응답 수(responseCount)가 표시되지 않는다. 관리자가 설문 관리 화면에서 응답 현황을 빠르게 파악하기 어렵다.

#### TASK-010: SurveyResponseRepository에 응답 수 카운트 메서드 추가

- **작업 ID**: TASK-010
- **작업명**: `SurveyResponseRepository`에 `countBySurveyId(Long surveyId)` 메서드 추가
- **설명**: `SurveyResponseRepository`에 설문별 응답 수를 반환하는 쿼리 메서드를 추가한다. Spring Data JPA 네이밍 규칙으로 자동 생성 가능 (`countBySurveyId`). soft-delete 된 응답은 제외해야 할 경우 `@Query`를 사용한다.
- **수정 파일**: `backend/src/main/java/igrus/web/survey/response/repository/SurveyResponseRepository.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-011: 설문 목록 응답 DTO에 responseCount 추가

- **작업 ID**: TASK-011
- **작업명**: 설문 목록 응답 DTO에 `responseCount` 필드 추가
- **설명**: 설문 목록 조회 응답 DTO에 `int responseCount` 필드를 추가한다. 해당 DTO의 `from()` 팩토리 메서드에서 `responseCount`를 매개변수로 받도록 수정한다.
- **수정 파일**: 설문 목록 응답 DTO (파일 확인 필요)
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-012: 설문 상세 응답 DTO에 responseCount 추가

- **작업 ID**: TASK-012
- **작업명**: 설문 상세 응답 DTO에 `responseCount` 필드 추가
- **설명**: 설문 상세 조회 응답 DTO에 `int responseCount` 필드를 추가한다.
- **수정 파일**: 설문 상세 응답 DTO (파일 확인 필요)
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-013: OpenAPI 설문 스키마에 responseCount 추가

- **작업 ID**: TASK-013
- **작업명**: OpenAPI 설문 스키마에 `responseCount` 필드 추가
- **설명**: `openapi/schemas/surveys.yaml`의 설문 목록/상세 응답 스키마에 `responseCount: integer` 필드를 추가한다. Orval 코드 재생성이 필요하다.
- **수정 파일**: `openapi/schemas/surveys.yaml`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-014: SurveyService에서 responseCount 조회 및 매핑

- **작업 ID**: TASK-014
- **작업명**: `SurveyService`의 목록/상세 조회에서 `responseCount` 포함
- **설명**: `SurveyService`의 설문 목록 조회 및 상세 조회 메서드에서 `SurveyResponseRepository.countBySurveyId()`를 호출하여 응답 수를 DTO에 포함한다. 목록 조회 시 N+1 문제 방지를 위해 `countBySurveyIdIn(List<Long> surveyIds)` 등 배치 쿼리도 검토한다.
- **수정 파일**: `backend/src/main/java/igrus/web/survey/service/SurveyService.java`
- **선행 작업**: TASK-010, TASK-011, TASK-012
- **예상 난이도**: 중

---

#### TASK-015: 설문 컨트롤러에서 responseCount 매핑

- **작업 ID**: TASK-015
- **작업명**: 설문 컨트롤러의 응답 매핑에 `responseCount` 추가
- **설명**: 설문 컨트롤러의 Generated 응답 객체 매핑에서 `responseCount` 필드를 설정한다.
- **수정 파일**: 설문 컨트롤러 (파일 확인 필요)
- **선행 작업**: TASK-013, TASK-014
- **예상 난이도**: 하

---

### Phase 3: 설문 관리자 응답 목록 조회 API (MEDIUM)

> **문제**: 관리자가 특정 설문에 제출된 응답 목록을 조회하는 API가 없다. 현재 `SurveyResponseService`는 `getMyResponse()` (본인 응답 조회)만 제공한다.

#### TASK-016: 관리자 응답 목록 조회 OpenAPI 스펙 추가

- **작업 ID**: TASK-016
- **작업명**: 관리자 설문 응답 목록 조회 API 엔드포인트 OpenAPI 스펙 작성
- **설명**: `GET /api/v1/admin/surveys/{surveyId}/responses` 엔드포인트를 OpenAPI 스펙에 추가한다. 응답 스키마에는 `responseId`, `userId`, `userName`, `submittedAt`, `answers` 목록 등을 포함한다. 페이지네이션을 고려하되, 초기 구현은 전체 목록 반환으로 시작할 수 있다.
- **수정 파일**: `openapi/paths/surveys.yaml`, `openapi/schemas/surveys.yaml`
- **선행 작업**: 없음
- **예상 난이도**: 중

---

#### TASK-017: 관리자 응답 목록 조회 응답 DTO 작성

- **작업 ID**: TASK-017
- **작업명**: 관리자 설문 응답 목록 조회 응답 DTO 작성
- **설명**: `igrus.web.survey.response.dto.response` 패키지에 관리자 응답 목록 조회용 DTO를 작성한다. 각 응답 항목에는 응답 ID, 응답자 정보, 제출 시각, 답변 요약 정보 등을 포함한다.
- **수정 파일**: 신규 파일 생성
- **선행 작업**: TASK-016
- **예상 난이도**: 하

---

#### TASK-018: SurveyResponseRepository에 설문별 응답 목록 조회 메서드 추가

- **작업 ID**: TASK-018
- **작업명**: `SurveyResponseRepository`에 설문별 응답 목록 조회 메서드 추가
- **설명**: `findAllBySurveyId(Long surveyId)` 또는 페이지네이션 지원 `findAllBySurveyId(Long surveyId, Pageable pageable)` 메서드를 추가한다. 필요 시 응답자 정보를 함께 fetch join한다.
- **수정 파일**: `backend/src/main/java/igrus/web/survey/response/repository/SurveyResponseRepository.java`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-019: SurveyResponseService에 관리자 응답 목록 조회 로직 추가

- **작업 ID**: TASK-019
- **작업명**: `SurveyResponseService`에 관리자 응답 목록 조회 메서드 추가
- **설명**: `getResponsesBySurveyId(Long surveyId)` 메서드를 추가한다. 설문 존재 여부 확인 → 응답 목록 조회 → DTO 변환 순서로 처리한다. 권한 검증은 컨트롤러 레벨에서 `@PreAuthorize`로 처리한다.
- **수정 파일**: `backend/src/main/java/igrus/web/survey/response/service/SurveyResponseService.java`
- **선행 작업**: TASK-017, TASK-018
- **예상 난이도**: 중

---

#### TASK-020: 관리자 설문 응답 목록 조회 컨트롤러 구현

- **작업 ID**: TASK-020
- **작업명**: 관리자 설문 응답 목록 조회 API 컨트롤러 구현
- **설명**: 기존 설문 관리자 컨트롤러(또는 신규 생성)에 `GET /api/v1/admin/surveys/{surveyId}/responses` 엔드포인트를 구현한다. `@PreAuthorize("hasRole('OPERATOR')")` 등 권한 제어를 추가한다.
- **수정 파일**: 설문 관리자 컨트롤러 (파일 확인 필요, 신규 생성 가능)
- **선행 작업**: TASK-016, TASK-019
- **예상 난이도**: 중

---

### Phase 4: 설문 응답 삭제 API (LOW)

> **문제**: 사용자가 제출한 설문 응답을 삭제하는 API가 없다. 응답 제출과 수정은 가능하나 삭제는 불가능하다.

#### TASK-021: 설문 응답 삭제 OpenAPI 스펙 추가

- **작업 ID**: TASK-021
- **작업명**: 설문 응답 삭제 API 엔드포인트 OpenAPI 스펙 작성
- **설명**: `DELETE /api/v1/surveys/{surveyId}/responses` 엔드포인트를 OpenAPI 스펙에 추가한다. 본인의 응답만 삭제 가능하다. 응답 수집이 마감(`CLOSED`)된 설문에서도 삭제가 가능한지 정책 결정이 필요하다.
- **수정 파일**: `openapi/paths/surveys.yaml`
- **선행 작업**: 없음
- **예상 난이도**: 하

---

#### TASK-022: SurveyResponseService에 응답 삭제 로직 추가

- **작업 ID**: TASK-022
- **작업명**: `SurveyResponseService`에 응답 삭제 메서드 추가
- **설명**: `deleteMyResponse(Long surveyId, Long userId)` 메서드를 추가한다. 본인 응답 확인 → soft delete 또는 hard delete 처리. 행사 연동 설문의 경우 `EventRegistration`에 미치는 영향 검토 필요 (설문 응답 삭제 시 행사 신청도 취소해야 하는지 정책 결정).
- **수정 파일**: `backend/src/main/java/igrus/web/survey/response/service/SurveyResponseService.java`
- **선행 작업**: TASK-021
- **예상 난이도**: 중

---

#### TASK-023: 설문 응답 삭제 컨트롤러 구현

- **작업 ID**: TASK-023
- **작업명**: 설문 응답 삭제 API 컨트롤러 구현
- **설명**: 설문 응답 컨트롤러에 `DELETE /api/v1/surveys/{surveyId}/responses` 엔드포인트를 구현한다. `@PreAuthorize("isAuthenticated()")` 적용, `SecurityUtils.requireCurrentUser()`로 사용자 식별.
- **수정 파일**: 설문 응답 컨트롤러 (파일 확인 필요)
- **선행 작업**: TASK-021, TASK-022
- **예상 난이도**: 하

---

### Phase 5: 외부인 설문 응답 통계 통합 (LOW)

> **문제**: `SurveyStatisticsService`가 `survey_responses` 테이블만 집계하므로, 외부인이 `external_survey_responses` 테이블에 저장한 응답은 통계에 포함되지 않는다.

#### TASK-024: SurveyStatisticsService에 외부인 응답 통계 통합

- **작업 ID**: TASK-024
- **작업명**: 설문 통계에 외부인 설문 응답(`ExternalSurveyResponse`) 포함
- **설명**: `SurveyStatisticsService`의 통계 집계 로직을 수정하여 `ExternalSurveyResponse` 테이블의 JSON 응답도 파싱하여 통계에 합산한다. `ExternalSurveyResponse`는 JSON 문자열로 저장되므로 역직렬화 후 기존 응답과 동일한 형태로 변환하여 집계한다. 성능 영향 검토 필요.
- **수정 파일**:
  - `backend/src/main/java/igrus/web/survey/statistics/service/SurveyStatisticsService.java`
  - `backend/src/main/java/igrus/web/event/repository/ExternalSurveyResponseRepository.java` (조회 메서드 추가)
- **선행 작업**: 없음
- **예상 난이도**: 상

---

### Phase 6: 코드 중복 제거 (LOW)

> **문제**: `EventRegistrationService`와 `ExternalEventRegistrationService` 양쪽에 `updateEventStatusAfterIncrement()` / `updateEventStatusAfterDecrement()` 메서드가 중복 존재한다.

#### TASK-025: updateEventStatusAfterIncrement/Decrement 공통 헬퍼 추출

- **작업 ID**: TASK-025
- **작업명**: 행사 상태 갱신 헬퍼를 공통 서비스로 추출
- **설명**: `EventRegistrationService`와 `ExternalEventRegistrationService`에 중복된 `updateEventStatusAfterIncrement(Long eventId)` 및 `updateEventStatusAfterDecrement(Long eventId)` 메서드를 하나의 공통 서비스/유틸리티로 추출한다. 방안:
  1. `EventStatusHelper` 같은 컴포넌트를 생성하여 두 서비스에서 주입받아 사용
  2. 또는 `EventService`에 해당 메서드를 배치하고 두 서비스에서 호출
  - 기존 두 서비스의 중복 메서드를 제거하고 공통 컴포넌트로 대체한다.
- **수정 파일**:
  - 신규: `backend/src/main/java/igrus/web/event/service/EventStatusHelper.java` (또는 기존 서비스에 배치)
  - `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` (중복 제거)
  - `backend/src/main/java/igrus/web/event/service/ExternalEventRegistrationService.java` (중복 제거)
- **선행 작업**: 없음
- **예상 난이도**: 중

---

## 의존성 그래프

```
Phase 1 (allowExternal 버그 수정):
  TASK-001 ──┐
  TASK-002 ──┤
             ├→ TASK-006 ──┐
  TASK-003 ──┤             ├→ TASK-007 ──┐
  TASK-004 ──┤             │             ├→ TASK-009
  TASK-005 ──┴─────────────┴→ TASK-008 ──┘

Phase 2 (설문 응답 수):
  TASK-010 ──┐
  TASK-011 ──┤
  TASK-012 ──┴→ TASK-014 ──┐
  TASK-013 ────────────────┴→ TASK-015

Phase 3 (관리자 응답 목록):
  TASK-016 ──┬→ TASK-017 ──┐
             │             ├→ TASK-019 ──┐
  TASK-018 ──┘             │            ├→ TASK-020
             ──────────────┘            │
  TASK-016 ─────────────────────────────┘

Phase 4 (응답 삭제):
  TASK-021 ──→ TASK-022 ──→ TASK-023

Phase 5 (외부인 통계): TASK-024 (독립)

Phase 6 (코드 중복): TASK-025 (독립)
```

## 우선순위 및 구현 순서

| 순서 | Phase | 우선순위 | 작업 범위 | 비고 |
|------|-------|----------|-----------|------|
| 1 | Phase 1 | HIGH | TASK-001 ~ TASK-009 | **버그 수정** — allowExternal API 매핑 누락 |
| 2 | Phase 6 | LOW | TASK-025 | 코드 품질 — 다른 작업과 독립적 |
| 3 | Phase 2 | MEDIUM | TASK-010 ~ TASK-015 | 기능 보완 — 설문 응답 수 표시 |
| 4 | Phase 3 | MEDIUM | TASK-016 ~ TASK-020 | 신규 API — 관리자 응답 목록 |
| 5 | Phase 4 | LOW | TASK-021 ~ TASK-023 | 신규 API — 응답 삭제 |
| 6 | Phase 5 | LOW | TASK-024 | 기능 보완 — 통계 통합 |

## 완료 기준

1. **Phase 1 완료 기준**: `cd backend && ./gradlew build` 성공, OpenAPI 스펙의 `allowExternal` 필드와 Java DTO 필드가 1:1 매핑됨
2. **Phase 2 완료 기준**: 설문 목록/상세 API 응답에 `responseCount` 포함, OpenAPI 스펙과 일치
3. **Phase 3 완료 기준**: `GET /api/v1/admin/surveys/{surveyId}/responses` API 동작, 권한 검증 포함
4. **Phase 4 완료 기준**: `DELETE /api/v1/surveys/{surveyId}/responses` API 동작, 본인 응답만 삭제 가능
5. **Phase 5 완료 기준**: 설문 통계 API가 회원 응답 + 외부인 응답 합산 결과 반환
6. **Phase 6 완료 기준**: 중복 메서드 제거, 빌드 및 기존 테스트 통과

## 정책 결정 필요 사항

| 항목 | 설명 | 결정 필요 시점 |
|------|------|---------------|
| 응답 삭제 시 행사 신청 연동 | 설문 응답 삭제 시 연결된 행사 신청도 취소해야 하는지 | Phase 4 착수 전 |
| 마감 후 응답 삭제 | 응답 수집 마감(CLOSED) 상태에서 기존 응답 삭제 허용 여부 | Phase 4 착수 전 |
| 외부인 통계 성능 | ExternalSurveyResponse JSON 파싱 성능 영향, 캐싱 필요 여부 | Phase 5 착수 전 |
