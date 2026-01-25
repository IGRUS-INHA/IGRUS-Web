package igrus.web.inquiry.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.inquiry.service.InquiryService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class InquiryController {

    private final InquiryService inquiryService;

    // ==================== 공개 API ====================

    @Operation(
            summary = "비회원 문의 작성",
            description = "비회원이 문의를 작성합니다. 이메일, 이름, 비밀번호가 필수입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    })
    @PostMapping("/guest")
    public ResponseEntity<CreateInquiryResponse> createGuestInquiry(
            @Valid @RequestBody CreateGuestInquiryRequest request
    ) {
        CreateInquiryResponse response = inquiryService.createGuestInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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
    public ResponseEntity<CreateInquiryResponse> createMemberInquiry(
            @Valid @RequestBody CreateMemberInquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreateInquiryResponse response = inquiryService.createMemberInquiry(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "비회원 문의 조회",
            description = "문의번호, 이메일, 비밀번호로 비회원 문의를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @PostMapping("/lookup")
    public ResponseEntity<InquiryResponse> lookupGuestInquiry(
            @Valid @RequestBody GuestInquiryLookupRequest request
    ) {
        InquiryResponse response = inquiryService.lookupGuestInquiry(request);
        return ResponseEntity.ok(response);
    }

    // ==================== 회원 전용 API ====================

    @Operation(
            summary = "내 문의 목록 조회",
            description = "로그인한 회원의 문의 목록을 조회합니다. JWT 인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지당 항목 수", example = "20"),
            @Parameter(name = "sort", description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryListResponse>> getMyInquiries(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InquiryListResponse> response = inquiryService.getMyInquiries(user.userId(), pageable);
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
        InquiryResponse response = inquiryService.getMyInquiry(id, user.userId());
        return ResponseEntity.ok(response);
    }

    // ==================== 관리자 전용 API ====================

    @Operation(
            summary = "전체 문의 목록 조회",
            description = "관리자가 모든 문의를 조회합니다. 유형과 상태로 필터링할 수 있습니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @Parameters({
            @Parameter(name = "type", description = "문의 유형 필터", example = "JOIN"),
            @Parameter(name = "status", description = "처리 상태 필터", example = "PENDING"),
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지당 항목 수", example = "20"),
            @Parameter(name = "sort", description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<InquiryListResponse>> getAllInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InquiryListResponse> response = inquiryService.getAllInquiries(type, status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "문의 상세 조회 (관리자)",
            description = "관리자가 특정 문의의 상세 정보(메모 포함)를 조회합니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<InquiryDetailResponse> getInquiryDetail(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id
    ) {
        InquiryDetailResponse response = inquiryService.getInquiryDetail(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "문의 상태 변경",
            description = "관리자가 문의의 처리 상태를 변경합니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateInquiryStatus(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateInquiryStatusRequest request
    ) {
        inquiryService.updateInquiryStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "답변 작성",
            description = "관리자가 문의에 답변을 작성합니다. 이미 답변이 있는 경우 에러가 발생합니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "답변 작성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 답변이 존재함")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{id}/reply")
    public ResponseEntity<InquiryReplyResponse> createReply(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateInquiryReplyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryReplyResponse response = inquiryService.createReply(id, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "답변 수정",
            description = "관리자가 기존 답변을 수정합니다. 답변이 없는 경우 에러가 발생합니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의 또는 답변을 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PutMapping("/{id}/reply")
    public ResponseEntity<InquiryReplyResponse> updateReply(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateInquiryReplyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryReplyResponse response = inquiryService.updateReply(id, request, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내부 메모 작성",
            description = "관리자가 문의에 대한 내부 메모를 작성합니다. 메모는 관리자에게만 보입니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "메모 작성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{id}/memo")
    public ResponseEntity<InquiryMemoResponse> createMemo(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateInquiryMemoRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryMemoResponse response = inquiryService.createMemo(id, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "문의 삭제",
            description = "관리자가 문의를 소프트 삭제합니다. OPERATOR 또는 ADMIN 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(
            @Parameter(description = "문의 ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        inquiryService.deleteInquiry(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
