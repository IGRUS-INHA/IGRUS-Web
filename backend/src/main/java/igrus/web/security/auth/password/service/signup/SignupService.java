package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final VerificationCodeGenerator verificationCodeGenerator;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

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
        String verificationCode = verificationCodeGenerator.generateVerificationCode();
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
     * 중복 검증을 수행합니다.
     */
    private void validateDuplicates(PasswordSignupRequest request) {
        if (userRepository.existsByStudentId(request.studentId())) {
            throw new DuplicateStudentIdException();
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        if (userRepository.existsByPhoneNumber(User.normalizePhoneNumber(request.phoneNumber()))) {
            throw new DuplicatePhoneNumberException();
        }
    }
}
