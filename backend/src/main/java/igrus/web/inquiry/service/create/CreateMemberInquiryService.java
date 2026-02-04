package igrus.web.inquiry.service.create;

import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.exception.InquiryNumberGenerationException;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.inquiry.service.support.InquiryAttachmentHelper;
import igrus.web.inquiry.service.support.InquiryNotificationService;
import igrus.web.inquiry.service.support.InquiryNumberGenerator;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 문의 생성 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateMemberInquiryService {

    private final MemberInquiryRepository memberInquiryRepository;
    private final UserRepository userRepository;
    private final InquiryNumberGenerator inquiryNumberGenerator;
    private final InquiryNotificationService inquiryNotificationService;
    private final InquiryAttachmentHelper inquiryAttachmentHelper;

    private static final int MAX_INQUIRY_NUMBER_RETRIES = 3;

    /**
     * 회원 문의를 생성합니다.
     *
     * @param request 회원 문의 생성 요청
     * @param userId 사용자 ID
     * @return 생성된 문의 응답
     */
    public InquiryCreateResponse createMemberInquiry(CreateMemberInquiryRequest request, Long userId) {
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

                inquiryAttachmentHelper.addAttachments(inquiry, request.getAttachments());

                MemberInquiry saved = memberInquiryRepository.save(inquiry);

                inquiryNotificationService.sendInquiryConfirmation(
                        user.getEmail(),
                        inquiryNumber,
                        request.getTitle()
                );

                log.info("회원 문의 생성: inquiryNumber={}, userId={}", inquiryNumber, userId);

                return InquiryCreateResponse.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("문의 번호 중복 발생, 재시도 중: attempt={}", attempt + 1);
                lastException = e;
            }
        }

        throw new InquiryNumberGenerationException(lastException);
    }
}
