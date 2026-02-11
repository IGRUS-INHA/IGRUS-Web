package igrus.web.inquiry.service.create;

import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.exception.InquiryNumberGenerationException;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.inquiry.service.support.InquiryAttachmentHelper;
import igrus.web.inquiry.service.support.InquiryNotificationService;
import igrus.web.inquiry.service.support.InquiryPersistenceExecutor;
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
    private final InquiryPersistenceExecutor inquiryPersistenceExecutor;
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
                MemberInquiry saved = inquiryPersistenceExecutor.persistInquiry(
                        inquiryNumber -> {
                            MemberInquiry inquiry = MemberInquiry.create(
                                    inquiryNumber,
                                    request.getType(),
                                    request.getTitle(),
                                    request.getContent(),
                                    user
                            );
                            inquiryAttachmentHelper.addAttachments(inquiry, request.getAttachments());
                            return inquiry;
                        },
                        memberInquiryRepository
                );

                log.info("회원 문의 생성: inquiryNumber={}, userId={}", saved.getInquiryNumber(), userId);

                try {
                    inquiryNotificationService.sendInquiryConfirmation(
                            user.getEmail(),
                            saved.getInquiryNumber(),
                            request.getTitle()
                    );
                } catch (Exception e) {
                    log.error("문의 접수 확인 이메일 발송 실패: inquiryNumber={}, userId={}", saved.getInquiryNumber(), userId, e);
                }

                return InquiryCreateResponse.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("문의 번호 중복 발생, 재시도 중: attempt={}", attempt + 1);
                lastException = e;
            }
        }

        throw new InquiryNumberGenerationException(lastException);
    }
}
