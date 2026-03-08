# 행사/설문 기능 보완 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-03-08 |
| 최종 업데이트 | 2026-03-08 19:00 |
| 검증 기준 문서 | `docs/criteria/event/event-survey-improvements-verification-criteria.md` |
| 테스트 케이스 문서 | `docs/test-case/event/event-survey-improvements-test-cases.md` |
| 작업 계획 문서 | `docs/tasks/event/event-survey-improvements-task-plan.md` |
| 전체 상태 | COMPLETED |

## 작업 진행 현황

### 그룹 1: Phase 1 - allowExternal 버그 수정
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | CreateEventRequest allowExternal 추가 | DONE | - | `backend/src/main/java/igrus/web/event/dto/request/CreateEventRequest.java` | Boolean allowExternal 필드 추가 |
| TASK-002 | UpdateEventRequest allowExternal 추가 | DONE | - | `backend/src/main/java/igrus/web/event/dto/request/UpdateEventRequest.java` | Boolean allowExternal 필드 추가 |
| TASK-003 | EventDetailResponse allowExternal 추가 | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventDetailResponse.java` | 필드 추가 + from() 매핑 |
| TASK-004 | EventCreateResponse allowExternal 추가 | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventCreateResponse.java` | 필드 추가 + from() 매핑 |
| TASK-005 | EventListResponse allowExternal 추가 | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventListResponse.java` | 필드 추가 + from() 매핑 |
| TASK-006 | EventService allowExternal 전달 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventService.java` | create: 12-param 오버로드 사용, update: null이면 기존값 유지 |
| TASK-007 | EventController allowExternal 매핑 | DONE | - | `backend/src/main/java/igrus/web/event/controller/EventController.java` | 생성/수정 요청 + 응답 4곳 매핑 |
| TASK-008 | AdminEventController allowExternal 매핑 | DONE | - | `backend/src/main/java/igrus/web/event/controller/AdminEventController.java` | 상세/목록 응답 2곳 매핑 |
| TASK-009 | allowExternal 빌드 검증 | DONE | - | - | BUILD SUCCESSFUL, 2702 tests passed, 0 failed |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 2: Phase 2 - 설문 응답 수 표시
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-010 | SurveyResponseRepository countBySurveyId | DONE | - | `backend/src/main/java/igrus/web/survey/response/repository/SurveyResponseRepository.java` | countBySurveyIdAndDeletedFalse + 배치 countBySurveyIdInAndDeletedFalse 추가 |
| TASK-011 | SurveyListResponse responseCount | DONE | - | `backend/src/main/java/igrus/web/survey/dto/response/SurveyListResponse.java` | int responseCount 필드 추가, from(Survey, int) 시그니처 변경 |
| TASK-012 | SurveyDetailResponse responseCount | DONE | - | `backend/src/main/java/igrus/web/survey/dto/response/SurveyDetailResponse.java` | int responseCount 필드 추가, from(Survey, int) 시그니처 변경 |
| TASK-013 | OpenAPI 설문 스키마 responseCount | DONE | - | `openapi/schemas/surveys.yaml` | SurveyDetailResponse, SurveyListResponse에 responseCount: integer 추가 |
| TASK-014 | SurveyService responseCount 조회 | DONE | - | `backend/src/main/java/igrus/web/survey/service/SurveyService.java` | 단건: countBySurveyIdAndDeletedFalse, 목록: 배치 countBySurveyIdInAndDeletedFalse (N+1 방지) |
| TASK-015 | 설문 컨트롤러 responseCount 매핑 | DONE | - | `backend/src/main/java/igrus/web/survey/dto/response/SurveyDetailResponseMapper.java`, `backend/src/main/java/igrus/web/survey/controller/SurveyController.java` | 상세/목록 응답 매핑에 responseCount 추가 |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 3: Phase 3 - 관리자 설문 응답 목록 조회 API
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-016 | 관리자 응답 목록 OpenAPI 스펙 | DONE | - | `openapi/paths/surveys.yaml`, `openapi/schemas/surveys.yaml`, `openapi/openapi.yaml` | AdminSurveyResponseListItem 스키마 + GET /api/v1/admin/surveys/{surveyId}/responses 경로 추가 |
| TASK-017 | 관리자 응답 목록 DTO | DONE | - | `backend/src/main/java/igrus/web/survey/response/dto/response/AdminSurveyResponseListItem.java` | responseId, userId, userName, submittedAt, answers 포함, SurveyResponseDetailResponse.AnswerResponse 재사용 |
| TASK-018 | SurveyResponseRepository 목록 조회 | DONE | - | `backend/src/main/java/igrus/web/survey/response/repository/SurveyResponseRepository.java` | findValidResponsesWithUserAndAnswersBySurveyId 추가 (user+answers+question+selectedOption+selectedRow fetch join) |
| TASK-019 | SurveyResponseService 관리자 조회 | DONE | - | `backend/src/main/java/igrus/web/survey/response/service/SurveyResponseService.java` | getResponsesBySurveyId: 설문 존재 확인 + deleted=false 응답 목록 조회 |
| TASK-020 | 관리자 컨트롤러 구현 | DONE | - | `backend/src/main/java/igrus/web/survey/response/controller/AdminSurveyResponseController.java` | AdminSurveyResponseApi implements, @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')") |

**그룹 상태**: DONE
**리뷰 이력**: R1 spec-reviewer FAIL → 수정 완료 (ApiSecurityConfig OPERATOR 접근 허용, 답변 그룹핑 로직 중복 제거)

---

### 그룹 4: Phase 4 - 설문 응답 삭제 API
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-021 | 설문 응답 삭제 OpenAPI 스펙 | DONE | - | `openapi/paths/surveys.yaml` | DELETE /api/v1/surveys/{surveyId}/responses 추가 (204/401/404/409) |
| TASK-022 | SurveyResponseService 삭제 로직 | DONE | - | `backend/src/main/java/igrus/web/survey/response/service/SurveyResponseService.java`, `backend/src/main/java/igrus/web/survey/response/exception/SurveyClosedException.java`, `backend/src/main/java/igrus/web/survey/exception/SurveyErrorCode.java`, `backend/src/main/java/igrus/web/event/repository/EventRepository.java` | CLOSED 설문 409, 행사 연동 취소 로직 포함 |
| TASK-023 | 설문 응답 삭제 컨트롤러 | DONE | - | `backend/src/main/java/igrus/web/survey/response/controller/SurveyResponseController.java` | @PreAuthorize("isAuthenticated()"), 204 No Content |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL → R2 spec-reviewer PASS + code-reviewer PASS

---

### 그룹 5: Phase 5 - 외부인 설문 응답 통계 통합
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-024 | SurveyStatisticsService 외부인 통합 | DONE | - | `backend/src/main/java/igrus/web/survey/statistics/service/SurveyStatisticsService.java`, `backend/src/main/java/igrus/web/event/repository/ExternalSurveyResponseRepository.java` | ExternalSurveyResponse JSON 파싱→합산, 파싱 오류 시 skip+로깅, 기존 테스트 Mock 보강 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer FAIL → R2 spec-reviewer PASS + code-reviewer PASS

---

### 그룹 6: Phase 6 - 코드 중복 제거
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-025 | EventStatusHelper 공통 추출 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventStatusHelper.java`, `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java`, `backend/src/main/java/igrus/web/event/service/ExternalEventRegistrationService.java`, `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java`, `backend/src/test/java/igrus/web/event/service/ExternalEventRegistrationServiceTest.java` | @Component, increment/decrement 공통 추출 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer PASS + code-reviewer PASS

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| EVTSRV-001 | CreateEventRequest allowExternal | TASK-001 | ✅ |
| EVTSRV-002 | UpdateEventRequest allowExternal | TASK-002 | ✅ |
| EVTSRV-003 | EventDetailResponse allowExternal | TASK-003 | ✅ |
| EVTSRV-004 | EventCreateResponse allowExternal | TASK-004 | ✅ |
| EVTSRV-005 | EventListResponse allowExternal | TASK-005 | ✅ |
| EVTSRV-006 | EventService.createEvent() allowExternal | TASK-006 | ✅ |
| EVTSRV-007 | EventService.updateEvent() allowExternal | TASK-006 | ✅ |
| EVTSRV-008 | allowExternal 동치 분할 (생성) | TASK-006,007 | ✅ |
| EVTSRV-009 | allowExternal 동치 분할 (수정) | TASK-006,007 | ✅ |
| EVTSRV-010 | EventController allowExternal 매핑 | TASK-007 | ✅ |
| EVTSRV-011 | AdminEventController allowExternal 매핑 | TASK-008 | ✅ |
| EVTSRV-012 | OpenAPI-DTO 전수 일치 | TASK-009 | ✅ |
| EVTSRV-013 | 빌드 및 테스트 통과 | TASK-009 | ✅ |
| EVTSRV-014 | SurveyResponseRepository 카운트 | TASK-010 | ✅ |
| EVTSRV-015 | SurveyListResponse responseCount | TASK-011 | ✅ |
| EVTSRV-016 | SurveyDetailResponse responseCount | TASK-012 | ✅ |
| EVTSRV-017 | OpenAPI 설문 responseCount | TASK-013 | ✅ |
| EVTSRV-018 | responseCount 경계값 | TASK-014 | ✅ |
| EVTSRV-019 | N+1 쿼리 방지 | TASK-014 | ✅ |
| EVTSRV-020 | 컨트롤러 responseCount 매핑 | TASK-015 | ✅ |
| EVTSRV-021 | 관리자 응답 목록 API | TASK-016,020 | ✅ |
| EVTSRV-022 | 설문 미존재 404 | TASK-019 | ✅ |
| EVTSRV-023 | 삭제된 설문 404 | TASK-019 | ✅ |
| EVTSRV-024 | 삭제된 응답 미포함 | TASK-019 | ✅ |
| EVTSRV-025 | 관리자 RBAC | TASK-020 | ✅ |
| EVTSRV-026 | 응답 0건 빈 목록 | TASK-019 | ✅ |
| EVTSRV-043 | 관리자 API OpenAPI 등록 | TASK-016 | ✅ |
| EVTSRV-027 | 응답 삭제 API | TASK-021,023 | ✅ |
| EVTSRV-028 | 본인 응답만 삭제 | TASK-022 | ✅ |
| EVTSRV-029 | CLOSED 삭제 불가 | TASK-022 | ✅ |
| EVTSRV-030 | 삭제 시 행사 취소 | TASK-022 | ✅ |
| EVTSRV-031 | 독립 설문 삭제 | TASK-022 | ✅ |
| EVTSRV-032 | 응답 미존재 404 | TASK-022 | ✅ |
| EVTSRV-033 | 삭제 인증 필수 | TASK-023 | ✅ |
| EVTSRV-034 | 삭제 상태 매트릭스 | TASK-022 | ✅ |
| EVTSRV-044 | 삭제 API OpenAPI 등록 | TASK-021 | ✅ |
| EVTSRV-035 | 외부인 통계 포함 | TASK-024 | ✅ |
| EVTSRV-036 | 외부인 JSON 파싱 | TASK-024 | ✅ |
| EVTSRV-037 | 외부인 없는 설문 회귀 | TASK-024 | ✅ |
| EVTSRV-038 | 외부인 통계 경계값 | TASK-024 | ✅ |
| EVTSRV-039 | JSON 파싱 오류 처리 | TASK-024 | ✅ |
| EVTSRV-040 | increment 공통화 | TASK-025 | ✅ |
| EVTSRV-041 | decrement 공통화 | TASK-025 | ✅ |
| EVTSRV-042 | 중복 제거 회귀 | TASK-025 | ✅ |

## 테스트 케이스 통과 현황
| TC-ID | 설명 | 관련 TASK | 상태 |
|-------|------|----------|:----:|
| TC-EVTSRV-001~018 | Phase 1 테스트 케이스 | TASK-001~009 | ✅ |
| TC-EVTSRV-019~028 | Phase 2 테스트 케이스 | TASK-010~015 | ✅ |
| TC-EVTSRV-029~039 | Phase 3 테스트 케이스 | TASK-016~020 | ✅ |
| TC-EVTSRV-040~049 | Phase 4 테스트 케이스 | TASK-021~023 | ✅ |
| TC-EVTSRV-050~059 | Phase 5 테스트 케이스 | TASK-024 | ✅ |
| TC-EVTSRV-060~062 | Phase 6 테스트 케이스 | TASK-025 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 3 | 필수 | spec-reviewer | ApiSecurityConfig `/api/v1/admin/**` catch-all이 `hasRole("ADMIN")`이라 OPERATOR 접근 불가 (EVTSRV-025 위반) | `/api/v1/admin/surveys/**`를 OPERATOR+ADMIN 경로 목록에 추가 |
| 2 | R1 | 3 | 권장 | code-reviewer | AdminSurveyResponseListItem.from()과 SurveyResponseDetailResponse.from()의 답변 그룹핑 로직 중복 | groupAnswersByQuestion() 공통 메서드 추출 |
| 3 | R1 | 4 | 필수 | code-reviewer | findBySurveyIdAndUserId가 deleted=true 응답도 반환하여 이미 삭제된 응답 재삭제 가능 | findBySurveyIdAndUserIdAndDeletedFalse 사용 |
| 4 | R1 | 4 | 필수 | code-reviewer | NOT_STARTED 상태에서 삭제 허용 (CLOSED만 차단) | OPEN만 허용하도록 조건 변경 |
| 5 | R1 | 4 | 권장 | code-reviewer | cancelLinkedEventRegistration 순서 의존성 + WAITING 미감소 규칙 주석 미비 | 주석 보강 |
