package igrus.web.security.auth.password.service.presignup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreSignupSendCodeService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final AuthEmailService authEmailService;
    private final VerificationCodeGenerator verificationCodeGenerator;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

    @Value("${app.mail.resend-rate-limit-seconds:60}")
    private long resendRateLimitSeconds;

    /**
     * 사전 이메일 인증 코드를 발송합니다.
     *
     * @param request 이메일 주소가 포함된 요청
     * @return 발송 완료 응답
     * @throws DuplicateEmailException 이미 가입된(ACTIVE) 이메일인 경우
     * @throws VerificationResendRateLimitedException 1분 내 재발송 요청 시
     */
    public VerificationResendResponse sendCode(ResendVerificationRequest request) {
        log.info("사전 인증 코드 발송 요청: email={}", request.email());

        // 이미 ACTIVE 상태인 사용자가 있으면 중복 가입 방지
        if (userRepository.existsByEmailAndStatus(request.email(), UserStatus.ACTIVE)) {
            log.warn("이미 가입된 이메일로 사전 인증 시도: email={}", request.email());
            throw new DuplicateEmailException();
        }

        // Rate Limiting 체크: 지정 시간 내 재발송 기록 확인
        Instant cutoffTime = Instant.now().minusSeconds(resendRateLimitSeconds);
        if (emailVerificationRepository.existsByEmailAndVerifiedFalseAndCreatedAtAfter(
                request.email(), cutoffTime)) {
            log.warn("사전 인증 코드 발송 Rate Limit 초과: email={}", request.email());
            throw new VerificationResendRateLimitedException();
        }

        // 기존 미인증 이메일 인증 레코드 삭제
        emailVerificationRepository.findByEmailAndVerifiedFalse(request.email())
                .ifPresent(emailVerificationRepository::delete);

        // 새 인증 코드 생성 및 저장
        String verificationCode = verificationCodeGenerator.generateVerificationCode();
        EmailVerification emailVerification = EmailVerification.create(
                request.email(),
                verificationCode,
                verificationCodeExpiry
        );
        emailVerificationRepository.save(emailVerification);

        // 이메일 발송 (비동기, 재시도 포함)
        authEmailService.sendVerificationEmail(request.email(), verificationCode);

        log.info("사전 인증 코드 발송 완료: email={}", request.email());

        return VerificationResendResponse.sent(request.email());
    }
}
