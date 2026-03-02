# OpenAPI Contract-First 컨트롤러 마이그레이션 가이드

## 개요

이 문서는 29개 백엔드 컨트롤러를 OpenAPI Contract-First 방식으로 마이그레이션할 때 참조하는 가이드이다.
마이그레이션 완료 레퍼런스: `StorageController`, `HealthController`.

---

## 1. 컨트롤러-OpenAPI 태그 매핑 테이블

> `useTags: 'true'` 설정으로 태그명에서 공백 제거 후 PascalCase로 변환하여 인터페이스 이름이 결정된다.
> 예: `Password Authentication` -> `PasswordAuthenticationApi`

### 마이그레이션 완료

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | 상태 |
|---|---------------|-----------|-------------|--------------|:-----------:|:----:|
| 1 | `HealthController` | `common.controller` | `Health` | `HealthApi` | 1 | 완료 |
| 2 | `StorageController` | `storage.controller` | `Storage` | `StorageApi` | 4 | 완료 |

### 게시판/커뮤니티 도메인 (7개 컨트롤러, 25개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 3 | `BoardController` | `community.board.controller` | `Board` | `BoardApi` | 2 | TASK-010 |
| 4 | `PostController` | `community.post.controller` | `Post` | `PostApi` | 7 | TASK-011 |
| 5 | `CommentController` | `community.comment.controller` | `Comment` | `CommentApi` | 4 | TASK-012 |
| 6 | `PostLikeController` | `community.like.post_like.controller` | `PostLike` | `PostLikeApi` | 3 | TASK-013 |
| 7 | `CommentLikeController` | `community.like.comment_like.controller` | `Comment Like` | `CommentLikeApi` | 2 | TASK-013 |
| 8 | `BookmarkController` | `community.bookmark.controller` | `Bookmark` | `BookmarkApi` | 3 | TASK-014 |
| 9 | `PinnedPostController` | `community.pinnedpost.controller` | `Pinned Post` | `PinnedPostApi` | 4 | TASK-015 |
| 10 | `CommentReportController` | `community.comment.controller` | `Comment Report` | `CommentReportApi` | 3 | TASK-016 |

### 인증 도메인 (1개 컨트롤러, 16개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 11 | `PasswordAuthController` | `security.auth.password.controller` | `Password Authentication` | `PasswordAuthenticationApi` | 16 | TASK-020 |

### Admin 도메인 (7개 컨트롤러, 28개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 12 | `AdminDashboardController` | `admin.dashboard.controller` | `Admin Dashboard` | `AdminDashboardApi` | 1 | TASK-030 |
| 13 | `AdminUserController` | `admin.user.controller` | `Admin User Management` | `AdminUserManagementApi` | 8 | TASK-031 |
| 14 | `AdminMemberController` | `security.auth.approval.controller` | `Admin Associate Approval` | `AdminAssociateApprovalApi` | 7 | TASK-032 |
| 15 | `AdminLoginHistoryController` | `security.auth.common.controller` | `Admin Login History` | `AdminLoginHistoryApi` | 1 | TASK-033 |
| 16 | `AdminAccountStatusChangeHistoryController` | `user.controller` | `Account Status Change History` | `AccountStatusChangeHistoryApi` | 1 | TASK-033 |
| 17 | `AdminInquiryController` | `inquiry.controller` | `Admin Inquiry` (*) | `AdminInquiryApi` (*) | 7 | TASK-034 |
| 18 | `AdminSemesterMemberController` | `user.semester.controller` | `Admin Semester Member` | `AdminSemesterMemberApi` | 3 | TASK-035 |

> (*) TASK-003에서 `Inquiry` -> `Admin Inquiry`로 태그 분리 필요

### 문의 도메인 (2개 컨트롤러, 5개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 19 | `GuestInquiryController` | `inquiry.controller` | `Guest Inquiry` (*) | `GuestInquiryApi` (*) | 2 | TASK-050 |
| 20 | `MemberInquiryController` | `inquiry.controller` | `Member Inquiry` (*) | `MemberInquiryApi` (*) | 3 | TASK-050 |

> (*) TASK-003에서 `Inquiry` -> `Guest Inquiry`, `Member Inquiry`로 태그 분리 필요

### 마이페이지 도메인 (1개 컨트롤러, 12개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 21 | `MyPageController` | `user.mypage.controller` | `MyPage` | `MyPageApi` | 12 | TASK-060 |

### 설문 도메인 (6개 컨트롤러, 29개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 22 | `SurveyController` | `survey.controller` | `Survey` | `SurveyApi` | 13 | TASK-070 |
| 23 | `SurveyQuestionController` | `survey.question.controller` | `Survey Question` | `SurveyQuestionApi` | 4 | TASK-071 |
| 24 | `SurveyQuestionOptionController` | `survey.question.controller` | `Survey Question Option` | `SurveyQuestionOptionApi` | 4 | TASK-071 |
| 25 | `SurveyQuestionRowController` | `survey.question.controller` | `Survey Question Row` | `SurveyQuestionRowApi` | 4 | TASK-071 |
| 26 | `SurveyResponseController` | `survey.response.controller` | `Survey Response` | `SurveyResponseApi` | 3 | TASK-072 |
| 27 | `SurveyAnonymousResponseController` | `survey.response.controller` | `Survey Anonymous Response` | `SurveyAnonymousResponseApi` | 1 | TASK-072 |

### 기타 도메인 (2개 컨트롤러, 7개 엔드포인트)

| # | 컨트롤러 클래스 | 패키지 경로 | OpenAPI 태그 | 생성 인터페이스 | 엔드포인트 수 | TASK-ID |
|---|---------------|-----------|-------------|--------------|:-----------:|:-------:|
| 28 | `PrivacyConsentController` | `security.auth.common.controller` | `Privacy Consent` | `PrivacyConsentApi` | 5 | TASK-080 |
| 29 | `SemesterMemberController` | `user.semester.controller` | `Semester Member` | `SemesterMemberApi` | 2 | TASK-081 |

### 태그 분리가 필요한 컨트롤러 (추가 참고)

`Comment Report` 태그는 `CommentReportController`(프론트 사용)와 `AdminUserController`의 일부(admin 관련)에서 공유한다.
OpenAPI 스펙에서 `admin.yaml`의 `adminCommentReports`, `adminCommentReportsByReportId` 오퍼레이션도 `Comment Report` 태그를 사용하고 있다.
`CommentReportController`가 이 모든 엔드포인트를 구현하는 구조이므로 태그 분리 없이 단일 `CommentReportApi` 구현이 가능하다.

---

## 2. 단계별 마이그레이션 체크리스트

각 컨트롤러를 마이그레이션할 때 아래 단계를 순서대로 수행한다.

### 사전 확인

- [ ] 대상 컨트롤러의 OpenAPI 태그가 위 매핑 테이블과 일치하는지 확인
- [ ] `./gradlew openApiGenerate` 실행 후, 대응하는 `{Tag}Api` 인터페이스가 `build/generated/openapi/` 아래에 존재하는지 확인
- [ ] 생성된 인터페이스의 메서드 시그니처(파라미터 타입, 반환 타입)와 기존 컨트롤러 메서드를 비교

### 클래스 레벨 변환

- [ ] `implements {Tag}Api` 추가
- [ ] `@RequestMapping` 어노테이션 제거 (인터페이스에 이미 정의됨)
- [ ] `@Tag` 어노테이션 제거
- [ ] `@SecurityRequirement` 어노테이션 제거
- [ ] `@Validated` 클래스 어노테이션 제거 (Bean Validation은 인터페이스에서 처리)
- [ ] `SwaggerConfig` import 제거

### 메서드 레벨 변환

각 엔드포인트 메서드에 대해:

- [ ] `@Override` 어노테이션 추가
- [ ] `@Operation` 어노테이션 제거
- [ ] `@ApiResponse`, `@ApiResponses` 어노테이션 제거
- [ ] `@Parameter` 어노테이션 제거
- [ ] `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` 어노테이션 제거
- [ ] 메서드 시그니처를 생성된 인터페이스의 시그니처와 일치시킴

### 파라미터 변환

- [ ] `@AuthenticationPrincipal AuthenticatedUser user` -> 메서드 시그니처에서 제거, 바디에서 `SecurityUtils.requireCurrentUser()` 사용
- [ ] `@ParameterObject @PageableDefault(...) Pageable pageable` -> 개별 `page`, `size`, `sort` 파라미터로 변경, 바디에서 `PageableUtils.of(page, size, sort)` 사용
- [ ] `@Valid @RequestBody` -> 인터페이스에서 이미 처리되므로 중복 어노테이션 제거
- [ ] `@PathVariable`, `@RequestParam` -> 인터페이스 시그니처에 이미 포함되므로 어노테이션 제거

### DTO 매핑 (요청)

- [ ] 생성된 모델 DTO(`igrus.web.generated.model.*`)를 메서드 파라미터 타입으로 사용
- [ ] 내부 DTO로 변환이 필요한 경우, 인라인 매핑 또는 매핑 헬퍼 사용

### DTO 매핑 (응답)

- [ ] 생성된 모델 DTO(`igrus.web.generated.model.*`)를 `ResponseEntity<T>`의 타입 인자로 사용
- [ ] 내부 DTO/서비스 결과를 생성된 모델 DTO로 변환
- [ ] 인라인 매핑 패턴 예시 (StorageController 참조):
  ```java
  return ResponseEntity.ok(new CreateUploadUrl200Response()
          .presignedUrl(result.presignedUrl())
          .objectKey(result.objectKey()));
  ```

### 권한 및 보안

- [ ] `@PreAuthorize` 어노테이션은 컨트롤러 구현체에 **유지** (제거하지 않음)
- [ ] 클래스 레벨 `@PreAuthorize`는 그대로 유지

### 검증 및 테스트

- [ ] `./gradlew compileJava`로 컴파일 성공 확인
- [ ] 기존 통합 테스트가 존재하면 실행하여 통과 확인
- [ ] 응답 JSON 구조 변경이 없는지 확인 (필드명, enum 직렬화 등)

### Import 정리

- [ ] `io.swagger.v3.*` import 모두 제거
- [ ] `org.springdoc.*` import 모두 제거
- [ ] `igrus.web.common.config.SwaggerConfig` import 제거
- [ ] `igrus.web.generated.api.*`, `igrus.web.generated.model.*` import 추가
- [ ] `igrus.web.common.util.SecurityUtils` import 추가 (인증 필요 시)
- [ ] `igrus.web.common.util.PageableUtils` import 추가 (페이지네이션 사용 시)

---

## 3. 특수 케이스 처리 가이드

### 3.1 HttpServletRequest/Response 사용 (PasswordAuthController)

`PasswordAuthController`의 4개 메서드(`login`, `logout`, `refreshToken`, `recoverAccount`)에서 쿠키 처리 및 IP 추출을 위해 Servlet API가 필요하다.
생성된 인터페이스 시그니처에는 Servlet API 파라미터가 포함되지 않으므로, 전용 유틸리티 `ServletContextUtil`을 사용한다.

#### 확정 패턴: `ServletContextUtil` (TASK-004 PoC 검증 완료)

위치: `igrus.web.common.util.ServletContextUtil`

```java
// 기존 방식 (마이그레이션 전)
public ResponseEntity<PasswordLoginResponse> login(
        PasswordLoginRequest request,
        HttpServletRequest httpRequest,      // <- 인터페이스에 없음
        HttpServletResponse httpResponse) {  // <- 인터페이스에 없음
    String ip = extractIpAddress(httpRequest);
    // ...
}

// 새 방식 (마이그레이션 후)
@Override
public ResponseEntity<PasswordLoginResponse> login(PasswordLoginRequest request) {
    HttpServletRequest httpRequest = ServletContextUtil.getCurrentRequest();
    HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();
    String ip = ServletContextUtil.extractIpAddress(httpRequest);
    String userAgent = ServletContextUtil.getCurrentUserAgent();
    // ...
}
```

#### PoC 검증 결과 (6/6 PASS)

| 검증 항목 | 결과 | 비고 |
|----------|:----:|------|
| `attrs.getResponse()` null 여부 | PASS | Spring MVC DispatcherServlet이 자동 등록, RequestContextListener 불필요 |
| Set-Cookie 헤더 클라이언트 전달 | PASS | `response.addHeader(SET_COOKIE, ...)` 정상 전달 확인 |
| X-Forwarded-For 첫 번째 IP 추출 | PASS | 쉼표 구분 첫 번째 값 반환 |
| X-Real-IP 추출 | PASS | X-Forwarded-For 없을 때 X-Real-IP 사용 |
| remoteAddr fallback | PASS | 프록시 헤더 없을 때 remoteAddr 반환 (MockMvc: 127.0.0.1) |
| User-Agent 헤더 추출 | PASS | 정상 추출 확인 |

검증 테스트 위치: `igrus.web.common.util.ServletContextUtilPocTest`

#### 제공 메서드

| 메서드 | 설명 | 용도 |
|--------|------|------|
| `getCurrentRequest()` | 현재 HttpServletRequest 반환 | 쿠키 읽기, 헤더 접근 |
| `getCurrentResponse()` | 현재 HttpServletResponse 반환 | Set-Cookie 설정 |
| `extractIpAddress(request)` | IP 추출 (X-Forwarded-For > X-Real-IP > remoteAddr) | 로그인 IP 기록 |
| `extractCurrentIpAddress()` | 현재 요청에서 IP 추출 (편의 메서드) | 위와 동일 |
| `getCurrentUserAgent()` | User-Agent 헤더 반환 | 로그인 이력 기록 |

#### 적용 대상 메서드 (TASK-020)

| 메서드 | Request 사용 | Response 사용 |
|--------|:----------:|:------------:|
| `login` | IP, User-Agent, 쿠키 읽기 | Set-Cookie |
| `logout` | 쿠키 읽기 | Set-Cookie (삭제) |
| `refreshToken` | 쿠키 읽기 | Set-Cookie |
| `recoverAccount` | - | Set-Cookie |

### 3.2 Inquiry 태그 분리

기존 `Inquiry` 단일 태그를 3개(`Guest Inquiry`, `Member Inquiry`, `Admin Inquiry`)로 분리하여, 각 컨트롤러가 독립된 인터페이스를 구현할 수 있도록 한다.

상세 변경 내용: TASK-003 결과 참조.

### 3.3 페이지네이션 변환

기존:
```java
@ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
Pageable pageable
```

변환 후:
```java
// 생성된 인터페이스의 파라미터: Integer page, Integer size, List<String> sort
Pageable pageable = PageableUtils.of(page, size, sort);
```

### 3.4 DTO 매핑 전략 (TASK-002 결정 사항)

내부 DTO와 생성된 모델 DTO는 필드가 동일하더라도 타입이 다르므로 반드시 매핑이 필요하다.

#### 전략 결정: "인라인 매핑 우선 + 공통 유틸 보조"

**별도 Mapper 클래스는 생성하지 않는다.** 이유:
- MapStruct 등 매핑 라이브러리 의존성 추가 없이 단순하게 유지
- StorageController 레퍼런스에서 검증된 인라인 패턴이 충분히 간결
- 컨트롤러별 매핑 로직이 상이하여 공통화 이점이 낮음

#### 패턴 1: 인라인 매핑 (기본 - 대부분의 경우)

생성된 모델 DTO의 fluent setter를 활용하여 컨트롤러 메서드 내에서 직접 매핑한다.

```java
// 요청 DTO: 생성 모델 -> 내부 DTO
CreatePresignedUrlRequest internalRequest = new CreatePresignedUrlRequest(
    request.getFileName(), request.getContentType(),
    request.getFileSize(), request.getPurpose());

// 응답 DTO: 내부 결과 -> 생성 모델
return ResponseEntity.ok(new CreateUploadUrl200Response()
    .presignedUrl(result.presignedUrl())
    .objectKey(result.objectKey()));
```

#### 패턴 2: 커스텀 간소화 페이지네이션 응답 (PostListPageResponse 등)

내부 서비스가 `Page<Entity>` 또는 내부 DTO를 반환하면, 컨트롤러에서 인라인으로 매핑한다.

```java
Page<Post> page = service.getPostList(boardCode, user, keyword, questionOnly, pageable);
PostListPageResponse response = new PostListPageResponse()
    .posts(page.getContent().stream()
        .map(post -> new PostListResponseItem()
            .id(post.getId())
            .title(post.getTitle()))
        .toList())
    .totalElements(page.getTotalElements())
    .totalPages(page.getTotalPages())
    .currentPage(page.getNumber())
    .hasNext(page.hasNext());
```

#### 패턴 3: Spring Page 전체 래핑 응답 (Page*Response 등)

`PageResponseMapper` 유틸리티를 사용하여 Spring `Page` -> 생성 모델의 `Page*Response`로 변환한다.
위치: `igrus.web.common.util.PageResponseMapper`

```java
PageAccountStatusChangeHistoryResponse response = PageResponseMapper.toSpringPageResponse(
    page,
    entity -> new PageAccountStatusChangeHistoryResponseContentInner()
        .changeType(entity.getChangeType().name())
        .changedAt(entity.getChangedAt()),
    PageAccountStatusChangeHistoryResponse::new,
    (r, content, meta) -> r
        .content(content)
        .totalElements(meta.totalElements())
        .totalPages(meta.totalPages())
        .number(meta.number())
        .size(meta.size())
        .numberOfElements(meta.numberOfElements())
        .first(meta.first())
        .last(meta.last())
        .empty(meta.empty())
        .pageable(meta.pageable())
        .sort(meta.sort())
);
```

#### 패턴 4: 동일 필드 구조의 단순 DTO (DashboardStatsResponse 등)

내부 DTO와 생성 모델이 동일한 필드명을 가지는 경우에도, 필드별로 인라인 매핑한다.

```java
igrus.web.admin.dashboard.dto.DashboardStatsResponse internal = service.getDashboardStats();
return ResponseEntity.ok(new igrus.web.generated.model.DashboardStatsResponse()
    .todayPostCount(internal.todayPostCount())
    .todayCommentCount(internal.todayCommentCount())
    .weeklyApprovedMemberCount(internal.weeklyApprovedMemberCount())
    .pendingInquiryCount(internal.pendingInquiryCount())
    .pendingAssociateCount(internal.pendingAssociateCount()));
```

---

## 4. openapi-generator 설정 참조

```groovy
openApiGenerate {
    generatorName = 'spring'
    inputSpec = file("${rootProject.projectDir}/../openapi/openapi.yaml").absolutePath
    outputDir = layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath
    apiPackage = 'igrus.web.generated.api'
    modelPackage = 'igrus.web.generated.model'
    configOptions = [
        interfaceOnly      : 'true',
        useTags            : 'true',
        useSpringBoot3     : 'true',
        useJakartaEe       : 'true',
        openApiNullable    : 'false',
        skipDefaultInterface: 'true',
        useBeanValidation  : 'true',
        documentationProvider: 'none',
    ]
    typeMappings = ['DateTime': 'java.time.Instant']
    importMappings = ['java.time.OffsetDateTime': 'java.time.Instant']
}
```

주요 설정 의미:
- `interfaceOnly: 'true'`: 인터페이스만 생성 (구현체는 수동 작성)
- `useTags: 'true'`: 태그별로 인터페이스 분리 생성
- `skipDefaultInterface: 'true'`: `default` 메서드 없이 순수 인터페이스로 생성, 모든 메서드를 구현체에서 반드시 구현해야 함
- `documentationProvider: 'none'`: 생성된 인터페이스에 Swagger 어노테이션 미포함
- `useBeanValidation: 'true'`: Bean Validation 어노테이션 포함
