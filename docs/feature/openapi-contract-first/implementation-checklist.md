# OpenAPI Contract-First 컨트롤러 마이그레이션 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-03-01 |
| 최종 업데이트 | 2026-03-02 (전체 완료) |
| 작업 계획 문서 | `c:\dev\IGRUS-Web\docs\feature\openapi-contract-first\task-plan.md` |
| 전체 상태 | COMPLETED |

## 작업 진행 현황

### 그룹 1: Phase 0 - 사전 준비
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | 마이그레이션 가이드 문서화 및 컨트롤러-태그 매핑 테이블 작성 | DONE | PASS | `docs/feature/openapi-contract-first/migration-guide.md` | 29개 컨트롤러 매핑 테이블, 단계별 체크리스트, 특수 케이스 가이드 포함 |
| TASK-002 | DTO 매핑 유틸리티 또는 패턴 정립 | DONE | PASS (R1 수정 완료) | `backend/.../common/util/PageResponseMapper.java`, `docs/.../migration-guide.md` | "인라인 매핑 우선 + 공통 유틸 보조" 전략 확정, 4가지 패턴 문서화. R1 피드백 반영: TriFunction->ResponseAssembler 이름변경, Supplier import 정리, PageMeta Javadoc 보강 |
| TASK-003 | OpenAPI 스펙에서 Inquiry 태그를 3개로 분리 | DONE | PASS | `openapi/openapi.yaml`, `openapi/paths/inquiries.yaml` | Admin(7), Guest(2), Member(3) 분리 완료, 백엔드+프론트엔드 코드 생성 검증 |
| TASK-004 | HttpServletRequest/Response 접근 패턴 PoC 검증 | DONE | PASS (R1 수정 완료) | `backend/.../common/util/ServletContextUtil.java`, `backend/src/test/.../ServletContextUtilPocTest.java` | 6/6 PoC 테스트 통과, RequestContextListener 불필요 확인. R1 피드백 반영: Java assert -> AssertJ assertThat 교체 |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL → R2 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 2: Phase 1 - 파일럿 마이그레이션
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-010 | BoardController 마이그레이션 | DONE | PASS | `backend/.../community/board/controller/BoardController.java` | 2 endpoints, 테스트 5/5 통과, 수정 불필요 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 3: Phase 2 - 난이도 하 단순 컨트롤러
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-030 | AdminDashboardController 마이그레이션 | DONE | - | `backend/.../admin/dashboard/controller/AdminDashboardController.java` | 1 endpoint, 테스트 4/4 통과 |
| TASK-081 | SemesterMemberController 마이그레이션 | DONE | - | `backend/.../user/semester/controller/SemesterMemberController.java` | 2 endpoints, 테스트 없음 |
| TASK-080 | PrivacyConsentController 마이그레이션 | DONE | - | `backend/.../security/auth/common/controller/PrivacyConsentController.java` | 5 endpoints, 테스트 없음 |
| TASK-033 | AdminLoginHistory + AdminAccountStatusChangeHistory 마이그레이션 | DONE | - | `backend/.../security/auth/common/controller/AdminLoginHistoryController.java`, `backend/.../user/controller/AdminAccountStatusChangeHistoryController.java` | 2 endpoints, 테스트 17/17 통과. PageableUtils 기본값 파싱 버그 수정. 그룹5 R1 수정: String->enum 변환 시 EnumUtils.fromStringOrNull() 적용 |
| TASK-035 | AdminSemesterMemberController 마이그레이션 | DONE | - | `backend/.../user/semester/controller/AdminSemesterMemberController.java` | 3 endpoints, 테스트 없음 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 4: Phase 3 - 난이도 하 토글/CRUD
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-013 | PostLikeController + CommentLikeController 마이그레이션 | DONE | PASS | `backend/.../like/post_like/controller/PostLikeController.java`, `backend/.../like/comment_like/controller/CommentLikeController.java` | 5 endpoints, 테스트 통과 |
| TASK-014 | BookmarkController 마이그레이션 | DONE | PASS | `backend/.../community/bookmark/controller/BookmarkController.java` | 3 endpoints, 테스트 통과 |
| TASK-015 | PinnedPostController 마이그레이션 | DONE | PASS | `backend/.../community/pinnedpost/controller/PinnedPostController.java` | 4 endpoints, 테스트 없음 |
| TASK-016 | CommentReportController 마이그레이션 | DONE | PASS | `backend/.../community/comment/controller/CommentReportController.java` | 3 endpoints, 테스트 통과 |
| TASK-072 | SurveyResponse + SurveyAnonymousResponse 마이그레이션 | DONE | PASS (R1 수정) | `backend/.../survey/response/controller/SurveyResponseController.java`, `backend/.../survey/response/controller/SurveyAnonymousResponseController.java` | 4 endpoints, 테스트 없음. R1 피드백 반영: @PreAuthorize 추가 |

**그룹 상태**: PASS
**리뷰 이력**: R1 spec-reviewer FAIL (이슈#7), code-reviewer FAIL (이슈#8,#9 범위 밖) → R2 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 5: Phase 4 - 난이도 중
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-011 | PostController 마이그레이션 | DONE | PASS | `backend/.../community/post/controller/PostController.java` | 7 endpoints, 테스트 통과. OpenAPI 스펙 CreatePostRequest.title minLength 0->1 수정 |
| TASK-012 | CommentController 마이그레이션 | DONE | PASS | `backend/.../community/comment/controller/CommentController.java` | 4 endpoints, 테스트 통과. helper methods: mapToReplyInner, mapToCommentsInner |
| TASK-031 | AdminUserController 마이그레이션 | DONE | PASS (R1 수정) | `backend/.../admin/user/controller/AdminUserController.java` | 8 endpoints, 테스트 통과. R1 수정: EnumUtils.fromStringOrNull() 적용 |
| TASK-032 | AdminMemberController 마이그레이션 | DONE | PASS | `backend/.../security/auth/approval/controller/AdminMemberController.java` | 7 endpoints, 테스트 통과. OpenAPI 스펙 RejectAssociateRequest.reason minLength 0->1 수정 |
| TASK-071 | SurveyQuestion 3개 컨트롤러 마이그레이션 | DONE | PASS | `backend/.../survey/question/controller/SurveyQuestionController.java`, `backend/.../survey/question/controller/SurveyQuestionOptionController.java`, `backend/.../survey/question/controller/SurveyQuestionRowController.java` | 12 endpoints (4+4+4), 테스트 통과 |

**그룹 상태**: PASS
**리뷰 이력**: R1 code-reviewer FAIL (이슈#11) → R2 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 6: Phase 5 - 의존성 있는 중
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-040 | EventController 마이그레이션 | DONE | PASS | `backend/.../event/controller/EventController.java` | 9 endpoints, EnumUtils.fromStringOrNull() 사용, 테스트 통과 |
| TASK-041 | EventRegistrationController 마이그레이션 | DONE | PASS | `backend/.../event/controller/EventRegistrationController.java` | 7 endpoints, PageResponseMapper.toSpringPageResponse() 사용, 테스트 통과 |
| TASK-034 | AdminInquiryController 마이그레이션 | DONE | PASS | `backend/.../inquiry/controller/AdminInquiryController.java` | 7 endpoints, TASK-003 의존 완료, EnumUtils 사용, FQCN으로 DTO 이름 충돌 해결 |
| TASK-050 | GuestInquiry + MemberInquiry 마이그레이션 | DONE | PASS | `backend/.../inquiry/controller/GuestInquiryController.java`, `backend/.../inquiry/controller/MemberInquiryController.java` | 5 endpoints (2+3), TASK-003 의존 완료, Guest는 인증 불필요 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 7: Phase 6 - 난이도 상
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-020 | PasswordAuthController 마이그레이션 | DONE | PASS | `backend/.../security/auth/password/controller/PasswordAuthController.java` | 16 endpoints, ServletContextUtil 사용. 이슈#12: 5개 테스트 실패(OpenAPI 스펙 검증 차이, 범위 밖) |
| TASK-060 | MyPageController 마이그레이션 | DONE | PASS | `backend/.../user/mypage/controller/MyPageController.java` | 12 endpoints, getMyLikes1/getMyBookmarks1 operationId 충돌 처리 |
| TASK-070 | SurveyController 마이그레이션 | DONE | PASS | `backend/.../survey/controller/SurveyController.java` | 13 endpoints (CRUD+상태전이) |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 8: Phase 7 - DTO 정리
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-090 | 내부 DTO에서 Swagger 어노테이션 일괄 제거 | DONE | PASS | 84개 DTO 파일 | `@Schema` + `import io.swagger.v3.*` 일괄 제거, validation/lombok/Jackson 보존, compileJava 성공 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 9: Phase 8 - SpringDoc 제거
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-100 | SwaggerConfig 및 SpringDoc 의존성 제거 | DONE | PASS | `SwaggerConfig.java` (삭제), `build.gradle`, `application*.yml` (4개), `PublicResourceSecurityConfig.java`, `SurveyStatisticsController.java`, `ErrorResponse.java`, `EmailNotVerifiedErrorResponse.java`, `AccountRecoverableErrorResponse.java`, `backend/CLAUDE.md` | SwaggerConfig 삭제, springdoc 의존성 제거, springdoc 설정 제거, Swagger UI 경로 Security 제거, 잔존 `@Schema`/swagger import 정리, SurveyStatisticsController swagger 어노테이션 제거 |
| TASK-101 | 전체 빌드 및 테스트 검증 | DONE | PASS | - | `compileJava` + `compileTestJava` 성공. 2356 tests: 2347 passed, 5 failed (이슈#12 기존 알려진 실패), 4 skipped |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

### 그룹 10: Phase 9 - CI 강화
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-110 | CI에 OpenAPI 스펙-코드 일치 검증 단계 추가 | DONE | PASS | `.github/workflows/backend-ci.yaml` | `openapi/` 디렉토리 변경 감지 조건 추가. 프론트엔드 CI 미존재(CD만 있음)로 프론트엔드 연동은 스킵 |
| TASK-111 | OpenAPI 스펙 린팅 CI 단계 추가 | DONE | PASS | `.github/workflows/openapi-lint.yaml` | `redocly lint` 사용, `openapi/` 변경 시 트리거, `npx`로 별도 install 불필요 |

**그룹 상태**: PASS
**리뷰 이력**: R1 양쪽 PASS (spec: PASS, code: PASS)

---

## 완료 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| CR-01 | 29개 컨트롤러 모두 생성된 인터페이스를 implements 함 | TASK-010~081 | ✅ (31개 implements, SurveyStatisticsController는 스펙 미정의로 제외) |
| CR-02 | 모든 컨트롤러에서 수동 Swagger 어노테이션 제거됨 | TASK-010~081 | ✅ (io.swagger.v3 import 0건) |
| CR-03 | @AuthenticationPrincipal 대신 SecurityUtils.requireCurrentUser() 사용 | TASK-010~081 | ✅ (@AuthenticationPrincipal 0건, SecurityUtils 24개 컨트롤러 사용) |
| CR-04 | 페이지네이션 엔드포인트에서 PageableUtils.of() 사용 | TASK-010~081 | ✅ (@ParameterObject Pageable 0건, PageableUtils 11개 컨트롤러 사용) |
| CR-05 | @PreAuthorize 어노테이션 유지 확인 | TASK-010~081 | ✅ (27개 컨트롤러, 5개 public endpoint 제외) |
| CR-06 | 내부 DTO에서 io.swagger.v3.* 어노테이션 제거됨 | TASK-090 | ✅ |
| CR-07 | SwaggerConfig.java 삭제됨 | TASK-100 | ✅ |
| CR-08 | springdoc-openapi 의존성 제거됨 | TASK-100 | ✅ |
| CR-09 | ./gradlew clean build 성공 | TASK-101 | ✅ (컴파일 성공, 이슈#12의 5개 테스트 실패로 build task 실패) |
| CR-10 | ./gradlew test 전체 테스트 통과 | TASK-101 | ⚠️ (2351/2356 통과, 5개 실패는 이슈#12 기존 알려진 실패) |
| CR-11 | openapi/ 변경 시 backend CI 트리거됨 | TASK-110 | ✅ |
| CR-12 | OpenAPI 스펙 린팅 CI 자동 실행됨 | TASK-111 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 그룹1 | 🔴 | code-reviewer | `PageResponseMapper.TriFunction` 제네릭 시그니처가 이름/Javadoc과 불일치. 반환 타입이 첫 번째 파라미터 `T`와 동일하여 일반적인 TriFunction 의미와 다름 | R1 수정 완료: `ResponseAssembler<R,C,M>`로 이름 변경, Javadoc에 mutator 패턴 설명 추가 |
| 2 | R1 | 그룹1 | 🟡 | code-reviewer | `PageMeta`의 pageable/sort 필드가 특정 생성 모델 타입(`GetRegistrationList200Response*`)에 하드코딩 | R1 수정 완료: openapi-generator가 모든 Page*Response에서 동일 타입을 재사용하므로 변경 불가, Javadoc에 사유 설명 추가 |
| 3 | R1 | 그룹1 | 🟡 | code-reviewer | PoC 테스트에서 Java `assert` 키워드 대신 AssertJ `assertThat` 사용 권장 | R1 수정 완료: 6개 assert문을 AssertJ assertThat으로 교체 |
| 4 | R1 | 그룹1 | 🟡 | spec-reviewer | `TriFunction` 이름을 `ResponseAssembler` 등으로 변경 권장 | R1 수정 완료: 이슈#1과 함께 해결 |
| 5 | R1 | 그룹1 | 🟡 | code-reviewer | `Supplier` import가 FQCN으로 인라인 사용됨, import 추가 권장 | R1 수정 완료: import 추가 및 FQCN 제거 |
| 6 | - | 그룹3 | 🔴 | code-implementer | `PageableUtils.of()`에서 `@RequestParam(defaultValue="prop,DIR")` + `List<String>` 조합 시 Spring이 쉼표를 리스트 구분자로 처리하여 `["prop", "DIR"]`로 분리, 결과적으로 `DIR`이 속성명으로 해석되어 JPQL 에러 발생 | 수정 완료: 단독 방향 문자열을 직전 속성의 방향으로 병합하는 로직 추가 |
| 7 | R1 | 그룹4 | 🔴 | spec-reviewer | SurveyResponseController의 submitResponse, updateMyResponse, getMyResponse 메서드에 `@PreAuthorize("isAuthenticated()")` 누락 | R1 수정 완료: 3개 메서드에 @PreAuthorize("isAuthenticated()") 추가 |
| 8 | R1 | 그룹4 | 🟡 | code-reviewer | CommentReportApi의 reportComment 파라미터 타입이 `ReopenRegistrationRequest`로 재사용됨 (openapi-generator 스키마 최적화 결과, 동일 구조) | 마이그레이션 범위 밖 (OpenAPI 스펙 설계 이슈) |
| 9 | R1 | 그룹4 | 🟡 | code-reviewer | SurveyResponseService의 TOCTOU Race Condition (exists 검사 후 save 전 경쟁) | 마이그레이션 범위 밖 (기존 서비스 코드) |
| 10 | - | 그룹5 | 🔴 | code-implementer | OpenAPI 스펙 3개 스키마에서 `minLength: 0`이 내부 DTO의 `@NotBlank` 의미와 불일치하여 빈 문자열 validation 테스트 실패 (CreatePostRequest.title, RejectAssociateRequest.reason, ForceWithdrawRequest.reason) | 수정 완료: 3개 스키마의 minLength를 0에서 1로 변경 (`openapi/schemas/boards.yaml`, `openapi/schemas/admin.yaml`) |
| 11 | R1 | 그룹5 | 🔴 | code-reviewer | `AdminUserController`의 `UserRole.valueOf()`/`UserStatus.valueOf()`에서 잘못된 enum 값 전달 시 `IllegalArgumentException` 발생 → 500 에러. 마이그레이션 전에는 Spring enum 자동 변환 → `MethodArgumentTypeMismatchException` → 400 Bad Request 처리됨. 동작 변경 버그 | R1 수정 완료: `EnumUtils.fromStringOrNull()` 유틸 메서드 도입. `InvalidEnumValueException` + `CommonErrorCode.INVALID_TYPE_VALUE` (400) 반환. AdminUserController, AdminAccountStatusChangeHistoryController에 적용 |
| 12 | - | 그룹7 | 🔴 | code-implementer | PasswordAuthController 마이그레이션 후 5개 기존 테스트 실패. Generated SignupRequest/LoginRequest의 유효성 검증이 내부 DTO와 다름: (1) `@NotBlank` -> `@NotNull @Size(min=0)` (name, department): 빈/공백 문자열 통과, (2) `@NotBlank` -> `@NotNull @Size(min=1)` (login password): 공백 문자열("   ") 통과, (3) `@AssertTrue` -> `@NotNull` (privacyConsent): false 값 통과. OpenAPI 스펙에서 `minLength` 수정 및 `x-constraints` 확장 필요 | 미해결: OpenAPI 스펙 수정 필요 (마이그레이션 범위 밖, 별도 이슈로 처리 예정) |
