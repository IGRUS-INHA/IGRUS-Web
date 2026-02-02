package igrus.web.inquiry.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.inquiry.service.read.GetMyInquiriesService;
import igrus.web.inquiry.service.read.GetMyInquiryService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 문의 컨트롤러.
 * 인증된 사용자 전용 API.
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class MemberInquiryController {

    private final CreateMemberInquiryService createMemberInquiryService;
    private final GetMyInquiriesService getMyInquiriesService;
    private final GetMyInquiryService getMyInquiryService;

    @Operation(
            summary = "회원 문의 작성",
            description = "로그인한 회원이 문의를 작성합니다. JWT 인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/member")
    public ResponseEntity<InquiryCreateResponse> createMemberInquiry(
            @Valid @RequestBody CreateMemberInquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 문의 목록 조회",
            description = "로그인한 회원의 문의 목록을 조회합니다. JWT 인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryListResponse>> getMyInquiries(
            @AuthenticationPrincipal AuthenticatedUser user,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<InquiryListResponse> response = getMyInquiriesService.getMyInquiries(user.userId(), pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 문의 상세 조회",
            description = "로그인한 회원의 특정 문의 상세 정보를 조회합니다. JWT 인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my/{id}")
    public ResponseEntity<InquiryResponse> getMyInquiry(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryResponse response = getMyInquiryService.getMyInquiry(id, user.userId());
        return ResponseEntity.ok(response);
    }
}
