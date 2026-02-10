package igrus.web.inquiry.service.create;

import igrus.web.inquiry.domain.GuestInquiry;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.exception.InquiryNumberGenerationException;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import igrus.web.inquiry.service.support.InquiryAttachmentHelper;
import igrus.web.inquiry.service.support.InquiryNotificationService;
import igrus.web.inquiry.service.support.InquiryPersistenceExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비회원 문의 생성 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateGuestInquiryService {

    private final GuestInquiryRepository guestInquiryRepository;
    private final InquiryPersistenceExecutor inquiryPersistenceExecutor;
    private final PasswordEncoder passwordEncoder;
    private final InquiryNotificationService inquiryNotificationService;
    private final InquiryAttachmentHelper inquiryAttachmentHelper;

    private static final int MAX_INQUIRY_NUMBER_RETRIES = 3;

    /**
     * 비회원 문의를 생성합니다.
     *
     * @param request 비회원 문의 생성 요청
     * @return 생성된 문의 응답
     */
    public InquiryCreateResponse createGuestInquiry(CreateGuestInquiryRequest request) {
        String passwordHash = passwordEncoder.encode(request.getPassword());

        DataIntegrityViolationException lastException = null;
        for (int attempt = 0; attempt < MAX_INQUIRY_NUMBER_RETRIES; attempt++) {
            try {
                GuestInquiry saved = inquiryPersistenceExecutor.persistInquiry(
                        inquiryNumber -> {
                            GuestInquiry inquiry = GuestInquiry.create(
                                    inquiryNumber,
                                    request.getType(),
                                    request.getTitle(),
                                    request.getContent(),
                                    request.getEmail(),
                                    request.getName(),
                                    passwordHash
                            );
                            inquiryAttachmentHelper.addAttachments(inquiry, request.getAttachments());
                            return inquiry;
                        },
                        guestInquiryRepository
                );

                inquiryNotificationService.sendInquiryConfirmation(
                        request.getEmail(),
                        saved.getInquiryNumber(),
                        request.getTitle()
                );

                log.info("비회원 문의 생성: inquiryNumber={}, email={}", saved.getInquiryNumber(), request.getEmail());

                return InquiryCreateResponse.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("문의 번호 중복 발생, 재시도 중: attempt={}", attempt + 1);
                lastException = e;
            }
        }

        throw new InquiryNumberGenerationException(lastException);
    }
}
