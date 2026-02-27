# 설문 통계 API 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-02-27 |
| 최종 업데이트 | 2026-02-28 (그룹 5 완료) |
| 검증 기준 문서 | `docs/criteria/survey/survey-statistics-verification-criteria.md` |
| 테스트 케이스 문서 | `backend/docs/test-case/survey/survey-statistics-test-cases.md` |
| 작업 계획 문서 | `docs/tasks/survey/survey-statistics-tasks.md` |
| 전체 상태 | DONE |

## 작업 진행 현황

### 그룹 1: DTO 설계 (TASK-001~006)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | SurveyStatisticsResponse 최상위 DTO | DONE | - | `survey/statistics/dto/response/SurveyStatisticsResponse.java` | record 타입, Instant 사용 |
| TASK-002 | QuestionStatisticsResponse 질문 공통 DTO | DONE | - | `survey/statistics/dto/response/QuestionStatisticsResponse.java` | @JsonInclude(NON_NULL) 적용 |
| TASK-003 | TextQuestionStatistics DTO | DONE | - | `survey/statistics/dto/response/TextQuestionStatistics.java` | record 타입 |
| TASK-004 | ScaleQuestionStatistics DTO | DONE | - | `survey/statistics/dto/response/ScaleQuestionStatistics.java` | BigDecimal average, nullable min/max |
| TASK-005 | OptionQuestionStatistics + OptionStatisticsItem DTO | DONE | - | `survey/statistics/dto/response/OptionQuestionStatistics.java`, `OptionStatisticsItem.java` | deleted 플래그 포함 |
| TASK-006 | GridQuestionStatistics + GridRowStatistics DTO | DONE | - | `survey/statistics/dto/response/GridQuestionStatistics.java`, `GridRowStatistics.java` | OptionStatisticsItem 재사용 |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 2: 서비스 + 컨트롤러 (TASK-007~010)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-007 | Repository 메서드 추가 | DONE | - | `survey/response/repository/SurveyResponseRepository.java`, `SurveyAnswerRepository.java`, `survey/question/repository/SurveyQuestionRepository.java` | 기존 Repository에 통계용 메서드 추가. fetch join으로 N+1 방지 |
| TASK-008 | SurveyStatisticsService 핵심 집계 로직 | DONE | - | `survey/statistics/service/SurveyStatisticsService.java` | 질문 타입별 통계 집계, BigDecimal HALF_UP scale=1, soft delete 규칙 적용 |
| TASK-009 | 로깅 추가 | DONE | - | `survey/statistics/service/SurveyStatisticsService.java` | @Slf4j, 시작/완료/실패 로그 포함 |
| TASK-010 | SurveyStatisticsController 구현 | DONE | - | `survey/statistics/controller/SurveyStatisticsController.java` | @PreAuthorize OPERATOR+ADMIN, @Positive, Swagger 문서화, 에러 응답 content 파라미터 생략 |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 3: 단위 테스트 - 카테고리별 (TASK-011~015)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-011 | TEXT 카테고리 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-010~013, 4건 (SHORT_ANSWER, PARAGRAPH, DATE, FILE_UPLOAD) |
| TASK-012 | SCALE 카테고리 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-020~025, 6건 (응답 수, 평균, min/max, 분포, 1건, 소수점) |
| TASK-013 | OPTION 카테고리 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-030~033, 4건 (MC 선택 수, 비율 합계, DROPDOWN, 미선택 0) |
| TASK-014 | CHECKBOX 카테고리 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-040~042, 3건 (선택 수, 비율>100%, responseCount 구분) |
| TASK-015 | GRID 카테고리 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-050~054, 5건 (MC_GRID 분포, 비율<=100%, CB_GRID 분포, 비율>100%, 미응답 행) |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 4: 단위 테스트 - 요약/엣지/상태 (TASK-016~019)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-016 | 전체 요약 통계 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-060~063, 4건 (총 응답 수, 삭제 응답 제외, 응답 기간, 0건 기간 null) |
| TASK-017 | 경계값/엣지 케이스 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-070~075, 6건 (0건, 1건, 전체 삭제, 비필수 부분 응답, 빈 응답, 삭제 Answer 제외) |
| TASK-018 | soft delete 통계 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-080~083, 4건 (삭제 질문, 삭제 선택지, 삭제 행, 전체 선택지 삭제) |
| TASK-019 | 설문 상태별 통계 단위 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceTest.java` | TC-STAT-090~094, 5건 (U+NS, P+O, P+C, 휴지통, 영구삭제 404) |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 5: 통합 테스트 (TASK-020~024)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-020 | 권한/보안 컨트롤러 통합 테스트 | DONE | - | `survey/statistics/controller/SurveyStatisticsControllerTest.java` | TC-STAT-001~005, 5건 (401, 403x2, 200, 부작용 없음) |
| TASK-021 | 부정 시나리오 컨트롤러 통합 테스트 | DONE | - | `survey/statistics/controller/SurveyStatisticsControllerTest.java` | TC-STAT-100~102, 3건 (404, 400x2) |
| TASK-022 | 복합 시나리오 통합 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java` | TC-STAT-110~114, 5건 (혼합 질문, 응답 수정, 질문 추가, 비회원 응답, displayOrder 정렬) |
| TASK-023 | N+1 쿼리 방지 성능 통합 테스트 | DONE | - | `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java` | TC-STAT-120, 1건 (질문 10개, 선택지 50개, 응답 100건, 3초 이내) |
| TASK-024 | 대량 응답 성능 테스트 (선택) | DONE | - | `survey/statistics/service/SurveyStatisticsServiceIntegrationTest.java` | TC-STAT-121, 1건 (질문 50개, 응답 1000건, 3초 이내) |

**그룹 상태**: DONE
**리뷰 이력**: -

---

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | 구현 | 그룹 5 | HIGH | - | MultipleBagFetchException: findAllBySurveyIdWithOptionsAndRows에서 options+rows 동시 fetch join 불가. 단위 테스트에서는 Mock으로 우회되어 미발견. | findAllBySurveyIdWithOptions + findAllBySurveyIdWithRows로 쿼리 분리. 영속성 컨텍스트 내 자동 병합으로 해결. |
