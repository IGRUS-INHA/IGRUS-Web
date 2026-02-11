 package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.webhook.baebdungi.service.BaebdungiWebhookService;
import igrus.web.security.auth.common.service.EmailVerificationAttemptService;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
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
public class VerifyEmailService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationAttemptService emailVerificationAttemptService;
    private final BaebdungiWebhookService baebdungiWebhookService;

    @Value("${app.mail.verification-max-attempts}")
    private int maxAttempts;

    /**
     * 이메일 인증을 처리합니다.
     *
     * @param request 이메일 인증 요청 정보
     * @return 인증 완료 응답
     */
    public PasswordSignupResponse verifyEmail(EmailVerificationRequest request) {
        log.info("이메일 인증 요청: email={}", request.email());

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

        // 인증 완료 처리
        verification.verify();
        emailVerificationRepository.save(verification);

        // User와 PasswordCredential 상태를 ACTIVE로 변경
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(VerificationCodeInvalidException::new);
        user.verifyEmail();
        userRepository.save(user);

        PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId())
            .orElseThrow(VerificationCodeInvalidException::new);
        credential.verifyEmail();
        passwordCredentialRepository.save(credential);

        // 뱁둥이봇 웹훅 호출 (비동기, 실패해도 인증 프로세스에 영향 없음)
        baebdungiWebhookService.sendSubmission(user);

        log.info("이메일 인증 완료: email={}", request.email());

        return PasswordSignupResponse.verified(request.email());
    }
}
