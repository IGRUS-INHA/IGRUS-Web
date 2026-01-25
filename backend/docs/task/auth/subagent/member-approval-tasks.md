# Subagent Task: 준회원 승인 (US6)

**Branch**: `feat/member-approval`
**Priority**: P2
**관련 태스크**: T052, T053, T055, T057, T065

## 목표

관리자가 준회원(ASSOCIATE)을 정회원(MEMBER)으로 승인할 수 있는 관리자 API를 구현합니다.

## 이미 구현된 것

- ✅ `MemberApprovalService`: `backend/src/main/java/igrus/web/security/auth/approval/service/MemberApprovalService.java`
  - `getPendingAssociates()` - 승인 대기 준회원 목록 조회
  - `approveAssociate(Long userId, String reason)` - 개별 승인
  - `approveBulk(List<Long> userIds, Long adminId)` - 일괄 승인
- ✅ `AssociateInfoResponse` DTO: `backend/src/main/java/igrus/web/security/auth/approval/dto/response/AssociateInfoResponse.java`
- ✅ `MemberApprovalRequest` DTO: `backend/src/main/java/igrus/web/security/auth/approval/dto/request/MemberApprovalRequest.java`
- ✅ `BulkApprovalRequest` DTO: `backend/src/main/java/igrus/web/security/auth/approval/dto/request/BulkApprovalRequest.java`
- ✅ `BulkApprovalResultResponse` DTO: `backend/src/main/java/igrus/web/security/auth/approval/dto/response/BulkApprovalResultResponse.java`
- ✅ `AdminMemberController`: `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java` (Swagger 문서 포함)
- ✅ `MemberApprovalServiceTest`: `backend/src/test/java/igrus/web/security/auth/approval/service/MemberApprovalServiceTest.java`
- ✅ `AdminMemberControllerTest`: `backend/src/test/java/igrus/web/security/auth/approval/controller/AdminMemberControllerTest.java`

## 구현할 태스크

### T052: MemberApprovalRequest DTO 생성

**경로**: `backend/src/main/java/igrus/web/security/auth/approval/dto/request/MemberApprovalRequest.java`

```java
public record MemberApprovalRequest(
    @Size(max = 500, message = "승인 사유는 500자 이내여야 합니다")
    String reason  // 선택 필드
) {}
```

### T053: BulkApprovalRequest DTO 생성

**경로**: `backend/src/main/java/igrus/web/security/auth/approval/dto/request/BulkApprovalRequest.java`

```java
public record BulkApprovalRequest(
    @NotEmpty(message = "승인할 사용자 ID 목록은 필수입니다")
    List<@NotNull Long> userIds,

    @Size(max = 500, message = "승인 사유는 500자 이내여야 합니다")
    String reason  // 선택 필드
) {}
```

### T055: AdminMemberController 생성

**경로**: `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java`

엔드포인트:
- `GET /api/v1/admin/members/pending` - 승인 대기 준회원 목록 조회
- `POST /api/v1/admin/members/{id}/approve` - 개별 승인
- `POST /api/v1/admin/members/approve/bulk` - 일괄 승인

```java
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController implements AdminMemberControllerApi {

    private final MemberApprovalService memberApprovalService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<AssociateInfoResponse>> getPendingMembers();

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> approveMember(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) MemberApprovalRequest request
    );

    @PostMapping("/approve/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> bulkApproveMembers(
        @Valid @RequestBody BulkApprovalRequest request
    );
}
```

### T065: Swagger 문서화

**경로**: `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberControllerApi.java`

```java
@Tag(name = "Admin - Member Approval", description = "준회원 승인 관리 API")
public interface AdminMemberControllerApi {

    @Operation(summary = "승인 대기 준회원 목록 조회")
    @ApiResponses(...)
    ResponseEntity<List<AssociateInfoResponse>> getPendingMembers();

    @Operation(summary = "준회원 개별 승인")
    @ApiResponses(...)
    ResponseEntity<Void> approveMember(Long id, MemberApprovalRequest request);

    @Operation(summary = "준회원 일괄 승인")
    @ApiResponses(...)
    ResponseEntity<Void> bulkApproveMembers(BulkApprovalRequest request);
}
```

### T057: 통합 테스트

**경로**: `backend/src/test/java/igrus/web/security/auth/approval/controller/AdminMemberControllerTest.java`

테스트 케이스:
1. 관리자 권한으로 준회원 목록 조회 성공
2. 일반 사용자 권한으로 목록 조회 시 403
3. 개별 승인 성공
4. 존재하지 않는 사용자 승인 시 에러
5. 이미 정회원인 사용자 승인 시 에러
6. 일괄 승인 성공
7. 빈 목록으로 일괄 승인 시 에러

## 참고 파일

- 서비스: `backend/src/main/java/igrus/web/security/auth/approval/service/MemberApprovalService.java`
- 단위 테스트: `backend/src/test/java/igrus/web/security/auth/approval/service/MemberApprovalServiceTest.java`
- 기존 컨트롤러 패턴: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
- Security 설정: `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`

## 완료 조건

- [x] MemberApprovalRequest DTO 생성 (`backend/src/main/java/igrus/web/security/auth/approval/dto/request/MemberApprovalRequest.java`)
- [x] BulkApprovalRequest DTO 생성 (`backend/src/main/java/igrus/web/security/auth/approval/dto/request/BulkApprovalRequest.java`)
- [x] BulkApprovalResultResponse DTO 생성 (`backend/src/main/java/igrus/web/security/auth/approval/dto/response/BulkApprovalResultResponse.java`)
- [x] AdminMemberController 생성 (`backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java`)
- [x] Swagger 문서화 (컨트롤러에 직접 어노테이션 포함 - dev 브랜치 패턴 적용)
- [x] 통합 테스트 작성 및 통과 (`backend/src/test/java/igrus/web/security/auth/approval/controller/AdminMemberControllerTest.java`)
- [x] `./gradlew test` 전체 테스트 통과

## 주의사항

- 이 컨트롤러는 새로 생성하는 것이므로 다른 worktree와 파일 충돌 없음
- `/api/v1/admin/**` 경로는 이미 `ApiSecurityConfig`에서 ADMIN 역할 필요하도록 설정됨
- OPERATOR 역할도 승인 가능하도록 `@PreAuthorize`에 포함할 것
- 에러 코드 추가 시 `ErrorCode.java`의 기존 M001-M004 코드 활용
