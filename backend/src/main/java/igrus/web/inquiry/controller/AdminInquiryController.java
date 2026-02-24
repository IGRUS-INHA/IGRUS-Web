package igrus.web.inquiry.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.inquiry.service.manage.*;
import igrus.web.inquiry.service.read.GetAllInquiriesService;
import igrus.web.inquiry.service.read.GetInquiryDetailService;
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
 * 관리자 문의 컨트롤러.
 * OPERATOR 또는 ADMIN 권한 필요.
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class AdminInquiryController {

    private final GetAllInquiriesService getAllInquiriesService;
    private final GetInquiryDetailService getInquiryDetailService;
    private final UpdateInquiryStatusService updateInquiryStatusService;
    private final CreateInquiryReplyService createInquiryReplyService;
    private final UpdateInquiryReplyService updateInquiryReplyService;
    private final CreateInquiryMemoService createInquiryMemoService;
    private final DeleteInquiryService deleteInquiryService;

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
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<InquiryListPageResponse> getAllInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<InquiryListResponse> page = getAllInquiriesService.getAllInquiries(type, status, pageable);
        return ResponseEntity.ok(InquiryListPageResponse.from(page));
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
        InquiryDetailResponse response = getInquiryDetailService.getInquiryDetail(id);
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
            @Valid @RequestBody UpdateInquiryStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        updateInquiryStatusService.updateInquiryStatus(id, request, user.userId());
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
        InquiryReplyResponse response = createInquiryReplyService.createReply(id, request, user.userId());
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
        InquiryReplyResponse response = updateInquiryReplyService.updateReply(id, request, user.userId());
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
        InquiryMemoResponse response = createInquiryMemoService.createMemo(id, request, user.userId());
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
        deleteInquiryService.deleteInquiry(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
