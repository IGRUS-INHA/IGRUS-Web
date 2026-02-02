package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.GuestInquiry;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryInvalidPasswordException;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비회원 문의 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LookupGuestInquiryService {

    private final GuestInquiryRepository guestInquiryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비회원 문의를 조회합니다.
     *
     * @param request 비회원 문의 조회 요청 (문의번호, 이메일, 비밀번호)
     * @return 문의 응답
     */
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
}
