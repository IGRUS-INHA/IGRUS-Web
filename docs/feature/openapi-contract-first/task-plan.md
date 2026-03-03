# OpenAPI Contract-First 컨트롤러 마이그레이션 작업 계획

## 개요

- **기능 설명**: 백엔드 컨트롤러 전체(29개)를 OpenAPI Contract-First 방식으로 마이그레이션한다. 각 컨트롤러가 `openapi-generator`가 생성한 인터페이스를 `implements`하도록 변경하고, 수동 Swagger 어노테이션을 모두 제거한다. 마이그레이션 완료 후 SpringDoc 의존성을 제거하고, CI에 스펙 검증 단계를 추가한다.
- **관련 문서**
  - 마이그레이션 완료 레퍼런스: `StorageController`, `HealthController`
  - OpenAPI 스펙: [`openapi/openapi.yaml`](../../../openapi/openapi.yaml)
  - 백엔드 개발 규칙: [`backend/CLAUDE.md`](../../../backend/CLAUDE.md) (Contract-First 섹션)
  - ADR: [`docs/adr/v20260301-openapi_redocly_bundling.md`](../../adr/v20260301-openapi_redocly_bundling.md)
- **작성일**: 2026-03-01
- **기술 스택**: Backend -- Java 21 + Spring Boot 3.5.9 + openapi-generator 7.12.0

---

## 마이그레이션 패턴 요약

각 컨트롤러 마이그레이션 시 공통적으로 수행하는 변환 패턴이다. 아래 패턴은 마이그레이션 완료된 `StorageController`에서 검증된 것이다.

### 변경 항목 체크리스트

| 항목 | Before (수동 방식) | After (Contract-First) |
|------|-------------------|----------------------|
| 클래스 선언 | `public class XController` | `public class XController implements XApi` |
| 클래스 어노테이션 | `@Tag`, `@SecurityRequirement`, `@RequestMapping` | 모두 제거 |
| 메서드 어노테이션 | `@Operation`, `@ApiResponse`, `@Parameter`, `@GetMapping` 등 | `@Override`만 유지 |
| 인증 파라미터 | `@AuthenticationPrincipal AuthenticatedUser user` | `SecurityUtils.requireCurrentUser()` |
| 페이지네이션 | `@ParameterObject Pageable pageable` | `PageableUtils.of(page, size, sort)` (개별 파라미터) |
| 권한 검사 | `@PreAuthorize` | 그대로 유지 (구현체에 명시) |
| 요청/응답 DTO | 내부 DTO 직접 사용 | 생성된 모델 DTO <-> 내부 DTO 매핑 |
| import | `io.swagger.v3.*`, `org.springdoc.*` | `igrus.web.generated.api.*`, `igrus.web.generated.model.*` |

### 특수 케이스

- **HttpServletRequest/Response 사용**: `PasswordAuthController`의 쿠키 처리에서 `HttpServletRequest`/`HttpServletResponse`가 필요하다. 생성된 인터페이스 시그니처에는 Servlet API 파라미터가 포함되지 않으므로, 메서드 바디에서 `RequestContextHolder.currentRequestAttributes()`를 통해 획득한다. 이 패턴은 TASK-004에서 PoC 검증한다.
- **다중 태그 컨트롤러**: `AdminMemberController`는 현재 `Admin Associate Approval` 태그만 사용하지만, `Admin Semester Member` 태그에 해당하는 `AdminSemesterMemberController`는 별도 컨트롤러이다. `useTags: 'true'` 설정으로 태그별로 인터페이스가 생성되므로 1:1 매핑이 가능하다.
- **DTO 내부 Swagger 어노테이션**: 일부 내부 DTO(`PasswordSignupRequest`, `MyProfileResponse` 등)에 `@Schema` 어노테이션이 포함되어 있다. Contract-First에서는 생성된 모델 DTO를 사용하므로, 내부 DTO의 Swagger 어노테이션은 마이그레이션 후 제거 가능하다.

---

## 작업 목록

### 0. 사전 준비 (공통 인프라)

#### TASK-001: 마이그레이션 가이드 문서화 및 컨트롤러-태그 매핑 테이블 작성

- **작업명**: 마이그레이션 체크리스트 및 컨트롤러-OpenAPI 태그 매핑 테이블 작성
- **설명**: 29개 컨트롤러와 OpenAPI 태그 간의 정확한 매핑 테이블을 작성하고, 마이그레이션 시 사용할 단계별 체크리스트를 문서화한다. 이를 통해 모든 마이그레이션 작업의 일관성을 보장한다.
- **선행 작업**: 없음
- **구현 범위**: docs
- **예상 난이도**: 하

#### TASK-002: DTO 매핑 유틸리티 또는 패턴 정립

- **작업명**: 내부 DTO <-> 생성 모델 DTO 간 매핑 전략 확립
- **설명**: 컨트롤러에서 내부 DTO와 `igrus.web.generated.model` 간의 변환을 어떻게 처리할지 패턴을 정립한다. `StorageController`의 인라인 매핑 패턴을 기본으로 하되, 복잡한 DTO(페이지네이션 응답 등)에 대한 매핑 헬퍼 메서드 또는 별도 Mapper 클래스 필요 여부를 결정한다.
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-003: OpenAPI 스펙에서 Inquiry 태그를 3개로 분리

- **작업명**: `Inquiry` 태그를 `Guest Inquiry`, `Member Inquiry`, `Admin Inquiry`로 분리
- **설명**: 현재 OpenAPI 스펙(`openapi/paths/inquiries.yaml`)에서 13개 오퍼레이션이 모두 `Inquiry` 단일 태그를 사용한다. `useTags: 'true'` + `skipDefaultInterface: 'true'` 설정으로 인해 단일 `InquiryApi` 인터페이스가 생성되면, 이를 `implements`하는 클래스는 **모든 메서드를 구현해야** 한다. 3개 컨트롤러(`GuestInquiryController`, `MemberInquiryController`, `AdminInquiryController`)가 하나의 인터페이스를 공유할 수 없으므로, 다음과 같이 태그를 분리한다:
  - **`Guest Inquiry`**: 비회원 문의 작성(`createGuestInquiry`), 비회원 문의 조회(`lookupGuestInquiry`) -- 2개 오퍼레이션
  - **`Member Inquiry`**: 회원 문의 작성(`createMemberInquiry`), 내 문의 목록 조회(`getMyInquiries`), 내 문의 상세 조회(`getMyInquiry`) -- 3개 오퍼레이션
  - **`Admin Inquiry`**: 전체 문의 목록 조회(`getAllInquiries`), 문의 상세 조회(`getInquiryDetail`), 문의 삭제(`deleteInquiry`), 문의 상태 변경(`updateInquiryStatus`), 답변 작성(`createReply`), 답변 수정(`updateReply`), 메모 작성(`createMemo`) -- 7개 오퍼레이션 (+ 1개 추가 가능)
  - 태그 분리 후 `./gradlew openApiGenerate`로 `GuestInquiryApi`, `MemberInquiryApi`, `AdminInquiryApi` 3개 인터페이스가 정상 생성되는지 확인한다.
  - 프론트엔드 Orval 코드 생성(`pnpm api:generate`)에도 영향이 있으므로, 프론트엔드 측 타입 변경도 확인한다.
- **선행 작업**: 없음
- **구현 범위**: openapi + backend (생성 확인) + frontend (Orval 재생성 확인)
- **예상 난이도**: 중

#### TASK-004: HttpServletRequest/Response 접근 패턴 PoC 검증

- **작업명**: `RequestContextHolder`를 활용한 Servlet API 접근 패턴 PoC(Proof of Concept) 검증
- **설명**: `PasswordAuthController`의 4개 메서드(`login`, `logout`, `refreshToken`, `recoverAccount`)가 쿠키 설정을 위해 `HttpServletRequest`/`HttpServletResponse`를 직접 사용한다. `openapi-generator`가 생성하는 인터페이스 시그니처에는 Servlet API 파라미터가 포함되지 않으며, `skipDefaultInterface: 'true'` 설정으로 `default` 메서드 오버라이딩도 불가능하다. 따라서 컨트롤러 메서드 바디에서 Servlet 객체를 획득하는 대체 패턴을 PoC로 검증한다.
  - **검증 대상 패턴**: `RequestContextHolder.getRequestAttributes()`를 통해 `HttpServletRequest`/`HttpServletResponse`를 획득하는 방식:
    ```java
    ServletRequestAttributes attrs = (ServletRequestAttributes)
        RequestContextHolder.currentRequestAttributes();
    HttpServletRequest httpRequest = attrs.getRequest();
    HttpServletResponse httpResponse = attrs.getResponse();
    ```
  - **PoC 검증 항목**:
    1. `attrs.getResponse()`가 `null`을 반환하지 않는지 확인 (Spring MVC의 `RequestContextListener` 또는 `DispatcherServlet` 설정에 따라 `null`일 수 있음)
    2. 획득한 `HttpServletResponse`에 `Set-Cookie` 헤더를 추가했을 때 실제 클라이언트에 전달되는지 확인
    3. `extractIpAddress`에서 사용하는 `HttpServletRequest.getHeader("X-Forwarded-For")`, `getRemoteAddr()` 등이 정상 동작하는지 확인
  - **대안**: `attrs.getResponse()`가 `null`인 경우, `RequestContextListener`를 Bean으로 등록하거나, Servlet 접근 로직을 별도 유틸 클래스(`ServletContextUtil`)로 추출하여 Spring Bean으로 주입하는 방식을 검토한다.
  - 검증 결과를 TASK-020에 반영하여 확정된 전략으로 문서화한다.
- **선행 작업**: 없음
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 1. 게시판/커뮤니티 도메인 (7개 컨트롤러, 25개 엔드포인트)

이 그룹은 가장 많은 컨트롤러를 포함하지만, 개별 컨트롤러의 복잡도는 낮~중간 수준이다. 단순 CRUD와 토글 패턴이 반복되며, `@AuthenticationPrincipal` -> `SecurityUtils` 변환과 `Pageable` -> `PageableUtils` 변환이 주요 작업이다.

#### TASK-010: BoardController 마이그레이션

- **작업명**: BoardController를 `BoardApi` 인터페이스 구현으로 전환
- **설명**: `BoardController`(110줄, 2개 엔드포인트)를 `implements BoardApi`로 변경한다. 게시판 목록 조회(GET), 게시판 상세 조회(GET) 2개 엔드포인트를 전환한다. 단순 조회 API로 페이지네이션 없이 비교적 간단하다.
- **대상 파일**: `community/board/controller/BoardController.java`
- **대상 테스트**: `community/board/controller/BoardControllerTest.java`
- **엔드포인트 수**: 2
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-011: PostController 마이그레이션

- **작업명**: PostController를 `PostApi` 인터페이스 구현으로 전환
- **설명**: `PostController`(358줄, 7개 엔드포인트)를 `implements PostApi`로 변경한다. 게시글 CRUD(4개) + 조회 통계/기록(2개) + 목록 조회(1개) 엔드포인트를 전환한다. 페이지네이션 사용 메서드 2개(`getPostList`, `getPostViewHistory`)에서 `PageableUtils.of()` 변환이 필요하다.
- **대상 파일**: `community/post/controller/PostController.java`
- **대상 테스트**: `community/post/controller/PostControllerTest.java`
- **엔드포인트 수**: 7
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-012: CommentController 마이그레이션

- **작업명**: CommentController를 `CommentApi` 인터페이스 구현으로 전환
- **설명**: `CommentController`(215줄, 4개 엔드포인트)를 `implements CommentApi`로 변경한다. 댓글 목록 조회, 작성, 수정, 삭제 엔드포인트를 전환한다.
- **대상 파일**: `community/comment/controller/CommentController.java`
- **대상 테스트**: `community/comment/controller/CommentControllerTest.java`
- **엔드포인트 수**: 4
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-013: PostLikeController + CommentLikeController 마이그레이션

- **작업명**: PostLikeController, CommentLikeController를 각각 `PostLikeApi`, `CommentLikeApi` 인터페이스 구현으로 전환
- **설명**: `PostLikeController`(157줄, 3개 엔드포인트)와 `CommentLikeController`(104줄, 2개 엔드포인트)를 전환한다. 두 컨트롤러 모두 동일한 토글 + 상태 조회 패턴이므로 한 작업으로 묶는다. `PostLikeController`의 `getMyLikes` 메서드에 페이지네이션 변환이 필요하다.
- **대상 파일**: `community/like/post_like/controller/PostLikeController.java`, `community/like/comment_like/controller/CommentLikeController.java`
- **대상 테스트**: `community/like/post_like/controller/PostLikeControllerTest.java`, `community/like/comment_like/controller/CommentLikeControllerTest.java`
- **엔드포인트 수**: 5 (3+2)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-014: BookmarkController 마이그레이션

- **작업명**: BookmarkController를 `BookmarkApi` 인터페이스 구현으로 전환
- **설명**: `BookmarkController`(157줄, 3개 엔드포인트)를 `implements BookmarkApi`로 변경한다. 토글, 상태 조회, 내 북마크 목록 조회를 전환한다. `getMyBookmarks`에 페이지네이션 변환이 필요하다.
- **대상 파일**: `community/bookmark/controller/BookmarkController.java`
- **대상 테스트**: `community/bookmark/controller/BookmarkControllerTest.java`
- **엔드포인트 수**: 3
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-015: PinnedPostController 마이그레이션

- **작업명**: PinnedPostController를 `PinnedPostApi` 인터페이스 구현으로 전환
- **설명**: `PinnedPostController`(158줄, 4개 엔드포인트)를 `implements PinnedPostApi`로 변경한다. 고정 게시글 목록 조회, 고정 생성, 고정 해제, 순서 변경 엔드포인트를 전환한다.
- **대상 파일**: `community/pinnedpost/controller/PinnedPostController.java`
- **대상 테스트**: 없음 (테스트 미존재, 필요 시 별도 생성)
- **엔드포인트 수**: 4
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-016: CommentReportController 마이그레이션

- **작업명**: CommentReportController를 `CommentReportApi` 인터페이스 구현으로 전환
- **설명**: `CommentReportController`(161줄, 3개 엔드포인트)를 `implements CommentReportApi`로 변경한다. 댓글 신고 접수, 신고 목록 조회, 신고 상태 변경 엔드포인트를 전환한다.
- **대상 파일**: `community/comment/controller/CommentReportController.java`
- **대상 테스트**: `community/comment/controller/CommentReportControllerTest.java`
- **엔드포인트 수**: 3
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 2. 인증 도메인 (1개 컨트롤러, 16개 엔드포인트)

이 그룹은 컨트롤러 수는 1개이지만, 엔드포인트가 16개로 가장 많고, `HttpServletRequest`/`HttpServletResponse` 직접 사용, 쿠키 처리, 토큰 로테이션 등 특수 로직이 많아 복잡도가 높다.

#### TASK-020: PasswordAuthController 마이그레이션

- **작업명**: PasswordAuthController를 `PasswordAuthenticationApi` 인터페이스 구현으로 전환
- **설명**: `PasswordAuthController`(530줄, 16개 엔드포인트)를 `implements PasswordAuthenticationApi`로 변경한다. 이 컨트롤러는 프로젝트에서 가장 많은 엔드포인트를 보유하고 있으며 다음과 같은 특수 처리가 필요하다:
  - **쿠키 처리 (TASK-004 PoC 결과 적용)**: `login`, `logout`, `refreshToken`, `recoverAccount` 4개 메서드에서 `HttpServletRequest`/`HttpServletResponse`를 사용한다. `skipDefaultInterface: 'true'` 설정으로 `default` 메서드 오버라이딩이 불가능하므로, TASK-004에서 검증한 `RequestContextHolder` 패턴을 적용한다. 구체적으로 메서드 바디 내에서 다음과 같이 Servlet 객체를 획득한다:
    ```java
    ServletRequestAttributes attrs = (ServletRequestAttributes)
        RequestContextHolder.currentRequestAttributes();
    HttpServletRequest httpRequest = attrs.getRequest();
    HttpServletResponse httpResponse = attrs.getResponse();
    ```
    - `httpRequest` 사용처: `login`에서 IP 추출(`extractIpAddress`), User-Agent 헤더 추출, `logout`/`refreshToken`에서 쿠키 추출(`cookieUtil.getRefreshTokenFromCookies`)
    - `httpResponse` 사용처: `login`/`refreshToken`/`recoverAccount`에서 `Set-Cookie` 헤더 추가, `logout`에서 쿠키 삭제
  - **IP 추출**: `extractIpAddress` private 메서드는 컨트롤러 내부에 유지한다. `HttpServletRequest`를 `RequestContextHolder`에서 획득하여 전달한다.
  - **Bean Validation**: `@Pattern` 어노테이션이 `@RequestParam`에 직접 적용된 경우(`checkReRegistrationEligibility`, `checkRecoveryEligibility`), 생성된 인터페이스에서 `useBeanValidation: 'true'` 설정으로 이미 검증이 포함되므로, 중복 검증이 없는지 확인한다.
  - **`@Validated` 클래스 어노테이션**: 생성된 인터페이스에서 Bean Validation이 처리되므로 제거한다.
- **대상 파일**: `security/auth/password/controller/PasswordAuthController.java`
- **대상 테스트**: 없음 (컨트롤러 통합 테스트 미존재)
- **엔드포인트 수**: 16
- **선행 작업**: TASK-002, TASK-004 (Servlet API 접근 패턴 PoC 완료 필수)
- **구현 범위**: backend
- **예상 난이도**: 상

---

### 3. Admin 도메인 (7개 컨트롤러, 28개 엔드포인트)

Admin 컨트롤러들은 대부분 `@PreAuthorize("hasRole('ADMIN')")` 또는 `hasAnyRole('OPERATOR', 'ADMIN')` 클래스 레벨 권한 설정을 가지며, 페이지네이션이 빈번하다.

#### TASK-030: AdminDashboardController 마이그레이션

- **작업명**: AdminDashboardController를 `AdminDashboardApi` 인터페이스 구현으로 전환
- **설명**: `AdminDashboardController`(47줄, 1개 엔드포인트)를 `implements AdminDashboardApi`로 변경한다. 대시보드 통계 조회 1개 엔드포인트만 있어 가장 간단한 마이그레이션 대상이다.
- **대상 파일**: `admin/dashboard/controller/AdminDashboardController.java`
- **대상 테스트**: `admin/dashboard/controller/AdminDashboardControllerTest.java`
- **엔드포인트 수**: 1
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-031: AdminUserController 마이그레이션

- **작업명**: AdminUserController를 `AdminUserManagementApi` 인터페이스 구현으로 전환
- **설명**: `AdminUserController`(246줄, 8개 엔드포인트)를 `implements AdminUserManagementApi`로 변경한다. 회원 목록/상세 조회, 역할 변경, 상태 변경, 정보 수정, 강제 탈퇴, 역할 변경 이력 등을 전환한다. 페이지네이션 사용 메서드가 다수 있다.
- **대상 파일**: `admin/user/controller/AdminUserController.java`
- **대상 테스트**: `admin/user/controller/AdminUserControllerTest.java`
- **엔드포인트 수**: 8
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-032: AdminMemberController 마이그레이션

- **작업명**: AdminMemberController를 `AdminAssociateApprovalApi` 인터페이스 구현으로 전환
- **설명**: `AdminMemberController`(317줄, 7개 엔드포인트)를 `implements AdminAssociateApprovalApi`로 변경한다. 준회원 승인/거절/일괄 처리 등의 엔드포인트를 전환한다. 클래스 레벨 `@PreAuthorize("hasRole('ADMIN')")` 유지에 주의한다. 페이지네이션 사용 메서드 3개(`getPendingAssociates`, `getRejectedAssociates`, `getDemotedAssociates`)에 `PageableUtils.of()` 변환이 필요하다.
- **대상 파일**: `security/auth/approval/controller/AdminMemberController.java`
- **대상 테스트**: `security/auth/approval/controller/AdminMemberControllerTest.java`
- **엔드포인트 수**: 7
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-033: AdminLoginHistoryController + AdminAccountStatusChangeHistoryController 마이그레이션

- **작업명**: AdminLoginHistoryController, AdminAccountStatusChangeHistoryController를 각각의 Api 인터페이스 구현으로 전환
- **설명**: `AdminLoginHistoryController`(87줄, 1개 엔드포인트)와 `AdminAccountStatusChangeHistoryController`(68줄, 1개 엔드포인트)를 전환한다. 두 컨트롤러 모두 페이지네이션 조회 1개 엔드포인트만 가진 단순한 구조이므로 한 작업으로 묶는다.
- **대상 파일**: `security/auth/common/controller/AdminLoginHistoryController.java`, `user/controller/AdminAccountStatusChangeHistoryController.java`
- **대상 테스트**: `security/auth/common/controller/AdminLoginHistoryControllerTest.java`, `user/controller/AdminAccountStatusChangeHistoryControllerTest.java`
- **엔드포인트 수**: 2 (1+1)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-034: AdminInquiryController 마이그레이션

- **작업명**: AdminInquiryController를 `AdminInquiryApi` 인터페이스 구현으로 전환
- **설명**: `AdminInquiryController`(205줄, 7개 엔드포인트)를 `implements AdminInquiryApi`로 변경한다. TASK-003에서 `Inquiry` 태그를 `Admin Inquiry`로 분리하여 생성된 `AdminInquiryApi` 인터페이스를 사용한다. 문의 목록 조회, 상세 조회, 답변 작성/수정, 상태 변경, 메모 작성, 문의 삭제 등의 관리자용 문의 관리 엔드포인트를 전환한다.
- **대상 파일**: `inquiry/controller/AdminInquiryController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 7
- **선행 작업**: TASK-002, TASK-003 (Inquiry 태그 분리 완료 필수)
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-035: AdminSemesterMemberController 마이그레이션

- **작업명**: AdminSemesterMemberController를 `AdminSemesterMemberApi` 인터페이스 구현으로 전환
- **설명**: `AdminSemesterMemberController`(94줄, 3개 엔드포인트)를 `implements AdminSemesterMemberApi`로 변경한다. 학기별 회원 등록, 제거, 후보 조회 엔드포인트를 전환한다.
- **대상 파일**: `user/semester/controller/AdminSemesterMemberController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 3
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 4. 행사 도메인 (2개 컨트롤러, 16개 엔드포인트)

행사 도메인은 컨트롤러 수는 적지만, 행사 상태 관리(생성/수정/취소/마감/재활성화 등)로 엔드포인트가 많고 비즈니스 로직이 복잡하다.

#### TASK-040: EventController 마이그레이션

- **작업명**: EventController를 `EventApi` 인터페이스 구현으로 전환
- **설명**: `EventController`(216줄, 9개 엔드포인트)를 `implements EventApi`로 변경한다. 행사 CRUD + 상태 변경(취소, 마감, 재활성화, 접수 재개) 등 복합 상태 전이 엔드포인트를 전환한다.
- **대상 파일**: `event/controller/EventController.java`
- **대상 테스트**: `event/integration/EventControllerIntegrationTest.java`
- **엔드포인트 수**: 9
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-041: EventRegistrationController 마이그레이션

- **작업명**: EventRegistrationController를 `EventRegistrationApi` 인터페이스 구현으로 전환
- **설명**: `EventRegistrationController`(173줄, 7개 엔드포인트)를 `implements EventRegistrationApi`로 변경한다. 행사 신청, 신청 취소, 신청자 목록, 승인/거절/복원 등의 엔드포인트를 전환한다. 페이지네이션 사용 메서드가 있다.
- **대상 파일**: `event/controller/EventRegistrationController.java`
- **대상 테스트**: `event/integration/EventRegistrationControllerIntegrationTest.java`
- **엔드포인트 수**: 7
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 5. 문의 도메인 (2개 컨트롤러, 5개 엔드포인트)

문의 도메인의 비관리자 컨트롤러들이다. TASK-003에서 `Inquiry` 태그를 `Guest Inquiry`, `Member Inquiry`로 분리하여 각각 독립된 인터페이스가 생성된다.

#### TASK-050: GuestInquiryController + MemberInquiryController 마이그레이션

- **작업명**: GuestInquiryController를 `GuestInquiryApi`, MemberInquiryController를 `MemberInquiryApi` 인터페이스 구현으로 전환
- **설명**: TASK-003에서 Inquiry 태그가 분리되어 `GuestInquiryApi`, `MemberInquiryApi` 인터페이스가 각각 생성된다. `GuestInquiryController`(64줄, 2개 엔드포인트)를 `implements GuestInquiryApi`로, `MemberInquiryController`(105줄, 3개 엔드포인트)를 `implements MemberInquiryApi`로 변경한다. 태그가 분리되었으므로 각 컨트롤러는 자신의 인터페이스에 정의된 메서드만 구현하면 된다.
- **대상 파일**: `inquiry/controller/GuestInquiryController.java`, `inquiry/controller/MemberInquiryController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 5 (2+3)
- **선행 작업**: TASK-002, TASK-003 (Inquiry 태그 분리 완료 필수)
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 6. 마이페이지 도메인 (1개 컨트롤러, 12개 엔드포인트)

#### TASK-060: MyPageController 마이그레이션

- **작업명**: MyPageController를 `MyPageApi` 인터페이스 구현으로 전환
- **설명**: `MyPageController`(324줄, 12개 엔드포인트)를 `implements MyPageApi`로 변경한다. 프로필 조회, 비밀번호 변경, 이메일 변경/인증, 전화번호 변경, 학번 수정, 내 게시글/댓글/좋아요/북마크 목록, 내 행사 신청 내역, 탈퇴 등 다양한 기능을 포함한다. 페이지네이션 사용 메서드가 5개 이상으로 `PageableUtils.of()` 변환이 빈번하다.
- **대상 파일**: `user/mypage/controller/MyPageController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 12
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 상
- **참고**: OpenAPI 스펙의 `My Page` 태그에 일부 엔드포인트가 다른 도메인 서비스(`GetMyLikedPostsService`, `GetMyBookmarksService`, `EventRegistrationService`)를 직접 참조하므로, 이 의존성은 그대로 유지한다.

---

### 7. 설문 도메인 (6개 컨트롤러, 29개 엔드포인트)

설문 도메인은 컨트롤러 수와 엔드포인트 수가 모두 많다. 설문 CRUD, 질문/선택지/행 관리, 응답 제출/수정 등 세분화된 API 구조를 가진다. 태그가 세분화되어 있어(`Survey`, `Survey Question`, `Survey Question Option`, `Survey Question Row`, `Survey Response`, `Survey Anonymous Response`) 인터페이스 매핑은 명확하다.

#### TASK-070: SurveyController 마이그레이션

- **작업명**: SurveyController를 `SurveyApi` 인터페이스 구현으로 전환
- **설명**: `SurveyController`(281줄, 13개 엔드포인트)를 `implements SurveyApi`로 변경한다. 설문 CRUD + 상태 관리(발행, 비발행, 공개, 비공개, 휴지통, 복원, 영구 삭제) + 목록 조회(전체, 휴지통) 등 다수의 엔드포인트를 전환한다. 엔드포인트 수가 많아 주의가 필요하다.
- **대상 파일**: `survey/controller/SurveyController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 13
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 상

#### TASK-071: SurveyQuestionController + SurveyQuestionOptionController + SurveyQuestionRowController 마이그레이션

- **작업명**: 설문 질문 관련 3개 컨트롤러를 각각의 Api 인터페이스 구현으로 전환
- **설명**: `SurveyQuestionController`(116줄, 4개 엔드포인트), `SurveyQuestionOptionController`(115줄, 4개 엔드포인트), `SurveyQuestionRowController`(115줄, 4개 엔드포인트)를 전환한다. 세 컨트롤러 모두 동일한 CRUD 패턴(목록 조회, 생성, 수정, 삭제)을 가지며, 구조가 거의 동일하므로 한 작업으로 묶는다.
- **대상 파일**: `survey/question/controller/SurveyQuestionController.java`, `survey/question/controller/SurveyQuestionOptionController.java`, `survey/question/controller/SurveyQuestionRowController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 12 (4+4+4)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 중

#### TASK-072: SurveyResponseController + SurveyAnonymousResponseController 마이그레이션

- **작업명**: SurveyResponseController, SurveyAnonymousResponseController를 각각의 Api 인터페이스 구현으로 전환
- **설명**: `SurveyResponseController`(97줄, 3개 엔드포인트)와 `SurveyAnonymousResponseController`(52줄, 1개 엔드포인트)를 전환한다. 응답 제출/조회/수정과 익명 응답 제출 엔드포인트를 전환한다.
- **대상 파일**: `survey/response/controller/SurveyResponseController.java`, `survey/response/controller/SurveyAnonymousResponseController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 4 (3+1)
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 8. 기타 도메인 (2개 컨트롤러, 7개 엔드포인트)

#### TASK-080: PrivacyConsentController 마이그레이션

- **작업명**: PrivacyConsentController를 `PrivacyConsentApi` 인터페이스 구현으로 전환
- **설명**: `PrivacyConsentController`(110줄, 5개 엔드포인트)를 `implements PrivacyConsentApi`로 변경한다. 개인정보 동의 최신 버전 조회, 동의 여부 확인, 재동의 필요 여부 확인, 동의 기록 조회, 동의 처리 등의 엔드포인트를 전환한다.
- **대상 파일**: `security/auth/common/controller/PrivacyConsentController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 5
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

#### TASK-081: SemesterMemberController 마이그레이션

- **작업명**: SemesterMemberController를 `SemesterMemberApi` 인터페이스 구현으로 전환
- **설명**: `SemesterMemberController`(67줄, 2개 엔드포인트)를 `implements SemesterMemberApi`로 변경한다. 학기별 회원 목록 조회, 학기 목록 조회 엔드포인트를 전환한다.
- **대상 파일**: `user/semester/controller/SemesterMemberController.java`
- **대상 테스트**: 없음 (컨트롤러 테스트 미존재)
- **엔드포인트 수**: 2
- **선행 작업**: TASK-002
- **구현 범위**: backend
- **예상 난이도**: 하

---

### 9. 내부 DTO Swagger 어노테이션 정리

#### TASK-090: 내부 DTO에서 Swagger 어노테이션 일괄 제거

- **작업명**: Contract-First 전환 완료된 내부 DTO 클래스에서 `@Schema` 등 Swagger 어노테이션 제거
- **설명**: 마이그레이션 완료 후, 내부 DTO 클래스에 남아있는 `io.swagger.v3.oas.annotations.media.Schema` 등의 어노테이션을 일괄 제거한다. 현재 약 58개 DTO 파일에 Swagger 어노테이션이 잔존한다. API 문서화는 OpenAPI 스펙에서 전적으로 관리하므로 내부 DTO의 Swagger 어노테이션은 불필요하다.
- **대상 파일 수**: 약 58개 (DTO, Request, Response 클래스)
- **선행 작업**: TASK-010 ~ TASK-081 (모든 컨트롤러 마이그레이션 완료)
- **구현 범위**: backend
- **예상 난이도**: 하 (기계적 작업)

---

### 10. SpringDoc 의존성 제거 및 정리

#### TASK-100: SwaggerConfig 및 SpringDoc 의존성 제거

- **작업명**: SwaggerConfig 클래스 삭제 및 SpringDoc 의존성을 build.gradle에서 제거
- **설명**: 모든 컨트롤러 마이그레이션이 완료되고 내부 DTO 정리(TASK-090)가 끝난 후, `SwaggerConfig.java` 클래스를 삭제하고 `build.gradle`에서 `springdoc-openapi-starter-webmvc-ui` 의존성을 제거한다.
  - 삭제 대상: `igrus.web.common.config.SwaggerConfig`
  - 의존성 제거: `implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14'`
  - `SwaggerConfig.SECURITY_SCHEME_NAME`을 참조하는 코드가 남아있지 않은지 확인한다.
  - Swagger UI 접근 경로(`/swagger-ui.html`)가 불필요해지므로 SecurityConfig에서 관련 경로 허용 설정도 제거한다.
- **대상 파일**: `common/config/SwaggerConfig.java`, `build.gradle`, `SecurityConfig.java`
- **선행 작업**: TASK-090
- **구현 범위**: backend
- **예상 난이도**: 중 (참조 체인 확인 필요)

#### TASK-101: 전체 빌드 및 테스트 검증

- **작업명**: SpringDoc 제거 후 전체 빌드 성공 및 테스트 통과 확인
- **설명**: SpringDoc 의존성 제거 후 `./gradlew clean build`가 성공하는지 확인하고, 모든 테스트(`./gradlew test`)가 통과하는지 검증한다. 컴파일 에러가 발생하면 잔존 Swagger import를 추가로 정리한다.
- **선행 작업**: TASK-100
- **구현 범위**: backend
- **예상 난이도**: 중

---

### 11. CI 파이프라인 강화

#### TASK-110: CI에 OpenAPI 스펙-코드 일치 검증 단계 추가

- **작업명**: backend-ci.yaml에 OpenAPI 스펙 변경 시 코드 빌드 검증 단계 추가
- **설명**: 현재 CI(`backend-ci.yaml`)는 `./gradlew build -x test`를 실행하며, `compileJava`가 `openApiGenerate`에 의존하므로 스펙 불일치 시 이미 컴파일 에러가 발생한다. 하지만 다음 추가 검증을 고려한다:
  - `openapi/` 폴더 변경 시에도 backend CI가 트리거되도록 변경 감지 조건을 확장한다 (현재는 `backend/` 변경만 감지).
  - OpenAPI 스펙 린팅 단계를 추가한다 (예: `spectral` 또는 `redocly lint`).
  - `openapi/` 변경 시 프론트엔드 CI도 트리거되도록 프론트엔드 CI 연동을 검토한다.
- **대상 파일**: `.github/workflows/backend-ci.yaml`
- **선행 작업**: TASK-101 (전체 빌드 성공 확인 후)
- **구현 범위**: devops
- **예상 난이도**: 중

#### TASK-111: OpenAPI 스펙 린팅 CI 단계 추가

- **작업명**: OpenAPI 스펙 문법/규칙 린팅 CI 작업 추가
- **설명**: `redocly lint` 또는 `spectral` 도구를 사용하여 OpenAPI 스펙 파일의 문법 오류, 네이밍 규칙, 보안 정의 누락 등을 자동 검사하는 CI 단계를 추가한다. `openapi/` 폴더 변경 시 트리거되도록 한다.
- **대상 파일**: `.github/workflows/` (신규 워크플로우 또는 기존 CI 확장)
- **선행 작업**: TASK-110
- **구현 범위**: devops
- **예상 난이도**: 중

---

## 작업 순서 및 의존성

### 의존성 다이어그램

```
TASK-001 (매핑 문서) ─┐
                      ├──> TASK-010~016 (커뮤니티)  ──┐
TASK-002 (DTO 패턴) ──┤                               │
                      ├──> TASK-030~035 (Admin*)   ──┤
                      ├──> TASK-040~041 (행사)      ──┤
                      ├──> TASK-060 (마이페이지)    ──┤
                      ├──> TASK-070~072 (설문)      ──┤
                      └──> TASK-080~081 (기타)      ──┤
                                                      │
TASK-003 (Inquiry    ─┬──> TASK-034 (AdminInquiry) ──┤──> TASK-090 (DTO 정리)
  태그 분리)           └──> TASK-050 (Guest/Member  ──┤      │
                             Inquiry)                 │      v
TASK-004 (Servlet    ─────> TASK-020 (인증)         ──┘  TASK-100 (SpringDoc 제거)
  API PoC)                                                    │
                                                              v
                                                          TASK-101 (전체 검증)
                                                              │
                                                              v
                                                          TASK-110 (CI 확장)
                                                              │
                                                              v
                                                          TASK-111 (스펙 린팅)

* TASK-034 (AdminInquiry)는 TASK-002 + TASK-003에 의존
* TASK-020 (PasswordAuth)는 TASK-002 + TASK-004에 의존
* TASK-050 (Guest/MemberInquiry)는 TASK-002 + TASK-003에 의존
```

### 권장 실행 순서 (병렬 가능 그룹)

| 순서 | 작업 | 병렬 가능 여부 |
|------|------|---------------|
| **Phase 0** | TASK-001 (매핑 문서), TASK-002 (DTO 패턴), TASK-003 (Inquiry 태그 분리), TASK-004 (Servlet API PoC) | 4개 병렬 |
| **Phase 1** | TASK-010 (Board, 난이도 하) -- 파일럿 마이그레이션 | 단독 (패턴 검증용) |
| **Phase 2** | TASK-030 (Dashboard, 하) + TASK-081 (Semester, 하) + TASK-080 (Privacy, 하) + TASK-033 (LoginHistory+StatusHistory, 하) + TASK-035 (AdminSemester, 하) | 5개 병렬 |
| **Phase 3** | TASK-013 (Like, 하) + TASK-014 (Bookmark, 하) + TASK-015 (PinnedPost, 하) + TASK-016 (CommentReport, 하) + TASK-072 (SurveyResponse, 하) | 5개 병렬 |
| **Phase 4** | TASK-011 (Post, 중) + TASK-012 (Comment, 중) + TASK-031 (AdminUser, 중) + TASK-032 (AdminMember, 중) + TASK-071 (SurveyQuestion, 중) | 5개 병렬 |
| **Phase 5** | TASK-040 (Event, 중) + TASK-041 (EventReg, 중) + TASK-034 (AdminInquiry, 중) + TASK-050 (Inquiry, 중) | 4개 병렬 |
| **Phase 6** | TASK-020 (PasswordAuth, 상) + TASK-060 (MyPage, 상) + TASK-070 (Survey, 상) | 3개 병렬 |
| **Phase 7** | TASK-090 (DTO 정리) | 단독 |
| **Phase 8** | TASK-100 (SpringDoc 제거) -> TASK-101 (전체 검증) | 순차 |
| **Phase 9** | TASK-110 (CI 확장) -> TASK-111 (스펙 린팅) | 순차 |

**Phase 0 작업 간 의존 관계**:
- TASK-001, TASK-002, TASK-003, TASK-004는 서로 독립적이므로 모두 병렬 진행 가능하다.
- TASK-003 완료는 Phase 5의 TASK-034, TASK-050의 선행 조건이다.
- TASK-004 완료는 Phase 6의 TASK-020의 선행 조건이다.

**Phase 1을 파일럿으로 먼저 진행하는 이유**: `BoardController`는 가장 단순한 컨트롤러(2개 엔드포인트, 페이지네이션 없음, 인증 불필요)로, 마이그레이션 패턴을 실제 코드에서 검증하기에 적합하다. 이 파일럿에서 발견된 문제점을 TASK-002의 DTO 매핑 전략에 반영한다.

---

## 구현 시 주의사항

### 기술적 고려사항

1. **`useTags: 'true'` 인터페이스 생성 규칙**: openapi-generator는 태그별로 하나의 Java 인터페이스를 생성한다. 태그명에서 공백은 제거되고 PascalCase로 변환된다 (예: `Password Authentication` -> `PasswordAuthenticationApi`). 컨트롤러 클래스명과 인터페이스명이 다를 수 있으므로 매핑을 정확히 확인해야 한다.

2. **`Inquiry` 태그 분리 (확정)**: `GuestInquiryController`, `MemberInquiryController`, `AdminInquiryController` 3개 컨트롤러가 기존에 `Inquiry` 태그를 공유한다. `useTags: 'true'` + `skipDefaultInterface: 'true'` 설정에서는 단일 `InquiryApi` 인터페이스가 생성되며, 이를 `implements`하는 클래스는 **모든 메서드를 구현해야** 한다. 여러 컨트롤러가 같은 인터페이스를 구현하면 Spring의 중복 매핑 충돌이 발생한다. 따라서 **TASK-003에서 OpenAPI 스펙의 태그를 `Guest Inquiry`, `Member Inquiry`, `Admin Inquiry`로 분리**하여, 각 컨트롤러가 독립된 인터페이스(`GuestInquiryApi`, `MemberInquiryApi`, `AdminInquiryApi`)를 구현하도록 한다.

3. **`MyPage` 태그의 `getMyLikes_1`, `getMyBookmarks_1` 중복 문제**: OpenAPI 스펙에서 `PostLike`/`Bookmark` 태그와 `My Page` 태그 양쪽에 유사한 엔드포인트가 있을 수 있다. `operationId` 접미사 `_1`은 중복 방지를 위한 것이므로, 실제 컨트롤러 구현 시 어떤 인터페이스의 메서드를 사용할지 명확히 해야 한다.

4. **`HttpServletRequest`/`HttpServletResponse` 접근 (확정)**: `PasswordAuthController`의 4개 메서드(`login`, `logout`, `refreshToken`, `recoverAccount`)에서 쿠키 처리를 위해 Servlet API 객체가 필요하다. `openapi-generator`가 생성하는 인터페이스 시그니처에는 Servlet API 파라미터가 포함되지 않으며, `skipDefaultInterface: 'true'` 설정으로 `default` 메서드 오버라이딩도 불가능하다. **TASK-004에서 검증한 `RequestContextHolder` 패턴을 적용한다**:
   ```java
   ServletRequestAttributes attrs = (ServletRequestAttributes)
       RequestContextHolder.currentRequestAttributes();
   HttpServletRequest httpRequest = attrs.getRequest();
   HttpServletResponse httpResponse = attrs.getResponse();
   ```
   - `login`: `httpRequest`에서 IP(`extractIpAddress`) 및 User-Agent 추출, `httpResponse`에 Refresh Token 쿠키 설정
   - `logout`: `httpRequest`에서 쿠키 추출, `httpResponse`에 쿠키 삭제
   - `refreshToken`: `httpRequest`에서 쿠키 추출, `httpResponse`에 새 쿠키 설정/삭제
   - `recoverAccount`: `httpResponse`에 Refresh Token 쿠키 설정
   - **주의**: `attrs.getResponse()`가 `null`을 반환할 수 있으므로, TASK-004 PoC에서 `RequestContextListener` 등록 필요 여부를 반드시 확인한다.

5. **`@Validated` 클래스 어노테이션**: `PasswordAuthController`에 붙어있는 `@Validated`는 `@RequestParam`에 `@Pattern` 등의 Bean Validation을 적용하기 위한 것이다. 생성된 인터페이스에서 이미 `useBeanValidation: 'true'`로 설정되어 있으므로, 인터페이스에 `@Validated`가 포함되는지 확인하고 중복을 방지한다.

### 잠재적 위험 요소

1. **DTO 매핑 누락**: 내부 DTO와 생성된 모델 DTO 간의 필드 매핑에서 누락이나 타입 불일치가 발생할 수 있다. 특히 enum 값, Instant 타입 변환, nullable 필드에 주의한다.

2. **기존 통합 테스트 실패**: 마이그레이션 후 응답 JSON 구조가 변경될 수 있다 (필드명, enum 직렬화 방식 등). 기존 통합 테스트의 JSON 매칭 검증을 업데이트해야 한다.

3. **`documentationProvider: 'none'` 설정**: 이 설정으로 생성된 인터페이스에 SpringDoc 어노테이션이 포함되지 않는다. 이는 SwaggerUI에서 이미 마이그레이션된 API가 표시되지 않을 수 있음을 의미한다. 마이그레이션 중간 단계에서는 미마이그레이션 컨트롤러만 SwaggerUI에 표시된다.

4. **컴파일 타임 의존성**: `compileJava`가 `openApiGenerate`에 의존하므로, OpenAPI 스펙 파일에 문법 오류가 있으면 전체 빌드가 실패한다. 스펙 수정 시 로컬에서 `./gradlew openApiGenerate`로 먼저 확인하는 습관이 필요하다.

### 기존 코드와의 통합 포인트

1. **SecurityUtils**: `igrus.web.common.util.SecurityUtils.requireCurrentUser()` -- 이미 구현되어 있으며, `StorageController`에서 사용 중이다. 모든 마이그레이션에서 동일하게 사용한다.

2. **PageableUtils**: `igrus.web.common.util.PageableUtils.of(page, size, sort)` -- 이미 구현되어 있다. `@ParameterObject Pageable`을 사용하는 모든 엔드포인트에서 이 유틸리티로 대체한다.

3. **SecurityConfig**: 마이그레이션 과정에서 경로 기반 보안 설정은 변경하지 않는다. `@PreAuthorize`는 컨트롤러 구현체에 유지한다.

4. **프론트엔드 Orval**: 동일한 `openapi/openapi.yaml`에서 프론트엔드 TypeScript 타입을 생성하므로, 스펙 변경 시 프론트엔드에도 영향이 있다. 태그 분리 등 스펙 변경이 필요한 경우 프론트엔드 팀과 협의한다.

---

## 완료 기준

### 마이그레이션 완료 체크리스트

- [ ] 29개 컨트롤러 모두 생성된 인터페이스를 `implements` 함
- [ ] 모든 컨트롤러에서 `@RequestMapping`, `@Tag`, `@Operation`, `@ApiResponse`, `@ApiResponses`, `@SecurityRequirement`, `@Parameter` 어노테이션 제거됨
- [ ] 모든 컨트롤러에서 `@AuthenticationPrincipal` 대신 `SecurityUtils.requireCurrentUser()` 사용
- [ ] 모든 페이지네이션 엔드포인트에서 `@ParameterObject Pageable` 대신 `PageableUtils.of()` 사용
- [ ] 모든 컨트롤러에서 `@PreAuthorize` 어노테이션 유지 확인
- [ ] 내부 DTO 클래스에서 `io.swagger.v3.*` 관련 어노테이션 모두 제거됨
- [ ] `SwaggerConfig.java` 삭제됨
- [ ] `build.gradle`에서 `springdoc-openapi-starter-webmvc-ui` 의존성 제거됨
- [ ] `./gradlew clean build` 성공
- [ ] `./gradlew test` 전체 테스트 통과

### CI/CD 체크리스트

- [ ] `openapi/` 폴더 변경 시 backend CI가 트리거됨
- [ ] OpenAPI 스펙 린팅이 CI에서 자동 실행됨

### 통계 요약

| 항목 | 수량 |
|------|------|
| 마이그레이션 대상 컨트롤러 | 29개 |
| 총 엔드포인트 수 | ~138개 |
| Swagger 어노테이션 제거 대상 파일 (컨트롤러) | 29개 |
| Swagger 어노테이션 제거 대상 파일 (DTO) | ~58개 |
| 컨트롤러 테스트 수정 대상 | 15개 (기존 테스트 보유) |
| 작업 그룹 | 11개 |
| 작업 항목 | 26개 |
| 예상 Phase | 10 Phase |
