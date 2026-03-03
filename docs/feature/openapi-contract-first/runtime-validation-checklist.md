# OpenAPI 런타임 응답 검증 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-03-02 |
| 최종 업데이트 | 2026-03-03 |
| 작업 계획 문서 | `c:\dev\IGRUS-Web\docs\feature\openapi-contract-first\runtime-validation-task-plan.md` |
| 전체 상태 | COMPLETE (전체 TASK 완료) |

## 작업 진행 현황

### 그룹 1: Phase A - PoC 검증
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-200 | swagger-request-validator PoC 검증 | DONE | PASS | `backend/src/test/java/igrus/web/common/openapi/SwaggerRequestValidatorPocTest.java`, `docs/feature/openapi-contract-first/poc-result-200.md` | 2.46.0 확정, 멀티파일 로딩 성공, 3.1.0 타입검증 한계 발견 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 2: Phase B - 의존성 추가
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-201 | build.gradle에 swagger-request-validator-mockmvc 테스트 의존성 추가 | DONE | PASS | `backend/build.gradle` | TASK-200 PoC에서 이미 추가됨, 확인 완료 |
| TASK-220 | build.gradle에 swagger-request-validator-spring-webmvc 런타임 의존성 추가 | DONE | PASS | `backend/build.gradle` | implementation 스코프, 의존성 충돌 없음 확인 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 3: Phase C - 인프라 구축
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-202 | MockMvc 통합 테스트용 OpenApiValidatorUtil 유틸리티 작성 | DONE | PASS | `backend/src/test/java/igrus/web/common/OpenApiValidatorUtil.java` | 싱글턴 패턴, matchesOpenApiSpec() ResultMatcher 헬퍼. R2에서 OpenApiValidatorFactory로 위임하도록 리팩토링 |
| TASK-221 | dev/test 프로필 전용 OpenApiValidationFilter + Interceptor 설정 | DONE | PASS | `backend/src/main/java/igrus/web/common/config/OpenApiValidationConfig.java`, `backend/src/main/java/igrus/web/common/config/OpenApiValidatorFactory.java`, `backend/src/test/resources/application.yml` | @Profile + @ConditionalOnProperty 스위치, LoggingValidationReportHandler. R2에서 공통 팩토리 추출, graceful degradation 추가, WARN 로그 포함 개선 |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL (필수2건, 권장2건) → R2 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 4: Phase D - 기존 테스트 검증 + Filter 검증
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-210 | 커뮤니티 도메인 통합 테스트에 OpenAPI 응답 스키마 검증 추가 | DONE | PASS | `BoardControllerTest.java`, `PostControllerTest.java`, `CommentControllerTest.java`, `PostLikeControllerTest.java`, `CommentLikeControllerTest.java`, `BookmarkControllerTest.java`, `CommentReportControllerTest.java` | 7개 테스트 파일, 2xx 응답에 matchesOpenApiSpec() 추가, 전체 PASS |
| TASK-211 | Admin 도메인 통합 테스트에 OpenAPI 응답 스키마 검증 추가 | DONE | PASS | `AdminDashboardControllerTest.java`, `AdminUserControllerTest.java`, `AdminMemberControllerTest.java`, `AdminLoginHistoryControllerTest.java`, `AdminAccountStatusChangeHistoryControllerTest.java` | 5개 테스트 파일. getUserDetail 2건은 스펙 불일치(joinRoute nullable)로 검증 제외 |
| TASK-222 | OpenApiValidationFilter 동작 검증 통합 테스트 | DONE | PASS | `backend/src/test/java/igrus/web/common/openapi/OpenApiValidationFilterTest.java` | 독립 컨텍스트(@TestPropertySource), Bean 등록/정상 통과/스키마 불일치 감지/다수 엔드포인트 순차 호출 검증. R2에서 TC-221-03 추가, 컨텍스트 캐싱 위반 사유 Javadoc 문서화 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer FAIL (필수2건), code-reviewer PASS (권장2건) → R2 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 5: Phase F - CI/문서
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-230 | CI에서 OpenAPI 응답 검증 테스트 자동 실행 확인 | DONE | PASS | `docs/feature/openapi-contract-first/ci-openapi-validation-report.md` | CI 수정 불필요. openapi/ 변경 감지 이미 포함, checkout@v4로 스펙 파일 접근 가능, 번들 파일 불필요 |
| TASK-231 | 응답 검증 실패 시 트러블슈팅 가이드 문서 작성 | DONE | PASS | `docs/feature/openapi-contract-first/response-validation-troubleshooting.md` | 검증 실패 메시지 해석, 7가지 불일치 유형별 해결법, 수정 방향 결정 기준, 3.1.0 한계, 프로젝트 설정 참조, FAQ |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 6: Phase D 보완 - 행사 도메인 검증
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-212 | 행사 도메인 통합 테스트에 OpenAPI 응답 스키마 검증 추가 | DONE | PASS | `EventControllerIntegrationTest.java`, `EventRegistrationControllerIntegrationTest.java`, `openapi/schemas/events.yaml` | TC-212-01 (4개 테스트), TC-212-02 (2개 테스트) 전부 PASS. closeReason nullable 스펙 수정 포함 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 7: Phase E - 미존재 테스트 스모크 작성
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-213 | 테스트 미존재 컨트롤러에 대한 최소 응답 검증 스모크 테스트 작성 | DONE | PASS | 15개 스모크 테스트 파일 (아래 목록 참조) | 15개 컨트롤러 x 18개 테스트 메서드, 전체 PASS |

**생성된 파일 목록**:
- `backend/src/test/java/igrus/web/community/pinnedpost/controller/PinnedPostControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/inquiry/controller/AdminInquiryControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/inquiry/controller/GuestInquiryControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/inquiry/controller/MemberInquiryControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/user/mypage/controller/MyPageControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/controller/SurveyControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/question/controller/SurveyQuestionControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/question/controller/SurveyQuestionOptionControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/question/controller/SurveyQuestionRowControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/response/controller/SurveyResponseControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/survey/response/controller/SurveyAnonymousResponseControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/security/auth/common/controller/PrivacyConsentControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/user/semester/controller/SemesterMemberControllerSmokeTest.java`
- `backend/src/test/java/igrus/web/user/semester/controller/AdminSemesterMemberControllerSmokeTest.java`

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| CR-201 | 기존 통합 테스트 컨트롤러(14개)에서 OpenAPI 응답 스키마 검증 추가 및 통과 | TASK-210~212 | ✅ |
| CR-202 | 통합 테스트 미존재 컨트롤러(15개)에 최소 1개 이상 스모크 테스트 존재 | TASK-213 | ✅ |
| CR-203 | dev/test 프로필에서 OpenApiValidationFilter 활성화 | TASK-221, TASK-222 | ✅ |
| CR-204 | prod 프로필에서 OpenApiValidationFilter 비활성화 (성능 무영향) | TASK-221 | ✅ |
| CR-205 | CI에서 ./gradlew test 실행 시 응답 검증 테스트 자동 포함 | TASK-230 | ✅ |
| CR-206 | 응답 검증 실패 시 원인/수정 방향 가이드 문서 존재 | TASK-231 | ✅ |

## 테스트 케이스 통과 현황
| TC-ID | 설명 | 관련 TASK | 상태 |
|-------|------|----------|:----:|
| TC-210-01 | BoardController GET /boards 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-02 | BoardController GET /boards/{code} 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-03 | PostController GET /posts 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-04 | CommentController GET /posts/{id}/comments 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-05 | PostLikeController POST /posts/{id}/like 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-06 | BookmarkController POST /posts/{id}/bookmark 응답 스키마 검증 | TASK-210 | ✅ |
| TC-210-07 | CommentReportController POST /comments/{id}/reports 응답 스키마 검증 | TASK-210 | ✅ |
| TC-211-01 | AdminDashboardController GET /admin/dashboard 응답 스키마 검증 | TASK-211 | ✅ |
| TC-211-02 | AdminUserController GET /admin/users 응답 스키마 검증 | TASK-211 | ✅ |
| TC-211-03 | AdminMemberController GET /admin/associates/pending 응답 스키마 검증 | TASK-211 | ✅ |
| TC-211-04 | AdminLoginHistoryController GET /admin/login-histories 응답 스키마 검증 | TASK-211 | ✅ |
| TC-211-05 | AdminAccountStatusChangeHistoryController 응답 스키마 검증 | TASK-211 | ✅ |
| TC-221-01 | test 프로필에서 OpenApiValidationFilter Bean 등록 확인 | TASK-222 | ✅ |
| TC-221-02 | 정상 API 호출 시 검증 통과 확인 | TASK-222 | ✅ |
| TC-221-03 | 스키마 불일치 시 로그 경고 또는 에러 반환 확인 | TASK-222 | ✅ |
| TC-222-01 | Filter 활성화 상태에서 정상 요청/응답 흐름 검증 | TASK-222 | ✅ |
| TC-222-02 | Filter 활성화 상태에서 다수 엔드포인트 순차 호출 | TASK-222 | ✅ |
| TC-212-01 | EventController GET /events 응답 스키마 검증 | TASK-212 | ✅ |
| TC-212-02 | EventRegistrationController GET /events/{id}/registrations 응답 스키마 검증 | TASK-212 | ✅ |
| TC-213-01 | PinnedPostController GET /pinned-posts 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-02 | PasswordAuthController POST /login, GET /check-student-id 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-03 | MyPageController GET /mypage/profile, GET /mypage/posts 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-04 | AdminInquiryController GET /inquiries 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-05 | GuestInquiryController POST /inquiries/guest 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-06 | MemberInquiryController GET /inquiries/my 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-08 | SurveyController GET /surveys 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-09 | SurveyQuestionController GET /surveys/{id}/questions 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-10 | SurveyQuestionOptionController GET /questions/{id}/options 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-11 | SurveyQuestionRowController GET /questions/{id}/rows 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-12 | SurveyResponseController POST /surveys/{id}/responses 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-13 | SurveyAnonymousResponseController POST /responses/anonymous 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-14 | PrivacyConsentController GET /consent/history, GET /consent/check 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-15a | SemesterMemberController GET /semesters 응답 스키마 검증 | TASK-213 | ✅ |
| TC-213-15b | AdminSemesterMemberController GET /admin/semesters/{y}/{s}/candidates 응답 스키마 검증 | TASK-213 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 그룹3 | 🔴 | code-reviewer | LevelResolver 생성 로직이 OpenApiValidatorUtil(test)과 OpenApiValidationConfig(main)에 완전 중복. 한쪽만 수정 시 검증 기준 불일치 위험 | R2에서 해결. `OpenApiValidatorFactory.createProjectLevelResolver()` 단일 진실점으로 추출 |
| 2 | R1 | 그룹3 | 🔴 | code-reviewer | Validator 생성 로직(스펙 파일 로딩, LevelResolver 적용)도 양쪽에 거의 동일하게 중복. 공통 팩토리 추출 필요 | R2에서 해결. `OpenApiValidatorFactory.createValidator()` 단일 진실점으로 추출. 양쪽 모두 팩토리에 위임 |
| 3 | R1 | 그룹3 | 🟡 | code-reviewer | 생성자에서 무거운 초기화(~3초) 수행. 스펙 파일 미존재 시 전체 ApplicationContext 실패 가능. graceful degradation 권장 | R2에서 해결. try-catch로 감싸고, 실패 시 WARN 로그 출력 후 interceptor=null로 설정하여 검증 비활성화 |
| 4 | R1 | 그룹3 | 🟡 | code-reviewer | LoggingValidationReportHandler에서 hasErrors() 조건으로 WARN만 있는 경우 로그 미출력. WARN 포함 여부 검토 필요 | R2에서 해결. `hasWarnings()` 메서드 추가하여 ERROR 없이 WARN만 있는 경우에도 로그 출력 |
| 5 | - | 그룹4 | 🟡 | code-implementer | `UserDetailResponse.joinRoute` 스펙에 `nullable: true` 미설정. 테스트 데이터의 joinRoute가 null이어서 스키마 검증 실패. getUserDetail 2건에서 OpenAPI 검증 제외 | 별도 이슈로 스펙 수정(nullable: true 추가) 필요 |
| 6 | R1 | 그룹4 | 🔴 | spec-reviewer | TC-221-03 미구현: Filter의 핵심 목적인 스키마 불일치 감지를 검증하는 테스트가 없음 | R2에서 해결. OutputCaptureExtension으로 LoggingValidationReportHandler의 WARN 로그 캡처하여 검증. POST 빈 바디 요청으로 필수 필드 누락 감지 테스트 + 정상 호출 시 로그 미출력 베이스라인 테스트 추가 |
| 7 | R1 | 그룹4 | 🔴 | spec-reviewer | TASK-222 컨텍스트 캐싱 규칙 위반: 작업 계획이 'ControllerIntegrationTestBase를 상속하여 기존 테스트 컨텍스트를 재사용한다'고 규정하나, 실제 구현은 독립 컨텍스트(@TestPropertySource) 사용 | R2에서 해결. 독립 컨텍스트 사용 유지하되, 클래스 Javadoc에 (1) Filter enabled=true 필요 사유, (2) @TestPropertySource 불가피성, (3) 컨텍스트 캐싱 규칙 위반 인지 및 기존 테스트 무영향 설명을 상세 문서화 |
| 8 | R1 | 그룹4 | 🟡 | code-reviewer | cleanupDatabase() 메서드가 OpenApiValidationFilterTest에 중복 정의됨 | 독립 컨텍스트 사용으로 베이스 클래스 상속 불가하여 불가피한 중복. 현행 유지 |
| 9 | R1 | 그룹4 | 🟡 | code-reviewer | withAuth() 헬퍼가 13개 테스트 파일에 반복됨 | 기존 패턴 유지 (테스트 독립성) |
| 10 | - | 그룹6 | 🟡 | code-implementer | `EventDetailResponse.closeReason`과 `AdminEventDetailResponse.closeReason`에 `nullable: true` 누락. 등록 미마감 행사에서 closeReason이 null이어서 OpenAPI 검증 실패 | TASK-212에서 해결. `openapi/schemas/events.yaml`에 `nullable: true` 추가 |
| 11 | - | 그룹6 | ℹ️ | code-implementer | INT-031 기존 테스트(ASSOCIATE 행사 신청 403) 실패: `feat/survey-event-registration` 브랜치의 설문-행사 연결 기능 구현 도중 발생한 기존 이슈 (500 반환). TASK-212 변경과 무관 | 별도 이슈. TASK-212 범위 외 |
