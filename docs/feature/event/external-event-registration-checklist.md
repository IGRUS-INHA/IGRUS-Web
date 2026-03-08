# 외부인 행사 신청 (External Event Registration) 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-03-06 |
| 최종 업데이트 | 2026-03-06 |
| 검증 기준 문서 | `docs/criteria/event/external-event-registration-verification-criteria.md` |
| 테스트 케이스 문서 | `docs/test-case/event/external-event-registration-test-cases.md` |
| 작업 계획 문서 | `docs/feature/event/external-event-registration-task-plan.md` |
| 전체 상태 | COMPLETE |

## 작업 진행 현황

### 그룹 1: Foundation (7 tasks)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | OpenAPI 관리자 취소 엔드포인트 추가 | DONE | - | `openapi/paths/events.yaml`, `openapi/openapi.yaml` | registrationsByRegistrationIdCancel 추가 |
| TASK-002 | openapi-generator 재생성 | DONE | - | `backend/build/generated/openapi/` | cancelRegistrationByAdmin 메서드 생성 확인 |
| TASK-003 | Flyway V48 - allowExternal 컬럼 | DONE | - | `backend/src/main/resources/db/migration/V48__add_allow_external_to_events.sql` | - |
| TASK-004 | Flyway V49 - EventRegistration 외부인 컬럼 | DONE | - | `backend/src/main/resources/db/migration/V49__add_external_columns_to_event_registrations.sql` | user_id nullable 변경 + 외부인 컬럼 6개 |
| TASK-005 | Flyway V50 - ExternalSurveyResponse 테이블 | DONE | - | `backend/src/main/resources/db/migration/V50__create_external_survey_responses_table.sql` | ENGINE=InnoDB, utf8mb4 |
| TASK-009 | 예외/ErrorCode 추가 | DONE | - | `backend/src/main/java/igrus/web/event/exception/EventErrorCode.java`, `ExternalRegistrationNotAllowedException.java`, `ExternalAlreadyRegisteredException.java`, `RegisteredMemberExistsException.java` | 3개 ErrorCode + 3개 예외 클래스 |
| TASK-018 | SecurityConfig 경로 추가 | DONE | - | `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java` | external permitAll + cancel OPERATOR+ |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL (경로 패턴 오류), R2 양쪽 PASS (2026-03-06)

---

### 그룹 2: Domain + Repository (5 tasks)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-006 | Event 엔티티 allowExternal | DONE | - | `backend/src/main/java/igrus/web/event/domain/Event.java` | allowExternal Boolean 필드, create() 오버로드, update() 오버로드 |
| TASK-007 | EventRegistration 엔티티 외부인 지원 | DONE | - | `backend/src/main/java/igrus/web/event/domain/EventRegistration.java` | user nullable, isExternal+4 외부인 필드, createExternal() 팩토리 |
| TASK-008 | ExternalSurveyResponse 엔티티 생성 | DONE | - | `backend/src/main/java/igrus/web/event/domain/ExternalSurveyResponse.java` | BaseEntity 미상속, JSON answers, create() 팩토리 |
| TASK-010 | EventRegistrationRepository 쿼리 추가 | DONE | - | `backend/src/main/java/igrus/web/event/repository/EventRegistrationRepository.java` | existsByEvent+studentId/phone, existsOverlappingExternalRegistration |
| TASK-011 | ExternalSurveyResponseRepository 생성 | DONE | - | `backend/src/main/java/igrus/web/event/repository/ExternalSurveyResponseRepository.java` | JpaRepository 상속 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (2026-03-06)

---

### 그룹 3: Service Layer (5 tasks)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-012 | ExternalEventRegistrationService 핵심 로직 | DONE | - | `backend/src/main/java/igrus/web/event/service/ExternalEventRegistrationService.java` | 전체 검증 로직 구현 완료 |
| TASK-013 | 준회원 조건부 허용 로직 변경 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | allowExternal=true 시 준회원 허용 |
| TASK-014 | 관리자 취소 서비스 구현 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java`, `backend/src/main/java/igrus/web/event/domain/EventChangeType.java` | cancelRegistrationByAdmin + REGISTRATION_CANCELED_BY_ADMIN 감사 이벤트 |
| TASK-015 | RegistrationListResponse 외부인 필드 | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/RegistrationListResponse.java`, `backend/src/main/java/igrus/web/event/controller/EventRegistrationController.java` | isExternal, phone 필드 + user null 분기 + 컨트롤러 매핑 |
| TASK-030 | 기존 서비스 user null 방어 | DONE | - | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | approveRegistration 외부인 분기 + handleReRegistration 외부인 사전 차단 + validateNoExternalTimeOverlap 추가 |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL (IllegalStateException + detached entity), R2 양쪽 PASS (2026-03-06)

---

### 그룹 4: Controllers + Unit Tests (5 tasks)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-016 | ExternalEventRegistrationController | DONE | - | `backend/src/main/java/igrus/web/event/controller/ExternalEventRegistrationController.java` | EventExternalRegistrationApi implements, 201 Created, surveyAnswers 매핑 |
| TASK-017 | 관리자 취소 컨트롤러 | DONE | - | `backend/src/main/java/igrus/web/event/controller/EventRegistrationController.java` | UnsupportedOperationException stub -> 서비스 연동 교체 |
| TASK-019 | Event allowExternal 단위 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/domain/EventTest.java` | TC-014, TC-015, update() 변경 테스트 7개 |
| TASK-020 | EventRegistration 외부인 팩토리 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/domain/EventRegistrationTest.java` | TC-031, TC-033, isExternal 차이 검증 3개 |
| TASK-021 | Bean Validation 단위 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/dto/ExternalRegisterEventRequestValidationTest.java` | TC-022~TC-026, TC-041~TC-052 총 17개 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer FAIL (로그 name 누락), code-reviewer PASS. 수정 후 PASS (2026-03-06)

---

### 그룹 5: Service + Integration Tests (7 tasks)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-022 | ExternalEventRegistrationService 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/service/ExternalEventRegistrationServiceTest.java` | TC-001~TC-013, TC-031, TC-040~TC-056, TC-074~TC-076 Mockito 단위 테스트. R2: TC-074~076 ListAppender 로그 검증 추가 |
| TASK-023 | 준회원 회귀 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java` | TC-078, TC-079 준회원 조건부 허용 회귀 테스트 추가. R2: TC-011 정원 공유 회귀 테스트 추가 |
| TASK-024 | 관리자 취소 서비스 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java` | TC-019, TC-020, TC-032, TC-036, TC-077 + 엣지케이스 9개 테스트 (CancelRegistrationByAdminTest) |
| TASK-025 | 외부인 컨트롤러 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/integration/ExternalEventRegistrationControllerIntegrationTest.java` | TC-057~TC-066, TC-069, TC-080 MockMvc 컨트롤러 통합 테스트. R2: TC-080 isExternal/phone/userId 상세 jsonPath 검증 추가 |
| TASK-026 | 관리자 취소 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/integration/ExternalEventRegistrationControllerIntegrationTest.java` | TC-021, TC-067, TC-068 (AdminCancelTest 네스트 클래스) |
| TASK-027 | 선발제 FSM 통합 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/integration/ExternalEventRegistrationFsmIntegrationTest.java` | TC-033~TC-039 실제 DB FSM 통합 테스트 |
| TASK-028 | 동시성 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/integration/ExternalEventConcurrencyTest.java` | TC-070~TC-073 멀티스레드 동시성 테스트 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer FAIL (TC-011 누락, TC-080 assertion 불충분, TC-074~076 로그 미검증), code-reviewer PASS. R2 양쪽 PASS (2026-03-06)

---

### 그룹 6: Documentation (1 task)
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-029 | 기존 검증 기준서 갱신 | DONE | - | `docs/criteria/event/external-event-registration-verification-criteria.md`, `docs/criteria/event/event-registration-verification-criteria.md`, `docs/criteria/event/event-verification-criteria.md` | GAP-EXT-01~05 모두 해결됨 |

**그룹 상태**: DONE
**리뷰 이력**: -

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| EXT-INV-01 | allowExternal 검사 | TASK-012, 016, 022, 025 | ✅ |
| EXT-INV-02 | studentId 중복 방지 | TASK-010, 012, 022 | ✅ |
| EXT-INV-03 | phone 중복 방지 | TASK-010, 012, 022 | ✅ |
| EXT-INV-04 | 정원 공유 | TASK-012, 022, 028 | ✅ |
| EXT-INV-05 | 준회원 조건부 허용 | TASK-013, 023, 025 | ✅ |
| EXT-INV-06 | allowExternal 기본값 | TASK-003, 006, 019 | ✅ |
| EXT-INV-07 | OPEN + 기간 내 검증 | TASK-012, 022 | ✅ |
| EXT-INV-08 | UNPUBLISHED 차단 | TASK-012, 022, 025 | ✅ |
| EXT-INV-09 | 관리자만 취소 | TASK-014, 017, 018, 024, 026, 030 | ✅ |
| EXT-INV-10 | 필수 필드 검증 | TASK-021 | ✅ |
| EXT-INV-11 | 외부인 설문 연동 | TASK-005, 008, 011, 012, 022 | ✅ |
| EXT-INV-12 | 동일 학번 가입 회원 존재 | TASK-012, 022 | ✅ |
| SEC-EXT-01~07 | 권한/보안 | TASK-018, 025, 026 | ✅ |
| Section 0-1 | REG-INV-04 변경 | TASK-013, 023, 029 | ✅ |
| Section 0-2 | SEC-REG-01 변경 | TASK-013, 023, 029 | ✅ |
| Section 0-3 | RegistrationListResponse 변경 | TASK-015, 025 | ✅ |

## 테스트 케이스 통과 현황
| TC-ID | 설명 | 관련 TASK | 상태 |
|-------|------|----------|:----:|
| TC-001~TC-030 | 도메인 규칙 | TASK-019~024 | ✅ |
| TC-031~TC-040 | 상태 모델 | TASK-020, 022, 027 | ✅ |
| TC-041~TC-056 | 입력 경계값 | TASK-021, 022 | ✅ |
| TC-057~TC-062 | 동치 분할 | TASK-025 | ✅ |
| TC-063~TC-069 | 권한/보안 | TASK-025, 026 | ✅ |
| TC-070~TC-073 | 동시성 | TASK-028 | ✅ |
| TC-074~TC-077 | 관측 가능성 | TASK-022, 024 | ✅ |
| TC-078~TC-080 | 회귀 테스트 | TASK-023, 025 | ✅ |

## GAP 해결 현황
| GAP ID | 내용 | 커버 작업 | 상태 |
|--------|------|----------|:----:|
| GAP-EXT-01 | REG-INV-04 본문 갱신 | TASK-029 | ✅ |
| GAP-EXT-02 | SEC-REG-01 및 매트릭스 갱신 | TASK-029 | ✅ |
| GAP-EXT-03 | OpenAPI 취소 엔드포인트 추가 | TASK-001 | ✅ |
| GAP-EXT-04 | Event 엔티티/DTO allowExternal 기술 | TASK-029 | ✅ |
| GAP-EXT-05 | DECISION-02 반영 갱신 | TASK-029 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 그룹1 | 필수 | code-reviewer | SecurityConfig 64행 경로 패턴 `/api/events/*/registrations` → `/api/v1/events/*/registrations` 누락 (기존 코드 버그) | FIXED |
| 2 | R1 | 그룹3 | 필수 | code-reviewer | ExternalEventRegistrationService에서 `IllegalStateException` 직접 사용 (CLAUDE.md 규칙 위반) | FIXED - SurveyResponseSerializationException 커스텀 예외 생성 |
| 3 | R1 | 그룹3 | 필수 | code-reviewer | `clearAutomatically=true` 이후 detached event 엔티티를 createExternal()에 전달 | FIXED - increment 후 event 재조회 |
| 4 | R1 | 그룹4 | 필수 | spec-reviewer | ExternalEventRegistrationController 로그 메시지에 name 필드 누락 (Section 6-1) | FIXED - name 필드 추가 |
| 5 | R2 | 그룹5 | 필수 | spec-reviewer | TC-011 누락 (외부인 정원 가득 시 회원 차단 테스트) | FIXED - EventRegistrationServiceTest에 TC-011 추가 |
| 6 | R2 | 그룹5 | 필수 | spec-reviewer | TC-080 assertion 불충분 (isExternal, phone, userId 개별 검증 누락) | FIXED - jsonPath 상세 필드 검증 추가 |
| 7 | R2 | 그룹5 | 필수 | spec-reviewer | TC-074~076 로그 검증 미흡 (ListAppender 미사용) | FIXED - Logback ListAppender로 로그 레벨/메시지 내용 검증 |
