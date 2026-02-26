# 설문(Survey) 도메인 테스트 케이스

**작성일**: 2026-02-25
**버전**: 1.1
**관련 스펙**: [설문 검증 기준서](../../criteria/survey/survey-criteria-v1.md)
**우선순위**: P0

> **2축 모델 기반**: 이 문서는 설문의 2축 상태 모델(SurveyVisibility + SurveyResponseStatus)과 휴지통 상태를 기준으로 테스트 케이스를 기술한다.
> - ✅ 구현 완료 / 테스트 통과
> - ⬜ 미구현 / 검토 필요

---

## 1. 개요

설문(Survey) 도메인의 테스트 케이스이다. 설문 엔티티의 생성, 2축 상태 모델(SurveyVisibility + SurveyResponseStatus), 휴지통 행동, 설문 수정, 응답 수락 조건, 설문 복사, 질문/선택지/행 엔티티, SurveyService CRUD, SurveyQuestionService를 검증한다.

---

## 2. 설문 엔티티 테스트 (SRV-xxx)

### 2.1 설문 생성 (Survey Creation)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-001 | 유효한 입력으로 설문 생성 성공 | 유효한 title, description, accessLevel | `Survey.create()` 호출 | 설문 객체 생성 성공 | ✅ |
| SRV-002 | 생성 시 초기 상태 검증 | 유효한 입력 | `Survey.create()` 호출 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED, trashedAt=null, deleted=false | ✅ |
| SRV-003 | 제목 1자 (최소 경계값) 생성 성공 | title="A" (1자) | `Survey.create()` 호출 | 성공, title="A" | ⬜ |
| SRV-004 | 제목 100자 (최대 경계값) 생성 성공 | title=100자 문자열 | `Survey.create()` 호출 | 성공, title=100자 | ⬜ |
| SRV-005 | 제목 101자 (초과) 생성 거부 | title=101자 문자열 | `Survey.create()` 호출 | 예외 발생 (제목 길이 초과) | ⬜ |
| SRV-006 | 제목 null 생성 거부 | title=null | `Survey.create()` 호출 | 예외 발생 (제목 필수) | ⬜ |
| SRV-007 | 제목 빈 문자열 생성 거부 | title="" | `Survey.create()` 호출 | 예외 발생 (제목 필수) | ⬜ |
| SRV-008 | 제목 공백만 있는 문자열 생성 거부 | title="   " | `Survey.create()` 호출 | 예외 발생 (제목 필수) | ⬜ |
| SRV-009 | 설명 null 생성 성공 (선택 필드) | description=null | `Survey.create()` 호출 | 성공, description=null | ✅ |
| SRV-010 | 설명 500자 (최대 경계값) 생성 성공 | description=500자 문자열 | `Survey.create()` 호출 | 성공, description=500자 | ⬜ |
| SRV-011 | 설명 501자 (초과) 생성 거부 | description=501자 문자열 | `Survey.create()` 호출 | 예외 발생 (설명 길이 초과) | ⬜ |
| SRV-012 | accessLevel PUBLIC 생성 성공 | accessLevel=PUBLIC | `Survey.create()` 호출 | 성공, accessLevel=PUBLIC | ✅ |
| SRV-013 | accessLevel ASSOCIATE 생성 성공 | accessLevel=ASSOCIATE | `Survey.create()` 호출 | 성공, accessLevel=ASSOCIATE | ✅ |
| SRV-014 | accessLevel MEMBER 생성 성공 | accessLevel=MEMBER | `Survey.create()` 호출 | 성공, accessLevel=MEMBER | ✅ |
| SRV-015 | accessLevel null 생성 거부 | accessLevel=null | `Survey.create()` 호출 | 예외 발생 (accessLevel 필수) | ⬜ |
| SRV-016 | deadline null (미설정) 생성 성공 | deadline=null | `Survey.create()` 호출 | 성공, deadline=null | ✅ |
| SRV-017 | deadline 미래 시점 생성 성공 | deadline=미래 Instant | `Survey.create()` 호출 | 성공, deadline=미래 시점 | ✅ |

### 2.2 공개 상태 전이 (SurveyVisibility) -- INV-04, INV-13 연관

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-020 | UNPUBLISHED -> PUBLISHED (publish 성공) | visibility=UNPUBLISHED, 질문 1개 이상, INV-13 충족 | `survey.publish()` 호출 | visibility=PUBLISHED | ✅ |
| SRV-021 | PUBLISHED -> UNPUBLISHED (unpublish 성공) | visibility=PUBLISHED | `survey.unpublish()` 호출 | visibility=UNPUBLISHED | ✅ |
| SRV-022 | 이미 PUBLISHED에서 publish 시도 -> 에러 | visibility=PUBLISHED | `survey.publish()` 호출 | 예외 발생 (이미 공개 상태) | ✅ |
| SRV-023 | 이미 UNPUBLISHED에서 unpublish 시도 -> 에러 | visibility=UNPUBLISHED | `survey.unpublish()` 호출 | 예외 발생 (이미 비공개 상태) | ✅ |
| SRV-024 | U+C -> publish -> P+C (재공개, 응답 데이터 보존) | visibility=UNPUBLISHED, responseStatus=CLOSED, 질문 1개 이상 | `survey.publish()` 호출 | visibility=PUBLISHED, responseStatus=CLOSED 유지, 기존 응답 데이터 보존 | ⬜ |
| SRV-025 | P+NS -> unpublish -> U+NS | visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.unpublish()` 호출 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED 유지 | ✅ |
| SRV-026 | 질문 0개에서 publish 시도 -> 에러 (INV-04) | visibility=UNPUBLISHED, 질문 0개 | `survey.publish()` 호출 | 예외 발생 (질문 최소 1개 필요) | ⬜ |
| SRV-027 | 질문 1개 (최소) publish 성공 (INV-04 경계값) | visibility=UNPUBLISHED, 질문 1개 | `survey.publish()` 호출 | visibility=PUBLISHED | ⬜ |
| SRV-028 | 질문 50개 (최대) publish 성공 (INV-04 경계값) | visibility=UNPUBLISHED, 질문 50개 | `survey.publish()` 호출 | visibility=PUBLISHED | ⬜ |
| SRV-029 | 재공개 시에도 INV-04, INV-13 재검증 | U+C, 질문 구성 변경 후 INV-13 위반 | `survey.publish()` 호출 | 예외 발생 (필수 구성요소 미충족) | ⬜ |

### 2.3 응답 수집 상태 전이 (SurveyResponseStatus)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-030 | NOT_STARTED -> OPEN (openResponse 성공) | visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.openResponse()` 호출 | responseStatus=OPEN | ✅ |
| SRV-031 | OPEN -> CLOSED (closeResponse 성공) | responseStatus=OPEN | `survey.closeResponse()` 호출 | responseStatus=CLOSED | ✅ |
| SRV-032 | CLOSED -> OPEN (응답 재개 성공) | visibility=PUBLISHED, responseStatus=CLOSED | `survey.openResponse()` 호출 | responseStatus=OPEN | ✅ |
| SRV-033 | NOT_STARTED -> CLOSED 시도 -> 에러 (금지된 전이) | responseStatus=NOT_STARTED | `survey.closeResponse()` 호출 | 예외 발생 (NOT_STARTED에서 CLOSED 전이 불가) | ✅ |
| SRV-034 | CLOSED -> NOT_STARTED 시도 -> 에러 (금지된 전이) | responseStatus=CLOSED | NOT_STARTED로 전이 시도 | 예외 발생 (CLOSED에서 NOT_STARTED 전이 불가) | ⬜ |
| SRV-035 | OPEN -> NOT_STARTED 시도 -> 에러 (금지된 전이) | responseStatus=OPEN | NOT_STARTED로 전이 시도 | 예외 발생 (OPEN에서 NOT_STARTED 전이 불가) | ⬜ |
| SRV-036 | UNPUBLISHED에서 openResponse 시도 -> 에러 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED | `survey.openResponse()` 호출 | 예외 발생 (비공개 설문에서 응답 시작 불가) | ✅ |
| SRV-037 | 이미 OPEN에서 openResponse 시도 -> 에러 | responseStatus=OPEN | `survey.openResponse()` 호출 | 예외 발생 (이미 응답 수집 중) | ✅ |
| SRV-038 | 이미 CLOSED에서 closeResponse 시도 -> 에러 | responseStatus=CLOSED | `survey.closeResponse()` 호출 | 예외 발생 (이미 응답 마감) | ✅ |

### 2.4 비공개 전환 시 자동 응답 마감 (INV-20)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-040 | P+O -> unpublish -> U+C (자동 마감, INV-20) | visibility=PUBLISHED, responseStatus=OPEN | `survey.unpublish()` 호출 | visibility=UNPUBLISHED, responseStatus=CLOSED (자동 마감) | ✅ |
| SRV-041 | P+C -> unpublish -> U+C (이미 CLOSED, 변경 없음) | visibility=PUBLISHED, responseStatus=CLOSED | `survey.unpublish()` 호출 | visibility=UNPUBLISHED, responseStatus=CLOSED 유지 | ✅ |
| SRV-042 | P+NS -> unpublish -> U+NS (NOT_STARTED 유지) | visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.unpublish()` 호출 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED 유지 | ✅ |

### 2.5 publishAndOpen 복합 전이

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-045 | U+NS -> publishAndOpen -> P+O 성공 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED, 질문 1개 이상, INV-13 충족 | `survey.publishAndOpen()` 호출 | visibility=PUBLISHED, responseStatus=OPEN | ✅ |
| SRV-046 | U+C -> publishAndOpen -> P+O 성공 | visibility=UNPUBLISHED, responseStatus=CLOSED, 질문 1개 이상, INV-13 충족 | `survey.publishAndOpen()` 호출 | visibility=PUBLISHED, responseStatus=OPEN | ⬜ |
| SRV-047 | P+NS -> publishAndOpen -> 에러 (이미 공개) | visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.publishAndOpen()` 호출 | 예외 발생 (이미 공개 상태) | ✅ |
| SRV-048 | P+O -> publishAndOpen -> 에러 (이미 공개) | visibility=PUBLISHED, responseStatus=OPEN | `survey.publishAndOpen()` 호출 | 예외 발생 (이미 공개 상태) | ✅ |
| SRV-049 | P+C -> publishAndOpen -> 에러 (이미 공개) | visibility=PUBLISHED, responseStatus=CLOSED | `survey.publishAndOpen()` 호출 | 예외 발생 (이미 공개 상태) | ✅ |

### 2.6 마감일 조건 (openResponse / publishAndOpen 마감일) -- INV-11 연관

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-050 | P+NS, deadline=null -> openResponse 성공 (D1) | visibility=PUBLISHED, responseStatus=NOT_STARTED, deadline=null | `survey.openResponse()` 호출 | responseStatus=OPEN | ✅ |
| SRV-051 | P+NS, deadline=미래 -> openResponse 성공 (D2) | visibility=PUBLISHED, responseStatus=NOT_STARTED, deadline=미래 Instant | `survey.openResponse()` 호출 | responseStatus=OPEN | ✅ |
| SRV-052 | P+NS, deadline=과거 -> openResponse 거부 (D3) | visibility=PUBLISHED, responseStatus=NOT_STARTED, deadline=과거 Instant | `survey.openResponse()` 호출 | 예외 발생 (마감일 경과) | ✅ |
| SRV-053 | P+C, deadline=null -> openResponse(재개) 성공 (D4) | visibility=PUBLISHED, responseStatus=CLOSED, deadline=null | `survey.openResponse()` 호출 | responseStatus=OPEN | ✅ |
| SRV-054 | P+C, deadline=미래 -> openResponse(재개) 성공 (D5) | visibility=PUBLISHED, responseStatus=CLOSED, deadline=미래 Instant | `survey.openResponse()` 호출 | responseStatus=OPEN | ⬜ |
| SRV-055 | P+C, deadline=과거 -> openResponse(재개) 거부 (D6, INV-11) | visibility=PUBLISHED, responseStatus=CLOSED, deadline=과거 Instant | `survey.openResponse()` 호출 | 예외 발생 (마감일 경과, 재개 불가) | ✅ |

### 2.7 휴지통 행동 (INV-03, INV-16, INV-17, INV-18)

#### 휴지통 이동 (활성 -> 휴지통)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-060 | U+NS+active -> trash 성공 (T1) | visibility=UNPUBLISHED, responseStatus=NOT_STARTED, trashedAt=null | `survey.trash()` 호출 | trashedAt != null, visibility/responseStatus 변경 없음 | ✅ |
| SRV-061 | P+NS+active -> trash 성공 (T2) | visibility=PUBLISHED, responseStatus=NOT_STARTED, trashedAt=null | `survey.trash()` 호출 | trashedAt != null | ✅ |
| SRV-062 | P+O+active -> trash 성공 (T3) | visibility=PUBLISHED, responseStatus=OPEN, trashedAt=null | `survey.trash()` 호출 | trashedAt != null | ✅ |
| SRV-063 | P+C+active -> trash 성공 (T4) | visibility=PUBLISHED, responseStatus=CLOSED, trashedAt=null | `survey.trash()` 호출 | trashedAt != null | ✅ |
| SRV-064 | U+C+active -> trash 성공 (T5) | visibility=UNPUBLISHED, responseStatus=CLOSED, trashedAt=null | `survey.trash()` 호출 | trashedAt != null | ⬜ |

#### 이미 휴지통 -> trash 시도 (에러)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-065 | U+NS+trashed -> trash 시도 -> 에러 (이미 휴지통) | trashedAt != null, visibility=UNPUBLISHED, responseStatus=NOT_STARTED | `survey.trash()` 호출 | 예외 발생 (이미 휴지통 상태) | ✅ |
| SRV-066 | P+NS+trashed -> trash 시도 -> 에러 | trashedAt != null, visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.trash()` 호출 | 예외 발생 (이미 휴지통 상태) | ⬜ |
| SRV-067 | P+O+trashed -> trash 시도 -> 에러 | trashedAt != null, visibility=PUBLISHED, responseStatus=OPEN | `survey.trash()` 호출 | 예외 발생 (이미 휴지통 상태) | ⬜ |
| SRV-068 | P+C+trashed -> trash 시도 -> 에러 | trashedAt != null, visibility=PUBLISHED, responseStatus=CLOSED | `survey.trash()` 호출 | 예외 발생 (이미 휴지통 상태) | ⬜ |
| SRV-069 | U+C+trashed -> trash 시도 -> 에러 | trashedAt != null, visibility=UNPUBLISHED, responseStatus=CLOSED | `survey.trash()` 호출 | 예외 발생 (이미 휴지통 상태) | ⬜ |

#### 휴지통 복원 (휴지통 -> 활성)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-070 | U+NS+trashed -> restore 성공 (원래 상태 유지) (T6) | trashedAt != null, visibility=UNPUBLISHED, responseStatus=NOT_STARTED | `survey.restoreFromTrash()` 호출 | trashedAt=null, visibility=UNPUBLISHED, responseStatus=NOT_STARTED 유지 | ✅ |
| SRV-071 | P+NS+trashed -> restore 성공 (T7) | trashedAt != null, visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.restoreFromTrash()` 호출 | trashedAt=null, visibility=PUBLISHED, responseStatus=NOT_STARTED 유지 | ⬜ |
| SRV-072 | P+O+trashed -> restore 성공 (T8) | trashedAt != null, visibility=PUBLISHED, responseStatus=OPEN | `survey.restoreFromTrash()` 호출 | trashedAt=null, visibility=PUBLISHED, responseStatus=OPEN 유지 | ⬜ |
| SRV-073 | P+C+trashed -> restore 성공 (T9) | trashedAt != null, visibility=PUBLISHED, responseStatus=CLOSED | `survey.restoreFromTrash()` 호출 | trashedAt=null, visibility=PUBLISHED, responseStatus=CLOSED 유지 | ⬜ |
| SRV-074 | U+C+trashed -> restore 성공 (T10) | trashedAt != null, visibility=UNPUBLISHED, responseStatus=CLOSED | `survey.restoreFromTrash()` 호출 | trashedAt=null, visibility=UNPUBLISHED, responseStatus=CLOSED 유지 | ⬜ |

#### 영구 삭제 (휴지통 -> deleted)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-075 | U+NS+trashed -> permanentDelete 성공 (T6) | trashedAt != null, visibility=UNPUBLISHED, responseStatus=NOT_STARTED | `survey.permanentDelete()` 호출 | deleted=true | ✅ |
| SRV-076 | P+NS+trashed -> permanentDelete 성공 (T7) | trashedAt != null, visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.permanentDelete()` 호출 | deleted=true | ⬜ |
| SRV-077 | P+O+trashed -> permanentDelete 성공 (T8) | trashedAt != null, visibility=PUBLISHED, responseStatus=OPEN | `survey.permanentDelete()` 호출 | deleted=true | ⬜ |
| SRV-078 | P+C+trashed -> permanentDelete 성공 (T9) | trashedAt != null, visibility=PUBLISHED, responseStatus=CLOSED | `survey.permanentDelete()` 호출 | deleted=true | ⬜ |
| SRV-079 | U+C+trashed -> permanentDelete 성공 (T10) | trashedAt != null, visibility=UNPUBLISHED, responseStatus=CLOSED | `survey.permanentDelete()` 호출 | deleted=true | ⬜ |

#### 활성 상태에서 복원/영구 삭제 시도 (에러)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-080 | active 상태에서 restore 시도 -> 에러 (휴지통 아님) | trashedAt=null | `survey.restoreFromTrash()` 호출 | 예외 발생 (휴지통 상태가 아님) | ✅ |
| SRV-081 | active 상태에서 permanentDelete 시도 -> 에러 (INV-18) | trashedAt=null | `survey.permanentDelete()` 호출 | 예외 발생 (활성 설문 영구 삭제 불가, INV-18 위반) | ✅ |

### 2.8 설문 수정 (INV-02, INV-05)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-085 | U+NS에서 update 성공 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED | `survey.update(title, description, accessLevel, deadline)` 호출 | 수정 성공, 모든 필드 반영 | ✅ |
| SRV-086 | P+NS에서 update 성공 | visibility=PUBLISHED, responseStatus=NOT_STARTED | `survey.update(...)` 호출 | 수정 성공 | ✅ |
| SRV-087 | P+O에서 update 성공 | visibility=PUBLISHED, responseStatus=OPEN | `survey.update(...)` 호출 | 수정 성공 (INV-02) | ✅ |
| SRV-088 | P+C에서 update 성공 | visibility=PUBLISHED, responseStatus=CLOSED | `survey.update(...)` 호출 | 수정 성공 | ✅ |
| SRV-089 | U+C에서 update 성공 | visibility=UNPUBLISHED, responseStatus=CLOSED | `survey.update(...)` 호출 | 수정 성공 | ⬜ |
| SRV-090 | 휴지통 상태에서 update 성공 | trashedAt != null | `survey.update(...)` 호출 | 수정 성공 (모든 상태에서 수정 가능) | ⬜ |
| SRV-091 | accessLevel 변경 성공 (INV-05) | 임의 상태, accessLevel=PUBLIC | `survey.update(..., accessLevel=MEMBER)` 호출 | accessLevel=MEMBER로 변경 성공 (INV-05) | ✅ |

### 2.9 응답 수락 조건 (INV-09, INV-16)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-095 | P+O+active -> 응답 수락 (isAcceptingResponses=true) | visibility=PUBLISHED, responseStatus=OPEN, trashedAt=null | `survey.isAcceptingResponses()` 호출 | true | ✅ |
| SRV-096 | U+NS+active -> 응답 거부 | visibility=UNPUBLISHED, responseStatus=NOT_STARTED, trashedAt=null | `survey.isAcceptingResponses()` 호출 | false (UNPUBLISHED + NOT_STARTED) | ✅ |
| SRV-097 | P+NS+active -> 응답 거부 (NOT_STARTED) | visibility=PUBLISHED, responseStatus=NOT_STARTED, trashedAt=null | `survey.isAcceptingResponses()` 호출 | false (NOT_STARTED) | ✅ |
| SRV-098 | P+C+active -> 응답 거부 (CLOSED) | visibility=PUBLISHED, responseStatus=CLOSED, trashedAt=null | `survey.isAcceptingResponses()` 호출 | false (CLOSED) | ✅ |
| SRV-099 | U+C+active -> 응답 거부 (UNPUBLISHED) | visibility=UNPUBLISHED, responseStatus=CLOSED, trashedAt=null | `survey.isAcceptingResponses()` 호출 | false (UNPUBLISHED) | ⬜ |
| SRV-100 | P+O+trashed -> 응답 거부 (INV-16, 휴지통) | visibility=PUBLISHED, responseStatus=OPEN, trashedAt != null | `survey.isAcceptingResponses()` 호출 | false (휴지통 설문, INV-16) | ✅ |

### 2.10 설문 복사 (INV-21~25)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SRV-105 | 활성 설문 복사 성공 -> 새 설문 (U,NS,active) 상태 (INV-21) | trashedAt=null, deleted=false | `Survey.copyFrom(original)` 호출 | 새 설문 생성, visibility=UNPUBLISHED, responseStatus=NOT_STARTED, trashedAt=null | ⬜ |
| SRV-106 | 복사본 제목에 " (복사본)" 접미사 붙음 | 원본 title="설문 제목" | `Survey.copyFrom(original)` 호출 | 복사본 title="설문 제목 (복사본)" | ⬜ |
| SRV-107 | 복사본 description, accessLevel 복사됨 | 원본 description="설명", accessLevel=MEMBER | `Survey.copyFrom(original)` 호출 | 복사본 description="설명", accessLevel=MEMBER | ⬜ |
| SRV-108 | 복사본 deadline=null 초기화 (INV-21) | 원본 deadline=미래 Instant | `Survey.copyFrom(original)` 호출 | 복사본 deadline=null (원본 마감일 복사 안 됨) | ⬜ |
| SRV-109 | 복사본 visibility=UNPUBLISHED, responseStatus=NOT_STARTED | 원본 visibility=PUBLISHED, responseStatus=OPEN | `Survey.copyFrom(original)` 호출 | 복사본 visibility=UNPUBLISHED, responseStatus=NOT_STARTED | ⬜ |
| SRV-110 | 복사 시 질문/선택지/행 복사됨 | 원본에 질문 3개 (선택지, 행 포함) | `Survey.copyFrom(original)` 호출 | 복사본에 동일 구조의 질문 3개 복사됨 | ⬜ |
| SRV-111 | 복사 시 응답 데이터 제외 (INV-22) | 원본에 응답 데이터 존재 | `Survey.copyFrom(original)` 호출 | 복사본에 응답 0건 | ⬜ |
| SRV-112 | 복사 시 soft delete된 질문 제외 (INV-23) | 원본에 deleted=true 질문 포함 | `Survey.copyFrom(original)` 호출 | 복사본에 삭제된 질문 미포함 | ⬜ |
| SRV-113 | 복사 시 soft delete된 선택지 제외 (INV-23) | 원본 질문에 deleted=true 선택지 포함 | `Survey.copyFrom(original)` 호출 | 복사본에 삭제된 선택지 미포함 | ⬜ |
| SRV-114 | 복사 시 soft delete된 행 제외 (INV-23) | 원본 질문에 deleted=true 행 포함 | `Survey.copyFrom(original)` 호출 | 복사본에 삭제된 행 미포함 | ⬜ |
| SRV-115 | 복사된 엔티티 새 ID 부여 (INV-25) | 원본 설문/질문/선택지/행에 기존 ID 존재 | `Survey.copyFrom(original)` 후 persist | 복사본 설문/질문/선택지/행 모두 새로운 ID 부여, 원본 ID와 불일치 | ⬜ |
| SRV-116 | 휴지통 설문 복사 시도 -> 거부 | trashedAt != null | `Survey.copyFrom(original)` 호출 | 예외 발생 (휴지통 설문 복사 불가) | ⬜ |
| SRV-117 | deleted 설문 복사 시도 -> 거부 | deleted=true | `Survey.copyFrom(original)` 호출 | 예외 발생 (영구 삭제된 설문 복사 불가) | ⬜ |

---

## 3. 질문 엔티티 테스트 (QST-xxx)

### 3.1 질문 생성 (11가지 유형)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-001 | SHORT_ANSWER 유형 질문 생성 성공 | questionType=SHORT_ANSWER, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=SHORT_ANSWER | ✅ |
| QST-002 | PARAGRAPH 유형 질문 생성 성공 | questionType=PARAGRAPH, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=PARAGRAPH | ✅ |
| QST-003 | MULTIPLE_CHOICE 유형 질문 생성 성공 | questionType=MULTIPLE_CHOICE, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=MULTIPLE_CHOICE | ✅ |
| QST-004 | CHECKBOX 유형 질문 생성 성공 | questionType=CHECKBOX, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=CHECKBOX | ✅ |
| QST-005 | DROPDOWN 유형 질문 생성 성공 | questionType=DROPDOWN, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=DROPDOWN | ✅ |
| QST-006 | LINEAR_SCALE 유형 질문 생성 성공 | questionType=LINEAR_SCALE, 유효한 title, scaleMin=1, scaleMax=5 | `SurveyQuestion.create()` 호출 | 성공, questionType=LINEAR_SCALE | ✅ |
| QST-007 | MULTIPLE_CHOICE_GRID 유형 질문 생성 성공 | questionType=MULTIPLE_CHOICE_GRID, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=MULTIPLE_CHOICE_GRID | ✅ |
| QST-008 | CHECKBOX_GRID 유형 질문 생성 성공 | questionType=CHECKBOX_GRID, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=CHECKBOX_GRID | ✅ |
| QST-009 | DATE 유형 질문 생성 성공 | questionType=DATE, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=DATE | ✅ |
| QST-010 | TIME 유형 질문 생성 성공 | questionType=TIME, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=TIME | ✅ |
| QST-011 | FILE_UPLOAD 유형 질문 생성 성공 | questionType=FILE_UPLOAD, 유효한 title | `SurveyQuestion.create()` 호출 | 성공, questionType=FILE_UPLOAD | ✅ |

### 3.2 질문 입력값 경계값 (criteria 3-2)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-015 | 질문 제목 1자 (최소) 성공 | title="A" (1자) | `SurveyQuestion.create()` 호출 | 성공, title="A" | ✅ |
| QST-016 | 질문 제목 200자 (최대) 성공 | title=200자 문자열 | `SurveyQuestion.create()` 호출 | 성공, title=200자 | ✅ |
| QST-017 | 질문 제목 201자 (초과) 거부 | title=201자 문자열 | `SurveyQuestion.create()` 호출 | 예외 발생 (제목 길이 초과) | ⬜ |
| QST-018 | 질문 제목 null 거부 | title=null | `SurveyQuestion.create()` 호출 | 예외 발생 (제목 필수) | ⬜ |
| QST-019 | 질문 제목 빈 문자열 거부 | title="" | `SurveyQuestion.create()` 호출 | 예외 발생 (제목 필수) | ⬜ |
| QST-020 | 질문 설명 null 성공 | description=null | `SurveyQuestion.create()` 호출 | 성공, description=null | ✅ |
| QST-021 | 질문 설명 500자 (최대) 성공 | description=500자 문자열 | `SurveyQuestion.create()` 호출 | 성공, description=500자 | ✅ |
| QST-022 | 질문 설명 501자 (초과) 거부 | description=501자 문자열 | `SurveyQuestion.create()` 호출 | 예외 발생 (설명 길이 초과) | ⬜ |
| QST-023 | required=true 성공 | required=true | `SurveyQuestion.create()` 호출 | 성공, required=true | ✅ |
| QST-024 | required=false 성공 | required=false | `SurveyQuestion.create()` 호출 | 성공, required=false | ✅ |
| QST-025 | displayOrder=0 (최소) 성공 | displayOrder=0 | `SurveyQuestion.create()` 호출 | 성공, displayOrder=0 | ✅ |

### 3.3 질문 유형별 필수 구성요소 (INV-13)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-030 | MULTIPLE_CHOICE 선택지 0개 -> 발행 시 검증 실패 | MULTIPLE_CHOICE 질문, 선택지 0개 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (선택지 1개 이상 필요) | ✅ |
| QST-031 | MULTIPLE_CHOICE 선택지 1개 이상 -> 발행 검증 성공 | MULTIPLE_CHOICE 질문, 선택지 2개 | 설문 발행(`publish()`) 시 검증 | 검증 통과 | ✅ |
| QST-032 | CHECKBOX 선택지 0개 -> 발행 시 검증 실패 | CHECKBOX 질문, 선택지 0개 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (선택지 1개 이상 필요) | ⬜ |
| QST-033 | DROPDOWN 선택지 0개 -> 발행 시 검증 실패 | DROPDOWN 질문, 선택지 0개 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (선택지 1개 이상 필요) | ⬜ |
| QST-034 | MULTIPLE_CHOICE_GRID 행 0개 -> 발행 검증 실패 (INV-06) | MULTIPLE_CHOICE_GRID 질문, 행 0개, 열 1개 이상 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (행 1개 이상 필요, INV-06) | ✅ |
| QST-035 | MULTIPLE_CHOICE_GRID 열 0개 -> 발행 검증 실패 (INV-06) | MULTIPLE_CHOICE_GRID 질문, 행 1개 이상, 열 0개 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (열 1개 이상 필요, INV-06) | ✅ |
| QST-036 | MULTIPLE_CHOICE_GRID 행 1개 + 열 1개 -> 발행 검증 성공 | MULTIPLE_CHOICE_GRID 질문, 행 1개, 열 1개 | 설문 발행(`publish()`) 시 검증 | 검증 통과 (최소 유효 구성) | ✅ |
| QST-037 | CHECKBOX_GRID 행 0개 -> 발행 검증 실패 | CHECKBOX_GRID 질문, 행 0개, 열 1개 이상 | 설문 발행(`publish()`) 시 검증 | 예외 발생 (행 1개 이상 필요) | ✅ |
| QST-038 | CHECKBOX_GRID 행 1개 + 열 1개 -> 발행 검증 성공 | CHECKBOX_GRID 질문, 행 1개, 열 1개 | 설문 발행(`publish()`) 시 검증 | 검증 통과 (최소 유효 구성) | ✅ |

### 3.4 선형 배율 범위 (INV-07)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-040 | scaleMin=1, scaleMax=5 -> 유효 | LINEAR_SCALE 질문, scaleMin=1, scaleMax=5 | `question.setScaleRange(1, 5)` 호출 | 성공, scaleMin=1, scaleMax=5 | ✅ |
| QST-041 | scaleMin=1, scaleMax=2 -> 유효 (최소 범위) | LINEAR_SCALE 질문, scaleMin=1, scaleMax=2 | `question.setScaleRange(1, 2)` 호출 | 성공 (최소 유효 범위, min < max) | ✅ |
| QST-042 | scaleMin=scaleMax -> 에러 | LINEAR_SCALE 질문, scaleMin=3, scaleMax=3 | `question.setScaleRange(3, 3)` 호출 | 예외 발생 (min == max 불가, INV-07) | ✅ |
| QST-043 | scaleMin > scaleMax -> 에러 | LINEAR_SCALE 질문, scaleMin=5, scaleMax=1 | `question.setScaleRange(5, 1)` 호출 | 예외 발생 (min > max 불가, INV-07) | ✅ |

### 3.5 질문 수 제한 (INV-04)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-045 | 질문 0개에서 발행 시도 -> 거부 | 설문에 활성 질문 0개 | `survey.publish()` 호출 | 예외 발생 (최소 1개 질문 필요, INV-04) | ⬜ |
| QST-046 | 질문 1개에서 발행 -> 성공 (최소) | 설문에 활성 질문 1개 | `survey.publish()` 호출 | 성공 (경계값 최소) | ⬜ |
| QST-047 | 질문 50개에서 발행 -> 성공 (최대) | 설문에 활성 질문 50개 | `survey.publish()` 호출 | 성공 (경계값 최대) | ⬜ |
| QST-048 | 질문 51개 추가 시도 -> 거부 (SURVEY_QUESTION_LIMIT_EXCEEDED) | 설문에 활성 질문 50개 | 51번째 질문 추가 시도 | 예외 발생 (SURVEY_QUESTION_LIMIT_EXCEEDED) | ⬜ |

### 3.6 선택지/행 입력값 경계값 (criteria 3-3)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-050 | 선택지 text 1자 (최소) 성공 | text="A" (1자) | `SurveyQuestionOption.create()` 호출 | 성공, text="A" | ✅ |
| QST-051 | 선택지 text 200자 (최대) 성공 | text=200자 문자열 | `SurveyQuestionOption.create()` 호출 | 성공, text=200자 | ✅ |
| QST-052 | 선택지 text 201자 (초과) 거부 | text=201자 문자열 | `SurveyQuestionOption.create()` 호출 | 예외 발생 (텍스트 길이 초과) | ⬜ |
| QST-053 | 선택지 text null 거부 | text=null | `SurveyQuestionOption.create()` 호출 | 예외 발생 (텍스트 필수) | ⬜ |
| QST-054 | 행 label 1자 (최소) 성공 | label="A" (1자) | `SurveyQuestionRow.create()` 호출 | 성공, label="A" | ✅ |
| QST-055 | 행 label 200자 (최대) 성공 | label=200자 문자열 | `SurveyQuestionRow.create()` 호출 | 성공, label=200자 | ✅ |
| QST-056 | 행 label 201자 (초과) 거부 | label=201자 문자열 | `SurveyQuestionRow.create()` 호출 | 예외 발생 (라벨 길이 초과) | ⬜ |

### 3.7 Soft Delete (INV-10, INV-14)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-060 | 질문 soft delete 후 설문 폼에서 숨겨짐 | 질문 존재, 응답 있음 | `question.delete()` 호출 후 설문 폼 조회 | 삭제된 질문이 폼에서 미노출 (INV-14) | ✅ |
| QST-061 | 질문 soft delete 후 기존 SurveyAnswer FK 유지 (INV-14) | 질문에 대한 응답(SurveyAnswer) 존재 | `question.delete()` 호출 | deleted=true, 기존 SurveyAnswer의 question FK 유효 | ⬜ |
| QST-062 | 선택지 soft delete 후 기존 응답 FK 유지 (INV-10) | 선택지에 대한 응답 존재 | `option.delete()` 호출 | deleted=true, 기존 응답의 option FK 유효 | ✅ |
| QST-063 | 행 soft delete 후 기존 응답 FK 유지 (INV-10) | 행에 대한 응답 존재 | `row.delete()` 호출 | deleted=true, 기존 응답의 row FK 유효 | ✅ |
| QST-064 | 삭제된 질문의 답변 결과 조회 시 확인 가능 | 질문 soft delete됨, 해당 질문에 응답 데이터 존재 | 결과 조회 API 호출 | 삭제된 질문의 답변 데이터도 결과에 포함 | ⬜ |
| QST-065 | 삭제된 선택지의 답변 결과 조회 시 확인 가능 | 선택지 soft delete됨, 해당 선택지 선택한 응답 존재 | 결과 조회 API 호출 | 삭제된 선택지의 답변 데이터도 결과에 포함 | ⬜ |

### 3.8 질문 복사 (INV-24, INV-25)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| QST-070 | 같은 설문 내 질문 복사 성공 (displayOrder 맨 뒤) | 설문에 질문 3개 (displayOrder 0,1,2) | 질문 1개 복사 | 성공, 복사된 질문 displayOrder=3 (맨 뒤) | ⬜ |
| QST-071 | 복사된 질문의 선택지/행도 복사됨 | 원본 질문에 선택지 3개, 행 2개 | 질문 복사 | 복사본에 선택지 3개, 행 2개 동일 구조 | ⬜ |
| QST-072 | 복사된 질문/선택지/행에 새 ID 부여 (INV-25) | 원본 질문/선택지/행에 기존 ID | 질문 복사 후 persist | 복사본 질문/선택지/행 모두 새 ID, 원본과 불일치 | ⬜ |
| QST-073 | 복사 시 soft delete된 선택지/행 제외 (INV-23) | 원본 질문에 deleted=true 선택지 1개, deleted=true 행 1개 | 질문 복사 | 복사본에 삭제된 선택지/행 미포함 | ⬜ |
| QST-074 | 질문 49개 설문에 1개 복사 -> 성공 (경계값) | 설문에 활성 질문 49개 | 질문 1개 복사 | 성공, 질문 수 50개 (INV-04 범위 내) | ⬜ |
| QST-075 | 질문 50개 설문에 1개 복사 -> 거부 (INV-24, SURVEY_QUESTION_LIMIT_EXCEEDED) | 설문에 활성 질문 50개 | 질문 1개 복사 시도 | 예외 발생 (SURVEY_QUESTION_LIMIT_EXCEEDED, INV-24) | ⬜ |
| QST-076 | 질문 49개 설문에 2개 복사 시도 -> 거부 (INV-24) | 설문에 활성 질문 49개 | 질문 2개 동시 복사 시도 | 예외 발생 (49 + 2 = 51 > 50, SURVEY_QUESTION_LIMIT_EXCEEDED) | ⬜ |

---

## 4. SurveyService 테스트 (SVC-SRV-xxx)

### 4.1 설문 CRUD

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-SRV-001 | 운영진 유효한 요청으로 설문 생성 성공 | OPERATOR 권한, 유효한 요청 | `surveyService.createSurvey(request, operatorId)` 호출 | 성공, 설문 ID 반환 | ✅ |
| SVC-SRV-002 | 일반 회원(MEMBER) 설문 생성 시도 -> 403 (SEC-01) | MEMBER 권한 | `surveyService.createSurvey(request, memberId)` 호출 | 403 Forbidden (SEC-01) | ✅ |
| SVC-SRV-003 | 준회원(ASSOCIATE) 설문 생성 시도 -> 403 (SEC-01) | ASSOCIATE 권한 | `surveyService.createSurvey(request, associateId)` 호출 | 403 Forbidden (SEC-01) | ⬜ |
| SVC-SRV-004 | 비인증 사용자 설문 생성 시도 -> 401 | 비인증 사용자 | 설문 생성 API 호출 | 401 Unauthorized | ⬜ |
| SVC-SRV-005 | 운영진 설문 단건 조회 성공 | OPERATOR 권한, 유효한 surveyId | `surveyService.getSurvey(surveyId, operatorId)` 호출 | 성공, 설문 정보 반환 | ✅ |
| SVC-SRV-006 | 삭제된(deleted) 설문 조회 -> 에러 | deleted=true 설문 | `surveyService.getSurvey(surveyId, userId)` 호출 | 예외 발생 (설문 없음) | ✅ |
| SVC-SRV-007 | 운영진 설문 수정 성공 | OPERATOR 권한, 유효한 수정 요청 | `surveyService.updateSurvey(surveyId, request, operatorId)` 호출 | 수정 성공 | ✅ |
| SVC-SRV-008 | MEMBER 설문 수정 시도 -> 403 | MEMBER 권한 | `surveyService.updateSurvey(surveyId, request, memberId)` 호출 | 403 Forbidden | ✅ |

### 4.2 상태 관리 (publish, unpublish, openResponse, closeResponse)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-SRV-010 | 운영진 설문 공개(publish) 성공 | OPERATOR 권한, UNPUBLISHED 설문, 질문 1개 이상 | `surveyService.publishSurvey(surveyId, operatorId)` 호출 | visibility=PUBLISHED | ✅ |
| SVC-SRV-011 | MEMBER 설문 공개 시도 -> 403 (SEC-02) | MEMBER 권한 | `surveyService.publishSurvey(surveyId, memberId)` 호출 | 403 Forbidden (SEC-02) | ✅ |
| SVC-SRV-012 | 운영진 비공개 전환(unpublish) 성공 | OPERATOR 권한, PUBLISHED 설문 | `surveyService.unpublishSurvey(surveyId, operatorId)` 호출 | visibility=UNPUBLISHED | ✅ |
| SVC-SRV-013 | MEMBER 비공개 전환 시도 -> 403 (SEC-09) | MEMBER 권한 | `surveyService.unpublishSurvey(surveyId, memberId)` 호출 | 403 Forbidden (SEC-09) | ✅ |
| SVC-SRV-014 | 운영진 응답 시작(openResponse) 성공 | OPERATOR 권한, PUBLISHED+NOT_STARTED | `surveyService.openResponse(surveyId, operatorId)` 호출 | responseStatus=OPEN | ✅ |
| SVC-SRV-015 | 운영진 응답 마감(closeResponse) 성공 | OPERATOR 권한, OPEN 설문 | `surveyService.closeResponse(surveyId, operatorId)` 호출 | responseStatus=CLOSED | ✅ |
| SVC-SRV-016 | 운영진 응답 재개(CLOSED->OPEN) 성공 | OPERATOR 권한, PUBLISHED+CLOSED, deadline 미경과 | `surveyService.openResponse(surveyId, operatorId)` 호출 | responseStatus=OPEN | ⬜ |
| SVC-SRV-017 | MEMBER 응답 마감 시도 -> 403 | MEMBER 권한 | `surveyService.closeResponse(surveyId, memberId)` 호출 | 403 Forbidden | ✅ |
| SVC-SRV-018 | 운영진 publishAndOpen 성공 | OPERATOR 권한, UNPUBLISHED+NOT_STARTED, 질문 1개 이상 | `surveyService.publishAndOpen(surveyId, operatorId)` 호출 | visibility=PUBLISHED, responseStatus=OPEN | ✅ |

### 4.3 휴지통 관리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-SRV-020 | 운영진 설문 휴지통 이동 성공 | OPERATOR 권한, 활성 설문 | `surveyService.trashSurvey(surveyId, operatorId)` 호출 | trashedAt 설정됨 | ✅ |
| SVC-SRV-021 | MEMBER 설문 휴지통 이동 시도 -> 403 | MEMBER 권한 | `surveyService.trashSurvey(surveyId, memberId)` 호출 | 403 Forbidden | ✅ |
| SVC-SRV-022 | 운영진 설문 휴지통 복원 성공 | OPERATOR 권한, 휴지통 설문 | `surveyService.restoreSurvey(surveyId, operatorId)` 호출 | trashedAt=null, 원래 상태 복원 | ✅ |
| SVC-SRV-023 | 운영진 설문 영구 삭제 (휴지통에서) 성공 | OPERATOR 권한, 휴지통 설문 | `surveyService.permanentDeleteSurvey(surveyId, operatorId)` 호출 | deleted=true | ✅ |
| SVC-SRV-024 | 활성 설문 영구 삭제 시도 -> 에러 (INV-18) | OPERATOR 권한, 활성 설문 (trashedAt=null) | `surveyService.permanentDeleteSurvey(surveyId, operatorId)` 호출 | 예외 발생 (활성 설문 영구 삭제 불가, INV-18) | ⬜ |
| SVC-SRV-025 | 휴지통 목록 조회 - 운영진 성공 | OPERATOR 권한, 휴지통 설문 존재 | `surveyService.getTrashedSurveyList(operatorId)` 호출 | 휴지통 설문 목록 반환 | ✅ |
| SVC-SRV-026 | 일반 목록에서 휴지통 설문 제외 확인 (INV-17) | 활성 설문 2개 + 휴지통 설문 1개 | `surveyService.getSurveyList()` 호출 | 활성 설문 2개만 반환, 휴지통 설문 제외 (INV-17) | ⬜ |

### 4.4 설문 복사

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-SRV-030 | 운영진 설문 복사 성공 | OPERATOR 권한, 활성 설문 | `surveyService.copySurvey(surveyId, operatorId)` 호출 | 새 설문 생성 (U,NS,active), 원본과 독립 | ⬜ |
| SVC-SRV-031 | MEMBER 설문 복사 시도 -> 403 (SEC-10) | MEMBER 권한 | `surveyService.copySurvey(surveyId, memberId)` 호출 | 403 Forbidden (SEC-10) | ⬜ |
| SVC-SRV-032 | 휴지통 설문 복사 시도 -> 에러 | OPERATOR 권한, 휴지통 설문 | `surveyService.copySurvey(surveyId, operatorId)` 호출 | 예외 발생 (휴지통 설문 복사 불가) | ⬜ |
| SVC-SRV-033 | 존재하지 않는 설문 복사 시도 -> 에러 | 존재하지 않는 surveyId | `surveyService.copySurvey(invalidId, operatorId)` 호출 | 예외 발생 (설문 없음) | ⬜ |
| SVC-SRV-034 | 비인가 접근 시 DB 상태 변경 없음 (SEC-06) | MEMBER 권한, 설문 복사 시도 | 복사 시도 후 DB 확인 | 403 예외 발생, DB에 새 설문 생성되지 않음 | ⬜ |

### 4.5 응답 관련

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-SRV-040 | 회원 중복 응답 방지 (INV-01) | MEMBER 권한, 이미 해당 설문에 응답 제출 완료 | 동일 설문에 재응답 시도 | 예외 발생 (중복 응답, INV-01) | ⬜ |
| SVC-SRV-041 | PUBLISHED+OPEN에서만 응답 가능 (INV-09) | PUBLISHED+CLOSED 설문 | 응답 제출 시도 | 예외 발생 (CLOSED 설문 응답 불가, INV-09) | ⬜ |
| SVC-SRV-042 | 휴지통 설문 응답 거부 (INV-16) | PUBLISHED+OPEN+trashed 설문 | 응답 제출 시도 | 예외 발생 (휴지통 설문 응답 불가, INV-16) | ⬜ |
| SVC-SRV-043 | 필수 질문 응답 누락 시 제출 거부 (INV-12) | required=true 질문에 대한 응답 누락 | 응답 제출 시도 | 예외 발생 (필수 질문 응답 누락, INV-12) | ⬜ |
| SVC-SRV-044 | 응답 제출 중 설문 마감 경합 처리 (INV-15) | 응답 작성 시작 시 OPEN, 제출 시점에 CLOSED | 응답 제출 시도 | 예외 발생 (설문 마감됨, 제출 시점 재검증, INV-15) | ⬜ |
| SVC-SRV-045 | 비회원 PUBLIC 설문 응답 성공 | accessLevel=PUBLIC, PUBLISHED+OPEN, 비인증 사용자 | 응답 제출 | 성공, SurveyResponse.user=null | ⬜ |
| SVC-SRV-046 | 비회원 ASSOCIATE 설문 응답 -> 401 (SEC-03) | accessLevel=ASSOCIATE, 비인증 사용자 | 응답 제출 시도 | 401 Unauthorized (SEC-03) | ⬜ |
| SVC-SRV-047 | ASSOCIATE가 MEMBER 설문 응답 -> 403 (SEC-04) | accessLevel=MEMBER, ASSOCIATE 권한 | 응답 제출 시도 | 403 Forbidden (SEC-04) | ⬜ |
| SVC-SRV-048 | 본인 응답 조회 성공 (accessLevel 변경 후에도) (INV-19) | MEMBER가 응답 제출 후, accessLevel이 OPERATOR로 축소 | 본인 응답 조회 | 성공, 본인 응답 반환 (INV-19) | ⬜ |
| SVC-SRV-049 | MEMBER 결과 조회 시도 -> 403 (SEC-05) | MEMBER 권한 | 설문 결과(전체 통계) 조회 시도 | 403 Forbidden (SEC-05) | ⬜ |
| SVC-SRV-050 | accessLevel 축소 후 기존 응답자 본인 응답 조회 성공 (SEC-07) | MEMBER가 MEMBER 설문에 응답 후, accessLevel OPERATOR로 변경 | MEMBER가 본인 응답 조회 | 200 OK, 본인 응답 반환 (SEC-07) | ⬜ |
| SVC-SRV-051 | accessLevel 축소 후 기존 응답자 타인 응답 조회 -> 403 (SEC-08) | MEMBER가 MEMBER 설문에 응답 후, accessLevel OPERATOR로 변경 | MEMBER가 타인 응답 조회 시도 | 403 Forbidden (SEC-08) | ⬜ |

---

## 5. SurveyQuestionService 테스트 (SVC-QST-xxx)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| SVC-QST-001 | 운영진 질문 추가 성공 | OPERATOR 권한, 유효한 설문 ID | `surveyQuestionService.addQuestion(surveyId, request, operatorId)` 호출 | 성공, 질문 추가됨 | ✅ |
| SVC-QST-002 | MEMBER 질문 추가 시도 -> 403 | MEMBER 권한 | `surveyQuestionService.addQuestion(surveyId, request, memberId)` 호출 | 403 Forbidden | ✅ |
| SVC-QST-003 | 운영진 질문 수정 성공 | OPERATOR 권한, 유효한 질문 ID | `surveyQuestionService.updateQuestion(questionId, request, operatorId)` 호출 | 수정 성공 | ✅ |
| SVC-QST-004 | 운영진 질문 삭제 (soft delete) 성공 | OPERATOR 권한, 유효한 질문 ID | `surveyQuestionService.deleteQuestion(questionId, operatorId)` 호출 | 질문 soft delete (deleted=true) | ✅ |
| SVC-QST-005 | 운영진 선택지 추가 성공 | OPERATOR 권한, 유효한 질문 ID | `surveyQuestionService.addOption(questionId, request, operatorId)` 호출 | 성공, 선택지 추가됨 | ✅ |
| SVC-QST-006 | 운영진 선택지 삭제 (soft delete) 성공 | OPERATOR 권한, 유효한 선택지 ID | `surveyQuestionService.deleteOption(optionId, operatorId)` 호출 | 선택지 soft delete (deleted=true) | ✅ |
| SVC-QST-007 | 운영진 행 추가 성공 | OPERATOR 권한, 유효한 질문 ID (그리드 유형) | `surveyQuestionService.addRow(questionId, request, operatorId)` 호출 | 성공, 행 추가됨 | ✅ |
| SVC-QST-008 | 운영진 행 삭제 (soft delete) 성공 | OPERATOR 권한, 유효한 행 ID | `surveyQuestionService.deleteRow(rowId, operatorId)` 호출 | 행 soft delete (deleted=true) | ✅ |
| SVC-QST-009 | 운영진 질문 복사 성공 | OPERATOR 권한, 유효한 설문/질문 ID, 질문 수 49개 이하 | `surveyQuestionService.copyQuestion(surveyId, questionId, operatorId)` 호출 | 성공, 질문 복사됨 (displayOrder 맨 뒤) | ⬜ |
| SVC-QST-010 | MEMBER 질문 복사 시도 -> 403 (SEC-11) | MEMBER 권한 | `surveyQuestionService.copyQuestion(surveyId, questionId, memberId)` 호출 | 403 Forbidden (SEC-11) | ⬜ |
| SVC-QST-011 | 질문 복사 시 질문 수 초과 -> 에러 (INV-24) | OPERATOR 권한, 설문에 질문 50개 | `surveyQuestionService.copyQuestion(surveyId, questionId, operatorId)` 호출 | 예외 발생 (SURVEY_QUESTION_LIMIT_EXCEEDED, INV-24) | ⬜ |
| SVC-QST-012 | 다른 설문의 질문이 아닌 경우 -> 에러 (SURVEY_QUESTION_NOT_BELONGS) | 질문이 다른 설문에 속함 | `surveyQuestionService.copyQuestion(surveyId, otherQuestionId, operatorId)` 호출 | 예외 발생 (SURVEY_QUESTION_NOT_BELONGS) | ⬜ |

---

## 6. 검증 기준 매핑 (Verification Criteria Mapping)

### 6.1 불변조건 (INV) 매핑

| 검증 기준 | 설명 | 커버 테스트 ID | 상태 |
|----------|------|-------------|------|
| INV-01 (회원 중복 응답 방지) | 회원은 설문당 최대 1회 응답 | SVC-SRV-040 | ⬜ |
| INV-02 (모든 상태 질문 수정 가능) | 질문 구조는 모든 상태에서 수정 가능 | SRV-085~090 | ⬜ |
| INV-03 (휴지통 2단계 삭제) | 활성 -> 휴지통 -> 영구 삭제 | SRV-060~081 | ⬜ |
| INV-04 (질문 수 1~50) | 발행 시 질문 최소 1개, 최대 50개 | SRV-026~028, QST-045~048 | ⬜ |
| INV-05 (모든 상태 accessLevel 변경 가능) | PUBLISHED에서도 accessLevel 변경 허용 | SRV-091 | ✅ |
| INV-06 (그리드 최소 구성) | 그리드 질문은 행+열 각 1개 이상 | QST-034~038 | ✅ |
| INV-07 (선형 배율 min < max) | scaleMin < scaleMax 필수 | QST-040~043 | ✅ |
| INV-08 (마감일 자동 응답 마감) | deadline 경과 시 자동 CLOSED | (통합 테스트 범위) | ⬜ |
| INV-09 (PUBLISHED+OPEN에서만 응답 가능) | 3가지 조건 충족 시에만 응답 허용 | SRV-095~100, SVC-SRV-041 | ⬜ |
| INV-10 (선택지/행 삭제 시 soft delete) | 기존 응답 FK 보호 | QST-062, QST-063, QST-065 | ⬜ |
| INV-11 (응답 재개 시 마감일 검증) | CLOSED->OPEN 재개 시 deadline 미래 필수 | SRV-053~055 | ⬜ |
| INV-12 (필수 질문 응답 누락 방지) | required=true 질문 응답 필수 | SVC-SRV-043 | ⬜ |
| INV-13 (질문 유형별 필수 구성요소) | 유형별 필수 구성요소 발행 시 검증 | SRV-020, SRV-029, QST-030~038 | ⬜ |
| INV-14 (질문 삭제 시 soft delete) | 기존 SurveyAnswer FK 보호 | QST-060, QST-061, QST-064 | ⬜ |
| INV-15 (응답 제출 중 설문 마감 경합) | 제출 시점 상태 재검증 | SVC-SRV-044 | ⬜ |
| INV-16 (휴지통 설문 응답 불가) | trashedAt 설정 시 응답 거부 | SRV-100, SVC-SRV-042 | ⬜ |
| INV-17 (휴지통 설문 목록 제외) | 일반 목록에서 휴지통 설문 제외 | SVC-SRV-026 | ⬜ |
| INV-18 (영구 삭제는 휴지통에서만) | 활성 설문 영구 삭제 불가 | SRV-081, SVC-SRV-024 | ⬜ |
| INV-19 (본인 응답 조회는 accessLevel 무관) | accessLevel 변경 후에도 본인 응답 조회 가능 | SVC-SRV-048, SVC-SRV-050 | ⬜ |
| INV-20 (비공개 전환 시 자동 응답 마감) | P+O에서 unpublish 시 자동 CLOSED | SRV-040~042 | ✅ |
| INV-21 (설문 복사 시 상태 초기화) | 복사본 (U,NS,active) 초기화 | SRV-105, SRV-108, SRV-109 | ⬜ |
| INV-22 (복사 시 응답 데이터 제외) | 복사본에 응답 0건 | SRV-111 | ⬜ |
| INV-23 (복사 시 soft delete 요소 제외) | 삭제된 질문/선택지/행 미복사 | SRV-112~114, QST-073 | ⬜ |
| INV-24 (질문 복사 시 질문 수 제한 검증) | 복사 후 50개 초과 시 거부 | QST-074~076, SVC-QST-011 | ⬜ |
| INV-25 (복사된 엔티티 새 ID 부여) | 복사본은 새 ID 부여 | SRV-115, QST-072 | ⬜ |

### 6.2 권한/보안 (SEC) 매핑

| 검증 기준 | 설명 | 커버 테스트 ID | 상태 |
|----------|------|-------------|------|
| SEC-01 (비운영진 설문 생성) | ASSOCIATE/MEMBER 설문 생성 403 | SVC-SRV-002, SVC-SRV-003 | ⬜ |
| SEC-02 (비운영진 발행) | MEMBER 설문 발행 403 | SVC-SRV-011 | ✅ |
| SEC-03 (비회원 ASSOCIATE 설문 응답) | 비인증 사용자 401 | SVC-SRV-046 | ⬜ |
| SEC-04 (ASSOCIATE MEMBER 설문 응답) | ASSOCIATE 403 | SVC-SRV-047 | ⬜ |
| SEC-05 (비운영진 결과 조회) | MEMBER 결과 조회 403 | SVC-SRV-049 | ⬜ |
| SEC-06 (비인가 부작용 없음) | 비인가 접근 시 DB 변경 없음 | SVC-SRV-034 | ⬜ |
| SEC-07 (accessLevel 축소 후 본인 응답 조회) | 본인 응답 조회 200 OK | SVC-SRV-050 | ⬜ |
| SEC-08 (accessLevel 축소 후 타인 응답 조회 차단) | 타인 응답 조회 403 | SVC-SRV-051 | ⬜ |
| SEC-09 (비운영진 비공개 전환) | MEMBER 비공개 전환 403 | SVC-SRV-013 | ✅ |
| SEC-10 (비운영진 설문 복사) | MEMBER 설문 복사 403 | SVC-SRV-031 | ⬜ |
| SEC-11 (비운영진 질문 복사) | MEMBER 질문 복사 403 | SVC-QST-010 | ⬜ |

---

## 7. 구현 현황 요약

| 카테고리 | 전체 | ✅ | ⬜ |
|---------|:---:|:---:|:---:|
| 설문 생성 (SRV-001~017) | 17 | 8 | 9 |
| 공개 상태 전이 (SRV-020~029) | 10 | 5 | 5 |
| 응답 수집 상태 전이 (SRV-030~038) | 9 | 7 | 2 |
| 비공개 전환 자동 마감 (SRV-040~042) | 3 | 3 | 0 |
| publishAndOpen (SRV-045~049) | 5 | 4 | 1 |
| 마감일 조건 (SRV-050~055) | 6 | 5 | 1 |
| 휴지통 행동 (SRV-060~081) | 22 | 9 | 13 |
| 설문 수정 (SRV-085~091) | 7 | 5 | 2 |
| 응답 수락 조건 (SRV-095~100) | 6 | 5 | 1 |
| 설문 복사 (SRV-105~117) | 13 | 0 | 13 |
| 질문 생성 (QST-001~011) | 11 | 11 | 0 |
| 질문 입력값 경계값 (QST-015~025) | 11 | 7 | 4 |
| 질문 필수 구성요소 (QST-030~038) | 9 | 7 | 2 |
| 선형 배율 범위 (QST-040~043) | 4 | 4 | 0 |
| 질문 수 제한 (QST-045~048) | 4 | 0 | 4 |
| 선택지/행 경계값 (QST-050~056) | 7 | 4 | 3 |
| Soft Delete (QST-060~065) | 6 | 3 | 3 |
| 질문 복사 (QST-070~076) | 7 | 0 | 7 |
| SurveyService CRUD (SVC-SRV-001~008) | 8 | 6 | 2 |
| SurveyService 상태 관리 (SVC-SRV-010~018) | 9 | 8 | 1 |
| SurveyService 휴지통 (SVC-SRV-020~026) | 7 | 5 | 2 |
| SurveyService 복사 (SVC-SRV-030~034) | 5 | 0 | 5 |
| SurveyService 응답 (SVC-SRV-040~051) | 12 | 0 | 12 |
| SurveyQuestionService (SVC-QST-001~012) | 12 | 8 | 4 |
| **합계** | **209** | **114** | **95** |

---

## 8. 구현된 테스트 클래스

### 8.1 SurveyTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/domain/SurveyTest.java`
- **범위**: Survey 도메인 로직 (생성, 2축 상태 전이, 휴지통, 수정, 응답 수락 조건)
- **커버 ID**: SRV-001~002, SRV-009, SRV-012~014, SRV-016~017, SRV-020~023, SRV-025, SRV-030~033, SRV-036~038, SRV-040~042, SRV-045, SRV-047~055, SRV-060~063, SRV-065, SRV-070, SRV-075, SRV-080~081, SRV-085~088, SRV-091, SRV-095~098, SRV-100

### 8.2 SurveyQuestionTest (도메인 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/domain/SurveyQuestionTest.java`
- **범위**: SurveyQuestion, SurveyQuestionOption, SurveyQuestionRow 도메인 로직 (생성, 입력값 검증, 필수 구성요소, 선형 배율, Soft Delete)
- **커버 ID**: QST-001~011, QST-015~016, QST-020~021, QST-023~025, QST-030~031, QST-034~038, QST-040~043, QST-050~051, QST-054~055, QST-060, QST-062~063

### 8.3 SurveyServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/service/SurveyServiceTest.java`
- **범위**: SurveyService 비즈니스 로직 (CRUD, 상태 관리, 휴지통, 권한 검증)
- **커버 ID**: SVC-SRV-001~002, SVC-SRV-005~008, SVC-SRV-010~015, SVC-SRV-017~018, SVC-SRV-020~023, SVC-SRV-025

### 8.4 SurveyQuestionServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/service/SurveyQuestionServiceTest.java`
- **범위**: SurveyQuestionService 비즈니스 로직 (질문 CRUD, 권한 검증)
- **커버 ID**: SVC-QST-001~004 (질문 추가/수정/삭제/조회)

### 8.5 SurveyQuestionOptionServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/service/SurveyQuestionOptionServiceTest.java`
- **범위**: SurveyQuestionOptionService 비즈니스 로직 (선택지 CRUD, 권한 검증, 소유권 검증)
- **커버 ID**: SVC-QST-005~006 (선택지 추가/삭제)

### 8.6 SurveyQuestionRowServiceTest (서비스 단위 테스트)
- **파일**: `backend/src/test/java/igrus/web/survey/service/SurveyQuestionRowServiceTest.java`
- **범위**: SurveyQuestionRowService 비즈니스 로직 (행 CRUD, 권한 검증, 소유권 검증)
- **커버 ID**: SVC-QST-007~008 (행 추가/삭제)

---

## 9. 관련 문서

- [설문 검증 기준서](../../criteria/survey/survey-criteria-v1.md) -- INV-01~25, SEC-01~11
- 설문 통합 테스트 케이스 (survey-integration-test-cases.md) -- 예정
- [행사 도메인 테스트 케이스](../event/event-domain-test-cases.md) -- 2축 모델 참고

---

## 10. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-25 | - | 최초 작성. 2축 상태 모델(SurveyVisibility + SurveyResponseStatus) 기반. 설문 엔티티 98건, 질문 엔티티 52건, SurveyService 41건, SurveyQuestionService 12건, 총 209건. 검증 기준서 INV-01~25, SEC-01~11 전수 매핑 |
| 1.1 | 2026-02-25 | Claude | 테스트 구현 상태 반영. SurveyTest, SurveyQuestionTest 도메인 테스트 및 SurveyServiceTest, SurveyQuestionServiceTest, SurveyQuestionOptionServiceTest, SurveyQuestionRowServiceTest 서비스 테스트 구현 완료. 총 114건 ✅ (54.5%). Section 7 구현 현황 표 ✅/⬜ 분리, Section 8 테스트 클래스 목록 갱신 |
