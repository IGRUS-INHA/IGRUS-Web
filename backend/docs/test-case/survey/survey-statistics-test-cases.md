# 설문 통계 API 테스트 케이스

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2026-02-27 |
| 버전 | 1.0 |
| 검증 기준 문서 | [설문 통계 API 검증 기준서](../../../docs/criteria/survey/survey-statistics-verification-criteria.md) |
| 대상 기능 | 설문 통계 API (`GET /api/v1/surveys/{surveyId}/statistics`) |
| 테스트 케이스 수 | 69건 |
| 관련 문서 | [설문 도메인 테스트 케이스](./survey-domain-test-cases.md), [설문 통합 테스트 케이스](./survey-integration-test-cases.md) |

> 이 문서는 설문 통계 API의 검증 기준서(v1.1, 69건)를 기반으로 작성된 테스트 케이스이다.
> 각 테스트 케이스에는 대응하는 검증 기준 ID를 명시한다.
> - ⬜ 미구현 / 검토 필요

### 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-27 | 초안 작성. 검증 기준서 v1.1 기반 69건 전수 매핑 |

---

## 1. 개요

설문 통계 API(`GET /api/v1/surveys/{surveyId}/statistics`)의 테스트 케이스이다. 다음 영역을 검증한다:

- **도메인 규칙/불변조건**: 삭제된 응답/답변 제외, 소수점 처리, soft delete 데이터 처리
- **권한/보안**: RBAC(운영자만 통계 조회 가능), 인증/인가
- **질문 타입별 통계 정확성**: TEXT, SCALE, OPTION, CHECKBOX, GRID 카테고리별 통계 구조 및 수치
- **전체 요약 통계**: 총 응답 수, 응답 기간
- **경계값/엣지 케이스**: 응답 0건, 1건, 모든 응답 삭제, 비필수 질문, soft delete된 질문/선택지/행
- **설문 상태별 통계 조회**: 2축 상태 모델 및 휴지통 상태
- **부정 시나리오**: 존재하지 않는 설문, 음수 ID, 비숫자 ID
- **복합 시나리오**: 혼합 질문 유형, 응답 수정 후 통계, 질문 추가 후 통계
- **성능**: N+1 쿼리 방지, 대량 응답 응답 시간

### 테스트 레벨 구분

| 테스트 레벨 | 검증 대상 | 테스트 더블 |
|-----------|----------|-----------|
| 단위 테스트 (서비스) | 통계 집계 로직, 타입별 통계 변환, 삭제 필터링 | Repository Mock (런던파) |
| 통합 테스트 (서비스) | DB에서 실제 데이터 조회 후 통계 정확성, N+1 검증 | 실제 DB (H2) |
| 컨트롤러 통합 테스트 (MockMvc) | HTTP 인증/인가, 응답 구조, 상태 코드 | 실제 서비스 (full context) |

### 질문 타입별 통계 응답 구조 (참고)

| 질문 카테고리 | 통계 구조 |
|-------------|----------|
| TEXT (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD) | `{ responseCount, textResponses: [string] }` |
| SCALE (LINEAR_SCALE) | `{ responseCount, average, min, max, distribution: { value: count } }` |
| OPTION (MULTIPLE_CHOICE, DROPDOWN) | `{ responseCount, options: [{ optionId, optionText, count, percentage }] }` |
| OPTION (CHECKBOX) | `{ responseCount, options: [{ optionId, optionText, count, percentage }] }` -- 비율 합계 > 100% 가능 |
| GRID (MULTIPLE_CHOICE_GRID) | `{ responseCount, rows: [{ rowId, rowLabel, options: [...] }] }` -- 행당 비율 합계 <= 100% |
| GRID (CHECKBOX_GRID) | `{ responseCount, rows: [{ rowId, rowLabel, options: [...] }] }` -- 행당 비율 합계 > 100% 가능 |

---

## 2. 권한/보안 테스트 (TC-STAT-001 ~ TC-STAT-005)

> **테스트 레벨**: 컨트롤러 통합 테스트 (MockMvc)
> **참조**: 검증 기준서 2절 권한/보안 정책

### TC-STAT-001: 비인증 사용자 통계 조회 차단

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SEC-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 보안 (Authentication) |
| **사전 조건** | 인증 없음, 설문 존재 |
| **테스트 절차** | 1. `GET /api/v1/surveys/{surveyId}/statistics` 요청 (토큰 없이) |
| **입력 데이터** | surveyId: 유효한 설문 ID, Authorization 헤더 없음 |
| **기대 결과** | 401 Unauthorized |
| **비고** | Spring Security 필터 레벨에서 차단 |

### TC-STAT-002: ASSOCIATE 통계 조회 차단

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SEC-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 보안 (Authorization, RBAC) |
| **사전 조건** | ASSOCIATE 권한으로 로그인한 상태, 유효한 액세스 토큰 보유, 설문 존재 |
| **테스트 절차** | 1. ASSOCIATE 토큰으로 `GET /api/v1/surveys/{surveyId}/statistics` 요청 |
| **입력 데이터** | surveyId: 유효한 설문 ID, Authorization: Bearer {ASSOCIATE 토큰} |
| **기대 결과** | 403 Forbidden |
| **비고** | OPERATOR 미만 역할 차단 |

### TC-STAT-003: MEMBER 통계 조회 차단

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SEC-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 보안 (Authorization, RBAC) |
| **사전 조건** | MEMBER 권한으로 로그인한 상태, 유효한 액세스 토큰 보유, 설문 존재 |
| **테스트 절차** | 1. MEMBER 토큰으로 `GET /api/v1/surveys/{surveyId}/statistics` 요청 |
| **입력 데이터** | surveyId: 유효한 설문 ID, Authorization: Bearer {MEMBER 토큰} |
| **기대 결과** | 403 Forbidden |
| **비고** | 기존 검증 기준서 SEC-05 연장 |

### TC-STAT-004: OPERATOR 통계 조회 성공

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SEC-04 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 (Authorization, RBAC) |
| **사전 조건** | OPERATOR 권한으로 로그인한 상태, 유효한 액세스 토큰 보유, 설문 존재, 응답 데이터 존재 |
| **테스트 절차** | 1. OPERATOR 토큰으로 `GET /api/v1/surveys/{surveyId}/statistics` 요청 |
| **입력 데이터** | surveyId: 유효한 설문 ID, Authorization: Bearer {OPERATOR 토큰} |
| **기대 결과** | 200 OK, 통계 데이터 반환 (totalResponseCount, 질문별 통계 포함) |
| **비고** | 설문 작성자가 아닌 운영자도 조회 가능 (역할로만 판단) |

### TC-STAT-005: 비인가 접근 시 부작용 없음

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SEC-05 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 보안 (Principle of Least Privilege) |
| **사전 조건** | MEMBER 권한으로 로그인한 상태, 유효한 액세스 토큰 보유 |
| **테스트 절차** | 1. MEMBER 토큰으로 `GET /api/v1/surveys/{surveyId}/statistics` 요청 2. DB 상태 변경이 없는지 확인 |
| **입력 데이터** | surveyId: 유효한 설문 ID, Authorization: Bearer {MEMBER 토큰} |
| **기대 결과** | 403 Forbidden, DB에 아무런 변경 사항 없음 (통계 조회는 읽기 전용) |
| **비고** | 기존 검증 기준서 SEC-06 연장. 통계 조회는 읽기 전용이므로 부작용 가능성 낮으나 확인 필요 |

---

## 3. TEXT 카테고리 통계 테스트 (TC-STAT-010 ~ TC-STAT-013)

> **테스트 레벨**: 단위 테스트 (서비스)
> **대상 질문 유형**: SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD
> **통계 구조**: `{ responseCount, textResponses: [string] }`

### TC-STAT-010: TEXT 유형 통계 - 응답 수 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-TXT-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | SHORT_ANSWER 질문 1개, 해당 질문에 TextSurveyAnswer 3건 존재 (deleted=false) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출하여 해당 질문의 통계 조회 |
| **입력 데이터** | surveyId: 대상 설문 ID, SHORT_ANSWER 질문에 텍스트 응답 3건 ("답변1", "답변2", "답변3") |
| **기대 결과** | 해당 질문의 responseCount = 3 |
| **비고** | TextSurveyAnswer.textValue 기반 집계 |

### TC-STAT-011: TEXT 유형 통계 - 텍스트 응답 목록 반환

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-TXT-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | PARAGRAPH 질문 1개, 해당 질문에 TextSurveyAnswer 3건 존재 (응답 제출 시각이 서로 다름) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출하여 해당 질문의 통계 조회 2. textResponses 목록의 내용과 정렬 순서 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, PARAGRAPH 질문에 텍스트 응답 3건 (제출 시각 순서: "첫번째 응답", "두번째 응답", "세번째 응답") |
| **기대 결과** | textResponses 목록 3건 반환, textValue 포함, SurveyResponse.createdAt 오름차순 정렬 |
| **비고** | 응답 본문을 운영자가 확인할 수 있어야 함 |

### TC-STAT-012: DATE 유형도 TEXT 카테고리 통계 구조 적용

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-TXT-03 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | DATE 질문 1개, 해당 질문에 TextSurveyAnswer 2건 존재 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출하여 해당 질문의 통계 조회 2. TEXT 유형과 동일한 통계 구조인지 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, DATE 질문에 텍스트 응답 2건 ("2026-02-01", "2026-02-15") |
| **기대 결과** | TEXT 유형과 동일한 구조로 responseCount = 2, textResponses 목록 2건 |
| **비고** | DATE, TIME, FILE_UPLOAD 모두 TextSurveyAnswer 사용. TIME, FILE_UPLOAD도 동일하게 적용됨을 대표 검증 |

### TC-STAT-013: FILE_UPLOAD 유형 텍스트(URL) 목록 반환

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-TXT-04 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | FILE_UPLOAD 질문 1개, 해당 질문에 URL 문자열 응답 2건 존재 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출하여 해당 질문의 통계 조회 2. textResponses에 URL 문자열이 포함되는지 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, FILE_UPLOAD 질문에 응답 2건 ("https://example.com/file1.pdf", "https://example.com/file2.pdf") |
| **기대 결과** | textResponses 목록에 URL 문자열 2건 포함 |
| **비고** | INV-30 (1차: URL 텍스트 저장) 관련 |

---

## 4. SCALE 카테고리 통계 테스트 (TC-STAT-020 ~ TC-STAT-025)

> **테스트 레벨**: 단위 테스트 (서비스)
> **대상 질문 유형**: LINEAR_SCALE
> **통계 구조**: `{ responseCount, average, min, max, distribution: { value: count } }`

### TC-STAT-020: LINEAR_SCALE 응답 수 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=5), NumericSurveyAnswer 5건 [1, 2, 3, 4, 5] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출하여 해당 질문의 통계 조회 |
| **입력 데이터** | LINEAR_SCALE 질문, 응답값: [1, 2, 3, 4, 5] |
| **기대 결과** | responseCount = 5 |
| **비고** | NumericSurveyAnswer.numericValue 기반 집계 |

### TC-STAT-021: LINEAR_SCALE 평균값 정확성 (나누어 떨어지는 경우)

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=5), NumericSurveyAnswer 5건 [1, 2, 3, 4, 5] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. average 값 확인 |
| **입력 데이터** | LINEAR_SCALE 질문, 응답값: [1, 2, 3, 4, 5] |
| **기대 결과** | average = 3.0 |
| **비고** | STAT-INV-08 적용. (1+2+3+4+5)/5 = 3.0, 나누어 떨어지는 경우 |

### TC-STAT-022: LINEAR_SCALE 최솟값/최댓값 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 경계값 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=10), NumericSurveyAnswer 3건 [2, 5, 8] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. min, max 값 확인 |
| **입력 데이터** | LINEAR_SCALE 질문 (1~10), 응답값: [2, 5, 8] |
| **기대 결과** | min = 2, max = 8 |
| **비고** | 실제 응답값 기준 min/max (스케일 범위 1~10이 아닌 실제 응답 기준) |

### TC-STAT-023: LINEAR_SCALE 값별 분포(히스토그램) 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-04 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=5), NumericSurveyAnswer 7건 [1, 1, 2, 3, 3, 3, 5] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. distribution 맵 확인 |
| **입력 데이터** | LINEAR_SCALE 질문 (1~5), 응답값: [1, 1, 2, 3, 3, 3, 5] |
| **기대 결과** | distribution: {1: 2, 2: 1, 3: 3, 4: 0, 5: 1} |
| **비고** | 미선택 값(4)도 0으로 포함되어야 함 |

### TC-STAT-024: LINEAR_SCALE 응답 1건일 때 평균=최솟값=최댓값

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-05 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=5), NumericSurveyAnswer 1건 [3] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. average, min, max, distribution 확인 |
| **입력 데이터** | LINEAR_SCALE 질문 (1~5), 응답값: [3] |
| **기대 결과** | average = 3.0, min = 3, max = 3, distribution: {1: 0, 2: 0, 3: 1, 4: 0, 5: 0} |
| **비고** | 경계 케이스: 응답이 1건일 때 모든 통계값이 해당 응답값과 동일 |

### TC-STAT-025: LINEAR_SCALE 소수점 반올림 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SCL-06 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | LINEAR_SCALE 질문 1개 (scaleMin=1, scaleMax=5), NumericSurveyAnswer 3건 [1, 2, 4] |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. average 값의 소수점 처리 확인 |
| **입력 데이터** | LINEAR_SCALE 질문 (1~5), 응답값: [1, 2, 4] |
| **기대 결과** | average = 2.3 (7/3 = 2.333... -> RoundingMode.HALF_UP, scale=1) |
| **비고** | STAT-INV-08 검증. 나누어 떨어지지 않는 소수 결과 확인 |

---

## 5. OPTION 카테고리 통계 테스트 (TC-STAT-030 ~ TC-STAT-033)

> **테스트 레벨**: 단위 테스트 (서비스)
> **대상 질문 유형**: MULTIPLE_CHOICE, DROPDOWN
> **통계 구조**: `{ responseCount, options: [{ optionId, optionText, count, percentage }] }`

### TC-STAT-030: MULTIPLE_CHOICE 옵션별 선택 수 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-OPT-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MULTIPLE_CHOICE 질문 1개, 옵션 A/B/C, OptionSurveyAnswer: A=3건, B=2건, C=0건 (총 응답자 5명) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 옵션별 count 확인 |
| **입력 데이터** | MC 질문, 옵션: [A, B, C], 응답: 응답자1=A, 응답자2=A, 응답자3=A, 응답자4=B, 응답자5=B |
| **기대 결과** | 옵션별 선택 수: A=3, B=2, C=0 |
| **비고** | OptionSurveyAnswer.selectedOption 기반 집계 |

### TC-STAT-031: MULTIPLE_CHOICE 옵션별 비율 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-OPT-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MULTIPLE_CHOICE 질문 1개, 총 5건 응답, A=3, B=2, C=0 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 옵션별 percentage 확인 3. 비율 합계 = 100% 확인 |
| **입력 데이터** | MC 질문, 옵션: [A, B, C], 총 응답 5건 |
| **기대 결과** | 비율: A=60.0%, B=40.0%, C=0.0% (합계 100.0%) |
| **비고** | 단일 선택이므로 비율 합계 100%. STAT-INV-08 소수점 정책 적용 |

### TC-STAT-032: DROPDOWN 옵션별 선택 수/비율 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-OPT-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | DROPDOWN 질문 1개, 옵션 X/Y/Z, OptionSurveyAnswer: X=2건, Y=1건, Z=0건 (총 응답자 3명) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 옵션별 count와 percentage 확인 |
| **입력 데이터** | DROPDOWN 질문, 옵션: [X, Y, Z], 응답: 응답자1=X, 응답자2=X, 응답자3=Y |
| **기대 결과** | 옵션별 선택 수: X=2, Y=1, Z=0. 비율: X=66.7%, Y=33.3%, Z=0.0% (합계 100.0%) |
| **비고** | MULTIPLE_CHOICE와 동일한 통계 구조 |

### TC-STAT-033: 미선택 옵션도 통계에 0으로 포함

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-OPT-04 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | MULTIPLE_CHOICE 질문 1개, 옵션 A/B/C, C는 아무도 선택하지 않음 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 미선택 옵션 C가 통계에 포함되는지 확인 |
| **입력 데이터** | MC 질문, 옵션: [A, B, C], 응답: 응답자1=A, 응답자2=B |
| **기대 결과** | 옵션 C: count=0, percentage=0.0%. 모든 옵션(A, B, C)이 통계 응답에 포함됨 |
| **비고** | 선택되지 않은 옵션도 통계에 포함되어야 전체 분포를 파악 가능 |

---

## 6. CHECKBOX 카테고리 통계 테스트 (TC-STAT-040 ~ TC-STAT-042)

> **테스트 레벨**: 단위 테스트 (서비스)
> **대상 질문 유형**: CHECKBOX
> **통계 구조**: `{ responseCount, options: [{ optionId, optionText, count, percentage }] }` -- 비율 합계 > 100% 가능

### TC-STAT-040: CHECKBOX 옵션별 선택 수 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-CHK-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | CHECKBOX 질문 1개, 옵션 A/B/C, 응답자1: A,B 선택 / 응답자2: B,C 선택 / 응답자3: A 선택 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 옵션별 count 확인 |
| **입력 데이터** | CHECKBOX 질문, 옵션: [A, B, C], 응답: 응답자1={A,B}, 응답자2={B,C}, 응답자3={A} |
| **기대 결과** | 옵션별 선택 수: A=2, B=2, C=1 |
| **비고** | 복수 선택이므로 선택지당 1개 OptionSurveyAnswer 생성 |

### TC-STAT-041: CHECKBOX 비율 합계가 100% 초과 가능

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-CHK-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | CHECKBOX 질문 1개, 총 응답자 3명, A=2, B=2, C=1 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 옵션별 percentage 확인 3. 비율 합계 > 100% 확인 |
| **입력 데이터** | CHECKBOX 질문, 총 응답자 3명 |
| **기대 결과** | 비율: A=66.7%, B=66.7%, C=33.3% (합계 166.7%). 비율 = 해당 옵션 선택 수 / 총 응답자 수 |
| **비고** | 복수 선택 특성으로 합계 100% 초과 가능. STAT-INV-08 소수점 반올림 적용 |

### TC-STAT-042: CHECKBOX 총 응답자 수와 개별 선택 합계 구분

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-CHK-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | CHECKBOX 질문 1개, 응답자 2명, 각각 3개씩 선택 (총 OptionSurveyAnswer 행 6개) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. responseCount와 개별 옵션 count 합계가 다른지 확인 |
| **입력 데이터** | CHECKBOX 질문, 옵션: [A, B, C, D], 응답자1={A,B,C}, 응답자2={B,C,D} |
| **기대 결과** | responseCount = 2 (SurveyResponse 기준), 옵션별 count 합계 = 6 (OptionSurveyAnswer 기준). responseCount != 옵션별 count 합계 |
| **비고** | 응답 수는 SurveyResponse 기준, 선택 수는 OptionSurveyAnswer 기준으로 구분 |

---

## 7. GRID 카테고리 통계 테스트 (TC-STAT-050 ~ TC-STAT-054)

> **테스트 레벨**: 단위 테스트 (서비스)
> **대상 질문 유형**: MULTIPLE_CHOICE_GRID, CHECKBOX_GRID
> **통계 구조**: `{ responseCount, rows: [{ rowId, rowLabel, options: [{ optionId, optionText, count, percentage }] }] }`

### TC-STAT-050: MC_GRID 행별 옵션 분포 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-GRD-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC_GRID 질문 1개, 행: [수학, 영어], 옵션: [만족, 불만족]. 응답자1: 수학=만족, 영어=불만족 / 응답자2: 수학=불만족, 영어=만족 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 행별 옵션 분포 확인 |
| **입력 데이터** | MC_GRID 질문, 행=[수학, 영어], 옵션=[만족, 불만족], 응답자2명 |
| **기대 결과** | 수학: {만족: 1, 불만족: 1}, 영어: {만족: 1, 불만족: 1} |
| **비고** | GridSurveyAnswer(selectedRow + selectedOption) 기반 집계 |

### TC-STAT-051: MC_GRID 행별 비율 합계 <= 100%

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-GRD-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC_GRID 질문 1개 (필수), 행마다 단일 선택, 응답자 3명 전원 응답 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 각 행의 옵션별 percentage 합계 확인 |
| **입력 데이터** | MC_GRID 질문 (required=true), 행=[항목1, 항목2], 옵션=[A, B, C], 응답자 3명 |
| **기대 결과** | 각 행 내 옵션별 비율 합계 <= 100%. 비율 = 해당 옵션 선택 수 / 전체 설문 응답자 수 |
| **비고** | MC_GRID는 행당 단일 선택. 비필수 질문에서 미응답자가 있으면 합계 < 100%. 비율 분모는 전체 설문 응답자 수 |

### TC-STAT-052: CB_GRID 행별 옵션 분포 (복수 선택)

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-GRD-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | CB_GRID 질문 1개, 행: [A, B], 옵션: [X, Y, Z]. 응답자1: A={X,Y}, B={Z} / 응답자2: A={X}, B={X,Y} |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 행별 옵션 분포 확인 |
| **입력 데이터** | CB_GRID 질문, 행=[A, B], 옵션=[X, Y, Z], 응답자 2명 |
| **기대 결과** | A: {X: 2, Y: 1, Z: 0}, B: {X: 1, Y: 1, Z: 1} |
| **비고** | 행당 복수 선택 가능 (CHECKBOX_GRID 특성) |

### TC-STAT-053: CB_GRID 행별 비율 합계 100% 초과 가능

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-GRD-04 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | CB_GRID 질문 1개, 복수 선택으로 인한 비율 합계 초과 케이스 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 각 행의 옵션별 percentage 합계 확인 |
| **입력 데이터** | CB_GRID 질문, 행=[A], 옵션=[X, Y, Z], 응답자 2명, A행: 응답자1={X,Y,Z}, 응답자2={X,Y} |
| **기대 결과** | A행: X=100.0%, Y=100.0%, Z=50.0% (합계 250.0% > 100%). 비율 = 해당 옵션 선택 수 / 전체 설문 응답자 수 |
| **비고** | CHECKBOX와 동일 논리로 행당 합계 > 100% 가능. 비율 분모는 전체 설문 응답자 수 |

### TC-STAT-054: 특정 행에 응답이 없는 경우 (비필수 질문)

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-GRD-05 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | CB_GRID 질문 1개 (required=false), 행: [A, B], 응답자가 A행만 응답하고 B행은 미응답 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 미응답 행 B의 통계 확인 |
| **입력 데이터** | CB_GRID 질문 (required=false), 행=[A, B], 옵션=[X, Y], 응답자1: A={X} (B행 미응답) |
| **기대 결과** | B행: 모든 옵션 count = 0 |
| **비고** | 비필수 질문의 미응답 행 처리 |

---

## 8. 전체 요약 통계 테스트 (TC-STAT-060 ~ TC-STAT-063)

> **테스트 레벨**: 단위 테스트 (서비스) / 통합 테스트 (서비스)

### TC-STAT-060: 총 응답 수 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SUM-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | 설문에 SurveyResponse 5건 (모두 deleted=false) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. totalResponseCount 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, SurveyResponse 5건 (deleted=false) |
| **기대 결과** | totalResponseCount = 5 |
| **비고** | STAT-INV-01에 의해 deleted=true 레코드 제외 |

### TC-STAT-061: 삭제된 응답 제외 후 총 응답 수

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SUM-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | 설문에 SurveyResponse 5건, 그 중 2건 deleted=true |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. totalResponseCount 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, SurveyResponse 5건 (3건 deleted=false, 2건 deleted=true) |
| **기대 결과** | totalResponseCount = 3 (deleted=true인 2건 제외) |
| **비고** | STAT-INV-01 검증. Repository 쿼리에 `deleted = false` 조건 포함 확인 |

### TC-STAT-062: 응답 기간 정확성

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SUM-03 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | 설문에 활성(deleted=false) 응답 여러 건, 첫 응답 createdAt = 2026-02-01T00:00:00Z, 마지막 응답 createdAt = 2026-02-25T00:00:00Z |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 응답 시작일/종료일 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, 응답 createdAt: 2026-02-01, 2026-02-10, 2026-02-25 |
| **기대 결과** | 응답 시작일 = 2026-02-01T00:00:00Z, 응답 종료일 = 2026-02-25T00:00:00Z |
| **비고** | 활성(deleted=false) 응답의 createdAt 기준 min/max |

### TC-STAT-063: 응답 0건일 때 응답 기간

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-SUM-04 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | 설문에 응답 0건 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 응답 기간 필드 확인 |
| **입력 데이터** | surveyId: 응답 0건인 설문 ID |
| **기대 결과** | 응답 기간 = null 또는 미표시 |
| **비고** | STAT-INV-03과 연관. 응답이 없을 때 응답 기간은 의미 없음 |

---

## 9. 응답 데이터 관련 경계값 테스트 (TC-STAT-070 ~ TC-STAT-075)

> **테스트 레벨**: 단위 테스트 (서비스) / 통합 테스트 (서비스)

### TC-STAT-070: 응답 0건 설문 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 경계값 |
| **사전 조건** | 설문 존재, 질문 존재 (MC 질문 1개, LINEAR_SCALE 질문 1개), 응답 0건 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 200 OK 반환 확인 3. 총 응답 수, 질문별 통계 확인 |
| **입력 데이터** | surveyId: 응답 0건인 설문 ID |
| **기대 결과** | 200 OK, totalResponseCount = 0, OPTION/CHECKBOX/GRID 유형의 각 옵션 percentage = 0.0, SCALE 유형 average = 0.0 또는 null. 0 나누기(division by zero) 발생하지 않음 |
| **비고** | STAT-INV-03. 분모 0일 때 비율은 0.0으로 처리. 에러를 발생시키지 않아야 함 |

### TC-STAT-071: 응답 1건 설문 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 경계값 |
| **사전 조건** | 설문에 응답 정확히 1건, 모든 질문에 응답 완료 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. totalResponseCount, 질문별 통계 확인 |
| **입력 데이터** | surveyId: 응답 1건인 설문 ID |
| **기대 결과** | totalResponseCount = 1, 각 질문에 해당 응답 정확히 반영 |
| **비고** | 최소 유효 응답 경계값 |

### TC-STAT-072: 모든 응답이 삭제된 설문

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-03 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 경계값 |
| **사전 조건** | 설문에 SurveyResponse 3건, 모두 deleted=true |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 결과가 응답 0건과 동일한지 확인 |
| **입력 데이터** | surveyId: 대상 설문 ID, SurveyResponse 3건 (모두 deleted=true) |
| **기대 결과** | 200 OK, totalResponseCount = 0, 빈 통계 (TC-STAT-070과 동일 결과) |
| **비고** | STAT-INV-01 + STAT-INV-03 복합 검증. 삭제된 응답 제외 후 유효 응답 0건 |

### TC-STAT-073: 비필수 질문에 일부만 응답한 경우

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-04 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | 비필수(required=false) MC 질문 1개, 응답자 5명 중 3명만 해당 질문에 응답 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 해당 질문의 responseCount 확인 |
| **입력 데이터** | MC 질문 (required=false), 총 응답자 5명, 해당 질문 응답자 3명 |
| **기대 결과** | 해당 질문 responseCount = 3. 미응답 2명은 통계에서 제외 |
| **비고** | 비필수 질문 미응답 시 SurveyAnswer 행 자체가 없음 |

### TC-STAT-074: 모든 질문이 비필수이고 빈 응답 제출

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-05 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | 모든 질문 required=false, 응답자가 아무 질문에도 응답하지 않고 제출 (빈 응답) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. totalResponseCount와 질문별 responseCount 확인 |
| **입력 데이터** | 모든 질문이 비필수인 설문, 응답자 1명 (아무 응답 없이 제출) |
| **기대 결과** | totalResponseCount = 1 (SurveyResponse는 존재), 각 질문 responseCount = 0 (SurveyAnswer가 없음), 텍스트 목록 비어 있음, 모든 옵션 count = 0 |
| **비고** | SurveyResponse는 카운트되지만, SurveyAnswer가 없으므로 질문별 응답 수는 0 |

### TC-STAT-075: 삭제된 SurveyAnswer가 질문별 통계에서 제외

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-06 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC 질문 1개, 옵션 A/B/C, 응답자 3명: 응답자1=A, 응답자2=B, 응답자3=A. 응답자3의 SurveyAnswer만 deleted=true (SurveyResponse 자체는 deleted=false) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 해당 질문의 옵션별 count와 responseCount 확인 |
| **입력 데이터** | MC 질문, 옵션=[A, B, C], 응답자3의 SurveyAnswer deleted=true |
| **기대 결과** | 옵션별 선택 수: A=1, B=1, C=0 (deleted=true인 응답자3의 A 선택이 제외됨). 해당 질문 responseCount = 2 |
| **비고** | STAT-INV-02 전용 검증. SurveyResponse는 deleted=false이지만 SurveyAnswer만 deleted=true인 경우 |

---

## 10. 질문/선택지 soft delete 관련 경계값 테스트 (TC-STAT-080 ~ TC-STAT-083)

> **테스트 레벨**: 단위 테스트 (서비스) / 통합 테스트 (서비스)
> **핵심 규칙**: STAT-INV-07 -- soft delete된 질문/선택지/행의 기존 답변은 통계에 **포함**하되, 삭제 여부를 표시한다.

### TC-STAT-080: soft delete된 질문의 기존 답변 통계 포함

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-10 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC 질문에 응답 3건 존재, 이후 해당 질문을 soft delete (deleted=true) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 삭제된 질문의 답변 3건이 통계에 포함되는지 확인 3. 질문 삭제 여부 표시 확인 |
| **입력 데이터** | MC 질문 (deleted=true), 기존 응답 3건 |
| **기대 결과** | 해당 질문의 답변 3건이 통계에 포함됨. 질문의 삭제 여부가 표시됨 (deleted=true 플래그) |
| **비고** | STAT-INV-07. 운영자는 삭제된 질문의 과거 답변도 확인 가능 |

### TC-STAT-081: soft delete된 선택지의 기존 선택 통계 포함

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-11 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC 질문, 옵션 A/B/C 중 C를 soft delete. 기존에 C를 선택한 응답 2건 존재 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 삭제된 옵션 C의 선택 수가 통계에 포함되는지 확인 3. C의 삭제 여부 표시 확인 |
| **입력 데이터** | MC 질문, 옵션 A(활성)/B(활성)/C(deleted=true), A 선택 3건, B 선택 1건, C 선택 2건 |
| **기대 결과** | 옵션별: A=3, B=1, C=2 (C에 삭제 표시). 삭제된 C의 응답도 통계에 포함 |
| **비고** | STAT-INV-07. INV-10 (선택지 soft delete)에 의해 FK 보존됨 |

### TC-STAT-082: soft delete된 행의 기존 응답 통계 포함

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-12 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC_GRID 질문, 행 [수학, 영어] 중 "수학" 행을 soft delete. 기존 "수학" 행 응답 존재 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 삭제된 "수학" 행의 분포가 통계에 포함되는지 확인 3. 행 삭제 여부 표시 확인 |
| **입력 데이터** | MC_GRID 질문, 행=[수학(deleted=true), 영어(활성)], 옵션=[만족, 불만족], "수학" 행 응답 2건 |
| **기대 결과** | "수학" 행의 분포가 통계에 포함됨. 행의 삭제 여부가 표시됨 (deleted=true 플래그) |
| **비고** | STAT-INV-07. INV-10 (행 soft delete) |

### TC-STAT-083: 모든 선택지가 soft delete된 질문

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-13 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 경계값 |
| **사전 조건** | MC 질문, 옵션 A/B/C 모두 soft delete (deleted=true). 기존 응답 존재 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 모든 삭제된 옵션의 기존 응답 수가 반환되는지 확인 |
| **입력 데이터** | MC 질문, 옵션 A(deleted=true)/B(deleted=true)/C(deleted=true), 기존 응답: A=2, B=1, C=1 |
| **기대 결과** | 모든 옵션이 삭제 표시와 함께 기존 응답 수 반환: A=2, B=1, C=1 |
| **비고** | 극단적 경우. 기존 데이터 보존이 핵심 |

---

## 11. 설문 상태별 통계 조회 테스트 (TC-STAT-090 ~ TC-STAT-094)

> **테스트 레벨**: 단위 테스트 (서비스) / 컨트롤러 통합 테스트 (MockMvc)
> **핵심 규칙**: STAT-INV-04 -- 설문이 존재하고 deleted=false이면 모든 상태에서 통계 조회 가능

### TC-STAT-090: UNPUBLISHED + NOT_STARTED 설문 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-20 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | (UNPUBLISHED, NOT_STARTED) 설문, 응답 0건 |
| **테스트 절차** | 1. OPERATOR 토큰으로 통계 API 조회 |
| **입력 데이터** | surveyId: (U, NS) 상태 설문 ID |
| **기대 결과** | 200 OK, 빈 통계 (totalResponseCount=0) |
| **비고** | STAT-INV-04. 아직 공개 전이지만 운영자는 내부 확인 가능 |

### TC-STAT-091: PUBLISHED + OPEN 설문 통계 조회 (실시간)

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-21 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | (PUBLISHED, OPEN) 설문, 응답 10건 진행 중 |
| **테스트 절차** | 1. OPERATOR 토큰으로 통계 API 조회 |
| **입력 데이터** | surveyId: (P, O) 상태 설문 ID, 현재 응답 10건 |
| **기대 결과** | 200 OK, 현재까지 제출된 응답 기준 통계 (totalResponseCount=10) |
| **비고** | 응답 수집 중 실시간 모니터링 시나리오 |

### TC-STAT-092: PUBLISHED + CLOSED 설문 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-22 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | (PUBLISHED, CLOSED) 설문, 응답 50건 |
| **테스트 절차** | 1. OPERATOR 토큰으로 통계 API 조회 |
| **입력 데이터** | surveyId: (P, C) 상태 설문 ID |
| **기대 결과** | 200 OK, 전체 응답 통계 반환 |
| **비고** | 가장 일반적인 사용 시나리오 (마감 후 결과 확인) |

### TC-STAT-093: 휴지통 설문 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-23 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | (PUBLISHED, CLOSED, trashedAt != null) 설문, 응답 존재 |
| **테스트 절차** | 1. OPERATOR 토큰으로 통계 API 조회 |
| **입력 데이터** | surveyId: 휴지통 상태 설문 ID |
| **기대 결과** | 200 OK, 정상 통계 반환 |
| **비고** | STAT-INV-05. 휴지통 이동만으로 통계 데이터 접근이 차단되지 않아야 함 |

### TC-STAT-094: 영구 삭제된 설문 통계 조회 시도

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-EDGE-24 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 비정상 |
| **사전 조건** | deleted=true인 설문 |
| **테스트 절차** | 1. OPERATOR 토큰으로 통계 API 조회 |
| **입력 데이터** | surveyId: 영구 삭제된 설문 ID |
| **기대 결과** | 404 Not Found (SurveyNotFoundException) |
| **비고** | STAT-INV-06. 영구 삭제된 설문은 존재하지 않는 것으로 처리 |

---

## 12. 부정 시나리오 테스트 (TC-STAT-100 ~ TC-STAT-102)

> **테스트 레벨**: 컨트롤러 통합 테스트 (MockMvc)

### TC-STAT-100: 존재하지 않는 설문 ID로 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-NEG-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 비정상 |
| **사전 조건** | surveyId=999999 (존재하지 않음) |
| **테스트 절차** | 1. OPERATOR 토큰으로 `GET /api/v1/surveys/999999/statistics` 요청 |
| **입력 데이터** | surveyId: 999999 |
| **기대 결과** | 404 Not Found (SurveyNotFoundException) |
| **비고** | 기존 SurveyNotFoundException 재사용 |

### TC-STAT-101: surveyId 음수로 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-NEG-02 |
| **우선순위** | 하 (P2) |
| **테스트 유형** | 비정상 |
| **사전 조건** | surveyId=-1 |
| **테스트 절차** | 1. OPERATOR 토큰으로 `GET /api/v1/surveys/-1/statistics` 요청 |
| **입력 데이터** | surveyId: -1 |
| **기대 결과** | 400 Bad Request |
| **비고** | @Positive Bean Validation 적용으로 음수 ID 차단 |

### TC-STAT-102: surveyId 비숫자 문자열로 통계 조회

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-NEG-03 |
| **우선순위** | 하 (P2) |
| **테스트 유형** | 비정상 |
| **사전 조건** | surveyId="abc" |
| **테스트 절차** | 1. OPERATOR 토큰으로 `GET /api/v1/surveys/abc/statistics` 요청 |
| **입력 데이터** | surveyId: "abc" |
| **기대 결과** | 400 Bad Request |
| **비고** | Spring 기본 타입 변환 에러 (MethodArgumentTypeMismatchException) |

---

## 13. 복합 시나리오 테스트 (TC-STAT-110 ~ TC-STAT-114)

> **테스트 레벨**: 통합 테스트 (서비스)

### TC-STAT-110: 다양한 질문 유형이 혼합된 설문 통계

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-INTEG-01 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | 설문에 SHORT_ANSWER, MC, CHECKBOX, LINEAR_SCALE, MC_GRID 각 1개씩 총 5개 질문, 응답 10건 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 각 질문 유형별 통계 구조 확인 3. 모든 질문의 responseCount 확인 |
| **입력 데이터** | 5개 질문 유형이 혼합된 설문, 응답 10건 |
| **기대 결과** | TEXT: textResponses 10건, MC: 옵션별 수/비율 (합계 100%), CHECKBOX: 복수선택 수/비율 (합계 > 100% 가능), LINEAR_SCALE: average/min/max/distribution, MC_GRID: 행별 옵션 분포. 모든 질문의 responseCount = 10 |
| **비고** | 실제 운영 시 가장 일반적인 형태. 전체 통계 정합성 확인 |

### TC-STAT-111: 응답 수정(PUT) 후 통계 반영

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-INTEG-02 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | MC 질문, 응답자가 옵션 A를 선택한 후 B로 수정 (INV-26: PUT 전체 교체) |
| **테스트 절차** | 1. 응답 수정 후 통계 서비스 메서드 호출 2. 수정 전 선택(A)이 아닌 수정 후 선택(B)만 반영되는지 확인 |
| **입력 데이터** | MC 질문, 옵션=[A, B], 응답자1: A -> B로 수정 |
| **기대 결과** | A=0, B=1 (수정 후 최종 응답만 반영) |
| **비고** | INV-26 (PUT 전체 교체) 이후 통계 정합성. orphanRemoval로 기존 답변 삭제됨 |

### TC-STAT-112: 질문 추가 후 기존 응답자 미응답 질문 통계

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-INTEG-03 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | 설문에 질문 2개, 응답 5건 제출 후 질문 1개 추가 |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 기존 질문과 새 질문의 responseCount 비교 |
| **입력 데이터** | 기존 질문 2개 (응답 5건), 새로 추가된 질문 1개 |
| **기대 결과** | 기존 질문 2개: responseCount = 5. 새 질문: responseCount = 0 |
| **비고** | INV-28 (질문 변경 후 기존 응답 유효). 새 질문에는 기존 응답자의 답변 없음 |

### TC-STAT-113: 비회원(PUBLIC) 응답 포함 통계

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-INTEG-04 |
| **우선순위** | 상 (P0) |
| **테스트 유형** | 정상 |
| **사전 조건** | PUBLIC 설문, 회원 응답 3건 + 비회원 응답 2건 (SurveyResponse.user = null) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. totalResponseCount 확인 |
| **입력 데이터** | PUBLIC 설문, 회원 응답 3건, 비회원 응답 2건 |
| **기대 결과** | totalResponseCount = 5 (회원 + 비회원 합산) |
| **비고** | SurveyResponse.user = null인 비회원 응답도 통계에 포함 |

### TC-STAT-114: 질문 displayOrder 순서로 통계 반환

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-INTEG-05 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | 질문 A(displayOrder=2), B(displayOrder=0), C(displayOrder=1) |
| **테스트 절차** | 1. 통계 서비스 메서드 호출 2. 질문별 통계의 순서 확인 |
| **입력 데이터** | 질문 3개 (displayOrder 순서: B=0, C=1, A=2) |
| **기대 결과** | 질문별 통계가 displayOrder 오름차순으로 정렬: B, C, A 순서 |
| **비고** | 설문 폼 순서와 통계 순서 일치. 운영자에게 일관된 경험 제공 |

---

## 14. 성능 테스트 (TC-STAT-120 ~ TC-STAT-121)

> **테스트 레벨**: 통합 테스트 (서비스)

### TC-STAT-120: N+1 쿼리 방지

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-PERF-01 |
| **우선순위** | 중 (P1) |
| **테스트 유형** | 정상 |
| **사전 조건** | 질문 10개, 각 질문에 선택지 5개, 응답 100건 |
| **테스트 절차** | 1. 통계 API 호출 2. 실행된 SQL 쿼리 수 확인 (Hibernate 쿼리 로깅 또는 쿼리 카운터) |
| **입력 데이터** | 대규모 설문: 질문 10개, 선택지 50개, 응답 100건 |
| **기대 결과** | 질문/선택지/행 조회가 fetch join 또는 @EntityGraph로 최적화됨. 쿼리 수가 질문 수에 비례하지 않음 |
| **비고** | 대규모 설문에서 성능 문제 방지. 쿼리 수가 합리적 범위 내인지 확인 |

### TC-STAT-121: 대량 응답 설문 통계 응답 시간

| 항목 | 내용 |
|------|------|
| **검증 기준 ID** | STAT-PERF-02 |
| **우선순위** | 하 (P2) |
| **테스트 유형** | 정상 |
| **사전 조건** | 질문 50개 (최대), 응답 1000건 |
| **테스트 절차** | 1. 통계 API 호출 2. 응답 시간 측정 |
| **입력 데이터** | 최대 규모 설문: 질문 50개, 응답 1000건 |
| **기대 결과** | 응답 시간 < 3초이면 PASS, 3초 이상이면 FAIL |
| **비고** | 목표치이며, 정식 SLA는 미확정. 추후 운영 환경 성능 측정 후 조정 가능 |

---

## 15. 도메인 규칙/불변조건 커버리지 매핑

> 검증 기준서의 STAT-INV-01~08이 어떤 테스트 케이스로 검증되는지 매핑한다.

| 검증 기준 | 설명 | 커버 테스트 케이스 ID | 상태 |
|----------|------|---------------------|------|
| STAT-INV-01 (삭제된 응답 제외) | deleted=true인 SurveyResponse는 통계에서 제외 | TC-STAT-061, TC-STAT-072 | ⬜ |
| STAT-INV-02 (삭제된 답변 제외) | deleted=true인 SurveyAnswer는 통계에서 제외 | TC-STAT-075 | ⬜ |
| STAT-INV-03 (응답 0건 통계 가능) | 응답 0건이어도 200 OK 반환 | TC-STAT-070, TC-STAT-063 | ⬜ |
| STAT-INV-04 (모든 상태 통계 가능) | 모든 유효 상태에서 통계 조회 가능 | TC-STAT-090, TC-STAT-091, TC-STAT-092 | ⬜ |
| STAT-INV-05 (휴지통 통계 가능) | 휴지통 설문도 통계 조회 가능 | TC-STAT-093 | ⬜ |
| STAT-INV-06 (영구 삭제 통계 불가) | deleted=true 설문은 404 반환 | TC-STAT-094 | ⬜ |
| STAT-INV-07 (삭제된 질문/선택지/행 답변 포함) | soft delete된 질문/선택지/행의 기존 답변 통계 포함 | TC-STAT-080, TC-STAT-081, TC-STAT-082, TC-STAT-083 | ⬜ |
| STAT-INV-08 (소수점 처리 HALF_UP) | 평균/비율은 HALF_UP 소수 첫째 자리 반올림 | TC-STAT-025, TC-STAT-041 | ⬜ |

---

## 16. 권한/보안 커버리지 매핑

| 검증 기준 | 설명 | 커버 테스트 케이스 ID | 상태 |
|----------|------|---------------------|------|
| STAT-SEC-01 (비인증 차단) | 비인증 사용자 401 | TC-STAT-001 | ⬜ |
| STAT-SEC-02 (ASSOCIATE 차단) | ASSOCIATE 403 | TC-STAT-002 | ⬜ |
| STAT-SEC-03 (MEMBER 차단) | MEMBER 403 | TC-STAT-003 | ⬜ |
| STAT-SEC-04 (OPERATOR 허용) | OPERATOR 200 OK | TC-STAT-004 | ⬜ |
| STAT-SEC-05 (부작용 없음) | 비인가 접근 시 DB 변경 없음 | TC-STAT-005 | ⬜ |

---

## 17. 검증 기준 전수 매핑 (Traceability Matrix)

> 검증 기준서의 69건이 모두 하나 이상의 테스트 케이스로 커버되는지 확인한다.

| 검증 기준 ID | 테스트 케이스 ID | 상태 |
|-------------|----------------|------|
| STAT-INV-01 | TC-STAT-061, TC-STAT-072 | ⬜ |
| STAT-INV-02 | TC-STAT-075 | ⬜ |
| STAT-INV-03 | TC-STAT-070, TC-STAT-063 | ⬜ |
| STAT-INV-04 | TC-STAT-090, TC-STAT-091, TC-STAT-092 | ⬜ |
| STAT-INV-05 | TC-STAT-093 | ⬜ |
| STAT-INV-06 | TC-STAT-094 | ⬜ |
| STAT-INV-07 | TC-STAT-080, TC-STAT-081, TC-STAT-082, TC-STAT-083 | ⬜ |
| STAT-INV-08 | TC-STAT-025, TC-STAT-041 | ⬜ |
| STAT-SEC-01 | TC-STAT-001 | ⬜ |
| STAT-SEC-02 | TC-STAT-002 | ⬜ |
| STAT-SEC-03 | TC-STAT-003 | ⬜ |
| STAT-SEC-04 | TC-STAT-004 | ⬜ |
| STAT-SEC-05 | TC-STAT-005 | ⬜ |
| STAT-TXT-01 | TC-STAT-010 | ⬜ |
| STAT-TXT-02 | TC-STAT-011 | ⬜ |
| STAT-TXT-03 | TC-STAT-012 | ⬜ |
| STAT-TXT-04 | TC-STAT-013 | ⬜ |
| STAT-SCL-01 | TC-STAT-020 | ⬜ |
| STAT-SCL-02 | TC-STAT-021 | ⬜ |
| STAT-SCL-03 | TC-STAT-022 | ⬜ |
| STAT-SCL-04 | TC-STAT-023 | ⬜ |
| STAT-SCL-05 | TC-STAT-024 | ⬜ |
| STAT-SCL-06 | TC-STAT-025 | ⬜ |
| STAT-OPT-01 | TC-STAT-030 | ⬜ |
| STAT-OPT-02 | TC-STAT-031 | ⬜ |
| STAT-OPT-03 | TC-STAT-032 | ⬜ |
| STAT-OPT-04 | TC-STAT-033 | ⬜ |
| STAT-CHK-01 | TC-STAT-040 | ⬜ |
| STAT-CHK-02 | TC-STAT-041 | ⬜ |
| STAT-CHK-03 | TC-STAT-042 | ⬜ |
| STAT-GRD-01 | TC-STAT-050 | ⬜ |
| STAT-GRD-02 | TC-STAT-051 | ⬜ |
| STAT-GRD-03 | TC-STAT-052 | ⬜ |
| STAT-GRD-04 | TC-STAT-053 | ⬜ |
| STAT-GRD-05 | TC-STAT-054 | ⬜ |
| STAT-SUM-01 | TC-STAT-060 | ⬜ |
| STAT-SUM-02 | TC-STAT-061 | ⬜ |
| STAT-SUM-03 | TC-STAT-062 | ⬜ |
| STAT-SUM-04 | TC-STAT-063 | ⬜ |
| STAT-EDGE-01 | TC-STAT-070 | ⬜ |
| STAT-EDGE-02 | TC-STAT-071 | ⬜ |
| STAT-EDGE-03 | TC-STAT-072 | ⬜ |
| STAT-EDGE-04 | TC-STAT-073 | ⬜ |
| STAT-EDGE-05 | TC-STAT-074 | ⬜ |
| STAT-EDGE-06 | TC-STAT-075 | ⬜ |
| STAT-EDGE-10 | TC-STAT-080 | ⬜ |
| STAT-EDGE-11 | TC-STAT-081 | ⬜ |
| STAT-EDGE-12 | TC-STAT-082 | ⬜ |
| STAT-EDGE-13 | TC-STAT-083 | ⬜ |
| STAT-EDGE-20 | TC-STAT-090 | ⬜ |
| STAT-EDGE-21 | TC-STAT-091 | ⬜ |
| STAT-EDGE-22 | TC-STAT-092 | ⬜ |
| STAT-EDGE-23 | TC-STAT-093 | ⬜ |
| STAT-EDGE-24 | TC-STAT-094 | ⬜ |
| STAT-NEG-01 | TC-STAT-100 | ⬜ |
| STAT-NEG-02 | TC-STAT-101 | ⬜ |
| STAT-NEG-03 | TC-STAT-102 | ⬜ |
| STAT-INTEG-01 | TC-STAT-110 | ⬜ |
| STAT-INTEG-02 | TC-STAT-111 | ⬜ |
| STAT-INTEG-03 | TC-STAT-112 | ⬜ |
| STAT-INTEG-04 | TC-STAT-113 | ⬜ |
| STAT-INTEG-05 | TC-STAT-114 | ⬜ |
| STAT-PERF-01 | TC-STAT-120 | ⬜ |
| STAT-PERF-02 | TC-STAT-121 | ⬜ |

---

## 18. 구현 현황 요약

| 카테고리 | 전체 | ⬜ |
|---------|:---:|:---:|
| 권한/보안 (TC-STAT-001~005) | 5 | 5 |
| TEXT 카테고리 통계 (TC-STAT-010~013) | 4 | 4 |
| SCALE 카테고리 통계 (TC-STAT-020~025) | 6 | 6 |
| OPTION 카테고리 통계 (TC-STAT-030~033) | 4 | 4 |
| CHECKBOX 카테고리 통계 (TC-STAT-040~042) | 3 | 3 |
| GRID 카테고리 통계 (TC-STAT-050~054) | 5 | 5 |
| 전체 요약 통계 (TC-STAT-060~063) | 4 | 4 |
| 응답 경계값 (TC-STAT-070~075) | 6 | 6 |
| soft delete 경계값 (TC-STAT-080~083) | 4 | 4 |
| 설문 상태별 (TC-STAT-090~094) | 5 | 5 |
| 부정 시나리오 (TC-STAT-100~102) | 3 | 3 |
| 복합 시나리오 (TC-STAT-110~114) | 5 | 5 |
| 성능 (TC-STAT-120~121) | 2 | 2 |
| **합계** | **56** | **56** |

> **참고**: 테스트 케이스 56건으로 검증 기준 69건을 전수 커버한다. 일부 검증 기준은 동일한 테스트 케이스로 복합 검증되며 (예: STAT-INV-01은 TC-STAT-061과 TC-STAT-072에서 검증), 일부 도메인 규칙(STAT-INV-01~08)은 직접적인 테스트 케이스보다는 다른 카테고리의 테스트 케이스에서 간접 검증된다.

---

## 19. 테스트 레벨별 분류

### 19.1 단위 테스트 (서비스 레이어, Repository Mock)

| 테스트 케이스 ID | 검증 대상 |
|----------------|----------|
| TC-STAT-010~013 | TEXT 카테고리 통계 집계 로직 |
| TC-STAT-020~025 | SCALE 카테고리 통계 집계 로직 |
| TC-STAT-030~033 | OPTION 카테고리 통계 집계 로직 |
| TC-STAT-040~042 | CHECKBOX 카테고리 통계 집계 로직 |
| TC-STAT-050~054 | GRID 카테고리 통계 집계 로직 |
| TC-STAT-060~063 | 전체 요약 통계 로직 |
| TC-STAT-070~075 | 응답 경계값 처리 로직 |
| TC-STAT-080~083 | soft delete 데이터 처리 로직 |
| TC-STAT-090~094 | 설문 상태별 접근 로직 |

### 19.2 통합 테스트 (서비스 레이어, 실제 DB)

| 테스트 케이스 ID | 검증 대상 |
|----------------|----------|
| TC-STAT-110~114 | 복합 시나리오 (혼합 질문, 응답 수정, 질문 추가 등) |
| TC-STAT-120~121 | N+1 쿼리 방지, 대량 데이터 성능 |

### 19.3 컨트롤러 통합 테스트 (MockMvc)

| 테스트 케이스 ID | 검증 대상 |
|----------------|----------|
| TC-STAT-001~005 | HTTP 인증/인가, RBAC |
| TC-STAT-100~102 | HTTP 에러 응답 (404, 400) |

---

## 20. 관련 문서

- [설문 통계 API 검증 기준서](../../../docs/criteria/survey/survey-statistics-verification-criteria.md) -- STAT-INV-01~08, STAT-SEC-01~05, 질문 타입별 통계 기준 등 69건
- [설문 도메인 테스트 케이스](./survey-domain-test-cases.md) -- 설문/질문/응답 도메인 로직 209건
- [설문 통합 테스트 케이스](./survey-integration-test-cases.md) -- 컨트롤러 RBAC, E2E 시나리오 등
- [설문 기능 검증 기준서](../../../docs/criteria/survey/survey-criteria-v1.md) -- INV-01~30, SEC-01~11
