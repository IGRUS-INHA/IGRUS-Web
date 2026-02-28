# 설문 통계 API 작업 계획

> **Feature**: 설문 통계 API (Survey Statistics API)
> **Status**: Draft
> **Created**: 2026-02-27
> **검증 기준서**: [survey-statistics-verification-criteria.md](../../criteria/survey/survey-statistics-verification-criteria.md)
> **테스트 케이스**: [survey-statistics-test-cases.md](../../../backend/docs/test-case/survey/survey-statistics-test-cases.md)

---

## 개요

설문 응답 데이터를 집계하여 질문 타입별 통계를 제공하는 REST API를 구현한다.
운영자(OPERATOR) 이상만 조회 가능하며, 단일 엔드포인트(`GET /api/v1/surveys/{surveyId}/statistics`)로 제공한다.

### API 명세 요약

| 항목 | 내용 |
|------|------|
| 엔드포인트 | `GET /api/v1/surveys/{surveyId}/statistics` |
| 인증 | 필수 (Bearer Token) |
| 인가 | OPERATOR 이상 |
| 성공 응답 | 200 OK |
| 에러 응답 | 401 (비인증), 403 (비인가), 404 (설문 없음), 400 (잘못된 요청) |

### 질문 타입별 통계 구조

| 카테고리 | 질문 유형 | 통계 구조 |
|----------|----------|----------|
| TEXT | SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD | responseCount, textResponses: [string] |
| SCALE | LINEAR_SCALE | responseCount, average, min, max, distribution: {value: count} |
| OPTION | MULTIPLE_CHOICE, DROPDOWN | responseCount, options: [{optionId, optionText, count, percentage}] |
| CHECKBOX | CHECKBOX | responseCount, options: [{optionId, optionText, count, percentage}] (합계 > 100% 가능) |
| GRID | MULTIPLE_CHOICE_GRID, CHECKBOX_GRID | responseCount, rows: [{rowId, rowLabel, options: [...]}] |

---

## 작업 목록

### Phase 1: 응답 DTO 설계 및 구현

> 통계 API의 응답 구조를 정의하는 DTO 클래스를 구현한다. 모든 후속 작업의 기반이 된다.

#### TASK-001: 전체 통계 응답 최상위 DTO 구현

- **작업명**: SurveyStatisticsResponse 최상위 응답 DTO 구현
- **설명**: 통계 API의 최상위 응답 DTO를 구현한다. 총 응답 수, 응답 기간(시작일/종료일), 질문별 통계 목록을 포함한다. 응답이 0건일 때 응답 기간은 null로 처리한다.
- **관련 검증 기준**: STAT-INV-03, STAT-SUM-01~04
- **관련 테스트 케이스**: TC-STAT-060~063
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **구현 파일**: `survey/statistics/dto/response/SurveyStatisticsResponse.java`
- **포함 필드**:
  - `totalResponseCount` (int): 유효 응답 수 (deleted=false 기준)
  - `responseStartedAt` (Instant, nullable): 첫 응답 시각
  - `responseEndedAt` (Instant, nullable): 마지막 응답 시각
  - `questionStatistics` (List\<QuestionStatisticsResponse\>): 질문별 통계 목록 (displayOrder 순)

#### TASK-002: 질문 공통 통계 DTO 구현

- **작업명**: QuestionStatisticsResponse 질문 공통 통계 DTO 구현
- **설명**: 질문 하나의 통계를 나타내는 DTO를 구현한다. 질문 유형(TEXT/SCALE/OPTION/CHECKBOX/GRID)에 따라 서로 다른 상세 통계 구조를 가지며, 질문의 삭제 여부(deleted) 플래그를 포함한다. 다형적 구조(sealed interface 또는 추상 클래스)를 사용하거나, questionType에 따라 하위 필드가 달라지는 단일 DTO를 사용할 수 있다.
- **관련 검증 기준**: STAT-INV-07, STAT-INTEG-05
- **관련 테스트 케이스**: TC-STAT-114
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/dto/response/QuestionStatisticsResponse.java`
- **포함 필드**:
  - `questionId` (Long)
  - `questionTitle` (String)
  - `questionType` (SurveyQuestionType)
  - `deleted` (boolean): 질문 soft delete 여부 (STAT-INV-07)
  - `responseCount` (int): 해당 질문의 유효 답변 수
  - `textStatistics` (nullable): TEXT 카테고리 상세
  - `scaleStatistics` (nullable): SCALE 카테고리 상세
  - `optionStatistics` (nullable): OPTION/CHECKBOX 카테고리 상세
  - `gridStatistics` (nullable): GRID 카테고리 상세

#### TASK-003: TEXT 카테고리 통계 DTO 구현

- **작업명**: TextQuestionStatistics DTO 구현
- **설명**: TEXT 카테고리(SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD) 질문의 통계 구조를 정의한다. 텍스트 응답 목록을 응답 제출 시각(SurveyResponse.createdAt) 오름차순으로 반환한다.
- **관련 검증 기준**: STAT-TXT-01~04
- **관련 테스트 케이스**: TC-STAT-010~013
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **구현 파일**: `survey/statistics/dto/response/TextQuestionStatistics.java`
- **포함 필드**:
  - `textResponses` (List\<String\>): 텍스트 응답 목록 (createdAt 오름차순)

#### TASK-004: SCALE 카테고리 통계 DTO 구현

- **작업명**: ScaleQuestionStatistics DTO 구현
- **설명**: LINEAR_SCALE 질문의 통계 구조를 정의한다. 평균, 최솟값, 최댓값, 값별 분포를 포함한다. 평균은 BigDecimal(HALF_UP, scale=1)로 소수 첫째 자리 반올림한다.
- **관련 검증 기준**: STAT-SCL-01~06, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-020~025
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **구현 파일**: `survey/statistics/dto/response/ScaleQuestionStatistics.java`
- **포함 필드**:
  - `average` (BigDecimal): 평균값 (HALF_UP, scale=1). 응답 0건 시 null 또는 0.0
  - `min` (Integer, nullable): 최솟값. 응답 0건 시 null
  - `max` (Integer, nullable): 최댓값. 응답 0건 시 null
  - `distribution` (Map\<Integer, Integer\>): 값별 응답 수 (scaleMin~scaleMax 전체 키 포함, 미선택은 0)

#### TASK-005: OPTION/CHECKBOX 카테고리 통계 DTO 구현

- **작업명**: OptionQuestionStatistics + OptionStatisticsItem DTO 구현
- **설명**: MULTIPLE_CHOICE, DROPDOWN, CHECKBOX 질문의 통계 구조를 정의한다. 옵션별 선택 수와 비율을 포함하며, 삭제된 옵션의 deleted 플래그를 표시한다. 비율은 BigDecimal(HALF_UP, scale=1)이다.
- **관련 검증 기준**: STAT-OPT-01~04, STAT-CHK-01~03, STAT-INV-07, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-030~033, TC-STAT-040~042
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 하
- **구현 파일**: `survey/statistics/dto/response/OptionQuestionStatistics.java`, `OptionStatisticsItem.java`
- **OptionStatisticsItem 포함 필드**:
  - `optionId` (Long)
  - `optionText` (String)
  - `deleted` (boolean): 옵션 soft delete 여부 (STAT-INV-07)
  - `count` (int): 선택 수
  - `percentage` (BigDecimal): 비율 (HALF_UP, scale=1)

#### TASK-006: GRID 카테고리 통계 DTO 구현

- **작업명**: GridQuestionStatistics + GridRowStatistics DTO 구현
- **설명**: MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 질문의 통계 구조를 정의한다. 행별 옵션 분포를 포함하며, 삭제된 행의 deleted 플래그를 표시한다. 비율 분모는 전체 설문 응답자 수이다.
- **관련 검증 기준**: STAT-GRD-01~05, STAT-INV-07, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-050~054
- **선행 작업**: TASK-005 (OptionStatisticsItem 재사용)
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/dto/response/GridQuestionStatistics.java`, `GridRowStatistics.java`
- **GridRowStatistics 포함 필드**:
  - `rowId` (Long)
  - `rowLabel` (String)
  - `deleted` (boolean): 행 soft delete 여부 (STAT-INV-07)
  - `options` (List\<OptionStatisticsItem\>): 옵션별 선택 수/비율

---

### Phase 2: 통계 집계 서비스 구현

> 핵심 비즈니스 로직인 통계 집계 서비스를 구현한다. Repository에서 데이터를 조회하고, 질문 타입별로 통계를 계산한다.

#### TASK-007: 통계 조회용 Repository 메서드 추가

- **작업명**: 통계 집계에 필요한 Repository 조회 메서드 추가
- **설명**: 통계 집계에 필요한 데이터 조회를 위한 Repository 메서드를 추가한다. deleted=false 조건 포함이 핵심이다. 기존 SurveyResponseRepository, SurveyAnswerRepository(없으면 생성)에 메서드를 추가하거나, 통계 전용 쿼리를 작성한다.
- **관련 검증 기준**: STAT-INV-01, STAT-INV-02, STAT-INV-07
- **관련 테스트 케이스**: TC-STAT-060, TC-STAT-061, TC-STAT-072, TC-STAT-075
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: 기존 `survey/response/repository/` 내 파일 또는 `survey/statistics/repository/` 신규
- **주요 쿼리 요구사항**:
  - 설문의 유효 응답 목록 조회 (`deleted = false`)
  - 유효 응답의 답변 목록 조회 (`SurveyAnswer.deleted = false`, `SurveyResponse.deleted = false`)
  - 질문/선택지/행 조회 시 soft delete된 항목 **포함** (STAT-INV-07)
  - N+1 방지를 위한 fetch join 또는 @EntityGraph 적용 (STAT-PERF-01)

#### TASK-008: SurveyStatisticsService 핵심 집계 로직 구현

- **작업명**: SurveyStatisticsService 통계 집계 서비스 구현
- **설명**: 통계 API의 핵심 비즈니스 로직을 구현한다. 설문 존재 여부 확인, 유효 응답 조회, 질문 타입별 통계 계산, DTO 변환을 수행한다.
- **관련 검증 기준**: STAT-INV-01~08, STAT-SUM-01~04, STAT-TXT-01~04, STAT-SCL-01~06, STAT-OPT-01~04, STAT-CHK-01~03, STAT-GRD-01~05
- **관련 테스트 케이스**: TC-STAT-010~063
- **선행 작업**: TASK-001~007
- **구현 범위**: backend
- **예상 난이도**: 상
- **구현 파일**: `survey/statistics/service/SurveyStatisticsService.java`
- **주요 구현 사항**:
  - `@Service`, `@Transactional(readOnly = true)`
  - `getSurveyStatistics(Long surveyId)` 메서드:
    1. 설문 조회 (deleted=true이면 SurveyNotFoundException)
    2. 유효 응답(deleted=false) 목록 조회 -> totalResponseCount 계산
    3. 응답 기간 계산 (min/max createdAt, 0건이면 null)
    4. 설문의 모든 질문 조회 (soft delete 포함, displayOrder 오름차순)
    5. 질문별 통계 계산 (타입별 분기):
       - TEXT: textValue 목록 수집, createdAt 오름차순 정렬
       - SCALE: average(HALF_UP, scale=1), min, max, distribution 계산
       - OPTION/CHECKBOX: 옵션별 count, percentage 계산
       - GRID: 행별 옵션 분포 계산
    6. DTO 변환 및 반환
  - 소수점 처리: `BigDecimal`, `RoundingMode.HALF_UP`, scale=1
  - 분모 0일 때 비율 = 0.0 (division by zero 방지)
  - GRID 비율 분모 = 전체 설문 응답자 수 (totalResponseCount)

#### TASK-009: 로깅 추가

- **작업명**: 통계 조회 서비스에 로그 메시지 추가
- **설명**: 검증 기준서 7절(관측 가능성)에 정의된 로그 메시지를 서비스에 추가한다.
- **관련 검증 기준**: 검증 기준서 7절 (관측 가능성)
- **관련 테스트 케이스**: 없음 (비기능)
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 하
- **로그 메시지**:
  - 시작: `설문 통계 조회 요청: surveyId={}, operatorId={}`
  - 완료: `설문 통계 조회 완료: surveyId={}, 총 응답 수={}`
  - 실패: `통계 조회 실패: 설문 없음 surveyId={}`

---

### Phase 3: 컨트롤러 구현

> HTTP 엔드포인트를 구현하고, 인증/인가 처리 및 Swagger 문서화를 수행한다.

#### TASK-010: SurveyStatisticsController 구현

- **작업명**: 통계 조회 API 컨트롤러 구현
- **설명**: `GET /api/v1/surveys/{surveyId}/statistics` 엔드포인트를 구현한다. OPERATOR 이상 권한 체크, @Positive 검증, Swagger 문서화를 포함한다.
- **관련 검증 기준**: STAT-SEC-01~05, STAT-NEG-02, STAT-NEG-03
- **관련 테스트 케이스**: TC-STAT-001~005, TC-STAT-100~102
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/controller/SurveyStatisticsController.java`
- **구현 사항**:
  - `@RestController`, `@RequestMapping("/api/v1/surveys")`
  - `@PreAuthorize("hasRole('OPERATOR')")` 또는 동등한 권한 체크
  - `@SecurityRequirement(name = "bearerAuth")`
  - `surveyId` 파라미터에 `@Positive` 적용
  - Swagger 어노테이션:
    - `@Operation(summary = "설문 통계 조회")`
    - `@ApiResponse(responseCode = "200", ..., useReturnTypeSchema = true)`
    - `@ApiResponse(responseCode = "401", description = "인증 필요")`
    - `@ApiResponse(responseCode = "403", description = "권한 없음")`
    - `@ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")`
  - CLAUDE.md Swagger 규칙 준수: 에러 응답에 content 파라미터 생략

---

### Phase 4: 단위 테스트 (서비스)

> 통계 집계 로직의 정확성을 단위 테스트로 검증한다. Repository를 Mock하는 런던파 방식을 사용한다.

#### TASK-011: TEXT 카테고리 통계 서비스 단위 테스트

- **작업명**: TEXT 카테고리 통계 집계 로직 단위 테스트 구현
- **설명**: SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD 질문 유형의 통계 집계 로직을 단위 테스트한다.
- **관련 검증 기준**: STAT-TXT-01~04
- **관련 테스트 케이스**: TC-STAT-010~013
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (4건):
  - TC-STAT-010: TEXT 응답 수 정확성 (SHORT_ANSWER, 3건 -> responseCount=3)
  - TC-STAT-011: 텍스트 응답 목록 반환 + createdAt 오름차순 정렬
  - TC-STAT-012: DATE 유형도 TEXT 카테고리 구조 적용
  - TC-STAT-013: FILE_UPLOAD URL 문자열 목록 반환

#### TASK-012: SCALE 카테고리 통계 서비스 단위 테스트

- **작업명**: SCALE 카테고리 통계 집계 로직 단위 테스트 구현
- **설명**: LINEAR_SCALE 질문 유형의 통계 집계 로직을 단위 테스트한다. 평균, 최솟값, 최댓값, 분포, 소수점 반올림을 검증한다.
- **관련 검증 기준**: STAT-SCL-01~06, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-020~025
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (6건):
  - TC-STAT-020: 응답 수 정확성 ([1,2,3,4,5] -> 5)
  - TC-STAT-021: 평균값 정확성 (나누어 떨어지는 경우, 3.0)
  - TC-STAT-022: 최솟값/최댓값 정확성 ([2,5,8] -> min=2, max=8)
  - TC-STAT-023: 값별 분포 히스토그램 ([1,1,2,3,3,3,5] -> {1:2,2:1,3:3,4:0,5:1})
  - TC-STAT-024: 응답 1건일 때 average=min=max
  - TC-STAT-025: 소수점 반올림 ([1,2,4] -> average=2.3, HALF_UP)

#### TASK-013: OPTION 카테고리 통계 서비스 단위 테스트

- **작업명**: OPTION 카테고리 통계 집계 로직 단위 테스트 구현
- **설명**: MULTIPLE_CHOICE, DROPDOWN 질문 유형의 통계 집계 로직을 단위 테스트한다. 옵션별 선택 수와 비율 정확성을 검증한다.
- **관련 검증 기준**: STAT-OPT-01~04
- **관련 테스트 케이스**: TC-STAT-030~033
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (4건):
  - TC-STAT-030: MC 옵션별 선택 수 (A=3, B=2, C=0)
  - TC-STAT-031: MC 옵션별 비율 (합계 100%)
  - TC-STAT-032: DROPDOWN 옵션별 선택 수/비율
  - TC-STAT-033: 미선택 옵션도 0으로 포함

#### TASK-014: CHECKBOX 카테고리 통계 서비스 단위 테스트

- **작업명**: CHECKBOX 카테고리 통계 집계 로직 단위 테스트 구현
- **설명**: CHECKBOX 질문 유형의 통계 집계 로직을 단위 테스트한다. 복수 선택 시 비율 합계가 100%를 초과할 수 있음을 검증한다.
- **관련 검증 기준**: STAT-CHK-01~03, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-040~042
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (3건):
  - TC-STAT-040: 옵션별 선택 수 (A=2, B=2, C=1)
  - TC-STAT-041: 비율 합계 > 100% (A=66.7%, B=66.7%, C=33.3%)
  - TC-STAT-042: 총 응답자 수와 개별 선택 합계 구분 (responseCount=2, 선택 합계=6)

#### TASK-015: GRID 카테고리 통계 서비스 단위 테스트

- **작업명**: GRID 카테고리 통계 집계 로직 단위 테스트 구현
- **설명**: MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 질문 유형의 통계 집계 로직을 단위 테스트한다. 행별 옵션 분포, 비율 분모(전체 응답자 수), 비필수 질문의 미응답 행 처리를 검증한다.
- **관련 검증 기준**: STAT-GRD-01~05, STAT-INV-08
- **관련 테스트 케이스**: TC-STAT-050~054
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 상
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (5건):
  - TC-STAT-050: MC_GRID 행별 옵션 분포
  - TC-STAT-051: MC_GRID 행별 비율 합계 <= 100%
  - TC-STAT-052: CB_GRID 행별 옵션 분포 (복수 선택)
  - TC-STAT-053: CB_GRID 행별 비율 합계 > 100% 가능
  - TC-STAT-054: 비필수 질문의 미응답 행 (모든 옵션 count=0)

#### TASK-016: 전체 요약 통계 서비스 단위 테스트

- **작업명**: 전체 요약 통계 (총 응답 수, 응답 기간) 단위 테스트 구현
- **설명**: 총 응답 수 정확성, 삭제된 응답 제외 후 총 응답 수, 응답 기간 정확성, 응답 0건일 때 응답 기간 null을 검증한다.
- **관련 검증 기준**: STAT-SUM-01~04, STAT-INV-01
- **관련 테스트 케이스**: TC-STAT-060~063
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (4건):
  - TC-STAT-060: 총 응답 수 정확성 (5건)
  - TC-STAT-061: 삭제된 응답 제외 후 총 응답 수 (5건 중 2건 deleted -> 3)
  - TC-STAT-062: 응답 기간 정확성 (min/max createdAt)
  - TC-STAT-063: 응답 0건일 때 응답 기간 null

#### TASK-017: 경계값/엣지 케이스 서비스 단위 테스트

- **작업명**: 응답 데이터 관련 경계값 단위 테스트 구현
- **설명**: 응답 0건, 1건, 모든 응답 삭제, 비필수 질문 부분 응답, 빈 응답 제출, 삭제된 SurveyAnswer 제외 등 경계값을 검증한다.
- **관련 검증 기준**: STAT-INV-01~03, STAT-EDGE-01~06
- **관련 테스트 케이스**: TC-STAT-070~075
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 상
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (6건):
  - TC-STAT-070: 응답 0건 통계 (200 OK, division by zero 미발생)
  - TC-STAT-071: 응답 1건 통계
  - TC-STAT-072: 모든 응답이 삭제된 경우 (유효 0건)
  - TC-STAT-073: 비필수 질문에 일부만 응답 (5명 중 3명)
  - TC-STAT-074: 모든 질문 비필수 + 빈 응답 제출 (totalResponseCount=1, 질문별 0)
  - TC-STAT-075: 삭제된 SurveyAnswer 질문별 통계에서 제외

#### TASK-018: soft delete된 질문/선택지/행 통계 단위 테스트

- **작업명**: soft delete된 질문/선택지/행의 기존 답변 통계 포함 단위 테스트 구현
- **설명**: STAT-INV-07에 따라, soft delete된 질문/선택지/행의 기존 답변이 통계에 포함되고 삭제 여부가 표시되는지 검증한다.
- **관련 검증 기준**: STAT-INV-07, STAT-EDGE-10~13
- **관련 테스트 케이스**: TC-STAT-080~083
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 상
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (4건):
  - TC-STAT-080: soft delete된 질문의 기존 답변 3건 포함 + deleted 플래그
  - TC-STAT-081: soft delete된 선택지의 기존 선택 포함 (C=2, deleted=true)
  - TC-STAT-082: soft delete된 행의 기존 응답 포함 + deleted 플래그
  - TC-STAT-083: 모든 선택지가 soft delete된 질문 (극단 경우)

#### TASK-019: 설문 상태별 통계 조회 서비스 단위 테스트

- **작업명**: 설문 2축 상태 모델 및 휴지통/영구 삭제 상태 통계 조회 단위 테스트 구현
- **설명**: 모든 유효 상태에서 통계 조회가 가능하고, 영구 삭제된 설문은 404를 반환하는지 검증한다.
- **관련 검증 기준**: STAT-INV-04~06, STAT-EDGE-20~24
- **관련 테스트 케이스**: TC-STAT-090~094
- **선행 작업**: TASK-008
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceTest.java`
- **테스트 항목** (5건):
  - TC-STAT-090: (U, NS) 설문 통계 조회 -> 200 OK
  - TC-STAT-091: (P, O) 설문 통계 조회 -> 200 OK (실시간)
  - TC-STAT-092: (P, C) 설문 통계 조회 -> 200 OK (일반적 사용)
  - TC-STAT-093: 휴지통 설문 통계 조회 -> 200 OK
  - TC-STAT-094: 영구 삭제 설문 -> 404 SurveyNotFoundException

---

### Phase 5: 컨트롤러 통합 테스트

> HTTP 인증/인가, 응답 구조, 상태 코드를 MockMvc 기반 통합 테스트로 검증한다.

#### TASK-020: 권한/보안 컨트롤러 통합 테스트

- **작업명**: 통계 API 인증/인가 컨트롤러 통합 테스트 구현
- **설명**: 비인증, ASSOCIATE, MEMBER, OPERATOR 역할별 접근 제어를 컨트롤러 통합 테스트로 검증한다. 비인가 접근 시 부작용이 없는지도 확인한다.
- **관련 검증 기준**: STAT-SEC-01~05
- **관련 테스트 케이스**: TC-STAT-001~005
- **선행 작업**: TASK-010
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/controller/SurveyStatisticsControllerTest.java`
- **테스트 항목** (5건):
  - TC-STAT-001: 비인증 사용자 -> 401
  - TC-STAT-002: ASSOCIATE -> 403
  - TC-STAT-003: MEMBER -> 403
  - TC-STAT-004: OPERATOR -> 200 OK
  - TC-STAT-005: MEMBER 접근 시 부작용 없음

#### TASK-021: 부정 시나리오 컨트롤러 통합 테스트

- **작업명**: 존재하지 않는 설문/음수 ID/비숫자 ID 컨트롤러 통합 테스트 구현
- **설명**: 유효하지 않은 surveyId에 대한 에러 응답을 검증한다.
- **관련 검증 기준**: STAT-NEG-01~03
- **관련 테스트 케이스**: TC-STAT-100~102
- **선행 작업**: TASK-010
- **구현 범위**: backend
- **예상 난이도**: 하
- **구현 파일**: `survey/statistics/controller/SurveyStatisticsControllerTest.java`
- **테스트 항목** (3건):
  - TC-STAT-100: 존재하지 않는 설문 ID -> 404
  - TC-STAT-101: 음수 surveyId -> 400 (@Positive)
  - TC-STAT-102: 비숫자 surveyId -> 400

---

### Phase 6: 통합 테스트 (서비스, 실제 DB)

> 실제 H2 DB 환경에서 통계 정합성을 검증하는 통합 테스트를 구현한다.

#### TASK-022: 복합 시나리오 통합 테스트

- **작업명**: 다양한 질문 유형 혼합, 응답 수정, 질문 추가, 비회원 응답 등 복합 시나리오 통합 테스트 구현
- **설명**: 실제 DB(H2) 환경에서 복합 시나리오의 통계 정합성을 검증한다. 단위 테스트로는 확인하기 어려운 데이터 연관 관계 및 실제 쿼리 동작을 검증한다.
- **관련 검증 기준**: STAT-INTEG-01~05
- **관련 테스트 케이스**: TC-STAT-110~114
- **선행 작업**: TASK-008, TASK-010
- **구현 범위**: backend
- **예상 난이도**: 상
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java`
- **테스트 항목** (5건):
  - TC-STAT-110: 5개 질문 유형 혼합 설문 통계 (전체 정합성)
  - TC-STAT-111: 응답 수정(PUT) 후 통계 반영 (수정 후 최종값만 반영)
  - TC-STAT-112: 질문 추가 후 기존 응답자 미응답 질문 통계
  - TC-STAT-113: 비회원(PUBLIC) 응답 포함 통계 (user=null)
  - TC-STAT-114: 질문 displayOrder 순서로 통계 반환

#### TASK-023: N+1 쿼리 방지 성능 통합 테스트

- **작업명**: N+1 쿼리 방지 검증 통합 테스트 구현
- **설명**: 실제 DB 환경에서 통계 조회 시 N+1 쿼리가 발생하지 않는지 검증한다. Hibernate 쿼리 로깅 또는 쿼리 카운터를 활용한다.
- **관련 검증 기준**: STAT-PERF-01
- **관련 테스트 케이스**: TC-STAT-120
- **선행 작업**: TASK-022
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java`
- **테스트 항목** (1건):
  - TC-STAT-120: 질문 10개, 선택지 50개, 응답 100건에서 쿼리 수 합리적 범위 확인

#### TASK-024: 대량 응답 성능 테스트 (선택)

- **작업명**: 대량 응답 설문 통계 응답 시간 성능 테스트 구현
- **설명**: 질문 50개, 응답 1000건 규모에서 응답 시간이 3초 이내인지 검증한다. 정식 SLA 미확정이므로 선택 사항이다.
- **관련 검증 기준**: STAT-PERF-02
- **관련 테스트 케이스**: TC-STAT-121
- **선행 작업**: TASK-023
- **구현 범위**: backend
- **예상 난이도**: 중
- **구현 파일**: `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java`
- **테스트 항목** (1건):
  - TC-STAT-121: 질문 50개, 응답 1000건 -> 응답 시간 < 3초

---

## 작업 순서 및 의존성

```
Phase 1: DTO 설계
  TASK-001 ──┐
  TASK-002 ──┤
  TASK-003 ──┤ (모두 병렬 가능)
  TASK-004 ──┤
  TASK-005 ──┤
  TASK-006 ──┘── TASK-005에 의존 (OptionStatisticsItem 재사용)

Phase 2: 서비스 구현
  TASK-007 ──┐
             ├── TASK-008 (Phase 1 전체 + TASK-007에 의존)
             └── TASK-009 (TASK-008에 의존)

Phase 3: 컨트롤러 구현
  TASK-010 (TASK-008에 의존)

Phase 4: 단위 테스트 (모두 TASK-008에 의존, 상호 병렬 가능)
  TASK-011 (TEXT)
  TASK-012 (SCALE)
  TASK-013 (OPTION)
  TASK-014 (CHECKBOX)
  TASK-015 (GRID)
  TASK-016 (요약 통계)
  TASK-017 (경계값)
  TASK-018 (soft delete)
  TASK-019 (설문 상태별)

Phase 5: 컨트롤러 통합 테스트 (TASK-010에 의존)
  TASK-020 (권한/보안)
  TASK-021 (부정 시나리오)

Phase 6: 통합 테스트 (TASK-008, TASK-010에 의존)
  TASK-022 (복합 시나리오) ── TASK-023 (N+1) ── TASK-024 (성능, 선택)
```

### 권장 실행 순서

| 순서 | 작업 | 비고 |
|:---:|------|------|
| 1 | TASK-001~005 | DTO 병렬 구현 (TASK-006은 TASK-005 이후) |
| 2 | TASK-006 | GRID DTO (OptionStatisticsItem 재사용) |
| 3 | TASK-007 | Repository 메서드 추가 |
| 4 | TASK-008 | 핵심 서비스 구현 (가장 중요, 난이도 상) |
| 5 | TASK-009, TASK-010 | 로깅 + 컨트롤러 (병렬 가능) |
| 6 | TASK-011~019 | 단위 테스트 (모두 병렬 가능, 구현과 동시 진행 권장) |
| 7 | TASK-020~021 | 컨트롤러 통합 테스트 (병렬 가능) |
| 8 | TASK-022 | 복합 시나리오 통합 테스트 |
| 9 | TASK-023 | N+1 성능 검증 |
| 10 | TASK-024 | 대량 응답 성능 테스트 (선택) |

---

## 구현 시 주의사항

### 기술적 고려사항

1. **패키지 구조**: `survey/statistics/` 서브패키지를 새로 생성한다. 기존 패키지 의존 방향 `statistics/ -> question/ -> survey/ (core)` 및 `statistics/ -> response/`를 따른다.
2. **소수점 처리**: 모든 비율과 평균에 `BigDecimal`, `RoundingMode.HALF_UP`, scale=1을 일관되게 적용한다. 분모 0일 때는 `BigDecimal.ZERO`(scale=1)를 반환한다.
3. **시간 클래스**: 응답 기간(responseStartedAt, responseEndedAt)은 `Instant`를 사용한다 (CLAUDE.md 규칙).
4. **soft delete 처리**: 질문/선택지/행은 deleted=true여도 통계에 **포함**하되 플래그를 표시한다(STAT-INV-07). 반면 SurveyResponse/SurveyAnswer의 deleted=true는 통계에서 **제외**한다(STAT-INV-01, STAT-INV-02). 이 두 규칙을 혼동하지 않도록 주의한다.
5. **GRID 비율 분모**: 행별 비율의 분모는 "해당 행의 응답자 수"가 아니라 "전체 설문 응답자 수(totalResponseCount)"이다.
6. **테스트 코드에 @Transactional 사용 금지**: CLAUDE.md 규칙에 따라 테스트에서 @Transactional을 사용하지 않는다. 통합 테스트에서는 별도의 데이터 정리 전략을 사용한다.

### 잠재적 위험 요소

1. **N+1 쿼리**: 질문별로 답변을 조회하면 질문 수만큼 추가 쿼리가 발생할 수 있다. 설문의 모든 답변을 한 번에 조회한 후 메모리에서 질문별로 그룹핑하는 전략을 권장한다.
2. **대량 데이터**: 응답 1000건 + 질문 50개 시 답변 레코드 최대 50,000건. 메모리 사용량과 쿼리 성능에 주의한다.
3. **STI(Single Table Inheritance) 쿼리**: SurveyAnswer가 STI 전략을 사용하므로, 서브클래스별 필드(textValue, numericValue, selectedOption 등)에 접근할 때 타입 캐스팅이 필요하다.
4. **동시성**: 통계 조회는 읽기 전용이므로 동시성 문제는 낮으나, OPEN 상태 설문의 실시간 통계 조회 시 진행 중인 응답과의 일관성은 eventual consistency로 처리한다.

### 기존 코드와의 통합 포인트

1. **Survey 엔티티**: `survey/domain/Survey.java` -- 설문 존재 여부 확인, deleted 체크
2. **SurveyQuestion 엔티티 계층**: `survey/question/domain/` -- STI(TextSurveyQuestion, LinearScaleSurveyQuestion, OptionSurveyQuestion, GridSurveyQuestion). 질문 유형 판별 및 옵션/행 접근
3. **SurveyAnswer 엔티티 계층**: `survey/response/domain/` -- STI(TextSurveyAnswer, NumericSurveyAnswer, OptionSurveyAnswer, GridSurveyAnswer). 답변 데이터 접근
4. **SurveyResponse 엔티티**: `survey/response/domain/SurveyResponse.java` -- 응답 목록, createdAt, deleted 필드
5. **SurveyNotFoundException**: `survey/exception/` -- 기존 예외 재사용
6. **SurveyRepository**: `survey/repository/` -- 설문 조회 (deleted=false 조건)
7. **SurveyResponseRepository**: `survey/response/repository/` -- 응답 목록 조회
8. **SurveyTestFixture**: 기존 테스트 픽스처 재사용 및 통계 전용 픽스처 추가 필요

---

## 완료 기준

### 검증 기준 충족 여부 체크리스트

- [ ] STAT-INV-01: 삭제된 응답(deleted=true)은 통계에서 제외
- [ ] STAT-INV-02: 삭제된 답변(deleted=true)은 통계에서 제외
- [ ] STAT-INV-03: 응답 0건 설문도 200 OK 반환 (에러 미발생)
- [ ] STAT-INV-04: 모든 유효 상태(2축)에서 통계 조회 가능
- [ ] STAT-INV-05: 휴지통 설문도 통계 조회 가능
- [ ] STAT-INV-06: 영구 삭제 설문은 404 반환
- [ ] STAT-INV-07: soft delete된 질문/선택지/행의 기존 답변 통계 포함 + 삭제 표시
- [ ] STAT-INV-08: 소수점 HALF_UP 소수 첫째 자리 반올림
- [ ] STAT-SEC-01~05: 역할별 접근 제어 (401/403/200)
- [ ] STAT-TXT-01~04: TEXT 카테고리 통계 정확성
- [ ] STAT-SCL-01~06: SCALE 카테고리 통계 정확성
- [ ] STAT-OPT-01~04: OPTION 카테고리 통계 정확성
- [ ] STAT-CHK-01~03: CHECKBOX 카테고리 통계 정확성
- [ ] STAT-GRD-01~05: GRID 카테고리 통계 정확성
- [ ] STAT-SUM-01~04: 전체 요약 통계 정확성
- [ ] STAT-EDGE-01~06: 응답 데이터 경계값 처리
- [ ] STAT-EDGE-10~13: soft delete 경계값 처리
- [ ] STAT-EDGE-20~24: 설문 상태별 통계 조회
- [ ] STAT-NEG-01~03: 부정 시나리오 에러 처리
- [ ] STAT-INTEG-01~05: 복합 시나리오 정합성
- [ ] STAT-PERF-01: N+1 쿼리 방지
- [ ] STAT-PERF-02: 대량 응답 응답 시간 < 3초 (선택)

### 테스트 케이스 통과 여부 체크리스트

- [ ] TC-STAT-001~005 (권한/보안) -- 5건
- [ ] TC-STAT-010~013 (TEXT) -- 4건
- [ ] TC-STAT-020~025 (SCALE) -- 6건
- [ ] TC-STAT-030~033 (OPTION) -- 4건
- [ ] TC-STAT-040~042 (CHECKBOX) -- 3건
- [ ] TC-STAT-050~054 (GRID) -- 5건
- [ ] TC-STAT-060~063 (요약 통계) -- 4건
- [ ] TC-STAT-070~075 (경계값) -- 6건
- [ ] TC-STAT-080~083 (soft delete) -- 4건
- [ ] TC-STAT-090~094 (설문 상태별) -- 5건
- [ ] TC-STAT-100~102 (부정 시나리오) -- 3건
- [ ] TC-STAT-110~114 (복합 시나리오) -- 5건
- [ ] TC-STAT-120~121 (성능) -- 2건
- **합계**: 69건 / 69건 (검증 기준서 전수 매핑)

---

## 검증 기준 <-> 작업 매핑 (Traceability)

| 검증 기준 ID | 매핑 작업 ID |
|-------------|------------|
| STAT-INV-01 | TASK-007, TASK-008, TASK-016, TASK-017 |
| STAT-INV-02 | TASK-007, TASK-008, TASK-017 |
| STAT-INV-03 | TASK-001, TASK-008, TASK-017 |
| STAT-INV-04 | TASK-008, TASK-019 |
| STAT-INV-05 | TASK-008, TASK-019 |
| STAT-INV-06 | TASK-008, TASK-019 |
| STAT-INV-07 | TASK-002, TASK-005, TASK-006, TASK-007, TASK-008, TASK-018 |
| STAT-INV-08 | TASK-004, TASK-005, TASK-006, TASK-008, TASK-012, TASK-014, TASK-015 |
| STAT-SEC-01~05 | TASK-010, TASK-020 |
| STAT-TXT-01~04 | TASK-003, TASK-008, TASK-011 |
| STAT-SCL-01~06 | TASK-004, TASK-008, TASK-012 |
| STAT-OPT-01~04 | TASK-005, TASK-008, TASK-013 |
| STAT-CHK-01~03 | TASK-005, TASK-008, TASK-014 |
| STAT-GRD-01~05 | TASK-006, TASK-008, TASK-015 |
| STAT-SUM-01~04 | TASK-001, TASK-008, TASK-016 |
| STAT-EDGE-01~06 | TASK-008, TASK-017 |
| STAT-EDGE-10~13 | TASK-008, TASK-018 |
| STAT-EDGE-20~24 | TASK-008, TASK-019 |
| STAT-NEG-01~03 | TASK-010, TASK-021 |
| STAT-INTEG-01~05 | TASK-008, TASK-022 |
| STAT-PERF-01 | TASK-007, TASK-023 |
| STAT-PERF-02 | TASK-024 |
