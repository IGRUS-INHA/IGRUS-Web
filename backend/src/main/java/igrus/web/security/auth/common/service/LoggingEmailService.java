package igrus.web.security.auth.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 로컬 환경용 이메일 서비스 구현체.
 * 실제 이메일을 발송하지 않고 로그로 출력합니다.
 */
@Slf4j
@Service
@Profile({"local", "test"})
public class LoggingEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String to, String code) {
        log.info("===== [로컬] 인증 코드 이메일 =====");
        log.info("수신자: {}", to);
        log.info("인증 코드: {}", code);
        log.info("================================");
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("===== [로컬] 비밀번호 재설정 이메일 =====");
        log.info("수신자: {}", to);
        log.info("재설정 링크: {}", resetLink);
        log.info("======================================");
    }

    @Override
    public void sendWelcomeEmail(String to, String name) {
        log.info("===== [로컬] 환영 이메일 =====");
        log.info("수신자: {}", to);
        log.info("이름: {}", name);
        log.info("============================");
    }
}