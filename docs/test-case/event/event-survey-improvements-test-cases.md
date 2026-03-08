# 행사/설문 기능 보완 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-03-08 |
| 검증 기준 문서 | `docs/criteria/event/event-survey-improvements-verification-criteria.md` |
| 작업 계획 문서 | `docs/tasks/event/event-survey-improvements-task-plan.md` |
| 대상 기능 | allowExternal 필드 매핑 버그 수정, 설문 응답 수 표시, 관리자 설문 응답 목록 조회 API, 설문 응답 삭제 API, 외부인 설문 응답 통계 통합, 코드 중복 제거 |
| 테스트 케이스 수 | 62개 |

---

## Phase 1: allowExternal 필드 매핑 버그 수정

### DTO 필드 존재 검증

#### TC-EVTSRV-001: CreateEventRequest DTO에 allowExternal 필드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-001 |
| **사전 조건** | OpenAPI 스펙 `openapi/schemas/events.yaml`에 `CreateEventRequest.allowExternal`이 정의된 상태 |
| **테스트 절차** | 1. `CreateEventRequest.java` 파일을 열어 `allowExternal` 필드 존재 여부 확인 <br> 2. 필드 타입이 `Boolean`(nullable)인지 확인 <br> 3. `@NotNull` 어노테이션이 없는지 확인 (OpenAPI 스펙에서 required가 아님) |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | `Boolean allowExternal` 필드가 존재하며, 필수 검증 어노테이션(`@NotNull`)이 없음 |
| **비고** | 현재 코드에는 해당 필드가 누락되어 있는 버그 |

#### TC-EVTSRV-002: UpdateEventRequest DTO에 allowExternal 필드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-002 |
| **사전 조건** | OpenAPI 스펙에 `UpdateEventRequest.allowExternal`이 정의된 상태 |
| **테스트 절차** | 1. `UpdateEventRequest.java` 파일을 열어 `allowExternal` 필드 존재 여부 확인 <br> 2. 필드 타입이 `Boolean`(nullable)인지 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | `Boolean allowExternal` 필드가 존재함 |
| **비고** | 현재 코드에는 해당 필드가 누락되어 있는 버그 |

#### TC-EVTSRV-003: EventDetailResponse DTO에 allowExternal 필드 존재 및 매핑 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-003 |
| **사전 조건** | `Event` 엔티티에 `getAllowExternal()` 메서드가 존재 |
| **테스트 절차** | 1. `EventDetailResponse.java`에 `Boolean allowExternal` 필드 존재 확인 <br> 2. `from(Event, boolean, boolean)` 메서드에서 `event.getAllowExternal()` 매핑 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함 |
| **비고** | - |

#### TC-EVTSRV-004: EventCreateResponse DTO에 allowExternal 필드 존재 및 매핑 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-004 |
| **사전 조건** | `Event` 엔티티에 `getAllowExternal()` 메서드가 존재 |
| **테스트 절차** | 1. `EventCreateResponse.java`에 `Boolean allowExternal` 필드 존재 확인 <br> 2. `from(Event)` 메서드에서 `event.getAllowExternal()` 매핑 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함 |
| **비고** | - |

#### TC-EVTSRV-005: EventListResponse DTO에 allowExternal 필드 존재 및 매핑 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-005 |
| **사전 조건** | OpenAPI 스펙에 `EventListResponse.allowExternal`이 정의된 상태 |
| **테스트 절차** | 1. `EventListResponse.java`에 `Boolean allowExternal` 필드 존재 확인 <br> 2. `from(Event)` 메서드에서 `event.getAllowExternal()` 매핑 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 필드가 존재하고, `from()` 메서드에서 엔티티 값을 정확히 매핑함 |
| **비고** | - |

### 서비스 레이어 데이터 흐름 검증

#### TC-EVTSRV-006: EventService.createEvent()에서 allowExternal 전달 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-006 |
| **사전 조건** | OPERATOR 이상 권한으로 로그인한 상태, 유효한 액세스 토큰 보유 |
| **테스트 절차** | 1. `allowExternal = true`를 포함한 행사 생성 요청 전송 <br> 2. 생성된 행사 상세 조회 <br> 3. 응답의 `allowExternal` 값이 `true`인지 확인 |
| **입력 데이터** | `POST /api/v1/events` — `{ ..., "allowExternal": true }` |
| **기대 결과** | 생성된 행사의 `allowExternal == true` |
| **비고** | `Event.create()` 12-param 오버로드에서 `Boolean.TRUE.equals(allowExternal)`로 null-safe 처리 |

#### TC-EVTSRV-007: EventService.updateEvent()에서 allowExternal 전달 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-007 |
| **사전 조건** | `allowExternal = false`인 행사가 존재, OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. `allowExternal = true`를 포함한 행사 수정 요청 전송 <br> 2. 수정된 행사 상세 조회 <br> 3. 응답의 `allowExternal` 값이 `true`로 변경되었는지 확인 |
| **입력 데이터** | `PUT /api/v1/events/{eventId}` — `{ ..., "allowExternal": true }` |
| **기대 결과** | 행사의 `allowExternal`이 `false` → `true`로 변경됨 |
| **비고** | null 전달 시 기존 값 유지는 TC-EVTSRV-011~014에서 별도 검증 |

### allowExternal 동치 분할 (생성)

#### TC-EVTSRV-008: 행사 생성 시 allowExternal = null (미전송) → false 저장

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-008 |
| **사전 조건** | OPERATOR 이상 권한으로 로그인한 상태 |
| **테스트 절차** | 1. `allowExternal` 필드를 포함하지 않은 행사 생성 요청 전송 <br> 2. 생성된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `POST /api/v1/events` — `allowExternal` 필드 미포함 |
| **기대 결과** | `event.getAllowExternal() == false` |
| **비고** | null-safe 기본값 처리 검증 |

#### TC-EVTSRV-009: 행사 생성 시 allowExternal = true → true 저장

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-008 |
| **사전 조건** | OPERATOR 이상 권한으로 로그인한 상태 |
| **테스트 절차** | 1. `allowExternal = true`를 포함한 행사 생성 요청 전송 <br> 2. 생성된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `POST /api/v1/events` — `{ ..., "allowExternal": true }` |
| **기대 결과** | `event.getAllowExternal() == true` |
| **비고** | - |

#### TC-EVTSRV-010: 행사 생성 시 allowExternal = false → false 저장

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-008 |
| **사전 조건** | OPERATOR 이상 권한으로 로그인한 상태 |
| **테스트 절차** | 1. `allowExternal = false`를 포함한 행사 생성 요청 전송 <br> 2. 생성된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `POST /api/v1/events` — `{ ..., "allowExternal": false }` |
| **기대 결과** | `event.getAllowExternal() == false` |
| **비고** | - |

### allowExternal 동치 분할 (수정)

#### TC-EVTSRV-011: 행사 수정 시 기존 false + 입력 null → false 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-009 |
| **사전 조건** | `allowExternal = false`인 행사가 존재 |
| **테스트 절차** | 1. `allowExternal` 필드를 포함하지 않은 행사 수정 요청 전송 <br> 2. 수정된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `PUT /api/v1/events/{eventId}` — `allowExternal` 필드 미포함 |
| **기대 결과** | `allowExternal == false` (기존 값 유지) |
| **비고** | null 입력 시 기존 값 유지 정책 검증 |

#### TC-EVTSRV-012: 행사 수정 시 기존 false + 입력 true → true 변경

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-009 |
| **사전 조건** | `allowExternal = false`인 행사가 존재 |
| **테스트 절차** | 1. `allowExternal = true`를 포함한 행사 수정 요청 전송 <br> 2. 수정된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `PUT /api/v1/events/{eventId}` — `{ ..., "allowExternal": true }` |
| **기대 결과** | `allowExternal == true` (변경됨) |
| **비고** | - |

#### TC-EVTSRV-013: 행사 수정 시 기존 true + 입력 null → true 유지

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-009 |
| **사전 조건** | `allowExternal = true`인 행사가 존재 |
| **테스트 절차** | 1. `allowExternal` 필드를 포함하지 않은 행사 수정 요청 전송 <br> 2. 수정된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `PUT /api/v1/events/{eventId}` — `allowExternal` 필드 미포함 |
| **기대 결과** | `allowExternal == true` (기존 값 유지) |
| **비고** | - |

#### TC-EVTSRV-014: 행사 수정 시 기존 true + 입력 false → false 변경

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-009 |
| **사전 조건** | `allowExternal = true`인 행사가 존재 |
| **테스트 절차** | 1. `allowExternal = false`를 포함한 행사 수정 요청 전송 <br> 2. 수정된 행사 상세 조회 <br> 3. `allowExternal` 값 확인 |
| **입력 데이터** | `PUT /api/v1/events/{eventId}` — `{ ..., "allowExternal": false }` |
| **기대 결과** | `allowExternal == false` (변경됨) |
| **비고** | - |

### 컨트롤러 매핑 검증

#### TC-EVTSRV-015: EventController 생성/수정/조회에서 allowExternal 양방향 매핑

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-010 |
| **사전 조건** | TASK-003 ~ TASK-006 완료 상태 |
| **테스트 절차** | 1. `createEvent()`: Generated `CreateEventRequest`에서 `getAllowExternal()`을 가져와 내부 DTO에 전달하는지 코드 확인 <br> 2. `updateEvent()`: Generated `UpdateEventRequest`에서 `getAllowExternal()`을 가져와 내부 DTO에 전달하는지 코드 확인 <br> 3. `mapToGetEvent200Response()`: 내부 DTO의 `allowExternal()`을 Generated 응답에 매핑하는지 코드 확인 <br> 4. `mapToEventListResponseInner()`: 내부 DTO의 `allowExternal()`을 Generated 응답에 매핑하는지 코드 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 4개 매핑 지점 모두 `allowExternal` 필드가 정확히 전달됨 |
| **비고** | - |

#### TC-EVTSRV-016: AdminEventController 관리자 응답에서 allowExternal 매핑

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-011 |
| **사전 조건** | TASK-003, TASK-005 완료 상태 |
| **테스트 절차** | 1. `mapToAdminEventDetailResponse()`: 내부 DTO의 `allowExternal()` 매핑 확인 <br> 2. `mapToAdminEventListResponse()`: 내부 DTO의 `allowExternal()` 매핑 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 관리자 API 응답에도 `allowExternal` 필드가 정확히 포함됨 |
| **비고** | - |

### OpenAPI-DTO 전수 일치 검증

#### TC-EVTSRV-017: OpenAPI 스펙 7개 스키마와 Java DTO 간 allowExternal 필드 전수 일치

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-012 |
| **사전 조건** | TASK-001 ~ TASK-008 완료 상태 |
| **테스트 절차** | 1. OpenAPI 스펙에서 `allowExternal`이 정의된 7개 스키마 나열 <br> 2. 각 스키마에 대응하는 Java DTO/매핑 코드에서 필드 존재 여부 대조: CreateEventRequest, UpdateEventRequest, EventDetailResponse, EventCreateResponse, EventListResponse, AdminEventDetailResponse(Generated+매핑), AdminEventListResponse(Generated+매핑) <br> 3. `cd backend && ./gradlew build`로 빌드 성공 확인 |
| **입력 데이터** | 없음 |
| **기대 결과** | 7개 스키마 모두 Java DTO에 `allowExternal` 필드가 존재하고, 빌드가 성공함 |
| **비고** | - |

### 회귀 테스트

#### TC-EVTSRV-018: Phase 1 완료 후 빌드 및 기존 테스트 통과

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-013 |
| **사전 조건** | TASK-001 ~ TASK-008 완료 상태 |
| **테스트 절차** | 1. `cd backend && ./gradlew build` 실행 <br> 2. 모든 컴파일 오류 없이 빌드 성공 확인 <br> 3. 모든 기존 테스트 통과 확인 |
| **입력 데이터** | 없음 |
| **기대 결과** | BUILD SUCCESSFUL, 테스트 실패 0건 |
| **비고** | - |

---

## Phase 2: 설문 응답 수 표시

### Repository 메서드 검증

#### TC-EVTSRV-019: SurveyResponseRepository에 응답 수 카운트 메서드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-014 |
| **사전 조건** | `SurveyResponse` 엔티티에 soft-delete 필터(`@SQLRestriction("deleted = false")` 등)가 존재 |
| **테스트 절차** | 1. `SurveyResponseRepository`에 `countBySurveyId(Long surveyId)` 또는 동등한 카운트 메서드 존재 확인 <br> 2. soft-delete된 응답이 카운트에서 제외되는지 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 카운트 메서드가 존재하며 삭제된 응답을 제외함 |
| **비고** | `@SQLRestriction`에 의한 자동 제외 또는 메서드명에 `DeletedFalse` 포함 |

### DTO 필드 존재 검증

#### TC-EVTSRV-020: SurveyListResponse에 responseCount 필드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-015 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `SurveyListResponse.java`에 `int responseCount` 또는 `Integer responseCount` 필드 존재 확인 <br> 2. `from()` 메서드 시그니처에 `responseCount` 매개변수 추가 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 필드와 매개변수가 존재함 |
| **비고** | 현재 `SurveyListResponse`에는 `responseCount` 필드가 없음 |

#### TC-EVTSRV-021: SurveyDetailResponse에 responseCount 필드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-016 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `SurveyDetailResponse.java`에 `int responseCount` 필드 존재 확인 <br> 2. `from()` 메서드 시그니처에 `responseCount` 매개변수 추가 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 필드와 매개변수가 존재함 |
| **비고** | 현재 `SurveyDetailResponse`에는 `responseCount` 필드가 없음 |

### OpenAPI 스펙 검증

#### TC-EVTSRV-022: OpenAPI 설문 스키마에 responseCount 필드 존재 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-017 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `openapi/schemas/surveys.yaml`에서 설문 목록 응답 스키마에 `responseCount` 필드 존재 확인 <br> 2. 설문 상세 응답 스키마에 `responseCount` 필드 존재 확인 <br> 3. 필드 타입이 `integer`인지 확인 |
| **입력 데이터** | 없음 (스펙 리뷰) |
| **기대 결과** | 목록 및 상세 응답 스키마 모두에 `responseCount: integer` 필드가 존재 |
| **비고** | - |

### responseCount 경계값 검증

#### TC-EVTSRV-023: 응답 0건인 설문의 responseCount 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-018 |
| **사전 조건** | 설문이 존재하나 응답이 0건인 상태 |
| **테스트 절차** | 1. 설문 목록 또는 상세 조회 API 호출 <br> 2. `responseCount` 값 확인 |
| **입력 데이터** | `GET /api/v1/surveys/{surveyId}` 또는 `GET /api/v1/surveys` |
| **기대 결과** | `responseCount == 0` |
| **비고** | 최소 경계값 |

#### TC-EVTSRV-024: 응답 1건인 설문의 responseCount 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-018 |
| **사전 조건** | 설문에 응답이 정확히 1건 존재 |
| **테스트 절차** | 1. 설문 상세 조회 API 호출 <br> 2. `responseCount` 값 확인 |
| **입력 데이터** | `GET /api/v1/surveys/{surveyId}` |
| **기대 결과** | `responseCount == 1` |
| **비고** | 최소 양의 경계값 |

#### TC-EVTSRV-025: 응답 다수건(10건)인 설문의 responseCount 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-018 |
| **사전 조건** | 설문에 응답이 10건 존재 |
| **테스트 절차** | 1. 설문 상세 조회 API 호출 <br> 2. `responseCount` 값 확인 |
| **입력 데이터** | `GET /api/v1/surveys/{surveyId}` |
| **기대 결과** | `responseCount == 10` |
| **비고** | - |

#### TC-EVTSRV-026: soft-delete된 응답이 있는 설문의 responseCount 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-018 |
| **사전 조건** | 설문에 응답 3건 존재, 그 중 1건이 soft-delete된 상태 |
| **테스트 절차** | 1. 설문 상세 조회 API 호출 <br> 2. `responseCount` 값 확인 |
| **입력 데이터** | `GET /api/v1/surveys/{surveyId}` |
| **기대 결과** | `responseCount == 2` (삭제된 1건 제외) |
| **비고** | soft-delete 필터링 정확성 검증 |

### 성능 검증

#### TC-EVTSRV-027: 설문 목록 조회 시 N+1 쿼리 방지 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-019 |
| **사전 조건** | 설문이 여러 건 존재 (최소 5건 이상) |
| **테스트 절차** | 1. `SurveyService`의 목록 조회 메서드에서 배치 쿼리(`countBySurveyIdIn(List<Long>)` 등) 사용 여부 코드 확인 <br> 2. 또는 SQL 로그를 활성화하여 설문 N건 조회 시 카운트 쿼리 실행 횟수 확인 |
| **입력 데이터** | `GET /api/v1/surveys` (목록 조회) |
| **기대 결과** | 설문 N건 조회 시 카운트 쿼리가 1회(배치) 또는 최소한의 횟수로 실행됨 |
| **비고** | N+1 대안: `@Query` 배치 카운트, `@Subselect`, JOIN 쿼리 |

### 컨트롤러 매핑 검증

#### TC-EVTSRV-028: 설문 컨트롤러에서 responseCount 매핑 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-020 |
| **사전 조건** | TASK-013, TASK-014 완료 상태 |
| **테스트 절차** | 1. 설문 컨트롤러의 목록 응답 매핑에서 `responseCount`를 Generated 응답에 설정하는지 코드 확인 <br> 2. 상세 응답 매핑에서 `responseCount`를 Generated 응답에 설정하는지 코드 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 목록/상세 API 응답 JSON에 `responseCount` 필드가 포함됨 |
| **비고** | - |

---

## Phase 3: 관리자 설문 응답 목록 조회 API

### OpenAPI 스펙 검증

#### TC-EVTSRV-029: 관리자 응답 목록 조회 API OpenAPI 스펙 등록 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-043 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `openapi/paths/surveys.yaml`에 `GET /api/v1/admin/surveys/{surveyId}/responses` 경로 존재 확인 <br> 2. 요청 파라미터(`surveyId` path parameter) 정의 확인 <br> 3. 응답 스키마(200 OK)에 응답 목록 구조 정의 확인 <br> 4. 보안 요구사항(bearerAuth) 명시 확인 |
| **입력 데이터** | 없음 (스펙 리뷰) |
| **기대 결과** | 엔드포인트가 OpenAPI 스펙에 완전히 정의되어 있으며, 요청/응답 스키마와 보안 요구사항이 포함됨 |
| **비고** | - |

### API 정상 동작 검증

#### TC-EVTSRV-030: 관리자 응답 목록 조회 정상 동작 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-021 |
| **사전 조건** | 설문이 존재하고 응답이 3건 있음, OPERATOR 이상 권한으로 로그인한 상태 |
| **테스트 절차** | 1. `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 200 응답 확인 <br> 3. 응답 목록에 `responseId`, 응답자 정보, `submittedAt`, 답변 목록이 포함되어 있는지 확인 <br> 4. 반환된 응답 건수가 3건인지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (Authorization: Bearer {OPERATOR 토큰}) |
| **기대 결과** | HTTP 200 OK, 응답 3건이 스키마대로 반환됨 |
| **비고** | - |

### 비정상 케이스 검증

#### TC-EVTSRV-031: 존재하지 않는 설문 ID로 조회 시 404 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-022 |
| **사전 조건** | OPERATOR 이상 권한으로 로그인한 상태 |
| **테스트 절차** | 1. 존재하지 않는 `surveyId`(예: 999999)로 `GET /api/v1/admin/surveys/999999/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/999999/responses` |
| **기대 결과** | HTTP 404 Not Found (`SurveyNotFoundException`) |
| **비고** | - |

#### TC-EVTSRV-032: soft-delete된 설문 ID로 조회 시 404 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-023 |
| **사전 조건** | 설문이 soft-delete된 상태, OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. 삭제된 설문의 `surveyId`로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{deletedSurveyId}/responses` |
| **기대 결과** | HTTP 404 Not Found |
| **비고** | soft-delete 일관성 검증 |

#### TC-EVTSRV-033: 관리자 응답 목록에 soft-delete된 응답 미포함 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-024 |
| **사전 조건** | 설문에 응답 3건 존재, 그 중 1건 soft-delete 상태, OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. 반환된 응답 목록의 건수 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` |
| **기대 결과** | 삭제되지 않은 응답 2건만 반환됨 |
| **비고** | - |

### 경계값 검증

#### TC-EVTSRV-034: 응답 0건인 설문에 대한 관리자 조회 시 빈 목록 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-026 |
| **사전 조건** | 설문이 존재하나 응답이 없음, OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. 반환된 목록이 빈 배열인지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` |
| **기대 결과** | HTTP 200 OK, 빈 목록(`[]`) 반환 (404가 아님) |
| **비고** | - |

### 권한/보안 검증

#### TC-EVTSRV-035: 비인증 사용자의 관리자 응답 목록 조회 시 401 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-025 |
| **사전 조건** | 설문이 존재하고 응답이 있음 |
| **테스트 절차** | 1. Authorization 헤더 없이 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (인증 없음) |
| **기대 결과** | HTTP 401 Unauthorized |
| **비고** | - |

#### TC-EVTSRV-036: ASSOCIATE 역할의 관리자 응답 목록 조회 시 403 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-025 |
| **사전 조건** | 설문이 존재하고 응답이 있음, ASSOCIATE 역할로 로그인 |
| **테스트 절차** | 1. ASSOCIATE 인증 토큰으로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (Authorization: Bearer {ASSOCIATE 토큰}) |
| **기대 결과** | HTTP 403 Forbidden |
| **비고** | - |

#### TC-EVTSRV-037: MEMBER 역할의 관리자 응답 목록 조회 시 403 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-025 |
| **사전 조건** | 설문이 존재하고 응답이 있음, MEMBER 역할로 로그인 |
| **테스트 절차** | 1. MEMBER 인증 토큰으로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (Authorization: Bearer {MEMBER 토큰}) |
| **기대 결과** | HTTP 403 Forbidden |
| **비고** | - |

#### TC-EVTSRV-038: OPERATOR 역할의 관리자 응답 목록 조회 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-025 |
| **사전 조건** | 설문이 존재하고 응답이 있음, OPERATOR 역할로 로그인 |
| **테스트 절차** | 1. OPERATOR 인증 토큰으로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (Authorization: Bearer {OPERATOR 토큰}) |
| **기대 결과** | HTTP 200 OK |
| **비고** | - |

#### TC-EVTSRV-039: ADMIN 역할의 관리자 응답 목록 조회 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-025 |
| **사전 조건** | 설문이 존재하고 응답이 있음, ADMIN 역할로 로그인 |
| **테스트 절차** | 1. ADMIN 인증 토큰으로 `GET /api/v1/admin/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/responses` (Authorization: Bearer {ADMIN 토큰}) |
| **기대 결과** | HTTP 200 OK |
| **비고** | - |

---

## Phase 4: 설문 응답 삭제 API

### OpenAPI 스펙 검증

#### TC-EVTSRV-040: 설문 응답 삭제 API OpenAPI 스펙 등록 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-044 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. `openapi/paths/surveys.yaml`에 `DELETE /api/v1/surveys/{surveyId}/responses` 경로 존재 확인 <br> 2. 요청 파라미터(`surveyId` path parameter) 정의 확인 <br> 3. 성공 응답이 `204 No Content`로 정의되어 있는지 확인 <br> 4. 에러 응답(401, 404, 409) 정의 확인 <br> 5. 보안 요구사항(bearerAuth) 명시 확인 |
| **입력 데이터** | 없음 (스펙 리뷰) |
| **기대 결과** | 엔드포인트가 OpenAPI 스펙에 완전히 정의되어 있으며, 204 성공 응답과 에러 응답(401, 404, 409), 보안 요구사항이 포함됨 |
| **비고** | - |

### 정상 삭제 검증

#### TC-EVTSRV-041: OPEN 상태 설문의 본인 응답 삭제 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-027, EVTSRV-034 |
| **사전 조건** | 설문 `responseStatus == OPEN`, 인증된 사용자의 응답이 존재, ASSOCIATE 이상 권한으로 로그인 |
| **테스트 절차** | 1. `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 <br> 3. 동일 설문에 대해 본인 응답 조회를 시도하여 삭제되었는지 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{surveyId}/responses` (Authorization: Bearer {사용자 토큰}) |
| **기대 결과** | HTTP 204 No Content, 이후 본인 응답 조회 시 404 |
| **비고** | - |

### 소유권 검증

#### TC-EVTSRV-042: 본인 응답만 삭제 — 타인 응답에 영향 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-028 |
| **사전 조건** | 사용자 A와 사용자 B가 각각 동일 설문에 응답 제출, 설문 `responseStatus == OPEN` |
| **테스트 절차** | 1. 사용자 A의 인증 토큰으로 `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 204 확인 <br> 3. 사용자 A의 응답이 삭제되었는지 확인 <br> 4. 사용자 B의 응답이 영향받지 않았는지 확인 (관리자 API로 응답 목록 조회) |
| **입력 데이터** | 사용자 A 토큰으로 `DELETE /api/v1/surveys/{surveyId}/responses` |
| **기대 결과** | 사용자 A의 응답만 삭제됨, 사용자 B의 응답은 그대로 존재 |
| **비고** | `findBySurveyIdAndUserId()`로 본인 응답만 조회하므로 타인 응답은 조회 자체가 불가 |

### 상태별 삭제 검증

#### TC-EVTSRV-043: CLOSED 상태 설문의 응답 삭제 불가 — 409 Conflict

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-029, EVTSRV-034 |
| **사전 조건** | 설문 `responseStatus == CLOSED`, 사용자의 응답이 존재, 인증된 상태 |
| **테스트 절차** | 1. CLOSED 상태 설문에 대해 `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 <br> 3. 응답이 삭제되지 않았는지 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{surveyId}/responses` (Authorization: Bearer {사용자 토큰}) |
| **기대 결과** | HTTP 409 Conflict (`SurveyClosedException`), 응답 데이터 변경 없음 |
| **비고** | 마감 후 데이터 무결성 보호 정책 |

### 교차 도메인 검증 (행사 신청 연동)

#### TC-EVTSRV-044: 행사 연결 설문의 응답 삭제 시 행사 신청 자동 취소

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-030 |
| **사전 조건** | 설문 S가 행사 E에 연결됨(`event.surveyId == S.id`), 사용자 U가 설문 S에 응답하고 행사 E에 신청함(승인 상태), 설문 `responseStatus == OPEN` |
| **테스트 절차** | 1. 사용자 U의 인증 토큰으로 `DELETE /api/v1/surveys/{S.id}/responses` 호출 <br> 2. HTTP 204 확인 <br> 3. 설문 응답이 삭제(soft-delete)되었는지 확인 <br> 4. 행사 E에 대한 사용자 U의 신청 상태가 `CANCELED`로 변경되었는지 확인 <br> 5. 행사 E의 `currentCount`가 1 감소했는지 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{S.id}/responses` (Authorization: Bearer {사용자 U 토큰}) |
| **기대 결과** | 설문 응답 삭제 + 행사 신청 자동 취소(CANCELED) + 정원 카운트 감소 |
| **비고** | 교차 도메인 트랜잭션 원자성 검증 |

#### TC-EVTSRV-045: 독립 설문(행사 미연결)의 응답 삭제 시 행사 부작용 없음

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-031 |
| **사전 조건** | 설문 S가 어떤 행사에도 연결되지 않음, 사용자가 설문 S에 응답함, 설문 `responseStatus == OPEN` |
| **테스트 절차** | 1. `DELETE /api/v1/surveys/{S.id}/responses` 호출 <br> 2. HTTP 204 확인 <br> 3. 설문 응답만 삭제되고 행사 관련 로직이 실행되지 않는지 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{S.id}/responses` (Authorization: Bearer {사용자 토큰}) |
| **기대 결과** | 설문 응답만 soft-delete됨, 행사 도메인에 부작용 없음 |
| **비고** | - |

### 엣지 케이스 검증

#### TC-EVTSRV-046: 본인 응답이 없는 설문에 대한 삭제 요청 시 404 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-032 |
| **사전 조건** | 사용자가 해당 설문에 응답하지 않은 상태, 인증된 상태 |
| **테스트 절차** | 1. `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{surveyId}/responses` (Authorization: Bearer {사용자 토큰}) |
| **기대 결과** | HTTP 404 Not Found (`SurveyResponseNotFoundException`) |
| **비고** | - |

#### TC-EVTSRV-047: 존재하지 않는 설문에 대한 응답 삭제 요청 시 404 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 비정상 |
| **검증 기준 ID** | EVTSRV-032 |
| **사전 조건** | 인증된 상태 |
| **테스트 절차** | 1. 존재하지 않는 `surveyId`(예: 999999)로 `DELETE /api/v1/surveys/999999/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/999999/responses` |
| **기대 결과** | HTTP 404 Not Found |
| **비고** | 설문 자체가 없는 경우 |

### 권한/보안 검증

#### TC-EVTSRV-048: 비인증 사용자의 응답 삭제 시 401 반환

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-033 |
| **사전 조건** | 없음 |
| **테스트 절차** | 1. Authorization 헤더 없이 `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{surveyId}/responses` (인증 없음) |
| **기대 결과** | HTTP 401 Unauthorized |
| **비고** | `@PreAuthorize("isAuthenticated()")` 적용 |

#### TC-EVTSRV-049: 인증된 ASSOCIATE 사용자의 응답 삭제 성공

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 보안 |
| **검증 기준 ID** | EVTSRV-033 |
| **사전 조건** | ASSOCIATE 역할로 로그인, 본인 응답이 존재, 설문 `responseStatus == OPEN` |
| **테스트 절차** | 1. ASSOCIATE 인증 토큰으로 `DELETE /api/v1/surveys/{surveyId}/responses` 호출 <br> 2. HTTP 응답 코드 확인 |
| **입력 데이터** | `DELETE /api/v1/surveys/{surveyId}/responses` (Authorization: Bearer {ASSOCIATE 토큰}) |
| **기대 결과** | HTTP 204 No Content |
| **비고** | ASSOCIATE 이상이면 본인 응답 삭제 가능 |

---

## Phase 5: 외부인 설문 응답 통계 통합

### 통합 통계 검증

#### TC-EVTSRV-050: 회원 + 외부인 응답 합산 통계 정확성 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-035 |
| **사전 조건** | 설문 S가 행사 E에 연결(`event.surveyId == S.id`, `event.allowExternal == true`), 회원 응답 3건, 외부인 응답 2건 존재, OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. `GET /api/v1/admin/surveys/{S.id}/statistics` 호출 <br> 2. `totalResponseCount`가 5 (3 + 2)인지 확인 <br> 3. 질문별 통계에서 외부인 응답이 포함되어 있는지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{S.id}/statistics` (Authorization: Bearer {OPERATOR 토큰}) |
| **기대 결과** | `totalResponseCount == 5`, 질문별 통계에 회원 + 외부인 응답이 합산됨 |
| **비고** | 현재 `SurveyStatisticsService`는 `survey_responses`만 집계하여 외부인 응답이 누락됨 |

### 질문 유형별 파싱 검증

#### TC-EVTSRV-051: TEXT 유형 외부인 응답 파싱 정확성

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-036 |
| **사전 조건** | TEXT 유형 질문에 대한 외부인 응답이 JSON으로 저장됨 |
| **테스트 절차** | 1. TEXT 유형 질문이 포함된 설문의 통계 조회 <br> 2. 외부인의 TEXT 응답이 통계에 정확히 반영되었는지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | TEXT 유형 외부인 응답이 정확히 집계됨 |
| **비고** | - |

#### TC-EVTSRV-052: OPTION 유형 외부인 응답 파싱 정확성

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-036 |
| **사전 조건** | OPTION 유형 질문에 대한 외부인 응답이 JSON으로 저장됨 |
| **테스트 절차** | 1. OPTION 유형 질문이 포함된 설문의 통계 조회 <br> 2. 외부인의 OPTION 응답이 선택지별 카운트에 정확히 반영되었는지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | OPTION 유형 외부인 응답이 선택지별 카운트에 정확히 합산됨 |
| **비고** | - |

#### TC-EVTSRV-053: SCALE 유형 외부인 응답 파싱 정확성

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-036 |
| **사전 조건** | SCALE 유형 질문에 대한 외부인 응답이 JSON으로 저장됨 |
| **테스트 절차** | 1. SCALE 유형 질문이 포함된 설문의 통계 조회 <br> 2. 외부인의 SCALE 응답이 통계에 정확히 반영되었는지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | SCALE 유형 외부인 응답이 정확히 집계됨 |
| **비고** | - |

#### TC-EVTSRV-054: GRID 유형 외부인 응답 파싱 정확성

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-036 |
| **사전 조건** | GRID 유형 질문에 대한 외부인 응답이 JSON으로 저장됨 |
| **테스트 절차** | 1. GRID 유형 질문이 포함된 설문의 통계 조회 <br> 2. 외부인의 GRID 응답이 통계에 정확히 반영되었는지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | GRID 유형 외부인 응답이 정확히 집계됨 |
| **비고** | - |

### 회귀 테스트

#### TC-EVTSRV-055: 외부인 응답이 없는 설문의 통계 — 기존 동작과 동일

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-037 |
| **사전 조건** | 설문에 회원 응답만 존재 (행사 미연결 또는 `allowExternal == false`), OPERATOR 이상 권한으로 로그인 |
| **테스트 절차** | 1. 외부인 응답이 없는 설문의 통계 조회 <br> 2. `totalResponseCount`가 회원 응답 수와 일치하는지 확인 <br> 3. 질문별 통계가 기존과 동일한지 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | `totalResponseCount`가 회원 응답 수와 일치, 기존 동작과 동일 |
| **비고** | 회귀 방지 검증 |

### 경계값 검증

#### TC-EVTSRV-056: 외부인 응답만 있는 설문 통계 (회원 0, 외부인 5)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-038 |
| **사전 조건** | 설문에 회원 응답 0건, 외부인 응답 5건 존재 |
| **테스트 절차** | 1. 통계 조회 API 호출 <br> 2. `totalResponseCount` 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | `totalResponseCount == 5` |
| **비고** | - |

#### TC-EVTSRV-057: 회원 응답만 있는 설문 통계 (회원 3, 외부인 0)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-038 |
| **사전 조건** | 설문에 회원 응답 3건, 외부인 응답 0건 존재 |
| **테스트 절차** | 1. 통계 조회 API 호출 <br> 2. `totalResponseCount` 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | `totalResponseCount == 3` |
| **비고** | - |

#### TC-EVTSRV-058: 응답이 모두 0건인 설문 통계 (회원 0, 외부인 0)

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 경계값 |
| **검증 기준 ID** | EVTSRV-038 |
| **사전 조건** | 설문에 회원 응답 0건, 외부인 응답 0건 |
| **테스트 절차** | 1. 통계 조회 API 호출 <br> 2. `totalResponseCount` 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | `totalResponseCount == 0` |
| **비고** | - |

### 예외 처리 검증

#### TC-EVTSRV-059: 잘못된 JSON 형식의 외부인 응답 파싱 오류 처리

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 예외 |
| **검증 기준 ID** | EVTSRV-039 |
| **사전 조건** | `external_survey_responses`에 잘못된 JSON이 저장된 비정상 상태 |
| **테스트 절차** | 1. 잘못된 JSON이 포함된 설문의 통계 조회 시도 <br> 2. 적절한 에러 응답 또는 해당 응답을 건너뛰는 처리 확인 |
| **입력 데이터** | `GET /api/v1/admin/surveys/{surveyId}/statistics` |
| **기대 결과** | `SurveyStatisticsAggregationException` 또는 해당 응답을 건너뛰고 나머지만 집계 [확인 필요: 구현 시 전체 실패 vs 부분 집계 정책 결정 필요] |
| **비고** | 구현 정책에 따라 기대 결과가 달라짐 |

---

## Phase 6: 코드 중복 제거

### 공통 헬퍼 추출 검증

#### TC-EVTSRV-060: updateEventStatusAfterIncrement 공통 헬퍼 추출 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-040 |
| **사전 조건** | 현재 `EventRegistrationService`와 `ExternalEventRegistrationService`에 동일한 `updateEventStatusAfterIncrement()` 메서드가 각각 `private`으로 존재 |
| **테스트 절차** | 1. 공통 컴포넌트(예: `EventStatusHelper`)에 `updateEventStatusAfterIncrement()` 메서드 존재 확인 <br> 2. `EventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 주입받아 사용하는지 확인 <br> 3. `ExternalEventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 주입받아 사용하는지 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 중복 메서드가 제거되고 단일 공통 컴포넌트로 통합됨 |
| **비고** | - |

#### TC-EVTSRV-061: updateEventStatusAfterDecrement 공통 헬퍼 추출 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 중 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-041 |
| **사전 조건** | 현재 `EventRegistrationService`에 `private`으로 존재 |
| **테스트 절차** | 1. 공통 컴포넌트에 `updateEventStatusAfterDecrement()` 메서드 존재 확인 <br> 2. `EventRegistrationService`에서 기존 `private` 메서드가 제거되고 공통 컴포넌트를 호출하는지 확인 |
| **입력 데이터** | 없음 (코드 리뷰) |
| **기대 결과** | 중복 메서드가 제거되고 단일 공통 컴포넌트로 통합됨 |
| **비고** | `ExternalEventRegistrationService`에는 현재 해당 메서드가 없으나 향후 외부인 취소 기능 구현 시 재사용 가능 |

### 회귀 테스트

#### TC-EVTSRV-062: 코드 중복 제거 후 기능 회귀 없음 확인

| 항목 | 내용 |
|------|------|
| **우선순위** | 상 |
| **테스트 유형** | 정상 |
| **검증 기준 ID** | EVTSRV-042 |
| **사전 조건** | TASK-025 완료 상태 |
| **테스트 절차** | 1. `cd backend && ./gradlew build` 실행하여 빌드 성공 확인 <br> 2. 기존 신청/취소/승인/거절 관련 테스트가 모두 통과하는지 확인 <br> 3. 정원 초과 시 자동 마감(`closeRegistrationByCapacity`) 동작이 유지되는지 확인 <br> 4. 취소 후 자리가 생겼을 때 재오픈 동작이 유지되는지 확인 |
| **입력 데이터** | `cd backend && ./gradlew build` |
| **기대 결과** | BUILD SUCCESSFUL, 모든 기존 테스트 통과, 기능 회귀 없음 |
| **비고** | - |

---

## 커버리지 매트릭스

| 검증 기준 ID | TC ID | 검증 상태 |
|-------------|-------|----------|
| EVTSRV-001 | TC-EVTSRV-001 | 커버됨 |
| EVTSRV-002 | TC-EVTSRV-002 | 커버됨 |
| EVTSRV-003 | TC-EVTSRV-003 | 커버됨 |
| EVTSRV-004 | TC-EVTSRV-004 | 커버됨 |
| EVTSRV-005 | TC-EVTSRV-005 | 커버됨 |
| EVTSRV-006 | TC-EVTSRV-006 | 커버됨 |
| EVTSRV-007 | TC-EVTSRV-007 | 커버됨 |
| EVTSRV-008 | TC-EVTSRV-008, 009, 010 | 커버됨 (3개 동치류) |
| EVTSRV-009 | TC-EVTSRV-011, 012, 013, 014 | 커버됨 (4개 조합) |
| EVTSRV-010 | TC-EVTSRV-015 | 커버됨 |
| EVTSRV-011 | TC-EVTSRV-016 | 커버됨 |
| EVTSRV-012 | TC-EVTSRV-017 | 커버됨 |
| EVTSRV-013 | TC-EVTSRV-018 | 커버됨 |
| EVTSRV-014 | TC-EVTSRV-019 | 커버됨 |
| EVTSRV-015 | TC-EVTSRV-020 | 커버됨 |
| EVTSRV-016 | TC-EVTSRV-021 | 커버됨 |
| EVTSRV-017 | TC-EVTSRV-022 | 커버됨 |
| EVTSRV-018 | TC-EVTSRV-023, 024, 025, 026 | 커버됨 (4개 경계값) |
| EVTSRV-019 | TC-EVTSRV-027 | 커버됨 |
| EVTSRV-020 | TC-EVTSRV-028 | 커버됨 |
| EVTSRV-021 | TC-EVTSRV-030 | 커버됨 |
| EVTSRV-022 | TC-EVTSRV-031 | 커버됨 |
| EVTSRV-023 | TC-EVTSRV-032 | 커버됨 |
| EVTSRV-024 | TC-EVTSRV-033 | 커버됨 |
| EVTSRV-025 | TC-EVTSRV-035, 036, 037, 038, 039 | 커버됨 (5개 역할) |
| EVTSRV-026 | TC-EVTSRV-034 | 커버됨 |
| EVTSRV-027 | TC-EVTSRV-041 | 커버됨 |
| EVTSRV-028 | TC-EVTSRV-042 | 커버됨 |
| EVTSRV-029 | TC-EVTSRV-043 | 커버됨 |
| EVTSRV-030 | TC-EVTSRV-044 | 커버됨 |
| EVTSRV-031 | TC-EVTSRV-045 | 커버됨 |
| EVTSRV-032 | TC-EVTSRV-046, 047 | 커버됨 |
| EVTSRV-033 | TC-EVTSRV-048, 049 | 커버됨 |
| EVTSRV-034 | TC-EVTSRV-041, 043 | 커버됨 (OPEN: 204, CLOSED: 409) |
| EVTSRV-035 | TC-EVTSRV-050 | 커버됨 |
| EVTSRV-036 | TC-EVTSRV-051, 052, 053, 054 | 커버됨 (4개 질문 유형) |
| EVTSRV-037 | TC-EVTSRV-055 | 커버됨 |
| EVTSRV-038 | TC-EVTSRV-056, 057, 058 | 커버됨 (3개 경계값 + TC-050) |
| EVTSRV-039 | TC-EVTSRV-059 | 커버됨 |
| EVTSRV-040 | TC-EVTSRV-060 | 커버됨 |
| EVTSRV-041 | TC-EVTSRV-061 | 커버됨 |
| EVTSRV-042 | TC-EVTSRV-062 | 커버됨 |
| EVTSRV-043 | TC-EVTSRV-029 | 커버됨 |
| EVTSRV-044 | TC-EVTSRV-040 | 커버됨 |
