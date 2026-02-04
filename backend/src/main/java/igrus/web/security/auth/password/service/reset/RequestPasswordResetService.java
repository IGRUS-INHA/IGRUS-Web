package igrus.web.security.auth.password.service.reset;

import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.repository.PasswordResetTokenRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 비밀번호 재설정 요청 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestPasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthEmailService authEmailService;

    @Value("${app.mail.password-reset-expiry}")
    private long passwordResetExpiry;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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

        // 재설정 링크 이메일 발송 (비동기, 재시도 포함)
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        authEmailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        log.info("비밀번호 재설정 이메일 발송 완료: userId={}, email={}", user.getId(), user.getEmail());
    }
}
