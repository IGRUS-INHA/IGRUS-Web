package igrus.web.security.auth.password.service.presignup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.EmailVerificationAttemptService;
import igrus.web.security.auth.password.dto.response.PreSignupVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreSignupVerifyCodeService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationAttemptService emailVerificationAttemptService;

    @Value("${app.mail.verification-max-attempts}")
    private int maxAttempts;

    /**
     * 사전 이메일 인증 코드를 확인합니다.
     *
     * @param request 이메일과 인증 코드가 포함된 요청
     * @return 인증 완료 응답
     * @throws VerificationCodeInvalidException 인증 코드가 잘못되었거나 존재하지 않는 경우
     * @throws VerificationCodeExpiredException 인증 코드가 만료된 경우
     * @throws VerificationAttemptsExceededException 인증 시도 횟수를 초과한 경우
     */
    public PreSignupVerificationResponse verifyCode(EmailVerificationRequest request) {
        log.info("사전 인증 코드 확인 요청: email={}", request.email());

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndVerifiedFalse(request.email())
                .orElseThrow(VerificationCodeInvalidException::new);

        // 만료 확인
        if (verification.isExpired()) {
            throw new VerificationCodeExpiredException();
        }

        // 시도 횟수 확인
        if (!verification.canAttempt(maxAttempts)) {
            throw new VerificationAttemptsExceededException();
        }

        // 인증 코드 확인 (Timing Attack 방지를 위해 MessageDigest.isEqual 사용)
        if (!MessageDigest.isEqual(
                verification.getCode().getBytes(StandardCharsets.UTF_8),
                request.code().getBytes(StandardCharsets.UTF_8))) {
            // 별도 트랜잭션으로 시도 횟수 증가 (롤백되지 않도록)
            emailVerificationAttemptService.incrementAttempts(verification.getId());
            throw new VerificationCodeInvalidException();
        }

        // 인증 완료 처리 및 소유권 토큰 발급
        String verificationToken = verification.verify();
        emailVerificationRepository.save(verification);

        log.info("사전 인증 코드 확인 완료: email={}", request.email());

        return PreSignupVerificationResponse.success(request.email(), verificationToken);
    }
}
