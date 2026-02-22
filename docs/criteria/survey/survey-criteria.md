# 설문 기능 검증 기준서

> **Status**: Draft
> **Last Updated**: 2026-02-22
> **Scope**: 설문 생성(Survey CRUD), 질문 관리(Question Management), 응답 제출(Response Submission), 결과 조회(Result View), 설문 상태 관리(Lifecycle)
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **Epic**: [#406 설문 기능](https://github.com/IGRUS-INHA/IGRUS-Web/issues/406)

## 목적

이 문서는 설문 도메인에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 도메인에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 설문 상태 제약, 질문 수 제한, 중복 응답 방지 등 핵심 비즈니스 규칙 |
| 2 | 상태 모델 | SurveyStatus FSM (DRAFT → PUBLISHED → CLOSED) |
| 3 | 입력 도메인 분할과 경계값 | 설문/질문/응답 입력값의 동치류와 경계값 |
| 4 | 권한/보안 정책 | RBAC (운영진 관리 vs 일반 회원 응답 vs 비회원 응답) |
| 5 | 관측 가능성 | 설문 라이프사이클 감사 로그 |
| 6 | 테스트 전략 | 테스트 레벨별 검증 항목 매핑 |

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

### INV-02: DRAFT 상태에서만 질문 구조 수정 가능

> PUBLISHED 또는 CLOSED 상태의 설문은 질문(추가/수정/삭제), 선택지, 그리드 행을 변경할 수 없다.

- **사전조건**: `survey.status == DRAFT`
- **위반 시**: 이미 수집된 응답과 질문 구조가 불일치하여 데이터 무결성 훼손
- **관련 코드**: `Survey.updateDraft()` — DRAFT 상태 검증 후 전체 수정 허용

### INV-03: DRAFT 상태에서만 삭제 가능

> PUBLISHED 또는 CLOSED 상태의 설문은 삭제할 수 없다.

- **사전조건**: `survey.isDraft() == true`
- **위반 시**: 응답 데이터 유실, 참조 무결성 훼손
- **관련 코드**: `Survey.isDraft()`, `SoftDeletableEntity.delete()`

### INV-04: 설문 질문 수 제한

> 설문에 포함된 질문은 최소 1개, 최대 50개이다.

- **적용 시점**: 설문 발행(DRAFT → PUBLISHED) 시 검증
- **경계값**: 0개 (발행 거부), 1개 (최소 유효), 50개 (최대 유효), 51개 (발행 거부)
- **위반 시**: 질문 없는 설문이 발행되거나, 과도한 질문 수로 응답 품질 저하

### INV-05: PUBLISHED 설문의 응답 권한 변경 불가

> PUBLISHED 상태에서는 `accessLevel`을 변경할 수 없다.

- **사전조건**: `survey.status == PUBLISHED`일 때 `updatePublished()`는 `accessLevel` 파라미터를 받지 않음
- **위반 시**: 이미 응답한 사용자와 새로운 권한 정책 간 불일치

### INV-06: 그리드 질문의 최소 구성

> `MULTIPLE_CHOICE_GRID`, `CHECKBOX_GRID` 유형의 질문은 최소 1개의 행(Row)과 1개의 열(Option)이 있어야 한다.

- **적용 시점**: 설문 발행 시 검증
- **위반 시**: 빈 그리드 질문이 응답자에게 노출

### INV-07: 선형 배율의 범위 유효성

> `LINEAR_SCALE` 유형의 질문에서 `scaleMin < scaleMax`이어야 한다.

- **관련 코드**: `SurveyQuestion.setScaleRange()` — min >= max 시 `IllegalArgumentException`
- **위반 시**: 응답 불가능한 척도 생성

### INV-08: 마감일 경과 시 자동 CLOSED 전환

> 마감일(`deadline`)이 설정된 PUBLISHED 설문은 마감일 경과 후 자동으로 CLOSED 상태가 된다.

- **사전조건**: `survey.status == PUBLISHED && survey.deadline != null && now > survey.deadline`
- **사후조건**: `survey.status == CLOSED`
- **구현 방식**: 스케줄러 또는 응답 시점 검증 (TBD)

### INV-09: CLOSED 설문은 응답 불가

> CLOSED 상태의 설문에는 새로운 응답을 제출할 수 없다.

- **사전조건**: 응답 제출 시 `survey.isPublished() == true` 검증
- **위반 시**: 마감된 설문에 응답이 추가되어 통계 오염

---

## 2. 상태 모델 (State Machine & Transitions)

### 2-1. 설문 상태 전이 (SurveyStatus FSM)

```
                    발행
┌─────────┐ ────────────────> ┌───────────┐
│  DRAFT  │                   │ PUBLISHED │
└─────────┘                   └─────┬─────┘
                                    │ 수동 마감 또는
                                    │ 마감일 경과
                                    ▼
                              ┌──────────┐
                              │  CLOSED  │  (종단 상태)
                              └──────────┘
```

**유효 전이**:

| 전이 | 트리거 | 사전조건 | 사후조건 | 관련 코드 |
|------|--------|---------|---------|----------|
| DRAFT → PUBLISHED | 운영진이 발행 | OPERATOR 이상 권한, 질문 1개 이상 | `status = PUBLISHED`, 응답 대상자에게 노출 | `Survey.publish()` |
| PUBLISHED → CLOSED | 수동 마감 | OPERATOR 이상 권한 | `status = CLOSED`, 응답 불가 | `Survey.close()` |
| PUBLISHED → CLOSED | 마감일 경과 | `deadline != null && now > deadline` | `status = CLOSED`, 자동 전환 | 스케줄러 (TBD) |

**금지된 전이 (Invalid Transition)**:

| 시도 | 예상 결과 | 이유 |
|------|----------|------|
| DRAFT → CLOSED | `IllegalStateException` | 발행을 거치지 않고 마감 불가 |
| PUBLISHED → DRAFT | `IllegalStateException` | 이미 응답이 수집되었을 수 있으므로 초안으로 되돌릴 수 없음 |
| CLOSED → DRAFT | `IllegalStateException` | 종단 상태에서 되돌릴 수 없음 |
| CLOSED → PUBLISHED | `IllegalStateException` | 종단 상태에서 재발행 불가 |

### 2-2. 설문 수정 가능 범위 (상태별)

| 필드 | DRAFT | PUBLISHED | CLOSED |
|------|:---:|:---:|:---:|
| 제목 (title) | O | O | X |
| 설명 (description) | O | O | X |
| 응답 권한 (accessLevel) | O | X | X |
| 마감일 (deadline) | O | O | X |
| 질문 구조 (추가/수정/삭제) | O | X | X |

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
| 설문 수정 (DRAFT) | 401 | 403 | 403 | **O** | **O** |
| 설문 수정 (PUBLISHED) | 401 | 403 | 403 | **O** | **O** |
| 설문 발행 | 401 | 403 | 403 | **O** | **O** |
| 설문 마감 | 401 | 403 | 403 | **O** | **O** |
| 설문 삭제 (DRAFT) | 401 | 403 | 403 | **O** | **O** |
| 설문 목록 조회 | 본인 권한에 해당하는 PUBLISHED만 | O | O | **O** (전체) | **O** (전체) |
| 설문 응답 (PUBLIC) | **O** | O | O | O | O |
| 설문 응답 (ASSOCIATE) | 401 | **O** | O | O | O |
| 설문 응답 (MEMBER) | 401 | 403 | **O** | O | O |
| 결과 조회 | 401 | 403 | 403 | **O** | **O** |

### 4-2. 권한 검증 체크리스트

| ID | 검증 항목 | 예상 결과 |
|----|----------|----------|
| SEC-01 | ASSOCIATE가 설문 생성 시도 | 403 Forbidden |
| SEC-02 | MEMBER가 설문 발행 시도 | 403 Forbidden |
| SEC-03 | 비회원이 ASSOCIATE 설문에 응답 시도 | 401 Unauthorized |
| SEC-04 | ASSOCIATE가 MEMBER 설문에 응답 시도 | 403 Forbidden |
| SEC-05 | MEMBER가 결과 조회 시도 | 403 Forbidden |
| SEC-06 | 비인가 접근이 상태를 변경하지 않는지 (부작용 없음) | DB 변경 없음 |

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
| 설문 삭제 | `surveys.surveys_deleted_by` | 삭제자 ID, 삭제 시각 |
| 설문 발행 | `surveys.surveys_status = PUBLISHED` | 상태 변경 시각 (`updatedAt`) |
| 설문 마감 | `surveys.surveys_status = CLOSED` | 상태 변경 시각 (`updatedAt`) |
| 응답 제출 | `survey_responses.survey_responses_created_by` | 응답자 ID, 제출 시각 |

### 5-2. 로그 메시지 (구현 시 추가)

| 서비스 | 시작 로그 | 완료 로그 | 실패 로그 |
|--------|---------|---------|---------|
| 설문 생성 | `설문 생성 요청: title` | `설문 생성 완료: surveyId` | - |
| 설문 발행 | `설문 발행 요청: surveyId` | `설문 발행 완료: surveyId` | `발행 실패: 질문 없음` |
| 설문 마감 | `설문 마감 요청: surveyId` | `설문 마감 완료: surveyId` | `마감 실패: 상태 불일치` |
| 응답 제출 | `응답 제출 요청: surveyId, userId` | `응답 제출 완료: responseId` | `제출 실패: 중복 응답` |

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
| INV-02 (DRAFT에서만 질문 수정) | - | 미작성 |
| INV-03 (DRAFT에서만 삭제) | - | 미작성 |
| INV-04 (질문 수 1~50) | - | 미작성 |
| INV-05 (PUBLISHED accessLevel 변경 불가) | - | 미작성 |
| INV-06 (그리드 최소 구성) | - | 미작성 |
| INV-07 (선형 배율 min < max) | - | 미작성 |
| INV-08 (마감일 자동 CLOSED) | - | 미작성 |
| INV-09 (CLOSED 응답 불가) | - | 미작성 |

### 6-3. 상태 전이 커버리지 (테스트 작성 후 업데이트)

| 전이 | 커버 테스트 | 상태 |
|------|-----------|------|
| DRAFT → PUBLISHED | - | 미작성 |
| PUBLISHED → CLOSED (수동) | - | 미작성 |
| PUBLISHED → CLOSED (자동) | - | 미작성 |
| DRAFT → CLOSED (금지) | - | 미작성 |
| PUBLISHED → DRAFT (금지) | - | 미작성 |
| CLOSED → DRAFT (금지) | - | 미작성 |
| CLOSED → PUBLISHED (금지) | - | 미작성 |

### 6-4. 권한 검증 커버리지 (테스트 작성 후 업데이트)

| 검증 | 커버 테스트 | 상태 |
|------|-----------|------|
| SEC-01 (비운영진 설문 생성) | - | 미작성 |
| SEC-02 (비운영진 발행) | - | 미작성 |
| SEC-03 (비회원 ASSOCIATE 설문 응답) | - | 미작성 |
| SEC-04 (ASSOCIATE MEMBER 설문 응답) | - | 미작성 |
| SEC-05 (비운영진 결과 조회) | - | 미작성 |
| SEC-06 (비인가 부작용 없음) | - | 미작성 |

---

## 관련 문서

- [#406 설문 기능 Epic](https://github.com/IGRUS-INHA/IGRUS-Web/issues/406) - 기능 스펙
- [#427 설문 도메인 모델 설계](https://github.com/IGRUS-INHA/IGRUS-Web/issues/427) - 엔티티 설계
- [QA Testing 관련 용어 정리 (Wiki)](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC) - 용어 및 개념 참조
