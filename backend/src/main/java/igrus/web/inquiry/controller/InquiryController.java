package igrus.web.inquiry.controller;

import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.inquiry.service.InquiryService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
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
public class InquiryController implements InquiryControllerApi {

    private final InquiryService inquiryService;

    // ==================== 공개 API ====================

    @Override
    @PostMapping("/guest")
    public ResponseEntity<CreateInquiryResponse> createGuestInquiry(
            @Valid @RequestBody CreateGuestInquiryRequest request
    ) {
        CreateInquiryResponse response = inquiryService.createGuestInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/member")
    public ResponseEntity<CreateInquiryResponse> createMemberInquiry(
            @Valid @RequestBody CreateMemberInquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreateInquiryResponse response = inquiryService.createMemberInquiry(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/lookup")
    public ResponseEntity<InquiryResponse> lookupGuestInquiry(
            @Valid @RequestBody GuestInquiryLookupRequest request
    ) {
        InquiryResponse response = inquiryService.lookupGuestInquiry(request);
        return ResponseEntity.ok(response);
    }

    // ==================== 회원 전용 API ====================

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryListResponse>> getMyInquiries(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InquiryListResponse> response = inquiryService.getMyInquiries(user.userId(), pageable);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my/{id}")
    public ResponseEntity<InquiryResponse> getMyInquiry(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryResponse response = inquiryService.getMyInquiry(id, user.userId());
        return ResponseEntity.ok(response);
    }

    // ==================== 관리자 전용 API ====================

    @Override
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

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<InquiryDetailResponse> getInquiryDetail(
            @PathVariable Long id
    ) {
        InquiryDetailResponse response = inquiryService.getInquiryDetail(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateInquiryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInquiryStatusRequest request
    ) {
        inquiryService.updateInquiryStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{id}/reply")
    public ResponseEntity<InquiryReplyResponse> createReply(
            @PathVariable Long id,
            @Valid @RequestBody CreateInquiryReplyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryReplyResponse response = inquiryService.createReply(id, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PutMapping("/{id}/reply")
    public ResponseEntity<InquiryReplyResponse> updateReply(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInquiryReplyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryReplyResponse response = inquiryService.updateReply(id, request, user.userId());
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{id}/memo")
    public ResponseEntity<InquiryMemoResponse> createMemo(
            @PathVariable Long id,
            @Valid @RequestBody CreateInquiryMemoRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InquiryMemoResponse response = inquiryService.createMemo(id, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        inquiryService.deleteInquiry(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
