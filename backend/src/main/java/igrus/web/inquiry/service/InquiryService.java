package igrus.web.inquiry.service;

import igrus.web.inquiry.domain.*;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.inquiry.exception.*;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberInquiryRepository memberInquiryRepository;
    private final GuestInquiryRepository guestInquiryRepository;
    private final UserRepository userRepository;
    private final InquiryNumberGenerator inquiryNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final InquiryNotificationService inquiryNotificationService;

    private static final int MAX_INQUIRY_NUMBER_RETRIES = 3;

    // === 문의 생성 (비회원) ===
    public CreateInquiryResponse createGuestInquiry(CreateGuestInquiryRequest request) {
        String passwordHash = passwordEncoder.encode(request.getPassword());

        DataIntegrityViolationException lastException = null;
        for (int attempt = 0; attempt < MAX_INQUIRY_NUMBER_RETRIES; attempt++) {
            try {
                String inquiryNumber = inquiryNumberGenerator.generate();

                GuestInquiry inquiry = GuestInquiry.create(
                        inquiryNumber,
                        request.getType(),
                        request.getTitle(),
                        request.getContent(),
                        request.getEmail(),
                        request.getName(),
                        passwordHash
                );

                addAttachments(inquiry, request.getAttachments());

                GuestInquiry saved = guestInquiryRepository.save(inquiry);

                inquiryNotificationService.sendInquiryConfirmation(
                        request.getEmail(),
                        inquiryNumber,
                        request.getTitle()
                );

                log.info("비회원 문의 생성: inquiryNumber={}, email={}", inquiryNumber, request.getEmail());

                return CreateInquiryResponse.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("문의 번호 중복 발생, 재시도 중: attempt={}", attempt + 1);
                lastException = e;
            }
        }

        throw new InquiryNumberGenerationException(lastException);
    }

    // === 문의 생성 (회원) ===
    public CreateInquiryResponse createMemberInquiry(CreateMemberInquiryRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        DataIntegrityViolationException lastException = null;
        for (int attempt = 0; attempt < MAX_INQUIRY_NUMBER_RETRIES; attempt++) {
            try {
                String inquiryNumber = inquiryNumberGenerator.generate();

                MemberInquiry inquiry = MemberInquiry.create(
                        inquiryNumber,
                        request.getType(),
                        request.getTitle(),
                        request.getContent(),
                        user
                );

                addAttachments(inquiry, request.getAttachments());

                MemberInquiry saved = memberInquiryRepository.save(inquiry);

                inquiryNotificationService.sendInquiryConfirmation(
                        user.getEmail(),
                        inquiryNumber,
                        request.getTitle()
                );

                log.info("회원 문의 생성: inquiryNumber={}, userId={}", inquiryNumber, userId);

                return CreateInquiryResponse.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("문의 번호 중복 발생, 재시도 중: attempt={}", attempt + 1);
                lastException = e;
            }
        }

        throw new InquiryNumberGenerationException(lastException);
    }

    // === 비회원 문의 조회 ===
    @Transactional(readOnly = true)
    public InquiryResponse lookupGuestInquiry(GuestInquiryLookupRequest request) {
        GuestInquiry inquiry = guestInquiryRepository.findByInquiryNumberAndEmail(
                        request.getInquiryNumber(),
                        request.getEmail())
                .orElseThrow(() -> new InquiryNotFoundException(request.getInquiryNumber()));

        if (!passwordEncoder.matches(request.getPassword(), inquiry.getPasswordHash())) {
            throw new InquiryInvalidPasswordException();
        }

        return InquiryResponse.from(inquiry);
    }

    // === 내 문의 목록 조회 (회원) ===
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getMyInquiries(Long userId, Pageable pageable) {
        return memberInquiryRepository.findByUserId(userId, pageable)
                .map(InquiryListResponse::from);
    }

    // === 내 문의 상세 조회 (회원) ===
    @Transactional(readOnly = true)
    public InquiryResponse getMyInquiry(Long inquiryId, Long userId) {
        MemberInquiry inquiry = memberInquiryRepository.findByIdAndUserId(inquiryId, userId)
                .orElseThrow(InquiryAccessDeniedException::new);

        return InquiryResponse.from(inquiry);
    }

    // === 전체 문의 목록 조회 (관리자) ===
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getAllInquiries(InquiryType type, InquiryStatus status, Pageable pageable) {
        return inquiryRepository.findByFilters(type, status, pageable)
                .map(InquiryListResponse::from);
    }

    // === 문의 상세 조회 (관리자) ===
    @Transactional(readOnly = true)
    public InquiryDetailResponse getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByIdWithAllRelations(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
        return InquiryDetailResponse.from(inquiry);
    }

    // === 문의 상태 변경 (관리자) ===
    public void updateInquiryStatus(Long inquiryId, UpdateInquiryStatusRequest request) {
        Inquiry inquiry = findInquiryById(inquiryId);
        inquiry.changeStatus(request.getStatus());

        log.info("문의 상태 변경: inquiryId={}, newStatus={}", inquiryId, request.getStatus());
    }

    // === 답변 작성 (관리자) ===
    public InquiryReplyResponse createReply(Long inquiryId, CreateInquiryReplyRequest request, Long operatorId) {
        Inquiry inquiry = findInquiryById(inquiryId);

        if (inquiry.hasReply()) {
            throw new InquiryAlreadyRepliedException(inquiryId);
        }

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new UserNotFoundException(operatorId));

        InquiryReply reply = InquiryReply.create(request.getContent(), operator);
        inquiry.setReply(reply);
        inquiry.complete();

        inquiryNotificationService.sendReplyNotification(
                inquiry.getAuthorEmail(),
                inquiry.getInquiryNumber(),
                inquiry.getTitle(),
                request.getContent()
        );

        log.info("문의 답변 작성: inquiryId={}, operatorId={}", inquiryId, operatorId);

        return InquiryReplyResponse.from(reply);
    }

    // === 답변 수정 (관리자) ===
    public InquiryReplyResponse updateReply(Long inquiryId, UpdateInquiryReplyRequest request, Long operatorId) {
        Inquiry inquiry = findInquiryById(inquiryId);

        if (!inquiry.hasReply()) {
            throw new InquiryReplyNotFoundException(inquiryId);
        }

        inquiry.getReply().updateContent(request.getContent());

        log.info("문의 답변 수정: inquiryId={}, operatorId={}", inquiryId, operatorId);

        return InquiryReplyResponse.from(inquiry.getReply());
    }

    // === 내부 메모 작성 (관리자) ===
    public InquiryMemoResponse createMemo(Long inquiryId, CreateInquiryMemoRequest request, Long operatorId) {
        Inquiry inquiry = findInquiryById(inquiryId);

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new UserNotFoundException(operatorId));

        InquiryMemo memo = InquiryMemo.create(request.getContent(), operator);
        inquiry.addMemo(memo);

        log.info("문의 내부 메모 작성: inquiryId={}, operatorId={}", inquiryId, operatorId);

        return InquiryMemoResponse.from(memo);
    }

    // === 문의 삭제 (관리자) ===
    public void deleteInquiry(Long inquiryId, Long operatorId) {
        Inquiry inquiry = findInquiryById(inquiryId);
        inquiry.delete(operatorId);

        log.info("문의 삭제: inquiryId={}, deletedBy={}", inquiryId, operatorId);
    }

    // === Private 메서드 ===

    private Inquiry findInquiryById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
    }

    private void addAttachments(Inquiry inquiry, List<AttachmentInfo> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            for (AttachmentInfo attachmentInfo : attachments) {
                InquiryAttachment attachment = InquiryAttachment.create(
                        attachmentInfo.getFileUrl(),
                        attachmentInfo.getFileName(),
                        attachmentInfo.getFileSize()
                );
                inquiry.addAttachment(attachment);
            }
        }
    }
}
