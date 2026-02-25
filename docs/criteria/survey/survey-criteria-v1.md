# 설문 기능 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-25
> **Scope**: 설문 생성(Survey CRUD), 질문 관리(Question Management), 응답 제출(Response Submission), 결과 조회(Result View), 설문 상태 관리(Lifecycle)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **Epic**: [#406 설문 기능](https://github.com/IGRUS-INHA/IGRUS-Web/issues/406)

### 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1 | 2026-02-23 | 초안 작성. 2축 상태 모델, 불변조건 19개, 상태 매트릭스 |
| v2 | 2026-02-25 | `DRAFT` → `UNPUBLISHED` 리네이밍. 공개 상태 양방향 전이(`unpublish()` 추가). 유효 상태 조합 4→5개. 시나리오 F11~F16 추가. INV-20 추가 |
| v3 | 2026-02-25 | 설문 복사·질문 복사 기능 추가. INV-21~INV-25 추가. 시나리오 F17~F20 추가. 권한 매트릭스·테스트 커버리지 업데이트 |

### v2 주요 변경 사항

**`DRAFT` → `UNPUBLISHED` 변경 근거:**

v1에서 `DRAFT → PUBLISHED`는 비가역 전이였다. 하지만 실제 운영 시나리오에서 "끝난 설문을 목록에서 숨기고 싶다"는 요구가 있었다. `PUBLISHED → DRAFT` 역전이를 허용하면 해결되지만, "DRAFT"는 "아직 작성 중"이라는 의미를 내포하여 "공개했다가 숨긴 설문"에 부적합하다.

2축 모델에서 `responseStatus`가 이미 "한번이라도 열었는지"를 구분하고 있으므로(`NOT_STARTED` vs `CLOSED`), visibility를 양방향으로 만들어도 정보 손실이 없다.

| visibility | responseStatus | 의미 |
|:---:|:---:|------|
| UNPUBLISHED | NOT_STARTED | 한번도 공개 안 한 설문 (= 기존 DRAFT) |
| UNPUBLISHED | CLOSED | 공개했다가 숨긴 설문 (응답 데이터 보존) |

따라서 `DRAFT`를 `UNPUBLISHED`로 리네이밍하고, `UNPUBLISHED ⇄ PUBLISHED` 양방향 전이를 도입한다.

**`DRAFT`를 3번째 enum 값으로 두지 않는 이유:**

`DRAFT`를 별도로 유지하면 "초안(첫 공개 전)"과 "재비공개(공개 후 숨김)"를 enum 레벨에서 구분할 수 있다. 하지만 이 구분은 `responseStatus`가 이미 제공하고 있다:

- 초안 → `(UNPUBLISHED, NOT_STARTED)` — 응답을 한번도 열지 않았으므로 NOT_STARTED
- 재비공개 → `(UNPUBLISHED, CLOSED)` — 응답을 열었다가 닫았으므로 CLOSED

즉 `DRAFT`를 추가해도 **새로운 정보가 생기지 않으며**, 오히려 유효 상태 조합과 전이 경로가 늘어나 복잡도만 증가한다. 만약 향후 "최초 공개 시각"이 필요해지면 `firstPublishedAt` 타임스탬프 컬럼 하나로 충분히 해결할 수 있다.

---

## 목적

이 문서는 설문 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 설문 상태 제약, 질문 수 제한, 중복 응답 방지 등 핵심 비즈니스 규칙 |
| 2 | 상태 모델 | 2축 상태 모델: 공개 상태(SurveyVisibility) + 응답 수집 상태(SurveyResponseStatus) |
| 3 | 입력 도메인 분할과 경계값 | 설문/질문/응답 입력값의 동치류와 경계값 |
| 4 | 권한/보안 정책 | RBAC (운영진 관리 vs 일반 회원 응답 vs 비회원 응답) |
| 5 | 관측 가능성 | 설문 라이프사이클 감사 로그 |
| 6 | 테스트 전략 | 테스트 레벨별 검증 항목 매핑 |

---

## 질문 유형 정의 (SurveyQuestionType)

설문에서 사용할 수 있는 11가지 질문 유형이다. 프론트엔드와 백엔드 모두 이 정의를 기준으로 구현한다.

| enum 값 | 한국어 명칭 | 설명 | 응답 방식 | 필수 구성요소 |
|---------|-----------|------|----------|-------------|
| `SHORT_ANSWER` | 단답형 | 한 줄 텍스트 입력 | 자유 텍스트 입력 | 없음 |
| `PARAGRAPH` | 서술형 | 여러 줄 텍스트 입력 | 자유 텍스트 입력 (긴 글) | 없음 |
| `MULTIPLE_CHOICE` | 객관식 (단일 선택) | 보기 중 1개 선택 | 라디오 버튼 | 선택지(Option) 1개 이상 |
| `CHECKBOX` | 체크박스 (복수 선택) | 보기 중 여러 개 선택 가능 | 체크박스 | 선택지(Option) 1개 이상 |
| `DROPDOWN` | 드롭다운 | 드롭다운 목록에서 1개 선택 | 드롭다운 셀렉트 | 선택지(Option) 1개 이상 |
| `LINEAR_SCALE` | 선형 배율 | 최솟값~최댓값 범위에서 점수 선택 (예: 1~5점) | 숫자 척도 선택 | scaleMin, scaleMax (min < max) |
| `MULTIPLE_CHOICE_GRID` | 객관식 그리드 | 행(Row)×열(Option) 표에서 행마다 1개 선택 | 행별 라디오 버튼 | 행(Row) 1개 이상 + 선택지(Option) 1개 이상 |
| `CHECKBOX_GRID` | 체크박스 그리드 | 행(Row)×열(Option) 표에서 행마다 여러 개 선택 | 행별 체크박스 | 행(Row) 1개 이상 + 선택지(Option) 1개 이상 |
| `DATE` | 날짜 | 날짜 입력 | 날짜 선택기 (Date Picker) | 없음 |
| `TIME` | 시간 | 시간 입력 | 시간 선택기 (Time Picker) | 없음 |
| `FILE_UPLOAD` | 파일 업로드 | 파일 첨부 | 파일 선택 / 드래그 앤 드롭 | 없음 |

### 그리드 유형 상세 설명

그리드 유형(`MULTIPLE_CHOICE_GRID`, `CHECKBOX_GRID`)은 표 형태의 질문이다.

```
예시: "각 과목의 만족도를 선택하세요"

             │ 매우 불만족 │ 불만족 │ 보통 │ 만족 │ 매우 만족
─────────────┼───────────┼───────┼─────┼─────┼──────────
 수학         │     ○     │   ○   │  ○  │  ○  │    ○
 영어         │     ○     │   ○   │  ○  │  ○  │    ○
 과학         │     ○     │   ○   │  ○  │  ○  │    ○
```

- **행(Row)**: 평가 대상 (위 예시에서 수학, 영어, 과학) → `SurveyQuestionRow`
- **열(Option)**: 선택 항목 (위 예시에서 매우 불만족~매우 만족) → `SurveyQuestionOption`
- **객관식 그리드**: 행마다 열 중 1개만 선택 (라디오 버튼)
- **체크박스 그리드**: 행마다 열 중 여러 개 선택 가능 (체크박스)

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

시스템 전체에서 **항상 참이어야 하는 조건**이다. 어떤 코드 변경이든 이 조건을 깨뜨리면 시스템 무결성이 훼손된다.

### INV-01: 회원 중복 응답 방지

> 회원(user_id not null)은 하나의 설문에 대해 최대 1회만 응답할 수 있다.

- **사전조건**: 응답 제출 전, `survey_responses` 테이블에서 `(survey_id, user_id)` 조합이 존재하지 않아야 함
- **사후조건**: 응답 제출 후 `(survey_id, user_id)` unique constraint 보장
- **위반 시**: 중복 응답으로 통계 왜곡
- **검증 계층**: DB unique constraint + 서비스 레벨 중복 검사
- **관련 코드**: `SurveyResponse` 엔티티 `@UniqueConstraint(columnNames = {"survey_responses_survey_id", "survey_responses_user_id"})`

### INV-02: 질문 구조는 모든 상태에서 수정 가능

> 질문(추가/수정/삭제), 선택지, 그리드 행은 UNPUBLISHED, PUBLISHED 어떤 공개 상태에서든, 그리고 어떤 응답 상태에서든 수정할 수 있다. (구글폼 방식)

- **주의사항**: 수정 전에 수집된 응답은 수정 전 질문 구조 기준으로 유지됨. 수정 후 응답만 새 구조 적용
- **위험**: 선택지/질문 삭제 시, 기존 `SurveyAnswer`의 FK가 깨질 수 있음 → soft delete 처리 (INV-10, INV-14 참조)

### INV-03: 설문 삭제는 휴지통을 거치는 2단계 삭제

> 설문 삭제는 **휴지통 이동 → 영구 삭제** 2단계로 진행된다. 모든 상태(UNPUBLISHED, PUBLISHED, 모든 응답 상태)에서 휴지통 이동이 가능하다.

| 상태 | `trashedAt` | `deleted` | 의미 | 자식 엔티티 |
|------|:-----------:|:---------:|------|:----------:|
| 활성 | `null` | `false` | 일반 설문 | 유효 |
| 휴지통 | `Instant` | `false` | 보류 중, 복원 가능 | 유효 (FK 보존) |
| 영구 삭제 | `Instant` | `true` | 스케줄러가 물리 삭제할 대상 | soft delete 처리 |

- **1단계 (휴지통 이동)**: `trashedAt = Instant.now()` 설정. `deleted`는 `false` 유지. 자식 엔티티(응답, 답변 등)의 FK가 유효한 상태로 보존됨.
- **2단계 (영구 삭제)**: 휴지통에서 관리자가 영구 삭제 시 `SoftDeletableEntity.delete()` 호출. 스케줄러가 `deleted = true`인 레코드를 물리 삭제할 수 있음.
- **복원**: 휴지통 상태에서 `trashedAt = null`로 복원 가능. 자식 엔티티는 변경 없음.
- **관련 필드**: `Survey.trashedAt` (Instant, nullable) — Survey 엔티티 전용 필드
- **`trashedAt`이 Instant인 이유**: 휴지통 이동 시점을 기록하여 "N일 경과 시 자동 영구 삭제" 같은 스케줄러 정책에 활용 가능

### INV-04: 설문 질문 수 제한

> 설문에 포함된 질문은 최소 1개, 최대 50개이다.

- **적용 시점**: 설문 공개(UNPUBLISHED → PUBLISHED) 시 검증. 재공개 시에도 매번 검증.
- **경계값**: 0개 (발행 거부), 1개 (최소 유효), 50개 (최대 유효), 51개 (발행 거부)
- **위반 시**: 질문 없는 설문이 발행되거나, 과도한 질문 수로 응답 품질 저하

### INV-05: PUBLISHED 설문의 응답 권한 변경 허용

> PUBLISHED 상태에서도 `accessLevel`을 변경할 수 있다.

- **주의사항**: PUBLIC → MEMBER 등으로 변경 시, 변경 전에 수집된 익명 응답과 변경 후 회원 응답이 혼재할 수 있음
- **관련 코드**: `Survey.update()` — accessLevel 파라미터 포함

### INV-19: 본인 응답 조회는 accessLevel과 무관

> 설문의 `accessLevel`이 변경되어 현재 권한으로는 설문에 접근할 수 없더라도, 본인이 제출한 응답은 항상 조회할 수 있다.

- **시나리오**: MEMBER가 `accessLevel = MEMBER` 설문에 응답 → 운영진이 `accessLevel = OPERATOR`로 변경 → 해당 MEMBER는 설문에 새로 응답할 수 없지만, 기존 본인 응답은 조회 가능
- **조회 조건**: `SurveyResponse.user == 요청자` (accessLevel 검증 생략)
- **설문 응답 제출**: 현재 `accessLevel` 기준으로 차단 (기존 로직 유지)
- **프론트엔드 연동**: 본인 응답 조회 API 응답에 사용자 ID와 역할(Role) 정보를 포함하여 프론트엔드에서 권한 기반 UI 분기에 활용

### INV-06: 그리드 질문의 최소 구성

> `MULTIPLE_CHOICE_GRID`, `CHECKBOX_GRID` 유형의 질문은 최소 1개의 행(Row)과 1개의 열(Option)이 있어야 한다.

- **적용 시점**: 설문 발행 시 검증
- **위반 시**: 빈 그리드 질문이 응답자에게 노출

### INV-07: 선형 배율의 범위 유효성

> `LINEAR_SCALE` 유형의 질문에서 `scaleMin < scaleMax`이어야 한다.

- **관련 코드**: `SurveyQuestion.setScaleRange()` — min >= max 시 `IllegalArgumentException`
- **위반 시**: 응답 불가능한 척도 생성

### INV-08: 마감일 경과 시 자동 응답 마감

> 마감일(`deadline`)이 설정된 설문은 마감일 경과 후 자동으로 응답 수집이 마감(`CLOSED`)된다.

- **사전조건**: `survey.responseStatus == OPEN && survey.deadline != null && now > survey.deadline`
- **사후조건**: `survey.responseStatus == CLOSED` (공개 상태 `visibility`는 변경 없음)
- **구현 방식**: 스케줄러 또는 응답 시점 검증 (TBD)

### INV-09: PUBLISHED + OPEN 상태에서만 응답 가능

> 공개 상태가 `PUBLISHED`이고 응답 상태가 `OPEN`인 설문에만 응답을 제출할 수 있다.

- **사전조건**: 응답 제출 시 `survey.visibility == PUBLISHED && survey.responseStatus == OPEN` 검증
- **UNPUBLISHED 거부 이유**: 비공개 설문은 응답자에게 노출되지 않음
- **NOT_STARTED 거부 이유**: 아직 응답 수집이 시작되지 않았으므로 응답 불가
- **CLOSED 거부 이유**: 마감된 설문에 응답이 추가되면 통계 오염
- **PUBLISHED + NOT_STARTED 거부 이유**: 설문은 공개되어 있지만 응답 수집이 아직 시작되지 않은 상태
- **PUBLISHED + CLOSED 거부 이유**: 설문은 공개되어 있지만 응답 수집이 마감된 상태

### INV-10: 선택지/행 삭제 시 기존 응답 보호

> 응답이 참조하고 있는 선택지(`SurveyQuestionOption`)나 그리드 행(`SurveyQuestionRow`)은 soft delete로 처리한다.

- **해결 방안**: soft delete 처리 — 삭제 플래그만 설정하고, 설문 폼에서는 숨기되 기존 응답의 FK는 유지
- **결과**: 결과 조회 시 삭제된 선택지/행의 답변도 확인 가능

### INV-11: 응답 재개 시 마감일 검증

> 응답 상태를 `CLOSED → OPEN`으로 재개할 때, 마감일이 설정되어 있다면 반드시 미래 시점이어야 한다.

- **사전조건**: `survey.deadline == null || survey.deadline > now`
- **위반 시**: 응답 재개 즉시 마감일 경과로 다시 CLOSED 전환

### INV-12: 필수 질문 응답 누락 방지

> `required = true`인 질문에 대해 응답이 누락되면 응답 제출을 거부한다.

- **검증 시점**: 응답 제출 시 서비스 레이어에서 검증
- **위반 시**: 필수 질문에 대한 데이터 수집 실패

### INV-13: 질문 유형별 필수 구성요소 검증

> 질문 유형에 따라 필수 구성요소가 있어야 발행 가능하다.

| 질문 유형 | 필수 구성요소 |
|----------|-------------|
| MULTIPLE_CHOICE, CHECKBOX, DROPDOWN | 선택지(Option) 1개 이상 |
| MULTIPLE_CHOICE_GRID, CHECKBOX_GRID | 선택지(Option) 1개 이상 + 행(Row) 1개 이상 |
| LINEAR_SCALE | scaleMin, scaleMax 설정 (min < max) |
| SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD | 없음 |

- **적용 시점**: 설문 발행 시 검증. 재공개 시에도 매번 검증.
- **위반 시**: 응답 불가능한 질문이 응답자에게 노출

### INV-14: 질문 삭제 시 기존 응답 처리

> 질문을 삭제하면, 해당 질문에 대한 기존 `SurveyAnswer` 레코드가 고아(orphan)가 된다. soft delete로 처리한다.

- **해결 방안**: soft delete 처리 — 삭제 플래그만 설정하고, 설문 폼에서는 숨기되 기존 응답의 FK는 유지
- **결과**: 결과 조회 시 삭제된 질문의 답변도 확인 가능

### INV-15: 응답 제출 중 설문 마감 경합

> 응답자가 설문을 작성하는 도중 응답 상태가 `CLOSED`로 전환될 수 있다.

- **시나리오**: 응답자가 폼을 열었을 때 `OPEN` → 작성 중 마감일 경과 → 제출 시 `CLOSED`
- **해결 방안**: 응답 제출 시점에 `survey.visibility == PUBLISHED && survey.responseStatus == OPEN` 재검증
- **위반 시**: 마감된 설문에 응답이 저장됨

### INV-16: 휴지통 설문은 응답 불가

> `trashedAt`이 설정된 설문은 상태(PUBLISHED 등)와 무관하게 응답을 받을 수 없다.

- **사전조건**: 응답 제출 시 `survey.trashedAt == null` 검증
- **위반 시**: 휴지통에 있는 설문에 응답이 저장됨

### INV-17: 휴지통 설문은 목록에서 제외

> `trashedAt`이 설정된 설문은 일반 설문 목록에서 노출되지 않는다. 관리자 휴지통 목록에서만 조회 가능하다.

- **일반 목록 조건**: `trashedAt IS NULL AND deleted = false`
- **휴지통 목록 조건**: `trashedAt IS NOT NULL AND deleted = false`

### INV-18: 영구 삭제는 휴지통 상태에서만 가능

> 영구 삭제(`SoftDeletableEntity.delete()`)는 `trashedAt`이 설정된 설문에 대해서만 수행할 수 있다. 활성 설문을 바로 영구 삭제할 수 없다.

- **사전조건**: `survey.trashedAt != null`
- **위반 시**: 실수로 활성 설문이 영구 삭제됨

### INV-20: 비공개 전환 시 자동 응답 마감 (v2 추가)

> `PUBLISHED + OPEN` 상태에서 비공개(`unpublish()`) 전환 시, 응답 수집이 자동으로 마감(`CLOSED`)된다.

- **사전조건**: `survey.visibility == PUBLISHED && survey.responseStatus == OPEN`
- **사후조건**: `survey.visibility == UNPUBLISHED && survey.responseStatus == CLOSED`
- **근거**: `(UNPUBLISHED, OPEN)` 상태는 모순(비공개인데 응답 수집 중)이므로 자동 마감하여 방지
- **대안**: 운영진이 수동으로 `closeResponse()` 후 `unpublish()` 호출도 가능 (결과 동일)

### INV-21: 설문 복사 시 상태 초기화

> 설문을 복사하면 새로운 설문이 생성되며, 상태는 항상 `(UNPUBLISHED, NOT_STARTED, active)`로 초기화된다.

- **복사 대상**: 활성 설문만 복사 가능 (`trashedAt == null && deleted == false`)
- **복사되는 필드**: title (+ " (복사본)" 접미사), description, accessLevel
- **초기화되는 필드**: `visibility = UNPUBLISHED`, `responseStatus = NOT_STARTED`, `trashedAt = null`, `deadline = null`
- **deadline 초기화 근거**: 원본의 마감일이 과거일 수 있으므로 복사본에 그대로 적용하면 즉시 마감 대상이 됨. 운영진이 복사 후 직접 설정하도록 null로 초기화
- **관련 코드**: `Survey.copyFrom(Survey original)`

### INV-22: 복사 시 응답 데이터 제외

> 설문을 복사할 때 `SurveyResponse`, `SurveyAnswer`는 복사 대상에서 제외된다. 복사된 설문은 응답이 0건인 상태로 시작한다.

- **근거**: 응답 데이터는 원본 설문에 종속된 데이터. 복사본은 별개의 설문이므로 응답을 공유할 수 없음
- **위반 시**: 복사본의 통계에 원본 응답이 포함되어 데이터 오염

### INV-23: 복사 시 soft delete된 요소 제외

> 설문 복사 및 질문 복사 시, `deleted = true`인 질문·선택지·행은 복사 대상에서 제외된다.

- **근거**: soft delete된 요소는 논리적으로 삭제된 상태. 복사본에는 활성 요소만 포함되어야 함
- **적용 범위**: `SurveyQuestion`, `SurveyQuestionOption`, `SurveyQuestionRow` 모두 해당

### INV-24: 질문 복사 시 질문 수 제한 검증

> 질문을 복사한 결과 대상 설문의 질문 수가 50개를 초과하면 복사를 거부한다. (INV-04 준수)

- **검증 시점**: 복사 실행 전에 `현재 질문 수 + 복사할 질문 수 <= 50` 검증
- **설문 복사 시**: 원본의 활성 질문 수가 50개를 초과하는 경우는 INV-04에 의해 원본에서도 불가능하므로, 설문 전체 복사 시에는 항상 통과. 단, 대상 설문에 이미 질문이 있는 경우(질문 복사 시) 검증 필요
- **위반 시**: `SURVEY_QUESTION_LIMIT_EXCEEDED` 에러

### INV-25: 복사된 엔티티는 새로운 ID를 부여받는다

> 복사된 설문·질문·선택지·행은 모두 새로운 ID(`GenerationType.IDENTITY`)를 부여받으며, 원본과 어떠한 ID도 공유하지 않는다.

- **근거**: 복사본은 원본과 완전히 독립된 엔티티. ID를 공유하면 수정/삭제 시 원본에 영향
- **cascade 활용**: `Survey` → `SurveyQuestion` 관계가 `CascadeType.ALL`이므로, 복사된 Survey를 `save()`하면 하위 질문·선택지·행도 자동 persist

---

## 2. 상태 모델 (State Machine & Transitions)

설문은 **2축 상태 모델**을 사용한다. 공개 상태와 응답 수집 상태를 독립적으로 관리하여, 1축 모델에서 표현할 수 없었던 조합(설문은 공개하되 응답은 아직 안 받는 상태 등)을 자연스럽게 지원한다.

### 2-1. 2축 개요

| 축 | enum | 값 | 의미 |
|----|------|-----|------|
| 축 1: 공개 상태 | `SurveyVisibility` | `UNPUBLISHED` | 비공개 (응답자에게 미노출) |
| | | `PUBLISHED` | 공개 (응답자에게 노출) |
| 축 2: 응답 수집 상태 | `SurveyResponseStatus` | `NOT_STARTED` | 아직 응답 수집을 시작하지 않음 |
| | | `OPEN` | 응답 수집 중 |
| | | `CLOSED` | 응답 마감 (한번 열렸다가 닫힘) |

### 2-1-1. 전체 상태 조합 (6가지)

| 공개 상태 | 응답 상태 | 유효? | 의미 | 불가능 사유 |
|:---------:|:---------:|:---:|------|:-----------|
| `UNPUBLISHED` | `NOT_STARTED` | ✅ | 비공개, 응답 시작 전 (초기 상태) | - |
| `UNPUBLISHED` | `OPEN` | ❌ | - | 비공개 설문은 응답을 받을 수 없음 (INV-20에 의해 자동 마감) |
| `UNPUBLISHED` | `CLOSED` | ✅ | 비공개, 이전에 공개+응답 수집 후 비공개 전환됨 (응답 데이터 보존) | - |
| `PUBLISHED` | `NOT_STARTED` | ✅ | 공개했지만 아직 응답 수집 시작 전 (미리보기) | - |
| `PUBLISHED` | `OPEN` | ✅ | 공개 + 응답 수집 중 | - |
| `PUBLISHED` | `CLOSED` | ✅ | 공개 + 응답 마감 (한번 열렸다가 닫힘) | - |

> **참고**: `UNPUBLISHED` 상태에서는 응답 상태가 `NOT_STARTED` 또는 `CLOSED`만 가능하다. `UNPUBLISHED`인 설문에서 `openResponse()`는 허용되지 않는다.

#### NOT_STARTED vs CLOSED 구분 근거 — 설계 대안 비교

응답 수집 상태를 설계할 때 3가지 대안을 검토했다.

| 대안 | 값 | 특징 |
|:---:|:--|:----|
| A | `CLOSED`, `OPEN` | 단순. "안 열림"과 "열렸다 닫힘" 구분 불가 |
| **B (채택)** | `NOT_STARTED`, `OPEN`, `CLOSED` | 의미 정확. Event 도메인 `RegistrationStatus`와 동일 패턴 |
| C | `null`, `OPEN`, `CLOSED` | null이 "시작 전"을 의미. DB NOT NULL 제약 깨짐, null 체크 부담 |

**시나리오별 대안 비교:**

| # | 시나리오 | 대안 A (`CLOSED/OPEN`) | **대안 B (`NS/OPEN/CLOSED`)** | 대안 C (`null/OPEN/CLOSED`) |
|:-:|:--------|:----------------------|:----------------------------|:---------------------------|
| 1 | 설문 생성 직후 | (U, **CLOSED**) — 한 번도 안 열었는데 "마감"? | (U, **NOT_STARTED**) — 의미 정확 | (U, **null**) — 의미는 맞지만 null 체크 |
| 2 | 설문 공개, 응답 대기 | (P, **CLOSED**) — 응답자에게 "마감됨"으로 보임 | (P, **NOT_STARTED**) — "곧 시작" 표시 가능 | (P, **null**) — null 체크 |
| 3 | 응답 수집 중 | (P, OPEN) — 동일 | (P, OPEN) — 동일 | (P, OPEN) — 동일 |
| 4 | 응답 마감 | (P, CLOSED) — "시작 전"과 구분 불가 | (P, CLOSED) — NOT_STARTED와 명확히 구분 | (P, CLOSED) — null과 구분 가능 |
| 5 | 프론트 안내 문구 분기 | 불가능 ("마감"만 표시) | "곧 시작됩니다" vs "마감되었습니다" 분기 가능 | 가능하지만 null 처리 |
| 6 | DB 직접 조회 시 | `CLOSED`만 보고 히스토리 유추 불가 | `NOT_STARTED`/`CLOSED` 구분으로 히스토리 유추 가능 | null이 의미 모호 |
| 7 | Event 도메인 일관성 | `RegistrationStatus`와 불일치 (2개 vs 3개) | `RegistrationStatus`와 동일 패턴 | 패턴 불일치 |
| 8 | 자동 마감 스케줄러 | OPEN만 대상 — 문제없음 | OPEN만 대상 — 문제없음 | OPEN만 대상 — 문제없음 |

**대안 B 채택 이유 요약:**

1. **의미 정확성**: "한 번도 안 열림"(NOT_STARTED)과 "열렸다 닫힘"(CLOSED)은 본질적으로 다른 상태
2. **프론트엔드 UI**: 응답자에게 "곧 시작" vs "마감" 안내를 자연스럽게 분기 가능
3. **DB 가독성**: 데이터만 봐도 설문의 운영 히스토리를 유추 가능
4. **Event 도메인 일관성**: `RegistrationStatus(NOT_STARTED/OPEN/CLOSED)`와 동일 패턴으로 학습 비용 감소

### 2-1-2. 축 1: 공개 상태 전이 (SurveyVisibility)

```
          공개(발행)
┌─────────────┐ ──────────> ┌───────────┐
│ UNPUBLISHED │             │ PUBLISHED │
└─────────────┘ <────────── └───────────┘
                  비공개
```

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| UNPUBLISHED → PUBLISHED | 운영진이 공개 | OPERATOR 이상 권한, 질문 1개 이상 (INV-04), 질문 유형별 필수 구성요소 충족 (INV-13) | `visibility = PUBLISHED` |
| PUBLISHED → UNPUBLISHED | 운영진이 비공개 전환 | OPERATOR 이상 권한. responseStatus가 OPEN이면 자동 마감(CLOSED) (INV-20) | `visibility = UNPUBLISHED` |

### 2-1-3. 축 2: 응답 수집 상태 전이 (SurveyResponseStatus)

```
                 응답 시작           응답 마감
┌─────────────┐ ──────────> ┌────────┐ ──────────> ┌────────┐
│ NOT_STARTED │             │  OPEN  │             │ CLOSED │
└─────────────┘             └────────┘ <────────── └────────┘
                                         응답 재개
```

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| NOT_STARTED → OPEN | 운영진이 최초 응답 시작 | OPERATOR 이상 권한, `visibility == PUBLISHED`, 마감일 미설정이거나 미래 | `responseStatus = OPEN` |
| OPEN → CLOSED | 수동 마감 | OPERATOR 이상 권한 | `responseStatus = CLOSED` |
| OPEN → CLOSED | 마감일 경과 | `deadline != null && now > deadline` | 자동 전환 |
| CLOSED → OPEN | 운영진이 응답 재개 | OPERATOR 이상 권한, `visibility == PUBLISHED`, 마감일 미설정이거나 미래 | `responseStatus = OPEN` |

**금지된 전이**:

| 시도 | 이유 |
|------|------|
| UNPUBLISHED 상태에서 OPEN 전환 | 비공개 설문은 응답을 받을 수 없음 |
| NOT_STARTED → CLOSED | 열지도 않았는데 마감할 수 없음 |
| CLOSED → NOT_STARTED | 한번 열리면 "시작 전" 상태로 되돌릴 수 없음 |
| OPEN → NOT_STARTED | 한번 열리면 "시작 전" 상태로 되돌릴 수 없음 |
| CLOSED → OPEN (마감일 경과) | 마감일이 이미 지났으면 재개해도 즉시 다시 CLOSED됨 |

### 2-1-4. 상태-행동 매트릭스 (전체 경우의 수)

아래 표는 설문의 **모든 유효 상태 조합 × 모든 행동**에 대한 결과를 정의한다. 구현과 테스트는 이 표를 기준으로 한다.

**표기법:**
- ✅ → 결과 상태: 성공 (전이 후 상태)
- ❌ 사유: 실패 (예외 발생)
- 상태 표기: `(visibility, responseStatus, trashed여부)` — 예: `(P, O, active)` = PUBLISHED + OPEN + 활성

#### 표 1: 상태 전이 행동

| # | 현재 상태 | `publish()` | `unpublish()` | `openResponse()` | `closeResponse()` | `publishAndOpen()` |
|:-:|:---------|:-----------|:-------------|:-----------------|:------------------|:-------------------|
| S1 | (U, NS, active) | ✅ → (P, NS, active) | ❌ 이미 비공개 | ❌ 비공개에서 불가 | ❌ NS→C 전이 불가 | ✅ → (P, O, active) |
| S2 | (P, NS, active) | ❌ 이미 공개 | ✅ → (U, NS, active) | ✅ → (P, O, active) | ❌ NS→C 전이 불가 | ❌ 이미 공개 |
| S3 | (P, O, active) | ❌ 이미 공개 | ✅ → (U, C, active) 자동마감 | ❌ 이미 OPEN | ✅ → (P, C, active) | ❌ 이미 공개 |
| S4 | (P, C, active) | ❌ 이미 공개 | ✅ → (U, C, active) | ✅ → (P, O, active) | ❌ 이미 CLOSED | ❌ 이미 공개 |
| S5 | (U, C, active) | ✅ → (P, C, active) | ❌ 이미 비공개 | ❌ 비공개에서 불가 | ❌ 이미 CLOSED | ✅ → (P, O, active) |
| S6 | (U, NS, trashed) | ✅ → (P, NS, trashed) | ❌ 이미 비공개 | ❌ 비공개에서 불가 | ❌ NS→C 전이 불가 | ✅ → (P, O, trashed) |
| S7 | (P, NS, trashed) | ❌ 이미 공개 | ✅ → (U, NS, trashed) | ✅ → (P, O, trashed) | ❌ NS→C 전이 불가 | ❌ 이미 공개 |
| S8 | (P, O, trashed) | ❌ 이미 공개 | ✅ → (U, C, trashed) 자동마감 | ❌ 이미 OPEN | ✅ → (P, C, trashed) | ❌ 이미 공개 |
| S9 | (P, C, trashed) | ❌ 이미 공개 | ✅ → (U, C, trashed) | ✅ → (P, O, trashed) | ❌ 이미 CLOSED | ❌ 이미 공개 |
| S10 | (U, C, trashed) | ✅ → (P, C, trashed) | ❌ 이미 비공개 | ❌ 비공개에서 불가 | ❌ 이미 CLOSED | ✅ → (P, O, trashed) |

> **참고**: 현재 구현에서 `publish()`, `unpublish()`, `openResponse()` 등 상태 전이 메서드는 `trashedAt`을 검사하지 않는다. 휴지통 상태에서의 상태 전이 차단은 Service 레이어에서 담당한다. (S6~S10에서 엔티티 레벨에서는 전이가 성공하지만, Service에서 휴지통 여부를 먼저 검증하여 차단해야 함)

#### 표 2: `openResponse()` 마감일 조건

| # | 현재 상태 | 마감일 | 결과 |
|:-:|:---------|:------|:----|
| D1 | (P, NS, active) | null (미설정) | ✅ → (P, O, active) |
| D2 | (P, NS, active) | 미래 시점 | ✅ → (P, O, active) |
| D3 | (P, NS, active) | 과거 시점 | ❌ 마감일 경과 |
| D4 | (P, C, active) | null (미설정) | ✅ → (P, O, active) |
| D5 | (P, C, active) | 미래 시점 | ✅ → (P, O, active) |
| D6 | (P, C, active) | 과거 시점 | ❌ 마감일 경과 |

#### 표 3: 휴지통 행동

| # | 현재 상태 | `trash()` | `restoreFromTrash()` | `permanentDelete()` |
|:-:|:---------|:---------|:--------------------|:-------------------|
| T1 | (U, NS, active) | ✅ → (U, NS, trashed) | ❌ 휴지통 아님 | ❌ 휴지통 아님 |
| T2 | (P, NS, active) | ✅ → (P, NS, trashed) | ❌ 휴지통 아님 | ❌ 휴지통 아님 |
| T3 | (P, O, active) | ✅ → (P, O, trashed) | ❌ 휴지통 아님 | ❌ 휴지통 아님 |
| T4 | (P, C, active) | ✅ → (P, C, trashed) | ❌ 휴지통 아님 | ❌ 휴지통 아님 |
| T5 | (U, C, active) | ✅ → (U, C, trashed) | ❌ 휴지통 아님 | ❌ 휴지통 아님 |
| T6 | (U, NS, trashed) | ❌ 이미 휴지통 | ✅ → (U, NS, active) | ✅ → deleted=true |
| T7 | (P, NS, trashed) | ❌ 이미 휴지통 | ✅ → (P, NS, active) | ✅ → deleted=true |
| T8 | (P, O, trashed) | ❌ 이미 휴지통 | ✅ → (P, O, active) | ✅ → deleted=true |
| T9 | (P, C, trashed) | ❌ 이미 휴지통 | ✅ → (P, C, active) | ✅ → deleted=true |
| T10 | (U, C, trashed) | ❌ 이미 휴지통 | ✅ → (U, C, active) | ✅ → deleted=true |

> **핵심**: 복원 시 `(visibility, responseStatus)`는 변경 없이 원래 상태 그대로 유지된다.

#### 표 4: 설문 수정 및 응답 제출

| # | 현재 상태 | `update()` | 응답 제출 | 설문 복사 | 설문 목록 노출 |
|:-:|:---------|:----------|:---------|:---------|:------------|
| A1 | (U, NS, active) | ✅ | ❌ UNPUBLISHED + NS | ✅ → 새 설문 (U,NS) | ✅ 관리자 목록 |
| A2 | (P, NS, active) | ✅ | ❌ NOT_STARTED | ✅ → 새 설문 (U,NS) | ✅ 전체 목록 (응답 불가 표시) |
| A3 | (P, O, active) | ✅ | ✅ 응답 가능 | ✅ → 새 설문 (U,NS) | ✅ 전체 목록 (응답 가능 표시) |
| A4 | (P, C, active) | ✅ | ❌ CLOSED | ✅ → 새 설문 (U,NS) | ✅ 전체 목록 (마감 표시) |
| A5 | (U, C, active) | ✅ | ❌ UNPUBLISHED | ✅ → 새 설문 (U,NS) | ✅ 관리자 목록 |
| A6 | (U, NS, trashed) | ✅ | ❌ 휴지통 | ❌ 휴지통 (INV-21) | ❌ 일반 목록 제외, 휴지통 목록 노출 |
| A7 | (P, NS, trashed) | ✅ | ❌ 휴지통 | ❌ 휴지통 (INV-21) | ❌ 일반 목록 제외, 휴지통 목록 노출 |
| A8 | (P, O, trashed) | ✅ | ❌ 휴지통 (INV-16) | ❌ 휴지통 (INV-21) | ❌ 일반 목록 제외, 휴지통 목록 노출 |
| A9 | (P, C, trashed) | ✅ | ❌ 휴지통 | ❌ 휴지통 (INV-21) | ❌ 일반 목록 제외, 휴지통 목록 노출 |
| A10 | (U, C, trashed) | ✅ | ❌ 휴지통 | ❌ 휴지통 (INV-21) | ❌ 일반 목록 제외, 휴지통 목록 노출 |

> **응답 제출 조건** (`isAcceptingResponses()`): `visibility == PUBLISHED && responseStatus == OPEN && trashedAt == null` — 이 3가지가 모두 참일 때만 A3 케이스.

#### 표 5: 대표 시나리오 흐름

| # | 시나리오 | 흐름 |
|:-:|:--------|:----|
| F1 | 기본 흐름 | `(U,NS)` → publish → `(P,NS)` → openResponse → `(P,O)` → closeResponse → `(P,C)` |
| F2 | 즉시 응답 시작 | `(U,NS)` → publishAndOpen → `(P,O)` → closeResponse → `(P,C)` |
| F3 | 응답 일시 중지 후 재개 | `(P,O)` → closeResponse → `(P,C)` → openResponse → `(P,O)` |
| F4 | 공개 후 대기 | `(U,NS)` → publish → `(P,NS)` — 응답자 미리보기만 가능 |
| F5 | 마감일 자동 마감 | `(P,O)` → [deadline 경과] → `(P,C)` |
| F6 | 휴지통 이동 후 복원 | `(P,O)` → trash → `(P,O,trashed)` → restore → `(P,O,active)` |
| F7 | 휴지통 → 영구 삭제 | `(P,C)` → trash → `(P,C,trashed)` → permanentDelete → deleted |
| F8 | 비공개에서 바로 휴지통 | `(U,NS)` → trash → `(U,NS,trashed)` → restore → `(U,NS,active)` |
| F9 | 마감 후 마감일 수정 후 재개 | `(P,C)` → update(deadline=미래) → openResponse → `(P,O)` |
| F10 | 마감일 경과 후 재개 시도 실패 | `(P,C, deadline=과거)` → openResponse → ❌ 마감일 경과 |
| F11 | 끝난 설문 숨기기 | `(P,C)` → unpublish → `(U,C)` |
| F12 | 진행 중 설문 내리기 (자동마감) | `(P,O)` → unpublish → `(U,C)` |
| F13 | 숨긴 설문 다시 공개 | `(U,C)` → publish → `(P,C)` |
| F14 | 숨긴 설문 공개 + 응답 재개 | `(U,C)` → publishAndOpen → `(P,O)` |
| F15 | 공개 미리보기 취소 | `(P,NS)` → unpublish → `(U,NS)` |
| F16 | 전체 라이프사이클 | `(U,NS)` → publish → open → close → unpublish → `(U,C)` → publish → open → close → `(P,C)` |
| F17 | 설문 복사 (기본) | `(P,O)` 설문 복사 → 새 설문 `(U,NS)` 생성 (응답 0건, deadline null) |
| F18 | 설문 복사 후 수정 발행 | 설문 복사 → `(U,NS)` → 제목/마감일 수정 → publish → `(P,NS)` → openResponse → `(P,O)` |
| F19 | 같은 설문 내 질문 복사 | `(U,NS)` 설문의 질문 1개 복사 → 같은 설문에 복사본 추가 (displayOrder 맨 뒤) |
| F20 | 질문 수 상한 도달 시 복사 실패 | 질문 49개인 설문에 2개 이상 질문 복사 시도 → ❌ `SURVEY_QUESTION_LIMIT_EXCEEDED` |

### 2-2. 설문 휴지통 전이 (SurveyVisibility FSM과 직교)

설문의 휴지통 상태는 2축 상태(`visibility`, `responseStatus`)와 **독립적**이다. 어떤 상태의 설문이든 휴지통에 넣고 복원할 수 있으며, 복원 시 원래 상태가 그대로 유지된다. (상세 경우의 수는 표 3 참조)

```
                  휴지통 이동              영구 삭제
┌────────────┐ ──────────────> ┌────────────┐ ──────────────> ┌────────────┐
│   활성     │                 │  휴지통    │                 │  영구 삭제  │
│ trashedAt  │  <──────────── │ trashedAt  │                 │ deleted    │
│  = null    │     복원        │  = Instant │                 │  = true    │
│ deleted    │                 │ deleted    │                 │            │
│  = false   │                 │  = false   │                 │            │
└────────────┘                 └────────────┘                 └────────────┘
```

| 전이 | 트리거 | 사전조건 | 사후조건 |
|------|--------|---------|---------|
| 활성 → 휴지통 | 운영진이 삭제 | OPERATOR 이상 권한 | `trashedAt = Instant.now()` |
| 휴지통 → 활성 | 운영진이 복원 | OPERATOR 이상 권한 | `trashedAt = null` |
| 휴지통 → 영구 삭제 | 운영진이 영구 삭제 | OPERATOR 이상 권한, `trashedAt != null` | `delete()` 호출 |

### 2-3. 설문 수정 가능 범위 (상태별)

| 필드 | U + NS | P + NS | P + O | P + C | U + C |
|------|:---:|:---:|:---:|:---:|:---:|
| 제목 (title) | O | O | O | O | O |
| 설명 (description) | O | O | O | O | O |
| 응답 권한 (accessLevel) | O | O | O | O | O |
| 마감일 (deadline) | O | O | O | O | O |
| 질문 구조 (추가/수정/삭제) | O | O | O | O | O |

---

## 3. 입력 도메인 분할과 경계값 (Equivalence Partitioning & BVA)

### 3-1. 설문 생성 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `title` | 1~100자 문자열 | null, 빈 문자열, 101자 이상 | 1자 (최소), 100자 (최대), 101자 (초과) |
| `description` | null, 1~500자 문자열 | 501자 이상 | 500자 (최대), 501자 (초과) |
| `accessLevel` | `PUBLIC`, `ASSOCIATE`, `MEMBER` | null, 유효하지 않은 값 | - |
| `deadline` | null, 미래 시점 | 과거 시점 | 현재 시각 직후 (최소 유효), 현재 시각 이전 (무효) |

### 3-2. 질문 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `title` | 1~200자 문자열 | null, 빈 문자열, 201자 이상 | 1자 (최소), 200자 (최대), 201자 (초과) |
| `description` | null, 1~500자 문자열 | 501자 이상 | 500자 (최대), 501자 (초과) |
| `questionType` | 11가지 enum 값 | null, 유효하지 않은 값 | - |
| `required` | true, false | null | - |
| `displayOrder` | 0 이상 정수 | 음수 | 0 (최소) |

### 3-3. 선택지/행 입력값

| 필드 | 유효 동치류 | 무효 동치류 | 경계값 |
|------|-----------|-----------|--------|
| `text` / `label` | 1~200자 문자열 | null, 빈 문자열, 201자 이상 | 1자 (최소), 200자 (최대), 201자 (초과) |
| `displayOrder` | 0 이상 정수 | 음수 | 0 (최소) |

### 3-4. 응답 입력값 (질문 유형별)

| 질문 유형 | 유효 동치류 | 무효 동치류 |
|----------|-----------|-----------|
| SHORT_ANSWER | 비어있지 않은 텍스트 | null (필수 질문일 때), 빈 문자열 (필수 질문일 때) |
| PARAGRAPH | 비어있지 않은 텍스트 | null (필수 질문일 때) |
| MULTIPLE_CHOICE | 해당 질문의 유효한 option ID 1개 | 존재하지 않는 option ID, 다른 질문의 option ID, null (필수) |
| CHECKBOX | 해당 질문의 유효한 option ID 1개 이상 | 빈 리스트 (필수 질문일 때), 존재하지 않는 option ID |
| DROPDOWN | 해당 질문의 유효한 option ID 1개 | MULTIPLE_CHOICE와 동일 |
| LINEAR_SCALE | scaleMin ~ scaleMax 범위의 정수 | 범위 밖 정수, null (필수) |
| MULTIPLE_CHOICE_GRID | 각 행마다 유효한 option ID 1개 | 행 누락 (필수), 존재하지 않는 row/option ID |
| CHECKBOX_GRID | 각 행마다 유효한 option ID 1개 이상 | 행 누락 (필수), 빈 선택 |
| DATE | 유효한 날짜 형식 | 잘못된 형식, null (필수) |
| TIME | 유효한 시간 형식 | 잘못된 형식, null (필수) |
| FILE_UPLOAD | 유효한 파일 URL | null (필수), 잘못된 URL |

### 3-5. 설문 질문 수 경계값

| 항목 | 유효 범위 | 경계 지점 |
|------|----------|----------|
| 질문 수 (발행 시) | 1 ~ 50개 | 0개 (발행 거부), 1개 (최소 유효), 50개 (최대 유효), 51개 (발행 거부) |
| 선택지 수 (MULTIPLE_CHOICE 등) | 1개 이상 | 0개 (무효), 1개 (최소 유효) |
| 그리드 행 수 | 1개 이상 | 0개 (무효), 1개 (최소 유효) |
| 선형 배율 범위 | min < max | min=1, max=2 (최소 유효), min=max (무효), min>max (무효) |

---

## 4. 권한/보안 정책 (RBAC & Authorization)

### 4-1. 역할별 접근 제어 매트릭스

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 설문 생성 | 401 | 403 | 403 | **O** | **O** |
| 설문 수정 (모든 상태) | 401 | 403 | 403 | **O** | **O** |
| 설문 복사 | 401 | 403 | 403 | **O** | **O** |
| 질문 복사 | 401 | 403 | 403 | **O** | **O** |
| 설문 발행 (공개) | 401 | 403 | 403 | **O** | **O** |
| 설문 비공개 전환 | 401 | 403 | 403 | **O** | **O** |
| 설문 응답 재개 (CLOSED→OPEN) | 401 | 403 | 403 | **O** | **O** |
| 설문 마감 | 401 | 403 | 403 | **O** | **O** |
| 설문 휴지통 이동 (모든 상태) | 401 | 403 | 403 | **O** | **O** |
| 설문 휴지통 복원 | 401 | 403 | 403 | **O** | **O** |
| 설문 영구 삭제 (휴지통에서) | 401 | 403 | 403 | **O** | **O** |
| 휴지통 목록 조회 | 401 | 403 | 403 | **O** | **O** |
| 설문 목록 조회 | 본인 권한에 해당하는 PUBLISHED만 | O | O | **O** (전체) | **O** (전체) |
| 설문 응답 (PUBLIC) | **O** | O | O | O | O |
| 설문 응답 (ASSOCIATE) | 401 | **O** | O | O | O |
| 설문 응답 (MEMBER) | 401 | 403 | **O** | O | O |
| 본인 응답 조회 | 401 | **본인 것만** | **본인 것만** | **본인 것만** | **본인 것만** |
| 결과 조회 (전체 통계) | 401 | 403 | 403 | **O** | **O** |

### 4-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 |
|----|----------|----------|
| SEC-01 | ASSOCIATE가 설문 생성 시도 | 403 Forbidden |
| SEC-02 | MEMBER가 설문 발행 시도 | 403 Forbidden |
| SEC-03 | 비회원이 ASSOCIATE 설문에 응답 시도 | 401 Unauthorized |
| SEC-04 | ASSOCIATE가 MEMBER 설문에 응답 시도 | 403 Forbidden |
| SEC-05 | MEMBER가 결과 조회 시도 | 403 Forbidden |
| SEC-06 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 |
| SEC-07 | accessLevel 축소 후 기존 응답자가 본인 응답 조회 | 200 OK (본인 응답 반환) |
| SEC-08 | accessLevel 축소 후 기존 응답자가 타인 응답 조회 시도 | 403 Forbidden |
| SEC-09 | MEMBER가 비공개 전환 시도 | 403 Forbidden |
| SEC-10 | MEMBER가 설문 복사 시도 | 403 Forbidden |
| SEC-11 | MEMBER가 질문 복사 시도 | 403 Forbidden |

### 4-3. 비회원 응답 정책

- PUBLIC 설문에 한해 비로그인 상태로 응답 가능
- 비회원 응답 시 `SurveyResponse.user = null`
- 비회원은 `(survey_id, user_id)` unique constraint 대상 아님 (user_id가 null)
- 중복 응답 방지: 브라우저 세션 또는 fingerprint 기반 (완벽하지 않음을 인정)

---

## 5. 관측 가능성 (Observability & Audit)

### 5-1. 감사 이력 (Audit Trail)

`SoftDeletableEntity`의 `createdBy`, `updatedBy`, `deletedBy` 필드로 기본 감사를 제공한다.

| 이벤트 | 저장소 | 기록 내용 |
|--------|--------|---------|
| 설문 생성 | `surveys.surveys_created_by` | 생성자 ID, 생성 시각 |
| 설문 수정 | `surveys.surveys_updated_by` | 수정자 ID, 수정 시각 |
| 설문 휴지통 이동 | `surveys.surveys_trashed_at` | 휴지통 이동 시각 |
| 설문 휴지통 복원 | `surveys.surveys_trashed_at = NULL` | 복원 시각 (`updatedAt`) |
| 설문 영구 삭제 | `surveys.surveys_deleted_by` | 삭제자 ID, 삭제 시각 |
| 설문 공개 | `surveys.surveys_visibility = PUBLISHED` | 공개 시각 (`updatedAt`) |
| 설문 비공개 전환 | `surveys.surveys_visibility = UNPUBLISHED` | 비공개 전환 시각 (`updatedAt`) |
| 응답 시작 | `surveys.surveys_response_status = OPEN` (NOT_STARTED → OPEN) | 최초 응답 시작 시각 (`updatedAt`) |
| 응답 재개 | `surveys.surveys_response_status = OPEN` (CLOSED → OPEN) | 응답 재개 시각 (`updatedAt`) |
| 응답 마감 | `surveys.surveys_response_status = CLOSED` | 응답 마감 시각 (`updatedAt`) |
| 응답 제출 | `survey_responses.survey_responses_created_by` | 응답자 ID, 제출 시각 |

### 5-2. 로그 메시지 (구현 시 추가)

| 서비스 | 시작 로그 | 완료 로그 | 실패 로그 |
|--------|---------|---------|---------|
| 설문 생성 | `설문 생성 요청: title` | `설문 생성 완료: surveyId` | - |
| 설문 공개 | `설문 공개 요청: surveyId` | `설문 공개 완료: surveyId` | `공개 실패: 질문 없음` |
| 설문 비공개 전환 | `설문 비공개 전환 요청: surveyId` | `설문 비공개 전환 완료: surveyId` | `비공개 전환 실패: 이미 비공개` |
| 응답 시작 | `응답 시작 요청: surveyId` | `응답 시작 완료: surveyId` | `응답 시작 실패: 비공개 설문` |
| 응답 마감 | `응답 마감 요청: surveyId` | `응답 마감 완료: surveyId` | `마감 실패: 상태 불일치` |
| 휴지통 이동 | `설문 휴지통 이동 요청: surveyId` | `설문 휴지통 이동 완료: surveyId` | - |
| 휴지통 복원 | `설문 복원 요청: surveyId` | `설문 복원 완료: surveyId` | - |
| 영구 삭제 | `설문 영구 삭제 요청: surveyId` | `설문 영구 삭제 완료: surveyId` | `영구 삭제 실패: 휴지통 상태 아님` |
| 응답 제출 | `응답 제출 요청: surveyId, userId` | `응답 제출 완료: responseId` | `제출 실패: 중복 응답` |
| 설문 복사 | `설문 복사 요청: sourceSurveyId` | `설문 복사 완료: newSurveyId` | `복사 실패: 원본 설문 없음` |
| 질문 복사 | `질문 복사 요청: surveyId, questionId` | `질문 복사 완료: newQuestionId` | `복사 실패: 질문 수 초과` |

---

## 6. 테스트 전략 (Test Strategy)

### 6-1. 테스트 레벨별 검증 범위

| 테스트 레벨 | 검증 대상 | 테스트 더블 |
|-----------|----------|-----------|
| 단위 테스트 | 엔티티 상태 전이, 입력 검증, 팩토리 메서드 | 없음 (실제 객체) |
| 서비스 통합 테스트 | CRUD 비즈니스 로직, 권한 검증, 중복 응답 방지 | 외부 서비스만 Mock (`TestExternalServiceConfig`) |
| 컨트롤러 통합 테스트 | HTTP 요청/응답, 인증/인가 | 실제 서비스 (full context) |

### 6-2. 불변조건 커버리지 (테스트 작성 후 업데이트)

| 불변조건 | 커버 테스트 | 상태 |
|---------|-----------|------|
| INV-01 (중복 응답 방지) | - | 미작성 |
| INV-02 (모든 상태 질문 수정 가능) | - | 미작성 |
| INV-03 (휴지통 2단계 삭제) | - | 미작성 |
| INV-04 (질문 수 1~50) | - | 미작성 |
| INV-05 (모든 상태 accessLevel 변경 가능) | - | 미작성 |
| INV-06 (그리드 최소 구성) | - | 미작성 |
| INV-07 (선형 배율 min < max) | - | 미작성 |
| INV-08 (마감일 자동 응답 마감) | - | 미작성 |
| INV-09 (PUBLISHED + OPEN에서만 응답 가능) | - | 미작성 |
| INV-10 (선택지/행 삭제 시 soft delete) | - | 미작성 |
| INV-11 (응답 재개 시 마감일 검증) | - | 미작성 |
| INV-12 (필수 질문 응답 누락 방지) | - | 미작성 |
| INV-13 (질문 유형별 필수 구성요소) | - | 미작성 |
| INV-14 (질문 삭제 시 soft delete) | - | 미작성 |
| INV-15 (응답 제출 중 설문 마감 경합) | - | 미작성 |
| INV-16 (휴지통 설문 응답 불가) | - | 미작성 |
| INV-17 (휴지통 설문 목록 제외) | - | 미작성 |
| INV-18 (영구 삭제는 휴지통에서만) | - | 미작성 |
| INV-19 (본인 응답 조회는 accessLevel 무관) | - | 미작성 |
| INV-20 (비공개 전환 시 자동 응답 마감) | - | 미작성 |
| INV-21 (설문 복사 시 상태 초기화) | - | 미작성 |
| INV-22 (복사 시 응답 데이터 제외) | - | 미작성 |
| INV-23 (복사 시 soft delete 요소 제외) | - | 미작성 |
| INV-24 (질문 복사 시 질문 수 제한 검증) | - | 미작성 |
| INV-25 (복사된 엔티티 새 ID 부여) | - | 미작성 |

### 6-3. 상태 전이 커버리지 (테스트 작성 후 업데이트)

**축 1: 공개 상태 (SurveyVisibility)**

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| UNPUBLISHED → PUBLISHED (공개) | - | 미작성 |
| PUBLISHED → UNPUBLISHED (비공개 전환) | - | 미작성 |
| UNPUBLISHED → PUBLISHED (재공개, U+C → P+C) | - | 미작성 |
| UNPUBLISHED에서 unpublish 시도 (금지) | - | 미작성 |
| PUBLISHED에서 publish 시도 (금지) | - | 미작성 |

**축 2: 응답 수집 상태 (SurveyResponseStatus)**

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| NOT_STARTED → OPEN (최초 응답 시작) | - | 미작성 |
| OPEN → CLOSED (수동 마감) | - | 미작성 |
| OPEN → CLOSED (마감일 경과 자동) | - | 미작성 |
| OPEN → CLOSED (unpublish 자동 마감, INV-20) | - | 미작성 |
| CLOSED → OPEN (응답 재개) | - | 미작성 |
| UNPUBLISHED에서 OPEN 전환 (금지) | - | 미작성 |
| NOT_STARTED → CLOSED (금지) | - | 미작성 |
| CLOSED → NOT_STARTED (금지) | - | 미작성 |
| OPEN → NOT_STARTED (금지) | - | 미작성 |
| CLOSED → OPEN (마감일 경과, 금지) | - | 미작성 |

### 6-4. 권한 검증 커버리지 (테스트 작성 후 업데이트)

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-01 (비운영진 설문 생성) | - | 미작성 |
| SEC-02 (비운영진 발행) | - | 미작성 |
| SEC-03 (비회원 ASSOCIATE 설문 응답) | - | 미작성 |
| SEC-04 (ASSOCIATE MEMBER 설문 응답) | - | 미작성 |
| SEC-05 (비운영진 결과 조회) | - | 미작성 |
| SEC-06 (비인가 부작용 없음) | - | 미작성 |
| SEC-07 (accessLevel 축소 후 본인 응답 조회) | - | 미작성 |
| SEC-08 (accessLevel 축소 후 타인 응답 조회 차단) | - | 미작성 |
| SEC-09 (비운영진 비공개 전환) | - | 미작성 |
| SEC-10 (비운영진 설문 복사) | - | 미작성 |
| SEC-11 (비운영진 질문 복사) | - | 미작성 |

---

## 관련 문서

- [#406 설문 기능 Epic](https://github.com/IGRUS-INHA/IGRUS-Web/issues/406) - 기능 스펙
- [#427 설문 도메인 모델 설계](https://github.com/IGRUS-INHA/IGRUS-Web/issues/427) - 엔티티 설계
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
