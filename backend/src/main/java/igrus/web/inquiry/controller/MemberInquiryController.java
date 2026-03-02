package igrus.web.inquiry.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.AttachmentResponse;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListPageResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.inquiry.service.read.GetMyInquiriesService;
import igrus.web.inquiry.service.read.GetMyInquiryService;
import igrus.web.generated.api.MemberInquiryApi;
import igrus.web.generated.model.CreateMemberInquiry201Response;
import igrus.web.generated.model.GetAllInquiries200Response;
import igrus.web.generated.model.GetAllInquiries200ResponseInquiriesInner;
import igrus.web.generated.model.LookupGuestInquiry200Response;
import igrus.web.generated.model.LookupGuestInquiry200ResponseAttachmentsInner;
import igrus.web.generated.model.LookupGuestInquiry200ResponseReply;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<CreateMemberInquiry201Response> createMemberInquiry(
            igrus.web.generated.model.CreateMemberInquiryRequest createMemberInquiryRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();

        CreateMemberInquiryRequest internalRequest = CreateMemberInquiryRequest.builder()
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

        InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(
                internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateMemberInquiry201Response()
                .id(response.getId())
                .inquiryNumber(response.getInquiryNumber())
                .message(response.getMessage()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetAllInquiries200Response> getMyInquiries(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        Page<InquiryListResponse> responsePage = getMyInquiriesService.getMyInquiries(
                user.userId(), pageable);
        InquiryListPageResponse pageResponse = InquiryListPageResponse.from(responsePage);

        GetAllInquiries200Response result = new GetAllInquiries200Response()
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
    public ResponseEntity<LookupGuestInquiry200Response> getMyInquiry(Long id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        InquiryResponse response = getMyInquiryService.getMyInquiry(id, user.userId());
        return ResponseEntity.ok(mapToLookupGuestInquiry200Response(response));
    }

    // ===== 매핑 헬퍼 =====

    private GetAllInquiries200ResponseInquiriesInner mapToInquiriesInner(InquiryListResponse r) {
        return new GetAllInquiries200ResponseInquiriesInner()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? GetAllInquiries200ResponseInquiriesInner.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? GetAllInquiries200ResponseInquiriesInner.StatusEnum.fromValue(r.getStatus().name())
                        : null)
                .statusDescription(r.getStatusDescription())
                .title(r.getTitle())
                .authorName(r.getAuthorName())
                .guest(r.isGuest())
                .hasReply(r.isHasReply())
                .attachmentCount(r.getAttachmentCount())
                .createdAt(r.getCreatedAt());
    }

    private LookupGuestInquiry200Response mapToLookupGuestInquiry200Response(InquiryResponse r) {
        return new LookupGuestInquiry200Response()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? LookupGuestInquiry200Response.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? LookupGuestInquiry200Response.StatusEnum.fromValue(r.getStatus().name())
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

    private LookupGuestInquiry200ResponseAttachmentsInner mapToAttachment(AttachmentResponse a) {
        return new LookupGuestInquiry200ResponseAttachmentsInner()
                .id(a.getId())
                .fileUrl(a.getFileUrl())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize());
    }

    private LookupGuestInquiry200ResponseReply mapToReply(InquiryReplyResponse r) {
        return new LookupGuestInquiry200ResponseReply()
                .id(r.getId())
                .content(r.getContent())
                .repliedByName(r.getRepliedByName())
                .createdAt(r.getCreatedAt());
    }
}
