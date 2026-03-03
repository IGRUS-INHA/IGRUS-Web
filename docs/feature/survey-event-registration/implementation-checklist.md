# 설문 연동 행사 신청 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-03-02 |
| 최종 업데이트 | 2026-03-03 (전체 테스트 PASS) |
| 검증 기준 문서 | `c:\dev\IGRUS-Web\docs\criteria\event\survey-event-registration-verification-criteria.md` |
| 테스트 케이스 문서 | `c:\dev\IGRUS-Web\docs\test-case\event\survey-event-registration-test-cases.md` |
| 작업 계획 문서 | `c:\dev\IGRUS-Web\docs\feature\survey-event-registration\task-plan.md` |
| 전체 상태 | COMPLETED |

## 작업 진행 현황

### 그룹 1: 기반 작업
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | Flyway 마이그레이션 (events 테이블 surveyId 추가) | DONE | - | `backend/src/main/resources/db/migration/V47__add_survey_id_to_events.sql` | FK 제약조건 포함 |
| TASK-003 | 예외 클래스 및 ErrorCode 추가 | DONE | - | `backend/src/main/java/igrus/web/event/exception/SurveyResponseRequiredException.java`, `backend/src/main/java/igrus/web/event/exception/SurveyNotReadyException.java`, `backend/src/main/java/igrus/web/event/exception/EventErrorCode.java` | SURVEY_RESPONSE_REQUIRED, SURVEY_NOT_READY 추가 |
| TASK-006 | SurveyAnswerFactory 컴포넌트 추출 | DONE | - | `backend/src/main/java/igrus/web/survey/response/service/SurveyAnswerFactory.java`, `backend/src/main/java/igrus/web/survey/response/service/SurveyResponseService.java` | 리팩토링, 외부 동작 변경 없음 |
| TASK-011 | OpenAPI 스펙 (행사 생성/수정 surveyId) | DONE | - | `openapi/schemas/events.yaml` | 7개 스키마에 surveyId 추가 (Create/Update Request, EventDetail/List/Create Response, AdminEventDetail/List Response) |

**그룹 상태**: PASS
**리뷰 이력**: R1 FAIL (FK 컬럼명 오류) → R2 PASS (양쪽 리뷰어)

---

### 그룹 2: 엔티티 및 행사 서비스
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-002 | Event 엔티티에 surveyId 필드 추가 | DONE | - | `backend/src/main/java/igrus/web/event/domain/Event.java` | surveyId 필드, create()/update() 시그니처 변경, hasSurvey() 추가 |
| TASK-004 | EventService.createEvent() 설문 연결 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventService.java`, `backend/src/main/java/igrus/web/event/dto/request/CreateEventRequest.java`, `backend/src/main/java/igrus/web/event/controller/EventController.java` | SurveyRepository 의존성 추가, validateSurveyExists() 구현 |
| TASK-005 | EventService.updateEvent() 설문 변경/해제 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventService.java`, `backend/src/main/java/igrus/web/event/dto/request/UpdateEventRequest.java`, `backend/src/main/java/igrus/web/event/controller/EventController.java` | surveyId 변경 로깅, COMPLETED 상태 차단은 기존 로직 활용 |
| TASK-007 | 설문 상태 검증 유틸리티 메서드 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | validateSurveyState() private 메서드, SurveyRepository 의존성 추가 |

**그룹 상태**: PASS
**리뷰 이력**: R1 PASS (양쪽 리뷰어). 권장: validateSurveyExists()에 findByIdAndDeletedFalseAndTrashedAtIsNull 활용, 주석 번호 정리

---

### 그룹 3: 통합 API 핵심 로직
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-008 | registerEventWithSurvey() 신규 메서드 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | 8가지 분기 매트릭스 구현, SurveyAnswerValidator/Factory/Repository 의존성 추가 |
| TASK-009 | registerEvent() 분기 로직 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java`, `backend/src/main/java/igrus/web/event/controller/EventRegistrationController.java` | 시그니처 변경 (surveyAnswers 파라미터), hasSurvey() 분기 |
| TASK-010 | handleReRegistration() 설문 검증 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | 재신청 시 설문 상태/응답 존재 검증 추가 |
| TASK-015 | 설문 연동 로그 메시지 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java`, `backend/src/main/java/igrus/web/event/service/EventService.java` | validateSurveyState WARN/INFO, registerEventWithSurvey DEBUG/INFO, EventService 생성 로그 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-PASS + code-FAIL(False Positive: SurveyResponse soft-delete 미지원 INV-27) → 실질 PASS. 권장: 중복 로직 추출, 주석 번호 정리

---

### 그룹 4: OpenAPI 및 DTO
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-012 | OpenAPI 스펙 (행사 신청 surveyAnswers) | DONE | - | `openapi/schemas/events.yaml`, `openapi/paths/events.yaml`, `openapi/openapi.yaml` | RegisterEventRequest 스키마 추가, requestBody required:false |
| TASK-013 | openapi-generator 재생성 | DONE | - | `backend/src/main/java/igrus/web/event/controller/EventRegistrationController.java` | generated 모델 -> SubmitAnswerRequest 매핑 추가, null 대신 실제 surveyAnswers 전달 |
| TASK-014 | 서비스 DTO + 컨트롤러 매핑 surveyId | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventDetailResponse.java`, `backend/src/main/java/igrus/web/event/dto/response/EventListResponse.java`, `backend/src/main/java/igrus/web/event/dto/response/EventCreateResponse.java`, `backend/src/main/java/igrus/web/event/controller/EventController.java`, `backend/src/main/java/igrus/web/event/controller/AdminEventController.java` | 3개 응답 DTO에 surveyId 추가, 3개 컨트롤러 매핑 헬퍼 업데이트 |

**그룹 상태**: PASS
**리뷰 이력**: R1 PASS (spec-reviewer). 권장: openapi-generator의 nullable 배열 초기화 동작 인지, 컨트롤러 방어 코드로 대응 완료

---

### 그룹 5: 테스트
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-016 | Event 도메인 단위 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/domain/EventTest.java` | SurveyEventLinkingTest @Nested 추가 (TC-001, TC-003, TC-009~012) |
| TASK-017 | EventService 설문 연결 단위 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/service/EventServiceTest.java` | SurveyEventLinkingTest @Nested 추가 (TC-004~008, TC-034~035, TC-041~044) |
| TASK-018 | EventRegistrationService 설문 연동 단위 테스트 | DONE | R1 FAIL → R2 수정 완료 | `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java` | 분기 #4 테스트 추가, TC-015 DisplayName 수정, TC-020 검증 개선, TC-047 verify 추가, FQCN->import 정리 |
| TASK-019 | 설문 연동 행사 신청 통합 테스트 | DONE | R1 수정 완료 | `backend/src/test/java/igrus/web/event/integration/EventRegistrationIntegrationTest.java` | TC-063 미사용 변수 제거 |
| TASK-020 | 권한 관련 통합 테스트 | DONE | R1 FAIL → R2 수정 완료 | `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java`, `backend/src/test/java/igrus/web/event/integration/EventRegistrationControllerIntegrationTest.java` | Controller 권한 테스트 추가 (TC-050,051,052,054,055), SurveyEventSecurityTest @Nested 클래스 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-FAIL + code-PASS → R2 양쪽 PASS. 권장: EventServiceTest FQCN 정리

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| SEVT-INV-01 | 행사-설문 연결은 선택 사항 | TASK-002, 004, 009, 016 | ✅ |
| SEVT-INV-02 | 행사당 설문은 최대 1개 | TASK-001, 002, 016 | ✅ |
| SEVT-INV-03 | 설문 재사용 가능 | TASK-001, 018 | ✅ |
| SEVT-INV-04 | 연결 대상 설문 존재 검증 | TASK-004, 005, 017 | ✅ |
| SEVT-INV-05 | 상태별 설문 변경 | TASK-002, 005, 017 | ✅ |
| SEVT-INV-06 | 설문 응답 필수 | TASK-008, 009, 010, 018, 019 | ✅ |
| SEVT-INV-07 | 미연결 행사 보존 | TASK-009, 018, 019 | ✅ |
| SEVT-INV-08 | 통합 API 원자적 처리 | TASK-008, 018, 019 | ✅ |
| SEVT-INV-09 | 응답 수정 무영향 | TASK-018 | ✅ |
| SEVT-INV-10 | 신청 시 설문 상태 검증 | TASK-007, 018, 019 | ✅ |
| SEVT-INV-11 | 설문 삭제/휴지통 정책 | TASK-007, 018, 019 | ✅ |
| SEVT-INV-12 | 기존 행사 신청 불변조건 보존 | TASK-009, 018, 019 | ✅ |
| SEVT-INV-13 | 기존 설문 불변조건 보존 | TASK-006, 008 | ✅ |

## 권한 검증 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| SEC-SEVT-02 | 준회원 차단 | TASK-020 | ✅ |
| SEC-SEVT-03 | 일반 회원 생성 차단 | TASK-020 | ✅ |
| SEC-SEVT-04 | 설문 응답 없이 신청 | TASK-018, 020 | ✅ |
| SEC-SEVT-05 | accessLevel 부족 | TASK-018 | ✅ |
| SEC-SEVT-06 | 비인가 부작용 없음 | TASK-020 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 5 | Critical | spec | 분기 #4 (CLOSED+answers+no response) 테스트 누락 | R2 해결: registerWithSurvey_ClosedNoResponseWithAnswers_ThrowsRequired 추가 |
| 2 | R1 | 5 | Critical | spec | TASK-020 Controller 권한 테스트(TC-050,051,054,055) 미구현 | R2 해결: SurveyEventSecurityTest @Nested 클래스에 5개 테스트 추가 (TC-050,051,052,054,055) |
