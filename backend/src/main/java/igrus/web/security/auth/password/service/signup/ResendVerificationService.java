package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
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
public class ResendVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthEmailService authEmailService;
    private final VerificationCodeGenerator verificationCodeGenerator;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

    @Value("${app.mail.resend-rate-limit-seconds:300}")
    private long resendRateLimitSeconds;

    /**
     * 인증 코드를 재발송합니다.
     *
     * @param request 재발송 요청 정보
     * @return 재발송 완료 응답
     * @throws VerificationResendRateLimitedException 5분 내 재발송 요청 시
     */
    public VerificationResendResponse resendVerification(ResendVerificationRequest request) {
        log.info("인증 코드 재발송 요청: email={}", request.email());

        // Rate Limiting 체크: 5분 내 재발송 기록 확인
        Instant cutoffTime = Instant.now().minusSeconds(resendRateLimitSeconds);
        if (emailVerificationRepository.existsByEmailAndVerifiedFalseAndCreatedAtAfter(
                request.email(), cutoffTime)) {
            log.warn("인증 코드 재발송 Rate Limit 초과: email={}", request.email());
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

        log.info("인증 코드 재발송 완료: email={}", request.email());

        return VerificationResendResponse.success(request.email());
    }
}
