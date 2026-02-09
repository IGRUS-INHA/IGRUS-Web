package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.EmailVerificationAttemptService;
import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 이메일 변경 인증 확인 서비스.
 * 인증 코드를 검증한 후 이메일을 변경합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VerifyEmailChangeService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationAttemptService emailVerificationAttemptService;

    @Value("${app.mail.verification-max-attempts}")
    private int maxAttempts;

    /**
     * 인증 코드를 확인하고 이메일을 변경합니다.
     *
     * @param userId  사용자 ID
     * @param request 이메일 인증 요청 정보 (이메일 + 인증 코드)
     */
    public void verifyAndChangeEmail(Long userId, EmailVerificationRequest request) {
        log.info("이메일 변경 인증 확인 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 해당 사용자의 미인증 레코드 조회 (userId 바인딩 검증)
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndUserIdAndVerifiedFalse(request.email(), userId)
                .orElseThrow(VerificationCodeInvalidException::new);

        // 3. 만료 체크
        if (verification.isExpired()) {
            throw new VerificationCodeExpiredException();
        }

        // 4. 시도 횟수 체크
        if (!verification.canAttempt(maxAttempts)) {
            throw new VerificationAttemptsExceededException();
        }

        // 5. 코드 일치 확인 (타이밍 공격 방지)
        if (!MessageDigest.isEqual(
                verification.getCode().getBytes(StandardCharsets.UTF_8),
                request.code().getBytes(StandardCharsets.UTF_8))) {
            emailVerificationAttemptService.incrementAttempts(verification.getId());
            throw new VerificationCodeInvalidException();
        }

        // 6. 이메일 중복 재검증 (race condition 방지)
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        // 7. 인증 완료 & 이메일 변경
        verification.verify();
        user.updateEmail(request.email());

        log.info("이메일 변경 완료 - userId: {}", userId);
    }
}
