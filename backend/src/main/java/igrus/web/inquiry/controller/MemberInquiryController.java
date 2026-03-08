package igrus.web.inquiry.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.AttachmentResponse;
import igrus.web.inquiry.dto.response.InquiryListPageResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.inquiry.service.read.GetMyInquiriesService;
import igrus.web.inquiry.service.read.GetMyInquiryService;
import igrus.web.generated.api.MemberInquiryApi;
import igrus.web.generated.model.ApiInquiryCreateResponse;
import igrus.web.generated.model.ApiInquiryListPageResponse;
import igrus.web.generated.model.ApiInquiryListResponse;
import igrus.web.generated.model.ApiInquiryResponse;
import igrus.web.generated.model.ApiAttachmentResponse;
import igrus.web.generated.model.ApiInquiryReplyResponse;
import igrus.web.generated.model.ApiCreateMemberInquiryRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원 문의 컨트롤러.
 * 인증된 사용자 전용 API.
 */
@RestController
@RequiredArgsConstructor
public class MemberInquiryController implements MemberInquiryApi {

    private final CreateMemberInquiryService createMemberInquiryService;
    private final GetMyInquiriesService getMyInquiriesService;
    private final GetMyInquiryService getMyInquiryService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiInquiryCreateResponse> createMemberInquiry(
            ApiCreateMemberInquiryRequest createMemberInquiryRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();

        var internalRequest = CreateMemberInquiryRequest.builder()
                .type(EnumUtils.fromStringOrNull(InquiryType.class, createMemberInquiryRequest.getType().getValue()))
                .title(createMemberInquiryRequest.getTitle())
                .content(createMemberInquiryRequest.getContent())
                .attachments(createMemberInquiryRequest.getAttachments() != null
                        ? createMemberInquiryRequest.getAttachments().stream()
                                .map(a -> AttachmentInfo.builder()
                                        .fileUrl(a.getFileUrl())
                                        .fileName(a.getFileName())
                                        .fileSize(a.getFileSize())
                                        .build())
                                .toList()
                        : null)
                .build();

        var response = createMemberInquiryService.createMemberInquiry(
                internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiInquiryCreateResponse()
                .id(response.getId())
                .inquiryNumber(response.getInquiryNumber())
                .message(response.getMessage()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiInquiryListPageResponse> getMyInquiries(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        var responsePage = getMyInquiriesService.getMyInquiries(
                user.userId(), pageable);
        var pageResponse = InquiryListPageResponse.from(responsePage);

        ApiInquiryListPageResponse result = new ApiInquiryListPageResponse()
                .inquiries(pageResponse.inquiries().stream()
                        .map(this::mapToInquiriesInner)
                        .toList())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .currentPage(pageResponse.currentPage())
                .hasNext(pageResponse.hasNext());
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiInquiryResponse> getMyInquiry(Long id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var response = getMyInquiryService.getMyInquiry(id, user.userId());
        return ResponseEntity.ok(mapToInquiryResponse(response));
    }

    // ===== 매핑 헬퍼 =====

    private ApiInquiryListResponse mapToInquiriesInner(InquiryListResponse r) {
        return new ApiInquiryListResponse()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? ApiInquiryListResponse.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? ApiInquiryListResponse.StatusEnum.fromValue(r.getStatus().name())
                        : null)
                .statusDescription(r.getStatusDescription())
                .title(r.getTitle())
                .authorName(r.getAuthorName())
                .guest(r.isGuest())
                .hasReply(r.isHasReply())
                .attachmentCount(r.getAttachmentCount())
                .createdAt(r.getCreatedAt());
    }

    private ApiInquiryResponse mapToInquiryResponse(InquiryResponse r) {
        return new ApiInquiryResponse()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? ApiInquiryResponse.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? ApiInquiryResponse.StatusEnum.fromValue(r.getStatus().name())
                        : null)
                .statusDescription(r.getStatusDescription())
                .title(r.getTitle())
                .content(r.getContent())
                .authorName(r.getAuthorName())
                .authorEmail(r.getAuthorEmail())
                .guest(r.isGuest())
                .attachments(r.getAttachments() != null
                        ? r.getAttachments().stream()
                                .map(this::mapToAttachment)
                                .toList()
                        : List.of())
                .reply(r.getReply() != null ? mapToReply(r.getReply()) : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt());
    }

    private ApiAttachmentResponse mapToAttachment(AttachmentResponse a) {
        return new ApiAttachmentResponse()
                .id(a.getId())
                .fileUrl(a.getFileUrl())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize());
    }

    private ApiInquiryReplyResponse mapToReply(InquiryReplyResponse r) {
        return new ApiInquiryReplyResponse()
                .id(r.getId())
                .content(r.getContent())
                .repliedByName(r.getRepliedByName())
                .createdAt(r.getCreatedAt());
    }
}
