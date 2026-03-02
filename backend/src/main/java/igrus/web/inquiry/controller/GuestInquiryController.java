package igrus.web.inquiry.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.AttachmentResponse;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.LookupGuestInquiryService;
import igrus.web.generated.api.GuestInquiryApi;
import igrus.web.generated.model.CreateMemberInquiry201Response;
import igrus.web.generated.model.LookupGuestInquiry200Response;
import igrus.web.generated.model.LookupGuestInquiry200ResponseAttachmentsInner;
import igrus.web.generated.model.LookupGuestInquiry200ResponseReply;
import igrus.web.generated.model.LookupGuestInquiryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 비회원 문의 컨트롤러.
 * 공개 API (인증 불필요).
 */
@RestController
@RequiredArgsConstructor
public class GuestInquiryController implements GuestInquiryApi {

    private final CreateGuestInquiryService createGuestInquiryService;
    private final LookupGuestInquiryService lookupGuestInquiryService;

    @Override
    public ResponseEntity<CreateMemberInquiry201Response> createGuestInquiry(
            igrus.web.generated.model.CreateGuestInquiryRequest createGuestInquiryRequest
    ) {
        CreateGuestInquiryRequest internalRequest = CreateGuestInquiryRequest.builder()
                .type(EnumUtils.fromStringOrNull(InquiryType.class, createGuestInquiryRequest.getType().getValue()))
                .title(createGuestInquiryRequest.getTitle())
                .content(createGuestInquiryRequest.getContent())
                .email(createGuestInquiryRequest.getEmail())
                .name(createGuestInquiryRequest.getName())
                .password(createGuestInquiryRequest.getPassword())
                .attachments(createGuestInquiryRequest.getAttachments() != null
                        ? createGuestInquiryRequest.getAttachments().stream()
                                .map(a -> AttachmentInfo.builder()
                                        .fileUrl(a.getFileUrl())
                                        .fileName(a.getFileName())
                                        .fileSize(a.getFileSize())
                                        .build())
                                .toList()
                        : null)
                .build();

        InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(internalRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateMemberInquiry201Response()
                .id(response.getId())
                .inquiryNumber(response.getInquiryNumber())
                .message(response.getMessage()));
    }

    @Override
    public ResponseEntity<LookupGuestInquiry200Response> lookupGuestInquiry(
            LookupGuestInquiryRequest lookupGuestInquiryRequest
    ) {
        GuestInquiryLookupRequest internalRequest = GuestInquiryLookupRequest.builder()
                .inquiryNumber(lookupGuestInquiryRequest.getInquiryNumber())
                .email(lookupGuestInquiryRequest.getEmail())
                .password(lookupGuestInquiryRequest.getPassword())
                .build();

        InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(internalRequest);
        return ResponseEntity.ok(mapToLookupGuestInquiry200Response(response));
    }

    // ===== 매핑 헬퍼 =====

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
