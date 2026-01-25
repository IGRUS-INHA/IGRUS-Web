package igrus.web.inquiry.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Inquiry", description = "문의 API")
public interface InquiryControllerApi {

    // ==================== 공개 API ====================

    @Operation(
            summary = "비회원 문의 작성",
            description = "비회원이 문의를 작성합니다. 이메일, 이름, 비밀번호가 필수입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    })
    ResponseEntity<CreateInquiryResponse> createGuestInquiry(CreateGuestInquiryRequest request);

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
    ResponseEntity<CreateInquiryResponse> createMemberInquiry(CreateMemberInquiryRequest request, AuthenticatedUser user);

    @Operation(
            summary = "비회원 문의 조회",
            description = "문의번호, 이메일, 비밀번호로 비회원 문의를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
    })
    ResponseEntity<InquiryResponse> lookupGuestInquiry(GuestInquiryLookupRequest request);

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
    ResponseEntity<Page<InquiryListResponse>> getMyInquiries(AuthenticatedUser user, Pageable pageable);

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
    ResponseEntity<InquiryResponse> getMyInquiry(
            @Parameter(description = "문의 ID", required = true) Long id,
            AuthenticatedUser user
    );

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
    ResponseEntity<Page<InquiryListResponse>> getAllInquiries(InquiryType type, InquiryStatus status, Pageable pageable);

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
    ResponseEntity<InquiryDetailResponse> getInquiryDetail(
            @Parameter(description = "문의 ID", required = true) Long id
    );

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
    ResponseEntity<Void> updateInquiryStatus(
            @Parameter(description = "문의 ID", required = true) Long id,
            UpdateInquiryStatusRequest request
    );

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
    ResponseEntity<InquiryReplyResponse> createReply(
            @Parameter(description = "문의 ID", required = true) Long id,
            CreateInquiryReplyRequest request,
            AuthenticatedUser user
    );

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
    ResponseEntity<InquiryReplyResponse> updateReply(
            @Parameter(description = "문의 ID", required = true) Long id,
            UpdateInquiryReplyRequest request,
            AuthenticatedUser user
    );

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
    ResponseEntity<InquiryMemoResponse> createMemo(
            @Parameter(description = "문의 ID", required = true) Long id,
            CreateInquiryMemoRequest request,
            AuthenticatedUser user
    );

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
    ResponseEntity<Void> deleteInquiry(
            @Parameter(description = "문의 ID", required = true) Long id,
            AuthenticatedUser user
    );
}
