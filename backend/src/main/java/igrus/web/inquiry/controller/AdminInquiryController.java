package igrus.web.inquiry.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.AttachmentResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.dto.response.InquiryListPageResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.dto.response.InquiryMemoResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.service.manage.CreateInquiryMemoService;
import igrus.web.inquiry.service.manage.CreateInquiryReplyService;
import igrus.web.inquiry.service.manage.DeleteInquiryService;
import igrus.web.inquiry.service.manage.UpdateInquiryReplyService;
import igrus.web.inquiry.service.manage.UpdateInquiryStatusService;
import igrus.web.inquiry.service.read.GetAllInquiriesService;
import igrus.web.inquiry.service.read.GetInquiryDetailService;
import igrus.web.generated.api.AdminInquiryApi;
import igrus.web.generated.model.CreateMemo201Response;
import igrus.web.generated.model.CreateMemoRequest;
import igrus.web.generated.model.CreateReplyRequest;
import igrus.web.generated.model.GetAllInquiries200Response;
import igrus.web.generated.model.GetAllInquiries200ResponseInquiriesInner;
import igrus.web.generated.model.GetInquiryDetail200Response;
import igrus.web.generated.model.LookupGuestInquiry200ResponseAttachmentsInner;
import igrus.web.generated.model.LookupGuestInquiry200ResponseReply;
import igrus.web.generated.model.UpdateReply200Response;
import igrus.web.generated.model.UpdateReplyRequest;
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
    public ResponseEntity<GetAllInquiries200Response> getAllInquiries(
            String type,
            String status,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        InquiryType typeEnum = EnumUtils.fromStringOrNull(InquiryType.class, type);
        InquiryStatus statusEnum = EnumUtils.fromStringOrNull(InquiryStatus.class, status);
        Pageable pageable = PageableUtils.of(page, size, sort);

        Page<InquiryListResponse> responsePage = getAllInquiriesService.getAllInquiries(
                typeEnum, statusEnum, pageable);
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<GetInquiryDetail200Response> getInquiryDetail(Long id) {
        InquiryDetailResponse response = getInquiryDetailService.getInquiryDetail(id);
        return ResponseEntity.ok(mapToInquiryDetail200Response(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> updateInquiryStatus(
            Long id,
            igrus.web.generated.model.UpdateInquiryStatusRequest updateInquiryStatusRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest internalRequest =
                igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest.builder()
                        .status(EnumUtils.fromStringOrNull(InquiryStatus.class, updateInquiryStatusRequest.getStatus().getValue()))
                        .build();
        updateInquiryStatusService.updateInquiryStatus(id, internalRequest, user.userId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<UpdateReply200Response> createReply(
            Long id,
            CreateReplyRequest createReplyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        CreateInquiryReplyRequest internalRequest = CreateInquiryReplyRequest.builder()
                .content(createReplyRequest.getContent())
                .build();
        InquiryReplyResponse response = createInquiryReplyService.createReply(
                id, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UpdateReply200Response()
                .id(response.getId())
                .content(response.getContent())
                .repliedByName(response.getRepliedByName())
                .createdAt(response.getCreatedAt()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<UpdateReply200Response> updateReply(
            Long id,
            UpdateReplyRequest updateReplyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        UpdateInquiryReplyRequest internalRequest = UpdateInquiryReplyRequest.builder()
                .content(updateReplyRequest.getContent())
                .build();
        InquiryReplyResponse response = updateInquiryReplyService.updateReply(
                id, internalRequest, user.userId());
        return ResponseEntity.ok(new UpdateReply200Response()
                .id(response.getId())
                .content(response.getContent())
                .repliedByName(response.getRepliedByName())
                .createdAt(response.getCreatedAt()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CreateMemo201Response> createMemo(
            Long id,
            CreateMemoRequest createMemoRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        CreateInquiryMemoRequest internalRequest = CreateInquiryMemoRequest.builder()
                .content(createMemoRequest.getContent())
                .build();
        InquiryMemoResponse response = createInquiryMemoService.createMemo(
                id, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateMemo201Response()
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

    private GetInquiryDetail200Response mapToInquiryDetail200Response(InquiryDetailResponse r) {
        return new GetInquiryDetail200Response()
                .id(r.getId())
                .inquiryNumber(r.getInquiryNumber())
                .type(r.getType() != null
                        ? GetInquiryDetail200Response.TypeEnum.fromValue(r.getType().name())
                        : null)
                .typeDescription(r.getTypeDescription())
                .status(r.getStatus() != null
                        ? GetInquiryDetail200Response.StatusEnum.fromValue(r.getStatus().name())
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

    private CreateMemo201Response mapToMemo(InquiryMemoResponse m) {
        return new CreateMemo201Response()
                .id(m.getId())
                .content(m.getContent())
                .writtenByName(m.getWrittenByName())
                .createdAt(m.getCreatedAt());
    }
}
