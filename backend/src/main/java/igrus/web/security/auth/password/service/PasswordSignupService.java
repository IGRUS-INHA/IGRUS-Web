package igrus.web.security.auth.password.service;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.common.service.EmailVerificationAttemptService;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordSignupService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final EmailVerificationAttemptService emailVerificationAttemptService;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

    @Value("${app.mail.verification-max-attempts}")
    private int maxAttempts;

    @Value("${app.mail.resend-rate-limit-seconds:300}")
    private long resendRateLimitSeconds;

    private static final String PRIVACY_POLICY_VERSION = "1.0";

    /**
     * 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청 정보
     * @return 회원가입 응답
     */
    public PasswordSignupResponse signup(PasswordSignupRequest request) {
        log.info("회원가입 요청: email={}", request.email());

        // 중복 검증
        validateDuplicates(request);

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // User 엔티티 생성 및 저장
        User user = User.create(
            request.studentId(),
            request.name(),
            request.email(),
            request.phoneNumber(),
            request.department(),
            request.motivation(),
            request.gender(),
            request.grade()
        );
        userRepository.save(user);

        // PasswordCredential 생성 및 저장
        PasswordCredential passwordCredential = PasswordCredential.create(user, encodedPassword);
        passwordCredentialRepository.save(passwordCredential);

        // PrivacyConsent 생성 및 저장
        PrivacyConsent privacyConsent = PrivacyConsent.create(user, PRIVACY_POLICY_VERSION);
        privacyConsentRepository.save(privacyConsent);

        // 기존 미인증 이메일 인증 레코드 삭제
        emailVerificationRepository.findByEmailAndVerifiedFalse(request.email())
            .ifPresent(emailVerificationRepository::delete);

        // 인증 코드 생성 및 저장
        String verificationCode = generateVerificationCode();
        EmailVerification emailVerification = EmailVerification.create(
            request.email(),
            verificationCode,
            verificationCodeExpiry
        );
        emailVerificationRepository.save(emailVerification);

        // 이메일 발송 (비동기, 재시도 포함)
        authEmailService.sendVerificationEmail(request.email(), verificationCode);

        log.info("회원가입 완료, 이메일 인증 대기: email={}", request.email());

        return PasswordSignupResponse.pendingVerification(request.email());
    }

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

        log.info("이메일 인증 완료: email={}", request.email());

        return PasswordSignupResponse.verified(request.email());
    }

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
        String verificationCode = generateVerificationCode();
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

    /**
     * 중복 검증을 수행합니다.
     */
    private void validateDuplicates(PasswordSignupRequest request) {
        if (userRepository.existsByStudentId(request.studentId())) {
            throw new DuplicateStudentIdException();
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicatePhoneNumberException();
        }
    }

    /**
     * 6자리 랜덤 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
