# 설문 통계 API 검증 기준서

> **Status**: Draft
> **작성일**: 2026-02-27
> **대상 기능**: 설문 통계 API (Survey Statistics API) - 설문 응답 데이터를 집계하여 질문 타입별 통계를 제공하는 API
> **Reference**: [QA Testing 관련 용어 정리](https://github.com/IGRUS-INHA/IGRUS-Web/wiki/QA-Testing-%EA%B4%80%EB%A0%A8-%EC%9A%A9%EC%96%B4-%EC%A0%95%EB%A6%AC)
> **관련 문서**:
> - [설문 기능 검증 기준서](./survey-criteria-v1.md) -- INV-01~30, SEC-01~11
> - [설문 도메인 테스트 케이스](../../backend/docs/test-case/survey/survey-domain-test-cases.md)
> - [설문 통합 테스트 케이스](../../backend/docs/test-case/survey/survey-integration-test-cases.md)

### 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1 | 2026-02-27 | 초안 작성. 설문 통계 API 검증 기준 총 66건. 도메인 규칙 7개(STAT-INV-01~07), 권한/보안 5개(STAT-SEC-01~05), 질문 타입별 통계 정확성 21건(TEXT 4, SCALE 5, OPTION 4, CHECKBOX 3, GRID 5), 전체 요약 4건, 경계값/엣지 케이스 17건, 부정 시나리오 3건, 복합 시나리오 5건, 성능 2건 |
| v1.1 | 2026-02-27 | 리뷰 피드백 반영 (66건 → 69건). [추가] STAT-INV-08 소수점 처리 정책(HALF_UP 소수 첫째 자리), STAT-SCL-06 소수 반올림 검증, STAT-EDGE-06 삭제된 SurveyAnswer 전용 검증. [수정] STAT-SCL-02 소수점 비고 확정, STAT-GRD-02/GRD-04 비율 분모 "전체 설문 응답자 수"로 명확화, STAT-NEG-02 기대 결과 "400 Bad Request" 단일 확정, STAT-EDGE-01 percentage=0.0 명시, STAT-TXT-02 정렬 기준 추가, STAT-INTEG-01 기대 결과 구체화, STAT-EDGE-05 기대 결과 명확화, STAT-PERF-02 PASS/FAIL 기준 명확화. [커버리지] STAT-INV-02에 STAT-EDGE-06 매핑 |

---

## 목적

이 문서는 설문 통계 API에서 **반드시 지켜져야 하는 규칙**을 명시하여, 코드 변경 시 검증 기준으로 사용한다.

QA Testing 용어 정리 wiki의 10개 영역 중, 이 기능에 직접 관련된 6개 영역을 적용한다:

| # | 영역 | 적용 이유 |
|---|------|----------|
| 1 | 도메인 규칙과 불변조건 | 삭제된 응답/답변 제외, 통계 정합성, soft delete 데이터 처리 규칙 |
| 2 | 입력 도메인 분할과 경계값 | 응답 0건/1건/다수건, 질문 유형별 통계 구조, 비필수 질문 미응답 |
| 3 | 권한/보안 정책 | RBAC (운영자만 통계 조회 가능), 설문 상태별 접근 규칙 |
| 4 | 상태 모델 | 2축 상태 모델(visibility + responseStatus) 및 휴지통 상태에서의 통계 접근 |
| 5 | 관측 가능성 | 통계 조회 로그, 성능 모니터링 |
| 6 | 테스트 전략 | 테스트 레벨별 검증 항목 매핑 |

---

## 1. 도메인 규칙과 불변조건 (Domain Rules & Invariants)

통계 API에서 **항상 참이어야 하는 조건**이다.

### STAT-INV-01: 삭제된 응답은 통계에서 제외

> 영구 삭제 표시된(`deleted = true`) `SurveyResponse`는 통계 집계에서 제외된다.

- **사전조건**: 통계 조회 시 `SurveyResponse`에 `deleted = true`인 레코드 존재
- **사후조건**: 해당 레코드의 답변은 총 응답 수 및 질문별 통계에서 모두 제외됨
- **위반 시**: 삭제된 응답이 통계에 포함되어 데이터 왜곡
- **검증 계층**: 서비스 레이어 (Repository 쿼리 조건에 `deleted = false` 포함)

### STAT-INV-02: 삭제된 답변은 통계에서 제외

> 영구 삭제 표시된(`deleted = true`) `SurveyAnswer`는 통계 집계에서 제외된다.

- **사전조건**: 통계 조회 시 `SurveyAnswer`에 `deleted = true`인 레코드 존재
- **사후조건**: 해당 답변은 질문별 통계에 반영되지 않음
- **위반 시**: 삭제된 답변이 통계에 포함되어 데이터 왜곡
- **검증 계층**: 서비스 레이어 (Repository 쿼리 조건에 `deleted = false` 포함)

### STAT-INV-03: 응답 0건 설문도 통계 조회 가능

> 응답이 0건인 설문에 대해서도 통계 API는 정상 응답을 반환한다. 빈 통계 구조를 반환하되, 에러를 발생시키지 않는다.

- **사전조건**: 설문 존재, 해당 설문에 `SurveyResponse`가 0건
- **사후조건**: 200 OK, 총 응답 수 = 0, 질문별 통계는 빈 구조 (응답 수 0, 빈 리스트/분포)
- **위반 시**: 응답 없는 설문에서 404 또는 500 에러 발생
- **관련**: 설문이 PUBLISHED 후 OPEN 전 상태이거나, OPEN 직후 아직 응답이 없는 경우

### STAT-INV-04: 모든 유효 상태의 설문에서 통계 조회 가능

> 설문의 2축 상태(visibility, responseStatus)와 무관하게, 설문이 존재하고 삭제되지 않았으면(`deleted = false`) 운영자는 통계를 조회할 수 있다.

- **UNPUBLISHED + NOT_STARTED**: 조회 가능 (아직 공개 전이지만, 운영자는 내부 확인 가능)
- **PUBLISHED + NOT_STARTED**: 조회 가능 (공개는 했지만 응답 수집 전)
- **PUBLISHED + OPEN**: 조회 가능 (응답 수집 중 실시간 통계 확인)
- **PUBLISHED + CLOSED**: 조회 가능 (마감 후 결과 확인 -- 가장 일반적인 사용)
- **UNPUBLISHED + CLOSED**: 조회 가능 (숨긴 설문의 결과 확인)
- **위반 시**: 특정 상태에서 통계 조회가 차단되어 운영자의 데이터 접근 제한

### STAT-INV-05: 휴지통 설문도 통계 조회 가능

> `trashedAt != null`인 휴지통 설문도 통계를 조회할 수 있다. 응답 데이터는 보존되므로 통계 접근을 차단할 이유가 없다.

- **사전조건**: 설문 `trashedAt != null`, `deleted = false`
- **사후조건**: 정상적으로 통계 반환
- **관련**: INV-03 (설문 삭제는 2단계 -- 휴지통에서 데이터 보존)
- **위반 시**: 휴지통 이동만으로 통계 데이터 접근 불가

### STAT-INV-06: 영구 삭제된 설문은 통계 조회 불가

> `deleted = true`인 설문은 통계를 조회할 수 없다. 설문 자체를 찾을 수 없으므로 404를 반환한다.

- **사전조건**: 설문 `deleted = true`
- **사후조건**: 404 Not Found (SurveyNotFoundException)
- **관련**: 기존 SurveyNotFoundException 재사용

### STAT-INV-07: soft delete된 질문/선택지/행의 통계 처리

> 통계 조회 시 soft delete된(`deleted = true`) 질문, 선택지, 행에 대한 기존 답변 데이터도 통계에 **포함**한다. 단, 해당 질문/선택지/행이 삭제되었음을 표시한다.

- **근거**: 운영자가 과거 수집된 응답 데이터의 전체 그림을 파악하려면, 삭제된 질문/선택지/행의 답변도 포함해야 함. (INV-14, INV-10 참조 -- soft delete로 FK 보존)
- **예시**: 선택지 "기타"를 삭제한 후에도, 이전에 "기타"를 선택한 응답의 수는 통계에 포함됨. 다만 해당 선택지가 삭제 상태임을 표시
- **위반 시**: 삭제된 선택지에 대한 응답이 누락되어 합계 불일치
- **주의**: STAT-INV-02는 `SurveyAnswer` 자체의 `deleted` 필드를 의미하고, 이 규칙은 **답변이 참조하는 질문/선택지/행**의 `deleted` 상태를 의미함. 두 규칙은 서로 다른 대상에 적용됨

### STAT-INV-08: 통계 수치의 소수점 처리 정책

> 통계 수치 중 평균(average)과 비율(percentage)은 **소수 첫째 자리 반올림(HALF_UP)**하여 반환한다.

- **적용 대상**: LINEAR_SCALE의 average, 모든 옵션/행의 percentage
- **반올림 방식**: `RoundingMode.HALF_UP`, 소수 첫째 자리까지 (scale=1)
- **예시**:
  - 평균 2.333... → 2.3
  - 비율 66.666... → 66.7
  - 비율 33.333... → 33.3
- **위반 시**: 소수점 이하 과도한 자릿수 표시 또는 반올림 방식 불일치로 합계 오차 발생
- **관련**: STAT-SCL-02, STAT-CHK-02, STAT-GRD-02, STAT-GRD-04

---

## 2. 권한/보안 정책 (RBAC & Authorization)

### 2-1. 역할별 접근 제어

| 작업 | 비인증 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 설문 통계 조회 | 401 | 403 | 403 | **O** | **O** |

- **설문 작성자가 아닌 운영자도 조회 가능**: 통계 조회 권한은 역할(OPERATOR 이상)로만 판단. 설문 작성자 여부는 검증하지 않음

### 2-2. 권한/보안 검증 항목

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-SEC-01 | 비인증 사용자 통계 조회 차단 | 권한/보안 (Authentication) | 인증 없음, 설문 존재 | `GET /api/v1/surveys/{surveyId}/statistics` 요청 (토큰 없이) | 401 Unauthorized | P0 | Spring Security 필터 레벨에서 차단 |
| STAT-SEC-02 | ASSOCIATE 통계 조회 차단 | 권한/보안 (Authorization, RBAC) | ASSOCIATE 토큰, 설문 존재 | `GET /api/v1/surveys/{surveyId}/statistics` 요청 | 403 Forbidden | P0 | OPERATOR 미만 역할 차단 |
| STAT-SEC-03 | MEMBER 통계 조회 차단 | 권한/보안 (Authorization, RBAC) | MEMBER 토큰, 설문 존재 | `GET /api/v1/surveys/{surveyId}/statistics` 요청 | 403 Forbidden | P0 | SEC-05 (기존 검증 기준서) 연장 |
| STAT-SEC-04 | OPERATOR 통계 조회 성공 | 권한/보안 (Authorization, RBAC) | OPERATOR 토큰, 설문 존재 | `GET /api/v1/surveys/{surveyId}/statistics` 요청 | 200 OK, 통계 데이터 반환 | P0 | - |
| STAT-SEC-05 | 비인가 접근 시 부작용 없음 | 권한/보안 (Principle of Least Privilege) | MEMBER 토큰 | 통계 조회 시도 후 DB 상태 확인 | 403 Forbidden, DB 변경 없음 | P1 | SEC-06 (기존) 연장. 통계 조회는 읽기 전용이므로 부작용 가능성 낮으나 확인 필요 |

---

## 3. 질문 타입별 통계 정확성 검증

### 3-1. TEXT 카테고리 (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-TXT-01 | TEXT 유형 통계: 응답 수 정확성 | 도메인 규칙 (Invariant) | SHORT_ANSWER 질문에 3건 응답 존재 | 통계 API 조회 | 해당 질문의 응답 수 = 3 | P0 | TextSurveyAnswer 기반 집계 |
| STAT-TXT-02 | TEXT 유형 통계: 텍스트 응답 목록 반환 | 도메인 규칙 (Postcondition) | PARAGRAPH 질문에 3건 텍스트 응답 | 통계 API 조회 | 텍스트 응답 목록 3건 반환 (textValue 포함) | P0 | 응답 본문을 운영자가 확인할 수 있어야 함. 응답 제출 시각(SurveyResponse.createdAt) 오름차순 정렬 |
| STAT-TXT-03 | DATE 유형도 TEXT 카테고리 통계 구조 적용 | 도메인 규칙 (Invariant) | DATE 질문에 2건 응답 | 통계 API 조회 | TEXT 유형과 동일한 구조로 응답 수 2, 텍스트 목록 2건 | P1 | DATE, TIME, FILE_UPLOAD 모두 TextSurveyAnswer |
| STAT-TXT-04 | FILE_UPLOAD 유형 텍스트(URL) 목록 반환 | 도메인 규칙 (Postcondition) | FILE_UPLOAD 질문에 URL 문자열 응답 2건 | 통계 API 조회 | 텍스트 목록에 URL 문자열 2건 포함 | P1 | INV-30 (1차: URL 텍스트 저장) |

### 3-2. SCALE 카테고리 (LINEAR_SCALE)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-SCL-01 | LINEAR_SCALE 응답 수 정확성 | 도메인 규칙 (Invariant) | LINEAR_SCALE 질문(1~5), 5건 응답 [1,2,3,4,5] | 통계 API 조회 | 응답 수 = 5 | P0 | NumericSurveyAnswer.numericValue 기반 |
| STAT-SCL-02 | LINEAR_SCALE 평균값 정확성 | 입력 도메인 분할 (Equivalence Partitioning) | LINEAR_SCALE 질문(1~5), 5건 응답 [1,2,3,4,5] | 통계 API 조회 | 평균 = 3.0 | P0 | STAT-INV-08 적용. 이 케이스는 나누어 떨어지는 경우 |
| STAT-SCL-03 | LINEAR_SCALE 최솟값/최댓값 정확성 | 경계값 분석 (Boundary Value Analysis) | LINEAR_SCALE 질문(1~10), 3건 응답 [2,5,8] | 통계 API 조회 | 최솟값 = 2, 최댓값 = 8 | P0 | 실제 응답 기준 min/max (스케일 범위가 아님) |
| STAT-SCL-04 | LINEAR_SCALE 값별 분포(히스토그램) 정확성 | 도메인 규칙 (Postcondition) | LINEAR_SCALE 질문(1~5), 응답 [1,1,2,3,3,3,5] | 통계 API 조회 | 분포: {1:2, 2:1, 3:3, 4:0, 5:1} | P0 | 미선택 값(4)도 0으로 포함 |
| STAT-SCL-05 | LINEAR_SCALE 응답 1건일 때 평균=최솟값=최댓값 | 경계값 분석 (BVA) | LINEAR_SCALE 질문(1~5), 응답 [3] | 통계 API 조회 | 평균=3.0, 최솟값=3, 최댓값=3, 분포: {1:0,2:0,3:1,4:0,5:0} | P1 | 경계 케이스 |
| STAT-SCL-06 | LINEAR_SCALE 소수점 반올림 정확성 | 도메인 규칙 (Postcondition) | LINEAR_SCALE 질문(1~5), 3건 응답 [1,2,4] | 통계 API 조회 | 평균 = 2.3 (7/3 = 2.333... → HALF_UP 소수 첫째 자리 반올림) | P0 | STAT-INV-08 검증. 나누어 떨어지지 않는 소수 결과 확인 |

### 3-3. OPTION 카테고리 (MULTIPLE_CHOICE, DROPDOWN)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-OPT-01 | MULTIPLE_CHOICE 옵션별 선택 수 정확성 | 도메인 규칙 (Invariant) | MC 질문, 옵션 A/B/C, 응답: A=3, B=2, C=0 | 통계 API 조회 | 옵션별 선택 수: A=3, B=2, C=0 | P0 | OptionSurveyAnswer.selectedOption 기반 |
| STAT-OPT-02 | MULTIPLE_CHOICE 옵션별 비율 정확성 | 도메인 규칙 (Postcondition) | MC 질문, 총 5건 응답, A=3, B=2, C=0 | 통계 API 조회 | 비율: A=60%, B=40%, C=0% (합계 100%) | P0 | 단일 선택이므로 합계 100% |
| STAT-OPT-03 | DROPDOWN 옵션별 선택 수/비율 정확성 | 도메인 규칙 (Invariant) | DROPDOWN 질문, 옵션 X/Y/Z, 응답: X=2, Y=1, Z=0 | 통계 API 조회 | 옵션별 선택 수: X=2, Y=1, Z=0, 비율 합계 100% | P0 | MULTIPLE_CHOICE와 동일 구조 |
| STAT-OPT-04 | 미선택 옵션도 통계에 0으로 포함 | 경계값 분석 (BVA) | MC 질문, 옵션 A/B/C 중 C는 아무도 선택하지 않음 | 통계 API 조회 | C: 선택 수=0, 비율=0% | P1 | 모든 옵션이 통계 응답에 포함됨 |

### 3-4. CHECKBOX 카테고리

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-CHK-01 | CHECKBOX 옵션별 선택 수 정확성 | 도메인 규칙 (Invariant) | CHECKBOX 질문, 옵션 A/B/C, 응답자1: A,B / 응답자2: B,C / 응답자3: A | 통계 API 조회 | 옵션별 선택 수: A=2, B=2, C=1 | P0 | 복수 선택이므로 선택지당 1개 OptionSurveyAnswer |
| STAT-CHK-02 | CHECKBOX 비율 합계가 100% 초과 가능 | 도메인 규칙 (Postcondition) | CHECKBOX, 총 응답자 3명, A=2, B=2, C=1 | 통계 API 조회 | 비율: A=66.7%, B=66.7%, C=33.3% (합계 166.7%) | P0 | 복수 선택 특성. 비율 = 해당 옵션 선택 수 / 총 응답자 수 |
| STAT-CHK-03 | CHECKBOX 총 응답자 수와 개별 선택 합계 구분 | 도메인 규칙 (Invariant) | CHECKBOX, 응답자 2명, 각각 3개씩 선택 | 통계 API 조회 | 총 응답자 수 = 2 (OptionSurveyAnswer 행 수 != 응답자 수) | P0 | 응답 수는 SurveyResponse 기준, 선택 수는 OptionSurveyAnswer 기준 |

### 3-5. GRID 카테고리 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-GRD-01 | MC_GRID 행별 옵션 분포 정확성 | 도메인 규칙 (Invariant) | MC_GRID 질문, 행: 수학/영어, 옵션: 만족/불만족. 응답자1: 수학=만족,영어=불만족 / 응답자2: 수학=불만족,영어=만족 | 통계 API 조회 | 수학: {만족:1, 불만족:1}, 영어: {만족:1, 불만족:1} | P0 | GridSurveyAnswer(selectedRow + selectedOption) 기반 |
| STAT-GRD-02 | MC_GRID 행별 비율 합계 ≤ 100% | 도메인 규칙 (Postcondition) | MC_GRID, 행마다 단일 선택 | 통계 API 조회 | 각 행의 옵션별 비율 합계 ≤ 100% | P0 | 비율 = 해당 옵션 선택 수 / 전체 설문 응답자 수. MC_GRID는 행당 단일 선택이므로 각 행 내 비율 합계 ≤ 100% (비필수 질문에서 미응답자가 있으면 합계 < 100%) |
| STAT-GRD-03 | CB_GRID 행별 옵션 분포 (복수 선택) | 도메인 규칙 (Invariant) | CB_GRID 질문, 행: A/B, 옵션: X/Y/Z. 응답자1: A={X,Y}, B={Z} / 응답자2: A={X}, B={X,Y} | 통계 API 조회 | A: {X:2, Y:1, Z:0}, B: {X:1, Y:1, Z:1} | P0 | 행당 복수 선택 |
| STAT-GRD-04 | CB_GRID 행별 비율 합계 100% 초과 가능 | 도메인 규칙 (Postcondition) | CB_GRID, 복수 선택 | 통계 API 조회 | 각 행의 옵션별 비율 합계 > 100% 가능 | P0 | 비율 = 해당 옵션 선택 수 / 전체 설문 응답자 수. CHECKBOX와 동일 논리로 행당 합계 > 100% 가능 |
| STAT-GRD-05 | 특정 행에 응답이 없는 경우 (비필수 질문) | 경계값 분석 (BVA) | CB_GRID, 비필수 질문, 응답자가 일부 행만 응답 | 통계 API 조회 | 미응답 행은 모든 옵션 선택 수 = 0 | P1 | 비필수 질문의 미응답 행 처리 |

---

## 4. 전체 요약 통계 검증

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-SUM-01 | 총 응답 수 정확성 | 도메인 규칙 (Invariant) | 설문에 SurveyResponse 5건 (deleted=false) | 통계 API 조회 | 총 응답 수 = 5 | P0 | deleted=true 레코드 제외 (STAT-INV-01) |
| STAT-SUM-02 | 삭제된 응답 제외 후 총 응답 수 | 도메인 규칙 (Invariant) | SurveyResponse 5건 중 2건 deleted=true | 통계 API 조회 | 총 응답 수 = 3 | P0 | STAT-INV-01 검증 |
| STAT-SUM-03 | 응답 기간 정확성 | 도메인 규칙 (Postcondition) | 첫 응답 2026-02-01, 마지막 응답 2026-02-25 | 통계 API 조회 | 응답 시작일 = 2026-02-01, 응답 종료일 = 2026-02-25 (createdAt 기준) | P1 | 활성(deleted=false) 응답 기준 min/max createdAt |
| STAT-SUM-04 | 응답 0건일 때 응답 기간 | 경계값 분석 (BVA) | 설문에 응답 0건 | 통계 API 조회 | 응답 기간 = null 또는 미표시 | P1 | STAT-INV-03과 연관 |

---

## 5. 엣지 케이스 및 경계값 검증

### 5-1. 응답 데이터 관련 경계값

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-EDGE-01 | 응답 0건 설문 통계 조회 | 경계값 분석 (BVA) | 설문 존재, 응답 0건 | 통계 API 조회 | 200 OK, 총 응답 수=0, 질문별 빈 통계 (OPTION/CHECKBOX/GRID 유형의 각 옵션 percentage=0.0, SCALE 유형 average=0.0 또는 null) | P0 | STAT-INV-03. 0 나누기 발생하지 않도록 분모 0일 때 비율은 0.0으로 처리 |
| STAT-EDGE-02 | 응답 1건 설문 통계 조회 | 경계값 분석 (BVA) | 설문에 응답 정확히 1건 | 통계 API 조회 | 총 응답 수=1, 각 질문에 해당 응답 반영 | P0 | 최소 유효 응답 |
| STAT-EDGE-03 | 모든 응답이 삭제된 설문 | 경계값 분석 (BVA) | 설문에 응답 3건 모두 deleted=true | 통계 API 조회 | 200 OK, 총 응답 수=0, 빈 통계 (STAT-EDGE-01과 동일 결과) | P0 | STAT-INV-01 + STAT-INV-03 복합 |
| STAT-EDGE-04 | 비필수 질문에 일부만 응답한 경우 | 입력 도메인 분할 (Equivalence Partitioning) | 비필수 MC 질문, 응답자 5명 중 3명만 응답 | 통계 API 조회 | 해당 질문 응답 수=3, 미응답 2명은 통계에서 제외 | P0 | 비필수 질문 미응답 시 SurveyAnswer 행 자체가 없음 |
| STAT-EDGE-05 | 모든 질문이 비필수이고 빈 응답 제출 | 경계값 분석 (BVA) | 모든 질문 required=false, 응답자가 아무 질문에도 응답하지 않고 제출 | 통계 API 조회 | 총 응답 수=1 (SurveyResponse는 존재), 각 질문 통계 항목은 포함되되 responseCount=0, 빈 텍스트 목록 또는 모든 옵션 선택 수 0 | P1 | SurveyResponse는 카운트, SurveyAnswer가 없으므로 질문별 응답 수 0 |
| STAT-EDGE-06 | 삭제된 SurveyAnswer가 질문별 통계에서 제외 | 도메인 규칙 (Invariant) | MC 질문, 옵션 A/B/C, 응답자 3명: 응답자1=A, 응답자2=B, 응답자3=A. 응답자3의 SurveyAnswer가 deleted=true | 통계 API 조회 | 해당 질문의 옵션별 선택 수: A=1, B=1, C=0 (deleted=true인 응답자3의 A 선택이 제외됨). 해당 질문 responseCount=2 | P0 | STAT-INV-02 전용 검증. SurveyResponse 자체는 deleted=false이지만, 해당 질문의 SurveyAnswer만 deleted=true인 경우를 검증 |

### 5-2. 질문/선택지 soft delete 관련 경계값

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-EDGE-10 | soft delete된 질문의 기존 답변 통계 포함 | 도메인 규칙 (Invariant) | MC 질문에 응답 3건 존재, 이후 질문 soft delete | 통계 API 조회 | 해당 질문의 답변 3건 통계에 포함, 질문 삭제 여부 표시 | P0 | STAT-INV-07. 운영자는 삭제된 질문의 과거 답변도 확인 가능 |
| STAT-EDGE-11 | soft delete된 선택지의 기존 선택 통계 포함 | 도메인 규칙 (Invariant) | MC 옵션 A/B/C 중 C soft delete. 기존 C 선택 응답 2건 | 통계 API 조회 | 옵션별: A=x, B=y, C=2 (삭제 표시). C의 응답도 통계에 포함 | P0 | STAT-INV-07. INV-10 (선택지 soft delete) |
| STAT-EDGE-12 | soft delete된 행의 기존 응답 통계 포함 | 도메인 규칙 (Invariant) | MC_GRID 행 "수학" soft delete. 기존 "수학" 행 응답 존재 | 통계 API 조회 | "수학" 행의 분포 포함, 행 삭제 여부 표시 | P0 | STAT-INV-07. INV-10 (행 soft delete) |
| STAT-EDGE-13 | 모든 선택지가 soft delete된 질문 | 경계값 분석 (BVA) | MC 질문의 옵션 A/B/C 모두 soft delete. 기존 응답 존재 | 통계 API 조회 | 모든 옵션이 삭제 표시와 함께 기존 응답 수 반환 | P1 | 극단적 경우. 기존 데이터 보존이 핵심 |

### 5-3. 설문 상태별 통계 조회

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-EDGE-20 | UNPUBLISHED + NOT_STARTED 설문 통계 조회 | 상태 모델 (State Transition Testing) | (U, NS) 설문, 응답 0건 | OPERATOR로 통계 API 조회 | 200 OK, 빈 통계 | P1 | STAT-INV-04. 아직 공개 전이지만 조회 가능 |
| STAT-EDGE-21 | PUBLISHED + OPEN 설문 통계 조회 (실시간) | 상태 모델 (State Transition Testing) | (P, O) 설문, 응답 10건 진행 중 | OPERATOR로 통계 API 조회 | 200 OK, 현재까지 제출된 응답 기준 통계 | P0 | 응답 수집 중 실시간 모니터링 시나리오 |
| STAT-EDGE-22 | PUBLISHED + CLOSED 설문 통계 조회 | 상태 모델 (State Transition Testing) | (P, C) 설문, 응답 50건 | OPERATOR로 통계 API 조회 | 200 OK, 전체 응답 통계 | P0 | 가장 일반적 사용 시나리오 |
| STAT-EDGE-23 | 휴지통 설문 통계 조회 | 상태 모델 (State Transition Testing) | (P, C, trashed) 설문, 응답 존재 | OPERATOR로 통계 API 조회 | 200 OK, 정상 통계 반환 | P0 | STAT-INV-05 |
| STAT-EDGE-24 | 영구 삭제된 설문 통계 조회 시도 | 상태 모델 (State Transition Testing) | deleted=true 설문 | OPERATOR로 통계 API 조회 | 404 Not Found (SurveyNotFoundException) | P0 | STAT-INV-06 |

### 5-4. 부정 시나리오 (Negative Testing)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-NEG-01 | 존재하지 않는 설문 ID로 통계 조회 | 입력 도메인 분할 (Equivalence Partitioning) | surveyId=999999 (존재하지 않음) | `GET /api/v1/surveys/999999/statistics` 요청 | 404 Not Found (SurveyNotFoundException) | P0 | 기존 SurveyNotFoundException 재사용 |
| STAT-NEG-02 | surveyId 음수로 통계 조회 | 경계값 분석 (BVA) | surveyId=-1 | `GET /api/v1/surveys/-1/statistics` 요청 | 400 Bad Request | P2 | @Positive Bean Validation 적용으로 음수 ID 차단 |
| STAT-NEG-03 | surveyId 비숫자 문자열로 통계 조회 | 입력 도메인 분할 (Equivalence Partitioning) | surveyId="abc" | `GET /api/v1/surveys/abc/statistics` 요청 | 400 Bad Request | P2 | Spring 기본 타입 변환 에러 |

---

## 6. 통계 데이터 정합성 검증 (복합 시나리오)

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-INTEG-01 | 다양한 질문 유형이 혼합된 설문 통계 | 도메인 규칙 (Invariant) | 설문에 SHORT_ANSWER, MC, CHECKBOX, LINEAR_SCALE, MC_GRID 각 1개씩 총 5개 질문, 응답 10건 | 통계 API 조회 | 질문별 responseCount가 정확하고, 각 유형의 통계 구조가 비어 있지 않음. TEXT는 텍스트 목록(10건), MC는 옵션별 수/비율(합계 100%), CHECKBOX는 복수선택 수/비율(합계 > 100% 가능), LINEAR_SCALE은 평균/min/max/분포, MC_GRID는 행별 옵션 분포 | P0 | 실제 운영 시 가장 일반적인 형태. 모든 질문의 responseCount가 응답 수(10)와 일치하는지 확인 |
| STAT-INTEG-02 | 응답 수정(PUT) 후 통계 반영 | 도메인 규칙 (Postcondition) | MC 질문, 응답자가 옵션 A 선택 후 B로 수정 (INV-26) | 통계 API 조회 | A=0, B=1 (수정 후 최종 응답만 반영) | P0 | INV-26 (PUT 전체 교체) 이후 통계 정합성. orphanRemoval로 기존 답변 삭제됨 |
| STAT-INTEG-03 | 질문 추가 후 기존 응답자 미응답 질문 통계 | 도메인 규칙 (Postcondition) | 설문에 질문 2개, 응답 5건 제출 후 질문 1개 추가 | 통계 API 조회 | 기존 질문 2개: 응답 수 5. 새 질문: 응답 수 0 | P1 | INV-28 (질문 변경 후 기존 응답 유효). 새 질문에는 기존 응답자의 답변 없음 |
| STAT-INTEG-04 | 비회원(PUBLIC) 응답 포함 통계 | 도메인 규칙 (Invariant) | PUBLIC 설문, 회원 응답 3건 + 비회원 응답 2건 | 통계 API 조회 | 총 응답 수 = 5 (회원 + 비회원 합산) | P0 | SurveyResponse.user = null인 응답도 통계에 포함 |
| STAT-INTEG-05 | 질문 displayOrder 순서로 통계 반환 | 도메인 규칙 (Postcondition) | 질문 A(displayOrder=2), B(displayOrder=0), C(displayOrder=1) | 통계 API 조회 | 질문별 통계가 displayOrder 오름차순으로 정렬: B, C, A | P1 | 설문 폼 순서와 통계 순서 일치 |

---

## 7. 관측 가능성 (Observability & Audit)

### 7-1. 로그 메시지

| 서비스 | 시작 로그 | 완료 로그 | 실패 로그 |
|--------|---------|---------|---------|
| 통계 조회 | `설문 통계 조회 요청: surveyId={}, operatorId={}` | `설문 통계 조회 완료: surveyId={}, 총 응답 수={}` | `통계 조회 실패: 설문 없음 surveyId={}` |

### 7-2. 성능 관련 검증

| ID | 검증 항목 | 검증 유형 | 사전 조건 | 검증 절차 | 기대 결과 | 우선순위 | 비고 |
|----|----------|----------|----------|----------|----------|---------|------|
| STAT-PERF-01 | N+1 쿼리 방지 | 테스트 전략 (Regression Testing) | 질문 10개, 각 질문에 선택지 5개, 응답 100건 | 통계 API 조회 시 실행 쿼리 수 확인 | 질문/선택지/행 조회가 fetch join 또는 @EntityGraph로 최적화됨. 쿼리 수가 질문 수에 비례하지 않음 | P1 | 대규모 설문에서 성능 문제 방지 |
| STAT-PERF-02 | 대량 응답 설문 통계 응답 시간 | 품질 목표 (Quality Goals) | 질문 50개(최대), 응답 1000건 | 통계 API 조회 | 응답 시간 < 3초이면 PASS, 3초 이상이면 FAIL | P2 | 목표치이며, 정식 SLA는 미확정. 추후 운영 환경 성능 측정 후 조정 가능 |

---

## 8. 테스트 전략 (Test Strategy)

### 8-1. 테스트 레벨별 검증 범위

| 테스트 레벨 | 검증 대상 | 테스트 더블 |
|-----------|----------|-----------|
| 단위 테스트 (서비스) | 통계 집계 로직, 타입별 통계 변환, 삭제 필터링 | Repository Mock (런던파) |
| 통합 테스트 (서비스) | DB에서 실제 데이터 조회 후 통계 정확성, N+1 검증 | 실제 DB (H2) |
| 컨트롤러 통합 테스트 (MockMvc) | HTTP 인증/인가, 응답 구조, 상태 코드 | 실제 서비스 (full context) |

### 8-2. 검증 기준 커버리지 (테스트 작성 후 업데이트)

#### 도메인 규칙/불변조건 (STAT-INV) 커버리지

| 검증 기준 | 커버 테스트 ID | 상태 |
|----------|-------------|------|
| STAT-INV-01 (삭제된 응답 제외) | STAT-SUM-02, STAT-EDGE-03 | 미작성 |
| STAT-INV-02 (삭제된 답변 제외) | STAT-EDGE-06 | 미작성 |
| STAT-INV-03 (응답 0건 통계 가능) | STAT-EDGE-01, STAT-SUM-04 | 미작성 |
| STAT-INV-04 (모든 상태 통계 가능) | STAT-EDGE-20~22 | 미작성 |
| STAT-INV-05 (휴지통 통계 가능) | STAT-EDGE-23 | 미작성 |
| STAT-INV-06 (영구 삭제 통계 불가) | STAT-EDGE-24 | 미작성 |
| STAT-INV-07 (삭제된 질문/선택지/행 답변 포함) | STAT-EDGE-10~13 | 미작성 |
| STAT-INV-08 (소수점 처리 HALF_UP) | STAT-SCL-06, STAT-CHK-02 | 미작성 |

#### 권한/보안 (STAT-SEC) 커버리지

| 검증 기준 | 커버 테스트 ID | 상태 |
|----------|-------------|------|
| STAT-SEC-01 (비인증 차단) | STAT-SEC-01 | 미작성 |
| STAT-SEC-02 (ASSOCIATE 차단) | STAT-SEC-02 | 미작성 |
| STAT-SEC-03 (MEMBER 차단) | STAT-SEC-03 | 미작성 |
| STAT-SEC-04 (OPERATOR 허용) | STAT-SEC-04 | 미작성 |
| STAT-SEC-05 (부작용 없음) | STAT-SEC-05 | 미작성 |

---

## 9. API 응답 구조 (참고)

### 9-1. 엔드포인트 정의

```
GET /api/v1/surveys/{surveyId}/statistics
```

- **인증**: 필수 (Bearer Token)
- **인가**: OPERATOR 이상
- **성공 응답**: 200 OK
- **에러 응답**: 401 (비인증), 403 (비인가), 404 (설문 없음)

### 9-2. 질문 타입별 통계 구조 (개념)

| 질문 카테고리 | 통계 구조 |
|-------------|----------|
| TEXT (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD) | `{ responseCount, textResponses: [string] }` |
| SCALE (LINEAR_SCALE) | `{ responseCount, average, min, max, distribution: { value: count } }` |
| OPTION (MULTIPLE_CHOICE, DROPDOWN) | `{ responseCount, options: [{ optionId, optionText, count, percentage }] }` |
| OPTION (CHECKBOX) | `{ responseCount, options: [{ optionId, optionText, count, percentage }] }` -- 비율 합계 > 100% 가능 |
| GRID (MULTIPLE_CHOICE_GRID) | `{ responseCount, rows: [{ rowId, rowLabel, options: [{ optionId, optionText, count, percentage }] }] }` -- 행당 합계 ≤ 100% (비율 분모: 전체 설문 응답자 수) |
| GRID (CHECKBOX_GRID) | `{ responseCount, rows: [{ rowId, rowLabel, options: [{ optionId, optionText, count, percentage }] }] }` -- 행당 합계 > 100% 가능 (비율 분모: 전체 설문 응답자 수) |

> **참고**: 위 구조는 개념적 정의이며, 실제 DTO 설계는 구현 시 결정한다. 통계 구조에는 삭제된 질문/선택지/행의 `deleted` 상태 플래그를 포함해야 한다 (STAT-INV-07).

---

## 10. 기존 검증 기준서와의 관계

| 기존 기준 | 통계 API 관련성 |
|----------|---------------|
| INV-01 (중복 응답 방지) | 회원당 1회 응답이므로, 통계에서 회원 응답은 중복 카운트 없음 |
| INV-03 (휴지통 2단계 삭제) | 휴지통 설문도 통계 조회 가능 (STAT-INV-05) |
| INV-10 (선택지/행 soft delete) | 삭제된 선택지/행의 답변도 통계 포함 (STAT-INV-07) |
| INV-14 (질문 soft delete) | 삭제된 질문의 답변도 통계 포함 (STAT-INV-07) |
| INV-26 (응답 수정 PUT 전체 교체) | 수정 후 최종 응답만 통계 반영 (STAT-INTEG-02) |
| INV-28 (질문 변경 후 기존 응답 유효) | 추가된 질문에 대한 기존 응답자 미응답 처리 (STAT-INTEG-03) |
| INV-29 (본인 응답 조회 범위) | 통계 API는 전체 집계이며, 본인 응답 조회(INV-29)와 별개의 API |
| SEC-05 (비운영진 결과 조회 차단) | STAT-SEC-03 (MEMBER 차단)으로 동일 규칙 적용 |

---

## 11. 구현 현황 요약

| 카테고리 | 전체 | 상태 |
|---------|:---:|:---:|
| 도메인 규칙/불변조건 (STAT-INV-01~08) | 8 | 미작성 |
| 권한/보안 (STAT-SEC-01~05) | 5 | 미작성 |
| TEXT 카테고리 통계 (STAT-TXT-01~04) | 4 | 미작성 |
| SCALE 카테고리 통계 (STAT-SCL-01~06) | 6 | 미작성 |
| OPTION 카테고리 통계 (STAT-OPT-01~04) | 4 | 미작성 |
| CHECKBOX 카테고리 통계 (STAT-CHK-01~03) | 3 | 미작성 |
| GRID 카테고리 통계 (STAT-GRD-01~05) | 5 | 미작성 |
| 전체 요약 통계 (STAT-SUM-01~04) | 4 | 미작성 |
| 응답 경계값 (STAT-EDGE-01~06) | 6 | 미작성 |
| soft delete 경계값 (STAT-EDGE-10~13) | 4 | 미작성 |
| 설문 상태별 (STAT-EDGE-20~24) | 5 | 미작성 |
| 부정 시나리오 (STAT-NEG-01~03) | 3 | 미작성 |
| 복합 시나리오 (STAT-INTEG-01~05) | 5 | 미작성 |
| 성능 (STAT-PERF-01~02) | 2 | 미작성 |
| **합계** | **69** | **미작성** |
