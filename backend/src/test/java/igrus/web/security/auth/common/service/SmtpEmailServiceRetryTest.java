package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.exception.email.EmailSendFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SmtpEmailService 단위 테스트.
 * 재시도 로직은 Spring Retry AOP에 의해 처리되므로,
 * 이 테스트는 기본 이메일 발송 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpEmailService 단위 테스트")
class SmtpEmailServiceRetryTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_CODE = "123456";
    private static final String TEST_RESET_LINK = "https://igrus.inha.ac.kr/reset?token=abc123";
    private static final String TEST_NAME = "홍길동";
    private static final String FROM_ADDRESS = "noreply@igrus.inha.ac.kr";

    @Nested
    @DisplayName("인증 코드 이메일 발송 테스트")
    class SendVerificationEmailTest {

        @Test
        @DisplayName("이메일 발송 성공")
        void sendVerificationEmail_success() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendVerificationEmail(TEST_EMAIL, TEST_CODE);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("이메일 발송 실패 시 EmailSendFailedException 발생")
        void sendVerificationEmail_failure_throwsException() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doThrow(new MailException("SMTP 연결 실패") {
            }).when(mailSender).send(any(SimpleMailMessage.class));

            // when & then
            assertThatThrownBy(() -> smtpEmailService.sendVerificationEmail(TEST_EMAIL, TEST_CODE))
                    .isInstanceOf(EmailSendFailedException.class);
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 이메일 발송 테스트")
    class SendPasswordResetEmailTest {

        @Test
        @DisplayName("이메일 발송 성공")
        void sendPasswordResetEmail_success() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("이메일 발송 실패 시 EmailSendFailedException 발생")
        void sendPasswordResetEmail_failure_throwsException() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doThrow(new MailException("SMTP 연결 실패") {
            }).when(mailSender).send(any(SimpleMailMessage.class));

            // when & then
            assertThatThrownBy(() -> smtpEmailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_LINK))
                    .isInstanceOf(EmailSendFailedException.class);
        }
    }

    @Nested
    @DisplayName("환영 이메일 발송 테스트")
    class SendWelcomeEmailTest {

        @Test
        @DisplayName("이메일 발송 성공")
        void sendWelcomeEmail_success() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendWelcomeEmail(TEST_EMAIL, TEST_NAME);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("이메일 발송 실패 시 EmailSendFailedException 발생")
        void sendWelcomeEmail_failure_throwsException() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doThrow(new MailException("SMTP 연결 실패") {
            }).when(mailSender).send(any(SimpleMailMessage.class));

            // when & then
            assertThatThrownBy(() -> smtpEmailService.sendWelcomeEmail(TEST_EMAIL, TEST_NAME))
                    .isInstanceOf(EmailSendFailedException.class);
        }
    }

    @Nested
    @DisplayName("재시도 포함 메서드 테스트")
    class WithRetryMethodsTest {

        @Test
        @DisplayName("sendVerificationEmailWithRetry - 성공 시 내부 메서드 호출")
        void sendVerificationEmailWithRetry_success_callsInternalMethod() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendVerificationEmailWithRetry(TEST_EMAIL, TEST_CODE);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("sendPasswordResetEmailWithRetry - 성공 시 내부 메서드 호출")
        void sendPasswordResetEmailWithRetry_success_callsInternalMethod() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendPasswordResetEmailWithRetry(TEST_EMAIL, TEST_RESET_LINK);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("sendWelcomeEmailWithRetry - 성공 시 내부 메서드 호출")
        void sendWelcomeEmailWithRetry_success_callsInternalMethod() {
            // given
            ReflectionTestUtils.setField(smtpEmailService, "fromAddress", FROM_ADDRESS);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            // when
            smtpEmailService.sendWelcomeEmailWithRetry(TEST_EMAIL, TEST_NAME);

            // then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }

    @Nested
    @DisplayName("복구 메서드 테스트")
    class RecoverMethodsTest {

        @Test
        @DisplayName("recoverVerificationEmail - 예외 발생 없이 로그만 출력")
        void recoverVerificationEmail_logsError() {
            // given
            EmailSendFailedException exception = new EmailSendFailedException();

            // when & then (예외 없이 정상 실행)
            smtpEmailService.recoverVerificationEmail(exception, TEST_EMAIL, TEST_CODE);
        }

        @Test
        @DisplayName("recoverPasswordResetEmail - 예외 발생 없이 로그만 출력")
        void recoverPasswordResetEmail_logsError() {
            // given
            EmailSendFailedException exception = new EmailSendFailedException();

            // when & then (예외 없이 정상 실행)
            smtpEmailService.recoverPasswordResetEmail(exception, TEST_EMAIL, TEST_RESET_LINK);
        }

        @Test
        @DisplayName("recoverWelcomeEmail - 예외 발생 없이 로그만 출력")
        void recoverWelcomeEmail_logsError() {
            // given
            EmailSendFailedException exception = new EmailSendFailedException();

            // when & then (예외 없이 정상 실행)
            smtpEmailService.recoverWelcomeEmail(exception, TEST_EMAIL, TEST_NAME);
        }
    }
}
