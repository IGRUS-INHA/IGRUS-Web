package igrus.web.inquiry.controller;

import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.LookupGuestInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 비회원 문의 컨트롤러.
 * 공개 API (인증 불필요).
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의 API")
public class GuestInquiryController {

    private final CreateGuestInquiryService createGuestInquiryService;
    private final LookupGuestInquiryService lookupGuestInquiryService;

    @Operation(
            summary = "비회원 문의 작성",
            description = "비회원이 문의를 작성합니다. 이메일, 이름, 비밀번호가 필수입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "문의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    })
    @PostMapping("/guest")
    public ResponseEntity<InquiryCreateResponse> createGuestInquiry(
            @Valid @RequestBody CreateGuestInquiryRequest request
    ) {
        InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);
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
        InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(request);
        return ResponseEntity.ok(response);
    }
}
