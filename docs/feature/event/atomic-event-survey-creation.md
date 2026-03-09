# 행사+설문 원자적 생성 API

> **Status**: Implemented
> **Last Updated**: 2026-03-09
> **Scope**: 행사 생성 시 설문을 함께 원자적으로 생성하는 백엔드 API

## 배경 및 문제

행사 생성 시 설문을 함께 만들면, 프론트엔드에서 개별 API를 순차 호출해야 했다:

1. `POST /api/v1/surveys` — 설문 생성
2. `POST /api/v1/surveys/{id}/questions` — 질문 생성 (반복)
3. `POST /api/v1/surveys/{id}/questions/{qId}/options` — 옵션 생성 (반복)
4. `POST /api/v1/events` — 행사 생성 (surveyId 연결)

**문제**: 1~3 단계가 성공했으나 4 단계(행사 생성)가 실패하면, 이미 생성된 설문이 고아 데이터로 DB에 남는다. 재시도할 때마다 고아 설문이 누적된다.

## 해결 방안

새 엔드포인트 `POST /api/v1/events/with-survey`를 추가하여, 행사 + 설문 + 질문 + 옵션을 하나의 `@Transactional`로 원자적 생성한다. 실패 시 전체 롤백되므로 고아 데이터가 발생하지 않는다.

기존 `POST /api/v1/events` (설문 없는 행사 또는 기존 설문 연결)는 하위 호환성을 위해 유지한다.

## API 스펙

### `POST /api/v1/events/with-survey`

**요청 바디** (`CreateEventWithSurveyRequest`):

```json
{
  "title": "행사 제목",
  "description": "행사 설명",
  "location": "장소",
  "eventStartAt": "2026-03-15T10:00:00Z",
  "eventEndAt": "2026-03-15T18:00:00Z",
  "registrationStartAt": "2026-03-10T00:00:00Z",
  "registrationEndAt": "2026-03-14T23:59:00Z",
  "capacity": 30,
  "registrationType": "AUTO_APPROVE",
  "allowExternal": false,
  "attachmentObjectKeys": [],
  "survey": {
    "title": "행사 신청 설문",
    "questions": [
      {
        "questionType": "SHORT_ANSWER",
        "title": "이름",
        "required": true,
        "displayOrder": 1
      },
      {
        "questionType": "MULTIPLE_CHOICE",
        "title": "참여 동기",
        "required": false,
        "displayOrder": 2,
        "options": ["관심", "친구 추천", "기타"]
      }
    ]
  }
}
```

**응답**: `201 Created` — `EventCreateResponse` (기존 행사 생성 응답과 동일)

```json
{
  "id": 1,
  "title": "행사 제목",
  "createdAt": "2026-03-09T12:00:00Z",
  "surveyId": 10,
  "allowExternal": false
}
```

### 지원하는 질문 타입

| questionType | 카테고리 | 옵션 필수 |
|-------------|---------|----------|
| `SHORT_ANSWER` | TEXT | 불필요 |
| `PARAGRAPH` | TEXT | 불필요 |
| `MULTIPLE_CHOICE` | OPTION | 최소 1개 |
| `CHECKBOX` | OPTION | 최소 1개 |
| `DROPDOWN` | OPTION | 최소 1개 |

### 검증 규칙

- 질문 수: 1개 이상 50개 이하
- OPTION 타입 질문: 공백이 아닌 선택지 최소 1개 필수
- 행사 날짜: 기존 `POST /api/v1/events`와 동일한 검증 적용
- 권한: OPERATOR 이상만 호출 가능
- `allowExternal=true` → 설문 `accessLevel`이 `PUBLIC`으로 설정

## 변경 파일

### OpenAPI 스펙
- `openapi/schemas/events.yaml` — 3개 스키마 추가 (`CreateEventWithSurveyRequest`, `CreateEventSurveyRequest`, `CreateEventSurveyQuestionRequest`)
- `openapi/paths/events.yaml` — `eventsWithSurvey` path 추가
- `openapi/openapi.yaml` — path 및 schema 등록

### 백엔드
- `EventWithSurveyService.java` — **신규** — 원자적 생성 서비스 (`@Transactional`)
- `EventController.java` — `createEventWithSurvey()` 메서드 추가

### 프론트엔드
- `useEvents.ts` — `useCreateEventWithSurvey` 훅 추가
- `EventCreatePage.tsx` — `onSubmit`에서 설문 유무에 따라 분기:
  - 설문 있음 → `createEventWithSurvey` (원자적 엔드포인트)
  - 설문 없음 → `createEvent` (기존 엔드포인트)

### 테스트
- `EventWithSurveyServiceTest.java` — **신규** — 16개 테스트 케이스

## 테스트 커버리지

| 카테고리 | 테스트 케이스 |
|---------|-------------|
| 성공 | 행사+설문 원자적 생성, 질문 타입별 생성 검증, ADMIN 권한 생성 |
| accessLevel | `allowExternal=true` → PUBLIC, `allowExternal=false` → MEMBER |
| 권한 | MEMBER 역할 거부, 거부 시 설문 미생성(롤백) |
| 날짜 검증 | 과거 신청 시작일, 마감일 < 시작일, 날짜 실패 시 롤백 |
| 질문 검증 | 빈 질문 목록, 50개 초과, 옵션 질문에 선택지 없음, 공백 선택지만, 실패 시 전체 롤백 |

## 변경하지 않은 것

- `EventEditPage` / `useSurveyEdit.ts` — 수정 시에는 설문이 이미 존재하므로 고아 문제 없음
- 기존 `POST /api/v1/events` — 하위 호환성 유지
- `useSurveyCreate.ts` — 기존 코드 유지 (EventCreatePage에서 `submitSurvey` 호출만 제거)
- DB 스키마 — 변경 없음 (Flyway 마이그레이션 불필요)
