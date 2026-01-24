package igrus.web.security.auth.password.service;

import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.common.service.EmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.repository.PasswordResetTokenRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mail.password-reset-expiry}")
    private long passwordResetExpiry;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * 비밀번호 복잡도 정규식
     * - 최소 8자 이상
     * - 영문 대문자 1개 이상
     * - 영문 소문자 1개 이상
     * - 숫자 1개 이상
     * - 특수문자 1개 이상
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    /**
     * 비밀번호 재설정 요청을 처리합니다.
     * 보안을 위해 존재하지 않는 학번도 동일한 응답을 반환합니다.
     *
     * @param studentId 학번
     */
    public void requestPasswordReset(String studentId) {
        log.info("비밀번호 재설정 요청: studentId={}", studentId);

        Optional<User> userOptional = userRepository.findByStudentId(studentId);

        if (userOptional.isEmpty()) {
            log.info("비밀번호 재설정 요청 - 존재하지 않는 학번: studentId={}", studentId);
            // 보안상 존재하지 않는 학번도 동일한 응답을 반환 (이메일 발송하지 않음)
            return;
        }

        User user = userOptional.get();

        // 기존 미사용 토큰 모두 무효화
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(user, token, passwordResetExpiry);
        passwordResetTokenRepository.save(resetToken);

        // 재설정 링크 이메일 발송
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        log.info("비밀번호 재설정 이메일 발송 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * 비밀번호를 재설정합니다.
     *
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     * @throws PasswordResetTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws PasswordResetTokenExpiredException 토큰이 만료된 경우
     * @throws InvalidPasswordFormatException 비밀번호 형식이 올바르지 않은 경우
     */
    public void resetPassword(String token, String newPassword) {
        log.info("비밀번호 재설정 시도");

        // 1. 토큰 유효성 검증
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> {
                    log.warn("비밀번호 재설정 실패 - 유효하지 않은 토큰");
                    return new PasswordResetTokenInvalidException();
                });

        if (resetToken.isExpired()) {
            log.warn("비밀번호 재설정 실패 - 만료된 토큰: userId={}", resetToken.getUser().getId());
            throw new PasswordResetTokenExpiredException();
        }

        // 2. 비밀번호 복잡도 검증
        validatePasswordFormat(newPassword);

        // 3. 비밀번호 변경
        User user = resetToken.getUser();
        PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.error("비밀번호 재설정 실패 - 자격 증명 없음: userId={}", user.getId());
                    return new PasswordResetTokenInvalidException();
                });

        String newPasswordHash = passwordEncoder.encode(newPassword);
        credential.changePassword(newPasswordHash);

        // 4. 토큰 사용 처리
        resetToken.markAsUsed();

        // 5. 모든 기존 토큰 무효화 (비밀번호 재설정 토큰 + 리프레시 토큰)
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("비밀번호 재설정 완료: userId={}", user.getId());
    }

    /**
     * 재설정 토큰의 유효성을 검증합니다.
     *
     * @param token 검증할 토큰
     * @return 토큰이 유효하면 true
     * @throws PasswordResetTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws PasswordResetTokenExpiredException 토큰이 만료된 경우
     */
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        log.info("비밀번호 재설정 토큰 검증");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> {
                    log.warn("토큰 검증 실패 - 유효하지 않은 토큰");
                    return new PasswordResetTokenInvalidException();
                });

        if (resetToken.isExpired()) {
            log.warn("토큰 검증 실패 - 만료된 토큰");
            throw new PasswordResetTokenExpiredException();
        }

        log.info("토큰 검증 성공");
        return true;
    }

    /**
     * 비밀번호 형식을 검증합니다.
     *
     * @param password 검증할 비밀번호
     * @throws InvalidPasswordFormatException 비밀번호 형식이 올바르지 않은 경우
     */
    public void validatePasswordFormat(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            log.warn("비밀번호 형식 검증 실패");
            throw new InvalidPasswordFormatException();
        }
    }
}
