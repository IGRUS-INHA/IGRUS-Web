package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.exception.email.EmailSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SMTP를 통한 실제 이메일 발송 서비스.
 * 프로덕션 환경에서 사용됩니다.
 */
@Slf4j
@Service
@Profile("!local & !test")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[IGRUS] 이메일 인증 코드");
        message.setText(buildVerificationEmailContent(code));

        sendEmail(message);
        log.info("인증 코드 이메일 발송 완료: to={}", to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[IGRUS] 비밀번호 재설정");
        message.setText(buildPasswordResetEmailContent(resetLink));

        sendEmail(message);
        log.info("비밀번호 재설정 이메일 발송 완료: to={}", to);
    }

    @Override
    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[IGRUS] 가입을 환영합니다");
        message.setText(buildWelcomeEmailContent(name));

        sendEmail(message);
        log.info("환영 이메일 발송 완료: to={}", to);
    }

    @Async("emailTaskExecutor")
    @Retryable(
            retryFor = EmailSendFailedException.class,
            maxAttempts = 4,
            backoff = @Backoff(
                    delay = 60000,      // 1분
                    multiplier = 3,     // 1분 → 3분 → 9분
                    maxDelay = 900000   // 최대 15분
            )
    )
    @Override
    public void sendVerificationEmailWithRetry(String to, String code) {
        log.debug("인증 코드 이메일 발송 시도 (재시도 포함): to={}", to);
        sendVerificationEmail(to, code);
    }

    @Async("emailTaskExecutor")
    @Retryable(
            retryFor = EmailSendFailedException.class,
            maxAttempts = 4,
            backoff = @Backoff(
                    delay = 60000,
                    multiplier = 3,
                    maxDelay = 900000
            )
    )
    @Override
    public void sendPasswordResetEmailWithRetry(String to, String resetLink) {
        log.debug("비밀번호 재설정 이메일 발송 시도 (재시도 포함): to={}", to);
        sendPasswordResetEmail(to, resetLink);
    }

    @Async("emailTaskExecutor")
    @Retryable(
            retryFor = EmailSendFailedException.class,
            maxAttempts = 4,
            backoff = @Backoff(
                    delay = 60000,
                    multiplier = 3,
                    maxDelay = 900000
            )
    )
    @Override
    public void sendWelcomeEmailWithRetry(String to, String name) {
        log.debug("환영 이메일 발송 시도 (재시도 포함): to={}", to);
        sendWelcomeEmail(to, name);
    }

    /**
     * 인증 코드 이메일 재시도 소진 시 복구 메서드
     */
    @Recover
    public void recoverVerificationEmail(EmailSendFailedException e, String to, String code) {
        log.error("인증 코드 이메일 발송 최종 실패 (재시도 소진): to={}, code={}", to, code);
    }

    /**
     * 비밀번호 재설정 이메일 재시도 소진 시 복구 메서드
     */
    @Recover
    public void recoverPasswordResetEmail(EmailSendFailedException e, String to, String resetLink) {
        log.error("비밀번호 재설정 이메일 발송 최종 실패 (재시도 소진): to={}, resetLink={}", to, resetLink);
    }

    /**
     * 환영 이메일 재시도 소진 시 복구 메서드
     */
    @Recover
    public void recoverWelcomeEmail(EmailSendFailedException e, String to, String name) {
        log.error("환영 이메일 발송 최종 실패 (재시도 소진): to={}, name={}", to, name);
    }

    private void sendEmail(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("이메일 발송 실패: to={}, error={}", message.getTo(), e.getMessage());
            throw new EmailSendFailedException();
        }
    }

    private String buildVerificationEmailContent(String code) {
        return """
            안녕하세요, IGRUS입니다.

            이메일 인증을 위한 인증 코드입니다.

            인증 코드: %s

            인증 코드는 10분간 유효합니다.
            본인이 요청하지 않은 경우 이 이메일을 무시해 주세요.

            감사합니다.
            IGRUS 드림
            """.formatted(code);
    }

    private String buildPasswordResetEmailContent(String resetLink) {
        return """
            안녕하세요, IGRUS입니다.

            비밀번호 재설정을 요청하셨습니다.
            아래 링크를 클릭하여 비밀번호를 재설정해 주세요.

            재설정 링크: %s

            링크는 30분간 유효합니다.
            본인이 요청하지 않은 경우 이 이메일을 무시해 주세요.

            감사합니다.
            IGRUS 드림
            """.formatted(resetLink);
    }

    private String buildWelcomeEmailContent(String name) {
        return """
            안녕하세요, %s님!

            IGRUS 가입을 환영합니다!

            IGRUS는 인하대학교 IT 동아리로,
            함께 성장하고 배우는 커뮤니티입니다.

            동아리 활동에 적극적으로 참여해 주세요.
            궁금한 점이 있으시면 언제든 문의해 주세요.

            감사합니다.
            IGRUS 드림
            """.formatted(name);
    }
}
