package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.InvalidCustomFieldException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.TemporaryStudentIdSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.exception.TempStudentIdNotAvailableException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.service.TempStudentIdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 임시 학번 회원가입 서비스.
 * 1~2월에 1학년 신입생이 임시 학번으로 회원가입할 수 있도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TempStudentIdSignupService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final String PRIVACY_POLICY_VERSION = "1.0";

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final TempStudentIdGeneratorService tempStudentIdGeneratorService;
    private final Clock clock;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

    /**
     * 임시 학번으로 회원가입을 처리합니다.
     *
     * @param request 임시 학번 회원가입 요청 정보
     * @return 회원가입 응답 (임시 학번 포함)
     */
    public PasswordSignupResponse signup(TemporaryStudentIdSignupRequest request) {
        log.info("임시 학번 회원가입 요청: email={}", request.email());

        // 1~2월 기간 검증
        validateEnrollmentPeriod();

        // 중복 검증 (이메일, 전화번호)
        validateDuplicates(request);

        // OTHER 교차 검증
        validateOtherFields(request);

        // 임시 학번 생성
        String tempStudentId = tempStudentIdGeneratorService.generateTempStudentId();

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // User 엔티티 생성 (임시 학번)
        User user = User.createWithTemporaryStudentId(
                tempStudentId,
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.department(),
                request.motivation(),
                request.wishes(),
                request.gender(),
                request.grade(),
                request.enrollmentStatus(),
                request.interests(),
                request.customInterest(),
                request.joinRoute(),
                request.customJoinRoute()
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

        // 인증 이메일 발송
        authEmailService.sendVerificationEmail(request.email(), verificationCode);

        // 임시 학번 안내 이메일 발송
        authEmailService.sendTemporaryStudentIdEmail(request.email(), request.name(), tempStudentId);

        log.info("임시 학번 회원가입 완료: email={}, tempStudentId={}", request.email(), tempStudentId);

        return PasswordSignupResponse.pendingVerificationWithTempId(request.email(), tempStudentId);
    }

    private void validateEnrollmentPeriod() {
        int month = LocalDate.now(clock).getMonthValue();
        if (month != 1 && month != 2) {
            throw new TempStudentIdNotAvailableException();
        }
    }

    private void validateDuplicates(TemporaryStudentIdSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicatePhoneNumberException();
        }
    }

    private void validateOtherFields(TemporaryStudentIdSignupRequest request) {
        if (request.interests() != null
                && request.interests().contains(Interest.OTHER)
                && (request.customInterest() == null || request.customInterest().isBlank())) {
            throw new InvalidCustomFieldException();
        }
        if (request.joinRoute() == JoinRoute.OTHER
                && (request.customJoinRoute() == null || request.customJoinRoute().isBlank())) {
            throw new InvalidCustomFieldException();
        }
    }
}
