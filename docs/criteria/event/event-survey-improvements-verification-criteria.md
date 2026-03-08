# 행사/설문 기능 보완 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-03-08
> **Scope**: allowExternal 필드 매핑 버그 수정, 설문 응답 수 표시, 관리자 설문 응답 목록 조회 API, 설문 응답 삭제 API, 외부인 설문 응답 통계 통합, 코드 중복 제거
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 문서**:
> - [작업 계획서](../../tasks/event/event-survey-improvements-task-plan.md)
> - [행사 검증 기준서](./event-verification-criteria.md)
> - [외부인 행사 신청 검증 기준서](./external-event-registration-verification-criteria.md)
> - [설문 검증 기준서](../survey/survey-criteria-v1.md)
> - [설문 통계 검증 기준서](../survey/survey-statistics-verification-criteria.md)
> - [설문-행사 연동 검증 기준서](./survey-event-registration-verification-criteria.md)

## 목적

이 문서는 행사/설문 기능 보완 작업(6개 Phase)에서 **반드시 검증해야 하는 기준**을 명시한다. 코드 변경 후 각 기준의 통과/실패를 판정하여 기능의 정확성을 보증한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 작업에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | OpenAPI-DTO 필드 일치, 응답 삭제-행사 연동 정책, CLOSED 설문 삭제 불가 |
| 2 | 상태 모델 | 설문 응답 삭제 시 설문/행사 상태 영향, 외부인 통계 통합 시 상태 필터링 |
| 3 | 입력 도메인 분할과 경계값 | allowExternal null/true/false 동치 분할, responseCount 경계값 |
| 4 | 권한/보안 정책 | 관리자 응답 조회 RBAC, 응답 삭제 본인 제한, 설문 accessLevel |
| 5 | 관측 가능성 | 응답 삭제 로그, 관리자 조회 로그 |
| 6 | 테스트 전략 | Phase별 테스트-검증 항목 매핑 |

---

## Phase 1: allowExternal 필드 매핑 버그 수정 (TASK-001 ~ TASK-009)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-001: CreateEventRequest DTO에 allowExternal 필드 존재

> OpenAPI 스펙 `CreateEventRequest` 스키마의 `allowExternal: boolean` 필드가 Java DTO `igrus.web.event.dto.request.CreateEventRequest`에 `Boolean allowExternal` 필드로 존재한다.

- **검증 유형**: 도메인 규칙 (OpenAPI-DTO 계약 일치)
- **관련 TASK**: TASK-001
- **사전조건**: OpenAPI 스펙 `openapi/schemas/events.yaml`에 `CreateEventRequest.allowExternal`이 정의됨
- **검증 절차**:
  1. `CreateEventRequest.java` 파일을 열어 `allowExternal` 필드가 존재하는지 확인
  2. 필드 타입이 `Boolean`(nullable)인지 확인
  3. `@NotNull` 어노테이션이 **없는지** 확인 (OpenAPI 스펙에서 required가 아님)
- **기대 결과**: `Boolean allowExternal` 필드가 존재하며, 필수 검증 어노테이션이 없음
- **비고**: 현재 코드에는 해당 필드가 누락되어 있음 (버그)

#### EVTSRV-002: UpdateEventRequest DTO에 allowExternal 필드 존재

> OpenAPI 스펙 `UpdateEventRequest` 스키마의 `allowExternal: boolean` 필드가 Java DTO에 존재한다.

- **검증 유형**: 도메인 규칙 (OpenAPI-DTO 계약 일치)
- **관련 TASK**: TASK-002
- **사전조건**: OpenAPI 스펙에 `UpdateEventRequest.allowExternal`이 정의됨
- **검증 절차**:
  1. `UpdateEventRequest.java` 파일을 열어 `allowExternal` 필드가 존재하는지 확인
  2. 필드 타입이 `Boolean`(nullable)인지 확인
- **기대 결과**: `Boolean allowExternal` 필드가 존재함
- **비고**: 현재 코드에는 해당 필드가 누락되어 있음 (버그)

#### EVTSRV-003: EventDetailResponse DTO에 allowExternal 필드 존재 및 매핑

> `EventDetailResponse`에 `allowExternal` 필드가 존재하며, `from(Event, boolean, boolean)` 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다.

- **검증 유형**: 도메인 규칙 (OpenAPI-DTO 계약 일치)
- **관련 TASK**: TASK-003
- **사전조건**: `Event` 엔티티에 `getAllowExternal()` 메서드가 존재함 (현재 구현 일치)
- **검증 절차**:
  1. `EventDetailResponse.java`에 `Boolean allowExternal` 필드가 존재하는지 확인
  2. `from(Event, boolean, boolean)` 메서드에서 `event.getAllowExternal()`을 매핑하는지 확인
- **기대 결과**: 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함
- **비고**: 현재 코드에는 해당 필드가 누락되어 있음 (버그)

#### EVTSRV-004: EventCreateResponse DTO에 allowExternal 필드 존재 및 매핑

> `EventCreateResponse`에 `allowExternal` 필드가 존재하며, `from(Event)` 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다.

- **검증 유형**: 도메인 규칙 (OpenAPI-DTO 계약 일치)
- **관련 TASK**: TASK-004
- **사전조건**: `Event` 엔티티에 `getAllowExternal()` 메서드가 존재함
- **검증 절차**:
  1. `EventCreateResponse.java`에 `Boolean allowExternal` 필드가 존재하는지 확인
  2. `from(Event)` 메서드에서 `event.getAllowExternal()`을 매핑하는지 확인
- **기대 결과**: 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함

#### EVTSRV-005: EventListResponse DTO에 allowExternal 필드 존재 및 매핑

> `EventListResponse`에 `allowExternal` 필드가 존재하며, `from(Event)` 팩토리 메서드에서 `event.getAllowExternal()`을 매핑한다.

- **검증 유형**: 도메인 규칙 (OpenAPI-DTO 계약 일치)
- **관련 TASK**: TASK-005
- **사전조건**: OpenAPI 스펙에 `EventListResponse.allowExternal`이 정의됨
- **검증 절차**:
  1. `EventListResponse.java`에 `Boolean allowExternal` 필드가 존재하는지 확인
  2. `from(Event)` 메서드에서 `event.getAllowExternal()`을 매핑하는지 확인
- **기대 결과**: 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함

#### EVTSRV-006: EventService.createEvent()에서 allowExternal 전달

> `EventService.createEvent()`에서 `request.allowExternal()`을 `Event.create()` 12-param 오버로드에 전달한다. null인 경우 기본값 `false` 처리는 `Event.create()` 내부에서 수행된다.

- **검증 유형**: 도메인 규칙 (DTO-서비스-엔티티 데이터 흐름)
- **관련 TASK**: TASK-006
- **사전조건**: TASK-001 완료, `Event.create()` 12-param 오버로드 존재 (현재 구현 일치)
- **검증 절차**:
  1. `EventService.createEvent()`에서 `Event.create()` 호출 시 `request.allowExternal()`이 전달되는지 확인
  2. `Event.create()` 내부에서 `Boolean.TRUE.equals(allowExternal)`로 null-safe 처리하는지 확인 (현재 구현 일치)
- **기대 결과**: `allowExternal=null`이면 `false`로 저장, `true`이면 `true`로 저장, `false`이면 `false`로 저장

#### EVTSRV-007: EventService.updateEvent()에서 allowExternal 전달

> `EventService.updateEvent()`에서 `request.allowExternal()`을 `event.update()` 오버로드에 전달한다. null인 경우 기존 값을 유지한다.

- **검증 유형**: 도메인 규칙 (DTO-서비스-엔티티 데이터 흐름)
- **관련 TASK**: TASK-006
- **사전조건**: TASK-002 완료, `Event.update()` 10-param 오버로드 존재 (현재 구현 일치)
- **검증 절차**:
  1. `EventService.updateEvent()`에서 `event.update()` 호출 시 `request.allowExternal()` (null이면 기존 값 `event.getAllowExternal()`)이 전달되는지 확인
  2. `Event.update()` 내부에서 전달된 값이 적용되는지 확인
- **기대 결과**: `allowExternal=null`이면 기존 값 유지, `true`이면 `true`로 변경, `false`이면 `false`로 변경

### 3. 입력 도메인 분할과 경계값

#### EVTSRV-008: allowExternal 동치 분할 (생성 시)

> 행사 생성 시 `allowExternal`의 3가지 동치류를 검증한다.

- **검증 유형**: 입력 도메인 분할 (동치 분할)
- **관련 TASK**: TASK-006, TASK-007
- **사전조건**: 유효한 행사 생성 요청

| 입력값 | 기대 결과 |
|--------|----------|
| `allowExternal = null` (필드 미전송) | `event.getAllowExternal() == false` |
| `allowExternal = true` | `event.getAllowExternal() == true` |
| `allowExternal = false` | `event.getAllowExternal() == false` |

- **검증 절차**:
  1. 각 동치류에 해당하는 행사 생성 요청을 전송
  2. 생성된 행사의 `allowExternal` 값을 조회하여 기대 결과와 비교
- **기대 결과**: 위 표의 기대 결과와 일치

#### EVTSRV-009: allowExternal 동치 분할 (수정 시)

> 행사 수정 시 `allowExternal`의 동치류를 검증한다. 기존 값에 따라 4가지 조합이 존재한다.

- **검증 유형**: 입력 도메인 분할 (동치 분할)
- **관련 TASK**: TASK-006, TASK-007

| 기존 값 | 입력값 | 기대 결과 |
|---------|--------|----------|
| `false` | `null` (미전송) | `false` 유지 |
| `false` | `true` | `true`로 변경 |
| `true` | `null` (미전송) | `true` 유지 |
| `true` | `false` | `false`로 변경 |

- **검증 절차**: 각 조합에 대해 수정 요청 전송 후 조회하여 기대 결과 확인
- **기대 결과**: 위 표의 기대 결과와 일치

### 4. 컨트롤러 매핑 검증

#### EVTSRV-010: EventController에서 allowExternal 매핑

> `EventController`의 생성/수정/조회 매핑에서 Generated DTO와 내부 DTO 간 `allowExternal` 필드가 양방향으로 매핑된다.

- **검증 유형**: 도메인 규칙 (컨트롤러-Generated DTO 계약 일치)
- **관련 TASK**: TASK-007
- **사전조건**: TASK-003 ~ TASK-006 완료
- **검증 절차**:
  1. `createEvent()`: Generated `CreateEventRequest`에서 `getAllowExternal()`을 가져와 내부 DTO에 전달하는지 확인
  2. `updateEvent()`: Generated `UpdateEventRequest`에서 `getAllowExternal()`을 가져와 내부 DTO에 전달하는지 확인
  3. `mapToGetEvent200Response()`: 내부 DTO의 `allowExternal()`을 Generated 응답에 매핑하는지 확인
  4. `mapToEventListResponseInner()`: 내부 DTO의 `allowExternal()`을 Generated 응답에 매핑하는지 확인
- **기대 결과**: 4개 매핑 지점 모두 `allowExternal` 필드가 정확히 전달됨

#### EVTSRV-011: AdminEventController에서 allowExternal 매핑

> `AdminEventController`의 관리자 응답 매핑에서 `allowExternal` 필드가 매핑된다.

- **검증 유형**: 도메인 규칙 (컨트롤러-Generated DTO 계약 일치)
- **관련 TASK**: TASK-008
- **사전조건**: TASK-003, TASK-005 완료
- **검증 절차**:
  1. `mapToAdminEventDetailResponse()`: 내부 DTO의 `allowExternal()`을 Generated 관리자 응답에 매핑하는지 확인
  2. `mapToAdminEventListResponse()`: 내부 DTO의 `allowExternal()`을 Generated 관리자 목록 응답에 매핑하는지 확인
- **기대 결과**: 관리자 API 응답에도 `allowExternal` 필드가 정확히 포함됨

### 5. OpenAPI 스펙-DTO 전수 일치 검증

#### EVTSRV-012: OpenAPI 스펙과 Java DTO 간 allowExternal 필드 전수 일치

> OpenAPI 스펙에 `allowExternal`이 정의된 7개 스키마 모두 대응하는 Java DTO에 해당 필드가 존재한다.

- **검증 유형**: 도메인 규칙 (스펙-구현 계약)
- **관련 TASK**: TASK-009
- **사전조건**: TASK-001 ~ TASK-008 완료

| OpenAPI 스키마 | Java DTO | 필드 존재 여부 |
|---------------|----------|---------------|
| `CreateEventRequest` | `CreateEventRequest.java` | 확인 필요 |
| `UpdateEventRequest` | `UpdateEventRequest.java` | 확인 필요 |
| `EventDetailResponse` | `EventDetailResponse.java` | 확인 필요 |
| `EventCreateResponse` | `EventCreateResponse.java` | 확인 필요 |
| `EventListResponse` | `EventListResponse.java` | 확인 필요 |
| `AdminEventDetailResponse` | Generated 코드 + 컨트롤러 매핑 | 확인 필요 |
| `AdminEventListResponse` | Generated 코드 + 컨트롤러 매핑 | 확인 필요 |

- **검증 절차**:
  1. OpenAPI 스펙에서 `allowExternal`이 정의된 모든 스키마를 나열
  2. 각 스키마에 대응하는 Java DTO/매핑 코드에서 필드 존재 여부를 대조
  3. `cd backend && ./gradlew build`로 빌드 성공 확인
- **기대 결과**: 7개 스키마 모두 Java DTO에 `allowExternal` 필드가 존재하고, 빌드가 성공함

#### EVTSRV-013: 빌드 및 기존 테스트 통과

> Phase 1 완료 후 전체 빌드 및 기존 테스트가 통과한다.

- **검증 유형**: 테스트 전략 (회귀 테스트)
- **관련 TASK**: TASK-009
- **사전조건**: TASK-001 ~ TASK-008 완료
- **검증 절차**:
  1. `cd backend && ./gradlew build` 실행
  2. 모든 컴파일 오류 없이 빌드 성공
  3. 모든 기존 테스트 통과
- **기대 결과**: BUILD SUCCESSFUL, 테스트 실패 0건

---

## Phase 2: 설문 응답 수 표시 (TASK-010 ~ TASK-015)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-014: SurveyResponseRepository에 응답 수 카운트 메서드 존재

> `SurveyResponseRepository`에 설문별 응답 수를 반환하는 메서드가 존재한다. soft-delete된 응답은 제외한다.

- **검증 유형**: 도메인 규칙
- **관련 TASK**: TASK-010
- **사전조건**: `SurveyResponse` 엔티티에 `@SQLRestriction("deleted = false")` 또는 유사한 soft-delete 필터가 존재
- **검증 절차**:
  1. `SurveyResponseRepository`에 `countBySurveyId(Long surveyId)` 또는 동등한 카운트 메서드가 존재하는지 확인
  2. soft-delete된 응답이 카운트에서 제외되는지 확인 (메서드명에 `DeletedFalse`가 포함되거나, `@SQLRestriction`에 의해 자동 제외)
- **기대 결과**: 카운트 메서드가 존재하며 삭제된 응답을 제외함

#### EVTSRV-015: 설문 목록 응답 DTO에 responseCount 필드 존재

> 설문 목록 응답 DTO(`SurveyListResponse`)에 `int responseCount` 필드가 존재하며, `from()` 팩토리 메서드에서 매개변수로 받는다.

- **검증 유형**: 도메인 규칙 (DTO 필드 존재)
- **관련 TASK**: TASK-011
- **사전조건**: 없음
- **검증 절차**:
  1. `SurveyListResponse.java`에 `int responseCount` 또는 `Integer responseCount` 필드가 존재하는지 확인
  2. `from()` 메서드 시그니처에 `responseCount` 매개변수가 추가되었는지 확인
- **기대 결과**: 필드와 매개변수가 존재함
- **비고**: 현재 `SurveyListResponse`에는 `responseCount` 필드가 없음

#### EVTSRV-016: 설문 상세 응답 DTO에 responseCount 필드 존재

> 설문 상세 응답 DTO(`SurveyDetailResponse`)에 `int responseCount` 필드가 존재하며, `from()` 팩토리 메서드에서 매개변수로 받는다.

- **검증 유형**: 도메인 규칙 (DTO 필드 존재)
- **관련 TASK**: TASK-012
- **사전조건**: 없음
- **검증 절차**:
  1. `SurveyDetailResponse.java`에 `int responseCount` 필드가 존재하는지 확인
  2. `from()` 메서드 시그니처에 `responseCount` 매개변수가 추가되었는지 확인
- **기대 결과**: 필드와 매개변수가 존재함
- **비고**: 현재 `SurveyDetailResponse`에는 `responseCount` 필드가 없음

#### EVTSRV-017: OpenAPI 설문 스키마에 responseCount 필드 존재

> OpenAPI 설문 스키마의 목록/상세 응답에 `responseCount: integer` 필드가 정의된다.

- **검증 유형**: 도메인 규칙 (OpenAPI 스펙 일치)
- **관련 TASK**: TASK-013
- **사전조건**: 없음
- **검증 절차**:
  1. `openapi/schemas/surveys.yaml`에서 설문 목록 응답 스키마에 `responseCount` 필드가 존재하는지 확인
  2. 설문 상세 응답 스키마에 `responseCount` 필드가 존재하는지 확인
  3. 필드 타입이 `integer`인지 확인
- **기대 결과**: 목록 및 상세 응답 스키마 모두에 `responseCount: integer` 필드가 존재
- **비고**: 현재 OpenAPI 설문 스키마에는 해당 필드가 없음

### 3. 입력 도메인 분할과 경계값

#### EVTSRV-018: responseCount 경계값 검증

> 응답 수가 0, 1, N건인 경우 모두 정확히 카운트된다.

- **검증 유형**: 입력 도메인 분할 (경계값 분석)
- **관련 TASK**: TASK-014
- **사전조건**: 설문이 존재함

| 시나리오 | 기대 결과 |
|---------|----------|
| 응답 0건 | `responseCount == 0` |
| 응답 1건 | `responseCount == 1` |
| 응답 다수건 (예: 10건) | `responseCount == 10` |
| 응답 중 soft-delete된 것이 있는 경우 (3건 중 1건 삭제) | `responseCount == 2` |

- **검증 절차**: 각 시나리오에 대해 설문 목록/상세 조회 API를 호출하고 `responseCount` 값을 확인
- **기대 결과**: 위 표의 기대 결과와 일치

#### EVTSRV-019: 설문 목록 조회 시 N+1 쿼리 방지

> 설문 목록 조회 시 각 설문별 `responseCount`를 조회할 때 N+1 쿼리가 발생하지 않는다.

- **검증 유형**: 테스트 전략 (성능 검증)
- **관련 TASK**: TASK-014
- **사전조건**: 설문이 여러 건 존재함
- **검증 절차**:
  1. `SurveyService`의 목록 조회 메서드에서 배치 쿼리(`countBySurveyIdIn(List<Long>)` 등)를 사용하는지 확인
  2. 또는 별도의 N+1 방지 전략이 적용되었는지 확인
- **기대 결과**: 설문 N건 조회 시 카운트 쿼리가 1회(배치) 또는 최소한의 횟수로 실행됨
- **비고**: N+1 대안으로 `@Query`를 사용한 배치 카운트, `@Subselect`, 또는 JOIN 쿼리 가능

#### EVTSRV-020: 컨트롤러에서 responseCount 매핑

> 설문 컨트롤러의 Generated 응답 매핑에서 `responseCount` 필드가 정확히 설정된다.

- **검증 유형**: 도메인 규칙 (컨트롤러 매핑)
- **관련 TASK**: TASK-015
- **사전조건**: TASK-013, TASK-014 완료
- **검증 절차**:
  1. 설문 컨트롤러의 목록 응답 매핑에서 `responseCount`를 Generated 응답에 설정하는지 확인
  2. 상세 응답 매핑에서 `responseCount`를 Generated 응답에 설정하는지 확인
- **기대 결과**: 목록/상세 API 응답 JSON에 `responseCount` 필드가 포함됨

---

## Phase 3: 관리자 설문 응답 목록 조회 API (TASK-016 ~ TASK-020)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-021: 관리자 응답 목록 조회 API 엔드포인트 존재

> `GET /api/v1/admin/surveys/{surveyId}/responses` 엔드포인트가 존재하며, 특정 설문에 제출된 응답 목록을 반환한다.

- **검증 유형**: 도메인 규칙 (API 계약)
- **관련 TASK**: TASK-016, TASK-020
- **사전조건**: OpenAPI 스펙에 해당 엔드포인트가 정의됨
- **검증 절차**:
  1. OpenAPI 스펙(`openapi/paths/surveys.yaml`)에 `GET /api/v1/admin/surveys/{surveyId}/responses`가 정의되어 있는지 확인
  2. 응답 스키마에 `responseId`, 응답자 정보, `submittedAt`, 답변 목록이 포함되어 있는지 확인
  3. 실제 API 호출 시 200 OK 응답을 반환하는지 확인
- **기대 결과**: 엔드포인트가 존재하고, 스펙에 정의된 응답 스키마대로 데이터를 반환함

#### EVTSRV-022: 관리자 응답 목록 조회 시 설문 존재 여부 확인

> 존재하지 않는 설문 ID로 조회 시 404 Not Found를 반환한다.

- **검증 유형**: 도메인 규칙 (엣지 케이스)
- **관련 TASK**: TASK-019
- **사전조건**: 없음
- **검증 절차**:
  1. 존재하지 않는 `surveyId`로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출
  2. HTTP 404 응답 확인
- **기대 결과**: `SurveyNotFoundException` — 404 Not Found

#### EVTSRV-023: 관리자 응답 목록 조회 시 삭제된 설문 필터링

> soft-delete된 설문 ID로 조회 시 404 Not Found를 반환한다.

- **검증 유형**: 도메인 규칙 (soft-delete 일관성)
- **관련 TASK**: TASK-019
- **사전조건**: 설문이 soft-delete된 상태
- **검증 절차**:
  1. 삭제된 설문의 `surveyId`로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출
  2. HTTP 404 응답 확인
- **기대 결과**: 404 Not Found

#### EVTSRV-024: 관리자 응답 목록에 삭제된 응답 미포함

> 관리자 응답 목록 조회 시 soft-delete된 응답은 결과에 포함되지 않는다.

- **검증 유형**: 도메인 규칙 (soft-delete 일관성)
- **관련 TASK**: TASK-019
- **사전조건**: 설문에 응답 3건 존재, 그 중 1건 soft-delete 상태
- **검증 절차**:
  1. `GET /api/v1/admin/surveys/{surveyId}/responses` 호출
  2. 반환된 응답 목록의 건수 확인
- **기대 결과**: 삭제되지 않은 응답 2건만 반환됨

### 4. 권한/보안 정책

#### EVTSRV-025: 관리자 응답 목록 조회 권한 검증 — OPERATOR 이상만 접근

> `GET /api/v1/admin/surveys/{surveyId}/responses`는 OPERATOR 이상의 역할을 가진 사용자만 접근할 수 있다.

- **검증 유형**: 권한/보안 정책 (RBAC)
- **관련 TASK**: TASK-020
- **사전조건**: 설문이 존재하고 응답이 있음

| 역할 | 기대 결과 |
|------|----------|
| 비인증 사용자 | 401 Unauthorized |
| ASSOCIATE | 403 Forbidden |
| MEMBER | 403 Forbidden |
| OPERATOR | 200 OK |
| ADMIN | 200 OK |

- **검증 절차**: 각 역할의 인증 토큰으로 API 호출 후 HTTP 상태 코드 확인
- **기대 결과**: 위 표의 기대 결과와 일치
- **비고**: `@PreAuthorize("hasRole('OPERATOR')")` 또는 SecurityConfig URL 패턴으로 제어

#### EVTSRV-026: 관리자 응답 조회 시 응답 0건

> 응답이 없는 설문에 대해 관리자 조회 시 빈 목록을 반환한다.

- **검증 유형**: 입력 도메인 분할 (경계값)
- **관련 TASK**: TASK-019
- **사전조건**: 설문이 존재하나 응답이 없음
- **검증 절차**:
  1. `GET /api/v1/admin/surveys/{surveyId}/responses` 호출
  2. 반환된 목록이 빈 배열인지 확인
- **기대 결과**: 200 OK, 빈 목록 반환 (404가 아님)

### 5. OpenAPI 스펙 등록 검증

#### EVTSRV-043: 관리자 설문 응답 목록 조회 API OpenAPI 스펙 등록

> `GET /api/v1/admin/surveys/{surveyId}/responses` 엔드포인트가 OpenAPI 스펙에 정의되어 있다.

- **검증 유형**: 도메인 규칙 (스펙-구현 계약)
- **관련 TASK**: TASK-016
- **사전조건**: 없음
- **검증 절차**:
  1. `openapi/paths/surveys.yaml`에 `GET /api/v1/admin/surveys/{surveyId}/responses` 경로가 정의되어 있는지 확인
  2. 요청 파라미터(`surveyId` path parameter)가 정의되어 있는지 확인
  3. 응답 스키마(200 OK)에 응답 목록 구조가 정의되어 있는지 확인
  4. 보안 요구사항(bearerAuth)이 명시되어 있는지 확인
- **기대 결과**: 엔드포인트가 OpenAPI 스펙에 완전히 정의되어 있으며, 요청/응답 스키마와 보안 요구사항이 포함됨

---

## Phase 4: 설문 응답 삭제 API (TASK-021 ~ TASK-023)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-027: 설문 응답 삭제 API 엔드포인트 존재

> `DELETE /api/v1/surveys/{surveyId}/responses` 엔드포인트가 존재하며, 인증된 사용자의 본인 응답을 삭제한다.

- **검증 유형**: 도메인 규칙 (API 계약)
- **관련 TASK**: TASK-021, TASK-023
- **사전조건**: OpenAPI 스펙에 해당 엔드포인트가 정의됨
- **검증 절차**:
  1. OpenAPI 스펙에 `DELETE /api/v1/surveys/{surveyId}/responses`가 정의되어 있는지 확인
  2. 실제 API 호출 시 정상 동작하는지 확인
- **기대 결과**: 엔드포인트가 존재하고, 본인 응답 삭제 시 204 No Content 반환

#### EVTSRV-028: 설문 응답 삭제는 본인 응답만 가능

> 인증된 사용자는 자신의 응답만 삭제할 수 있다. 다른 사용자의 응답을 삭제하려 하면 404 Not Found를 반환한다.

- **검증 유형**: 권한/보안 정책 (소유권 검증)
- **관련 TASK**: TASK-022
- **사전조건**: 사용자 A와 사용자 B가 각각 동일 설문에 응답을 제출함
- **검증 절차**:
  1. 사용자 A의 인증 토큰으로 `DELETE /api/v1/surveys/{surveyId}/responses` 호출
  2. 사용자 A의 응답이 삭제되었는지 확인
  3. 사용자 B의 응답은 영향받지 않았는지 확인
- **기대 결과**: 사용자 A의 응답만 삭제됨
- **비고**: 서비스 레벨에서 `findBySurveyIdAndUserId()`로 본인 응답만 조회하므로, 타인 응답은 조회 자체가 불가 (404)

#### EVTSRV-029: CLOSED 상태 설문의 응답 삭제 불가 (정책 확정)

> 응답 수집이 마감(CLOSED)된 설문에서는 응답 삭제가 불가능하다.

- **검증 유형**: 도메인 규칙 (비즈니스 정책)
- **관련 TASK**: TASK-022
- **사전조건**: 설문의 `responseStatus == CLOSED`, 사용자의 응답이 존재함
- **검증 절차**:
  1. CLOSED 상태 설문에 대해 `DELETE /api/v1/surveys/{surveyId}/responses` 호출
  2. 삭제가 거부되는지 확인
- **기대 결과**: `SurveyClosedException` — 409 Conflict
- **비고**: OPEN 상태에서만 삭제 허용. NOT_STARTED 상태에서는 응답 자체가 존재할 수 없으므로 해당 없음. 설문이 이미 마감된 상태에서의 변경 시도이므로 Conflict가 적절함

#### EVTSRV-030: 설문 응답 삭제 시 연결된 행사 신청 취소 (정책 확정)

> 설문 응답 삭제 시, 해당 설문과 연결된 행사의 신청(EventRegistration)이 존재하면 함께 취소한다.

- **검증 유형**: 도메인 규칙 (교차 도메인 정책)
- **관련 TASK**: TASK-022
- **사전조건**:
  - 설문 S가 행사 E에 연결됨 (`event.surveyId == S.id`)
  - 사용자 U가 설문 S에 응답하고 행사 E에 신청함
  - 설문 `responseStatus == OPEN`
- **검증 절차**:
  1. 사용자 U의 인증 토큰으로 `DELETE /api/v1/surveys/{S.id}/responses` 호출
  2. 설문 응답이 삭제(soft-delete)되었는지 확인
  3. 행사 E에 대한 사용자 U의 신청 상태가 `CANCELED`로 변경되었는지 확인
  4. 행사 E의 `currentCount`가 1 감소했는지 확인 (승인된 신청이었던 경우)
- **기대 결과**: 설문 응답 삭제 + 행사 신청 자동 취소 + 정원 카운트 감소
- **비고**: 행사가 연결되지 않은 독립 설문의 응답 삭제 시에는 행사 신청 취소가 발생하지 않음

#### EVTSRV-031: 설문 응답 삭제 — 연결된 행사가 없는 독립 설문

> 행사와 연결되지 않은 독립 설문의 응답 삭제 시에는 행사 신청 취소 로직이 실행되지 않는다.

- **검증 유형**: 도메인 규칙 (엣지 케이스)
- **관련 TASK**: TASK-022
- **사전조건**: 설문 S가 어떤 행사에도 연결되지 않음 (`event.surveyId`에 S.id가 없음)
- **검증 절차**:
  1. 독립 설문에 대해 `DELETE /api/v1/surveys/{S.id}/responses` 호출
  2. 설문 응답만 삭제되고, 행사 관련 로직이 실행되지 않는지 확인
- **기대 결과**: 설문 응답만 soft-delete됨, 행사 도메인에 부작용 없음

#### EVTSRV-032: 설문 응답 삭제 — 응답이 존재하지 않는 경우

> 설문에 본인 응답이 없는 상태에서 삭제 요청 시 404 Not Found를 반환한다.

- **검증 유형**: 도메인 규칙 (엣지 케이스)
- **관련 TASK**: TASK-022
- **사전조건**: 사용자가 해당 설문에 응답하지 않았음
- **검증 절차**:
  1. `DELETE /api/v1/surveys/{surveyId}/responses` 호출
  2. 404 응답 확인
- **기대 결과**: `SurveyResponseNotFoundException` — 404 Not Found

### 4. 권한/보안 정책

#### EVTSRV-033: 설문 응답 삭제 권한 검증

> 설문 응답 삭제는 인증된 사용자만 가능하다.

- **검증 유형**: 권한/보안 정책 (인증)
- **관련 TASK**: TASK-023
- **사전조건**: 없음

| 역할 | 기대 결과 |
|------|----------|
| 비인증 사용자 | 401 Unauthorized |
| 인증된 사용자 (ASSOCIATE 이상) | 본인 응답 삭제 가능 |

- **검증 절차**: 비인증 상태에서 `DELETE /api/v1/surveys/{surveyId}/responses` 호출 후 401 응답 확인
- **기대 결과**: 비인증 시 401, 인증 시 정상 처리
- **비고**: `@PreAuthorize("isAuthenticated()")` 적용, `SecurityUtils.requireCurrentUser()`로 사용자 식별

### 2. 상태 모델

#### EVTSRV-034: 설문 응답 삭제 가능 상태 매트릭스

> 설문의 `responseStatus`에 따른 응답 삭제 가능 여부를 정의한다.

- **검증 유형**: 상태 모델 (상태별 동작)
- **관련 TASK**: TASK-022

| responseStatus | 삭제 가능 여부 | 사유 |
|---------------|--------------|------|
| NOT_STARTED | 해당 없음 | 응답 자체가 존재할 수 없음 |
| OPEN | 가능 | 응답 수집 중, 사용자가 응답 철회 가능 |
| CLOSED | 불가 (`SurveyClosedException` — 409 Conflict) | 마감 후 데이터 무결성 보호 |

- **검증 절차**: OPEN 상태에서 삭제 성공(204 No Content), CLOSED 상태에서 삭제 거부(409 Conflict)를 각각 확인
- **기대 결과**: 위 표의 동작과 일치

### 5. OpenAPI 스펙 등록 검증

#### EVTSRV-044: 설문 응답 삭제 API OpenAPI 스펙 등록

> `DELETE /api/v1/surveys/{surveyId}/responses` 엔드포인트가 OpenAPI 스펙에 정의되어 있다.

- **검증 유형**: 도메인 규칙 (스펙-구현 계약)
- **관련 TASK**: TASK-021
- **사전조건**: 없음
- **검증 절차**:
  1. `openapi/paths/surveys.yaml`에 `DELETE /api/v1/surveys/{surveyId}/responses` 경로가 정의되어 있는지 확인
  2. 요청 파라미터(`surveyId` path parameter)가 정의되어 있는지 확인
  3. 성공 응답이 `204 No Content`로 정의되어 있는지 확인
  4. 에러 응답(401, 404, 409)이 정의되어 있는지 확인
  5. 보안 요구사항(bearerAuth)이 명시되어 있는지 확인
- **기대 결과**: 엔드포인트가 OpenAPI 스펙에 완전히 정의되어 있으며, 204 성공 응답과 에러 응답, 보안 요구사항이 포함됨

---

## Phase 5: 외부인 설문 응답 통계 통합 (TASK-024)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-035: 설문 통계에 외부인 응답 포함

> `SurveyStatisticsService`가 `survey_responses` 테이블(회원 응답)과 `external_survey_responses` 테이블(외부인 응답)을 모두 집계하여 통합 통계를 반환한다.

- **검증 유형**: 도메인 규칙 (데이터 통합)
- **관련 TASK**: TASK-024
- **사전조건**:
  - 설문 S에 회원 응답 3건, 외부인 응답 2건이 존재
  - 설문 S가 행사 E에 연결됨 (`event.surveyId == S.id`, `event.allowExternal == true`)
- **검증 절차**:
  1. `GET /api/v1/admin/surveys/{S.id}/statistics` 호출
  2. `totalResponseCount`가 5 (3 + 2)인지 확인
  3. 질문별 통계에서 외부인 응답이 포함되어 있는지 확인
- **기대 결과**: `totalResponseCount == 5`, 질문별 통계에 회원 + 외부인 응답이 합산됨
- **비고**: 현재 `SurveyStatisticsService`는 `survey_responses`만 집계하여 외부인 응답이 누락됨

#### EVTSRV-036: 외부인 응답 JSON 파싱 정확성

> `ExternalSurveyResponse`의 JSON 문자열(`surveyResponses` 컬럼)이 정확히 파싱되어 기존 응답과 동일한 형태로 통계에 합산된다.

- **검증 유형**: 도메인 규칙 (데이터 변환 정확성)
- **관련 TASK**: TASK-024
- **사전조건**: 외부인 설문 응답이 JSON 형태로 저장됨
- **검증 절차**:
  1. 각 질문 유형(TEXT, OPTION, SCALE, GRID)에 대해 외부인 응답이 정확히 파싱되는지 확인
  2. 파싱된 결과가 회원 응답과 동일한 통계 계산에 사용되는지 확인
- **기대 결과**: 모든 질문 유형에서 외부인 응답이 정확히 집계됨

#### EVTSRV-037: 외부인 응답이 없는 설문의 통계

> 외부인 응답이 없는 설문(행사 미연결 또는 `allowExternal == false`)의 통계는 기존 동작과 동일하다.

- **검증 유형**: 도메인 규칙 (회귀 테스트)
- **관련 TASK**: TASK-024
- **사전조건**: 설문에 회원 응답만 존재
- **검증 절차**:
  1. 외부인 응답이 없는 설문의 통계를 조회
  2. 기존 동작과 동일한 결과를 반환하는지 확인
- **기대 결과**: `totalResponseCount`가 회원 응답 수와 일치, 질문별 통계가 기존과 동일

### 3. 입력 도메인 분할과 경계값

#### EVTSRV-038: 외부인 응답 통계 경계값

> 외부인 응답 수의 경계값에서 통계가 정확하다.

- **검증 유형**: 입력 도메인 분할 (경계값 분석)
- **관련 TASK**: TASK-024

| 시나리오 | 회원 응답 | 외부인 응답 | 기대 totalResponseCount |
|---------|----------|-----------|----------------------|
| 외부인만 | 0 | 5 | 5 |
| 회원만 | 3 | 0 | 3 |
| 혼합 | 3 | 2 | 5 |
| 모두 0 | 0 | 0 | 0 |

- **검증 절차**: 각 시나리오에 대해 통계 조회 후 `totalResponseCount` 확인
- **기대 결과**: 위 표의 기대 결과와 일치

#### EVTSRV-039: 외부인 응답 JSON 파싱 오류 처리

> 외부인 응답의 JSON이 잘못된 형식인 경우 적절한 오류 처리가 된다.

- **검증 유형**: 도메인 규칙 (외부 의존성 장애 정책)
- **관련 TASK**: TASK-024
- **사전조건**: `external_survey_responses`에 잘못된 JSON이 저장됨 (비정상 상태)
- **검증 절차**:
  1. 잘못된 JSON이 포함된 설문의 통계 조회 시도
  2. 적절한 에러 응답 또는 해당 응답을 건너뛰는 처리가 되는지 확인
- **기대 결과**: `SurveyStatisticsAggregationException` 또는 해당 응답을 건너뛰고 나머지만 집계 (구현 정책에 따름)
- **비고**: 구현 시 정책 결정 필요 — 오류 시 전체 실패 vs 부분 집계

---

## Phase 6: 코드 중복 제거 (TASK-025)

### 1. 도메인 규칙과 불변조건

#### EVTSRV-040: updateEventStatusAfterIncrement 공통 헬퍼 추출

> `EventRegistrationService`와 `ExternalEventRegistrationService`에 중복된 `updateEventStatusAfterIncrement()` 메서드가 단일 공통 컴포넌트로 추출된다.

- **검증 유형**: 도메인 규칙 (코드 품질)
- **관련 TASK**: TASK-025
- **사전조건**: 현재 두 서비스에 동일한 메서드가 각각 `private`으로 존재함 (현재 구현 확인 완료)
- **검증 절차**:
  1. 공통 컴포넌트(예: `EventStatusHelper`)에 `updateEventStatusAfterIncrement()` 메서드가 존재하는지 확인
  2. `EventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 주입받아 사용하는지 확인
  3. `ExternalEventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 주입받아 사용하는지 확인
- **기대 결과**: 중복 메서드가 제거되고 단일 공통 컴포넌트로 통합됨

#### EVTSRV-041: updateEventStatusAfterDecrement 공통 헬퍼 추출

> `EventRegistrationService`에 존재하는 `updateEventStatusAfterDecrement()` 메서드가 공통 컴포넌트로 추출된다.

- **검증 유형**: 도메인 규칙 (코드 품질)
- **관련 TASK**: TASK-025
- **사전조건**: 현재 `EventRegistrationService`에 `private`으로 존재함
- **검증 절차**:
  1. 공통 컴포넌트에 `updateEventStatusAfterDecrement()` 메서드가 존재하는지 확인
  2. `EventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 호출하는지 확인
- **기대 결과**: 중복 메서드가 제거되고 단일 공통 컴포넌트로 통합됨
- **비고**: `ExternalEventRegistrationService`에는 현재 `updateEventStatusAfterDecrement()`가 없으나, 향후 외부인 취소 기능 구현 시 재사용 가능

#### EVTSRV-042: 코드 중복 제거 후 기능 회귀 없음

> 코드 중복 제거 후 행사 신청/취소/승인/거절 기능이 기존과 동일하게 동작한다.

- **검증 유형**: 테스트 전략 (회귀 테스트)
- **관련 TASK**: TASK-025
- **사전조건**: TASK-025 완료
- **검증 절차**:
  1. `cd backend && ./gradlew build` 실행하여 빌드 성공 확인
  2. 기존 신청/취소/승인/거절 관련 테스트가 모두 통과하는지 확인
  3. 정원 초과 시 자동 마감(`closeRegistrationByCapacity`) 동작이 유지되는지 확인
  4. 취소 후 자리가 생겼을 때 재오픈 동작이 유지되는지 확인
- **기대 결과**: 모든 기존 테스트 통과, 기능 회귀 없음

---

## 기존 문서 영향 분석

이 작업으로 인해 기존 검증 기준서의 다음 항목에 영향이 발생한다.

### 영향 1: 설문 검증 기준서 — 설문 응답 삭제 기능 추가

**[ACTION REQUIRED]**: Phase 4 구현 후 `docs/criteria/survey/survey-criteria-v1.md`에 응답 삭제 관련 불변조건을 추가해야 한다.
- 삭제 가능 상태 (OPEN만), CLOSED 불가 정책
- 교차 도메인 영향 (행사 신청 취소)

### 영향 2: 설문 통계 검증 기준서 — 외부인 응답 통합

**[ACTION REQUIRED]**: Phase 5 구현 후 `docs/criteria/survey/survey-statistics-verification-criteria.md`에 외부인 응답 통계 통합 관련 내용을 추가해야 한다.

### 영향 3: OpenAPI 스펙 변경 시 프론트엔드 코드 재생성

**[ACTION REQUIRED]**: Phase 1, 2, 3, 4에서 OpenAPI 스펙 변경 후 프론트엔드 `pnpm api:generate`를 실행하여 TypeScript 타입을 재생성해야 한다.

---

## 검증 항목 요약표

| ID | Phase | 검증 항목 | 관련 TASK | 검증 유형 |
|-----|-------|----------|-----------|----------|
| EVTSRV-001 | 1 | CreateEventRequest DTO allowExternal 필드 | TASK-001 | 도메인 규칙 |
| EVTSRV-002 | 1 | UpdateEventRequest DTO allowExternal 필드 | TASK-002 | 도메인 규칙 |
| EVTSRV-003 | 1 | EventDetailResponse DTO allowExternal 매핑 | TASK-003 | 도메인 규칙 |
| EVTSRV-004 | 1 | EventCreateResponse DTO allowExternal 매핑 | TASK-004 | 도메인 규칙 |
| EVTSRV-005 | 1 | EventListResponse DTO allowExternal 매핑 | TASK-005 | 도메인 규칙 |
| EVTSRV-006 | 1 | EventService.createEvent() allowExternal 전달 | TASK-006 | 도메인 규칙 |
| EVTSRV-007 | 1 | EventService.updateEvent() allowExternal 전달 | TASK-006 | 도메인 규칙 |
| EVTSRV-008 | 1 | allowExternal 동치 분할 (생성) | TASK-006,007 | 입력 도메인 분할 |
| EVTSRV-009 | 1 | allowExternal 동치 분할 (수정) | TASK-006,007 | 입력 도메인 분할 |
| EVTSRV-010 | 1 | EventController allowExternal 매핑 | TASK-007 | 도메인 규칙 |
| EVTSRV-011 | 1 | AdminEventController allowExternal 매핑 | TASK-008 | 도메인 규칙 |
| EVTSRV-012 | 1 | OpenAPI-DTO allowExternal 전수 일치 (7개) | TASK-009 | 도메인 규칙 |
| EVTSRV-013 | 1 | 빌드 및 기존 테스트 통과 | TASK-009 | 테스트 전략 |
| EVTSRV-014 | 2 | SurveyResponseRepository 카운트 메서드 | TASK-010 | 도메인 규칙 |
| EVTSRV-015 | 2 | SurveyListResponse responseCount 필드 | TASK-011 | 도메인 규칙 |
| EVTSRV-016 | 2 | SurveyDetailResponse responseCount 필드 | TASK-012 | 도메인 규칙 |
| EVTSRV-017 | 2 | OpenAPI 설문 스키마 responseCount 필드 | TASK-013 | 도메인 규칙 |
| EVTSRV-018 | 2 | responseCount 경계값 (0, 1, N, soft-delete) | TASK-014 | 입력 도메인 분할 |
| EVTSRV-019 | 2 | N+1 쿼리 방지 | TASK-014 | 테스트 전략 |
| EVTSRV-020 | 2 | 컨트롤러 responseCount 매핑 | TASK-015 | 도메인 규칙 |
| EVTSRV-021 | 3 | 관리자 응답 목록 조회 API 엔드포인트 | TASK-016,020 | 도메인 규칙 |
| EVTSRV-022 | 3 | 존재하지 않는 설문 조회 → 404 | TASK-019 | 도메인 규칙 |
| EVTSRV-023 | 3 | 삭제된 설문 조회 → 404 | TASK-019 | 도메인 규칙 |
| EVTSRV-024 | 3 | 삭제된 응답 미포함 | TASK-019 | 도메인 규칙 |
| EVTSRV-025 | 3 | 관리자 RBAC (OPERATOR+) | TASK-020 | 권한/보안 정책 |
| EVTSRV-026 | 3 | 응답 0건 → 빈 목록 | TASK-019 | 입력 도메인 분할 |
| EVTSRV-043 | 3 | 관리자 응답 목록 API OpenAPI 스펙 등록 | TASK-016 | 도메인 규칙 |
| EVTSRV-027 | 4 | 설문 응답 삭제 API 엔드포인트 | TASK-021,023 | 도메인 규칙 |
| EVTSRV-028 | 4 | 본인 응답만 삭제 가능 | TASK-022 | 권한/보안 정책 |
| EVTSRV-029 | 4 | CLOSED 설문 응답 삭제 불가 | TASK-022 | 도메인 규칙 |
| EVTSRV-030 | 4 | 응답 삭제 시 행사 신청 자동 취소 | TASK-022 | 도메인 규칙 |
| EVTSRV-031 | 4 | 독립 설문 응답 삭제 (행사 부작용 없음) | TASK-022 | 도메인 규칙 |
| EVTSRV-032 | 4 | 응답 미존재 시 삭제 → 404 | TASK-022 | 도메인 규칙 |
| EVTSRV-033 | 4 | 응답 삭제 인증 필수 | TASK-023 | 권한/보안 정책 |
| EVTSRV-034 | 4 | 응답 삭제 가능 상태 매트릭스 | TASK-022 | 상태 모델 |
| EVTSRV-044 | 4 | 설문 응답 삭제 API OpenAPI 스펙 등록 | TASK-021 | 도메인 규칙 |
| EVTSRV-035 | 5 | 통계에 외부인 응답 포함 | TASK-024 | 도메인 규칙 |
| EVTSRV-036 | 5 | 외부인 JSON 파싱 정확성 | TASK-024 | 도메인 규칙 |
| EVTSRV-037 | 5 | 외부인 없는 설문 회귀 테스트 | TASK-024 | 도메인 규칙 |
| EVTSRV-038 | 5 | 외부인 통계 경계값 | TASK-024 | 입력 도메인 분할 |
| EVTSRV-039 | 5 | 외부인 JSON 파싱 오류 처리 | TASK-024 | 외부 의존성 장애 |
| EVTSRV-040 | 6 | updateEventStatusAfterIncrement 공통화 | TASK-025 | 도메인 규칙 |
| EVTSRV-041 | 6 | updateEventStatusAfterDecrement 공통화 | TASK-025 | 도메인 규칙 |
| EVTSRV-042 | 6 | 코드 중복 제거 후 회귀 없음 | TASK-025 | 테스트 전략 |
