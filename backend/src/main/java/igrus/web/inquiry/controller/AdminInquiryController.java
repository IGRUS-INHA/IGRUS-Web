package igrus.web.inquiry.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.service.manage.CreateInquiryMemoService;
import igrus.web.inquiry.service.manage.CreateInquiryReplyService;
import igrus.web.inquiry.service.manage.DeleteInquiryService;
import igrus.web.inquiry.service.manage.UpdateInquiryReplyService;
import igrus.web.inquiry.service.manage.UpdateInquiryStatusService;
import igrus.web.inquiry.service.read.GetAllInquiriesService;
import igrus.web.inquiry.service.read.GetInquiryDetailService;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.dto.response.AttachmentResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.dto.response.InquiryListPageResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.dto.response.InquiryMemoResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.generated.api.AdminInquiryApi;
import igrus.web.generated.model.ApiAttachmentResponse;
import igrus.web.generated.model.ApiCreateInquiryMemoRequest;
import igrus.web.generated.model.ApiCreateInquiryReplyRequest;
import igrus.web.generated.model.ApiInquiryDetailResponse;
import igrus.web.generated.model.ApiInquiryListPageResponse;
import igrus.web.generated.model.ApiInquiryListResponse;
import igrus.web.generated.model.ApiInquiryMemoResponse;
import igrus.web.generated.model.ApiInquiryReplyResponse;
import igrus.web.generated.model.ApiUpdateInquiryReplyRequest;
import igrus.web.generated.model.ApiUpdateInquiryStatusRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 문의 컨트롤러.
 * OPERATOR 또는 ADMIN 권한 필요.
 */
@RestController
@RequiredArgsConstructor
public class AdminInquiryController implements AdminInquiryApi {

    private final GetAllInquiriesService getAllInquiriesService;
    private final GetInquiryDetailService getInquiryDetailService;
    private final UpdateInquiryStatusService updateInquiryStatusService;
    private final CreateInquiryReplyService createInquiryReplyService;
    private final UpdateInquiryReplyService updateInquiryReplyService;
    private final CreateInquiryMemoService createInquiryMemoService;
    private final DeleteInquiryService deleteInquiryService;

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiInquiryListPageResponse> getAllInquiries(
            String type,
            String status,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        InquiryType typeEnum = EnumUtils.fromStringOrNull(InquiryType.class, type);
        InquiryStatus statusEnum = EnumUtils.fromStringOrNull(InquiryStatus.class, status);
        Pageable pageable = PageableUtils.of(page, size, sort);

        var responsePage = getAllInquiriesService.getAllInquiries(
                typeEnum, statusEnum, pageable);
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiInquiryDetailResponse> getInquiryDetail(Long id) {
        var response = getInquiryDetailService.getInquiryDetail(id);
        return ResponseEntity.ok(mapToInquiryDetail200Response(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> updateInquiryStatus(
            Long id,
            ApiUpdateInquiryStatusRequest updateInquiryStatusRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var internalRequest =
                UpdateInquiryStatusRequest.builder()
                        .status(EnumUtils.fromStringOrNull(InquiryStatus.class, updateInquiryStatusRequest.getStatus().getValue()))
                        .build();
        updateInquiryStatusService.updateInquiryStatus(id, internalRequest, user.userId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiInquiryReplyResponse> createReply(
            Long id,
            ApiCreateInquiryReplyRequest createInquiryReplyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var internalRequest = CreateInquiryReplyRequest.builder()
                .content(createInquiryReplyRequest.getContent())
                .build();
        var response = createInquiryReplyService.createReply(
                id, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiInquiryReplyResponse()
                .id(response.getId())
                .content(response.getContent())
                .repliedByName(response.getRepliedByName())
                .createdAt(response.getCreatedAt()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiInquiryReplyResponse> updateReply(
            Long id,
            ApiUpdateInquiryReplyRequest updateInquiryReplyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var internalRequest = UpdateInquiryReplyRequest.builder()
                .content(updateInquiryReplyRequest.getContent())
                .build();
        var response = updateInquiryReplyService.updateReply(
                id, internalRequest, user.userId());
        return ResponseEntity.ok(new ApiInquiryReplyResponse()
                .id(response.getId())
                .content(response.getContent())
                .repliedByName(response.getRepliedByName())
                .createdAt(response.getCreatedAt()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiInquiryMemoResponse> createMemo(
            Long id,
            ApiCreateInquiryMemoRequest createInquiryMemoRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var internalRequest = CreateInquiryMemoRequest.builder()
                .content(createInquiryMemoRequest.getContent())
                .build();
        var response = createInquiryMemoService.createMemo(
                id, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiInquiryMemoResponse()
                .id(response.getId())
                .content(response.getContent())
                .writtenByName(response.getWrittenByName())
                .createdAt(response.getCreatedAt()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteInquiry(Long id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        deleteInquiryService.deleteInquiry(id, user.userId());
        return ResponseEntity.noContent().build();
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

    private ApiInquiryDetailResponse mapToInquiryDetail200Response(InquiryDetailResponse r) {
        return new ApiInquiryDetailResponse()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? ApiInquiryDetailResponse.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? ApiInquiryDetailResponse.StatusEnum.fromValue(r.getStatus().name())
                        : null)
                .statusDescription(r.getStatusDescription())
                .title(r.getTitle())
                .content(r.getContent())
                .authorName(r.getAuthorName())
                .authorEmail(r.getAuthorEmail())
                .authorUserId(r.getAuthorUserId())
                .guest(r.isGuest())
                .attachments(r.getAttachments() != null
                        ? r.getAttachments().stream()
                                .map(this::mapToAttachment)
                                .toList()
                        : List.of())
                .reply(r.getReply() != null ? mapToReply(r.getReply()) : null)
                .memos(r.getMemos() != null
                        ? r.getMemos().stream()
                                .map(this::mapToMemo)
                                .toList()
                        : List.of())
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

    private ApiInquiryMemoResponse mapToMemo(InquiryMemoResponse m) {
        return new ApiInquiryMemoResponse()
                .id(m.getId())
                .content(m.getContent())
                .writtenByName(m.getWrittenByName())
                .createdAt(m.getCreatedAt());
    }
}
