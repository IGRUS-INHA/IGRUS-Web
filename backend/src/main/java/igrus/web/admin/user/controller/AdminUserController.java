package igrus.web.admin.user.controller;

import igrus.web.admin.user.dto.UserListResponse;
import igrus.web.admin.user.service.GetUserListService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Admin User Management", description = "관리자 회원 관리 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminUserController {

    private final GetUserListService getUserListService;

    @Operation(
            summary = "회원 목록 조회",
            description = "회원 목록을 조회합니다. 검색어, 역할, 상태로 필터링 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
            description = "조회 성공",
    content = @Content(schema = @Schema(implementation = Page.class))
            ),
    @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
    @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<UserListResponse>> getUserList(
            @Parameter(description = "검색어 (이름 또는 학번)") @RequestParam(required = false) String keyword,
            @Parameter(description = "역할 필터") @RequestParam(required = false) UserRole role,
            @Parameter(description = "상태 필터") @RequestParam(required = false) UserStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(getUserListService.getUserList(keyword, role, status, pageable));
    }
}
