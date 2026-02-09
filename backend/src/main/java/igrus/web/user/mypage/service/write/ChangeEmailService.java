package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangeEmailRequest;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 변경 서비스.
 * 비밀번호를 확인한 후 새 이메일로 인증 코드를 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeEmailService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final VerificationCodeGenerator verificationCodeGenerator;

    @Value("${app.mail.verification-code-expiry}")
    private long verificationCodeExpiry;

    /**
     * 이메일 변경을 요청합니다.
     * 비밀번호 확인 → 현재 이메일과 동일 여부 → 중복 체크 → 인증 코드 발송
     *
     * @param userId  사용자 ID
     * @param request 이메일 변경 요청 정보
     */
    public void changeEmail(Long userId, ChangeEmailRequest request) {
        log.info("이메일 변경 요청 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 비밀번호 확인
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("비밀번호 불일치 - userId: {}", userId);
            throw new InvalidCredentialsException();
        }

        // 3. 현재 이메일과 동일한지 체크
        if (request.newEmail().equals(user.getEmail())) {
            log.warn("현재 이메일과 동일 - userId: {}", userId);
            throw new DuplicateEmailException(request.newEmail());
        }

        // 4. 새 이메일 중복 체크
        if (userRepository.existsByEmail(request.newEmail())) {
            log.warn("이메일 중복 - email: {}", request.newEmail());
            throw new DuplicateEmailException(request.newEmail());
        }

        // 5. 기존 미인증 레코드 삭제
        emailVerificationRepository.findByEmailAndVerifiedFalse(request.newEmail())
                .ifPresent(emailVerificationRepository::delete);

        // 6. 인증 코드 생성 & 저장
        String code = verificationCodeGenerator.generateVerificationCode();
        EmailVerification verification = EmailVerification.create(
                request.newEmail(), code, verificationCodeExpiry
        );
        emailVerificationRepository.save(verification);

        // 7. 인증 이메일 발송 (비동기)
        authEmailService.sendVerificationEmail(request.newEmail(), code);

        log.info("이메일 변경 인증 코드 발송 - userId: {}", userId);
    }
}
