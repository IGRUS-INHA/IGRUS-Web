package igrus.web.admin.user.controller;

import igrus.web.admin.user.dto.ChangeUserRoleRequest;
import igrus.web.admin.user.dto.ChangeUserStatusRequest;
import igrus.web.admin.user.dto.UserDetailResponse;
import igrus.web.admin.user.dto.UserListPageResponse;
import igrus.web.admin.user.dto.UserListResponse;
import igrus.web.admin.user.dto.UserRoleHistoryResponse;
import igrus.web.admin.user.service.ChangeUserRoleService;
import igrus.web.admin.user.service.ChangeUserStatusService;
import igrus.web.admin.user.service.GetUserDetailService;
import igrus.web.admin.user.service.GetUserListService;
import igrus.web.admin.user.service.GetUserRoleHistoryService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.common.config.SwaggerConfig;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Admin User Management", description = "관리자 회원 관리 API (OPERATOR 이상, 권한 변경은 ADMIN 전용)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminUserController {

    private final GetUserListService getUserListService;
    private final GetUserDetailService getUserDetailService;
    private final ChangeUserRoleService changeUserRoleService;
    private final ChangeUserStatusService changeUserStatusService;
    private final GetUserRoleHistoryService getUserRoleHistoryService;

    @Operation(
            summary = "권한 변경 이력 조회",
            description = "권한 변경 이력을 조회합니다. 사용자, 역할, 변경자, 날짜 범위로 필터링 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role-histories")
    public ResponseEntity<Page<UserRoleHistoryResponse>> getRoleHistories(
            @Parameter(description = "대상 사용자 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "변경 전 역할 필터") @RequestParam(required = false) UserRole previousRole,
            @Parameter(description = "변경 후 역할 필터") @RequestParam(required = false) UserRole newRole,
            @Parameter(description = "변경자 ID 필터") @RequestParam(required = false) Long changedBy,
            @Parameter(description = "시작 일시") @RequestParam(required = false) Instant startDate,
            @Parameter(description = "종료 일시") @RequestParam(required = false) Instant endDate,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(getUserRoleHistoryService.getUserRoleHistories(
                userId, previousRole, newRole, changedBy, startDate, endDate, pageable));
    }

    @Operation(
            summary = "회원 목록 조회",
            description = "회원 목록을 조회합니다. 검색어, 역할, 상태로 필터링 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
            description = "조회 성공",
    content = @Content(schema = @Schema(implementation = UserListPageResponse.class))
            ),
    @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
    @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)", content = @Content)
    })
    @GetMapping
    public ResponseEntity<UserListPageResponse> getUserList(
            @Parameter(description = "검색어 (이름 또는 학번)") @RequestParam(required = false) String keyword,
            @Parameter(description = "역할 필터") @RequestParam(required = false) UserRole role,
            @Parameter(description = "상태 필터") @RequestParam(required = false) UserStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserListResponse> page = getUserListService.getUserList(keyword, role, status, pageable);
        return ResponseEntity.ok(UserListPageResponse.from(page));
    }

    @Operation(
            summary = "회원 상세 조회",
            description = "특정 회원의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserDetail(
            @Parameter(description = "사용자 ID") @PathVariable Long userId
    ) {
        return ResponseEntity.ok(getUserDetailService.getUserDetail(userId));
    }

    @Operation(
            summary = "회원 권한 변경",
            description = "회원의 권한을 변경합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "자기 자신 권한 변경, 동일 역할 변경, 또는 마지막 ADMIN 권한 변경 시도", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<Void> changeUserRole(
            @Parameter(description = "대상 사용자 ID") @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        changeUserRoleService.changeUserRole(userId, request.role(), authenticatedUser.userId());
        return ResponseEntity.noContent().build();
    }
}
