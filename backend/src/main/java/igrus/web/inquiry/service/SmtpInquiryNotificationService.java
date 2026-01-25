package igrus.web.inquiry.service;

import igrus.web.security.auth.common.exception.email.EmailSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!local & !test")
@RequiredArgsConstructor
public class SmtpInquiryNotificationService implements InquiryNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Override
    public void sendInquiryConfirmation(String email, String inquiryNumber, String title) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[IGRUS] 문의가 접수되었습니다");
        message.setText(buildInquiryConfirmationContent(inquiryNumber, title));

        sendEmail(message);
        log.info("문의 접수 확인 이메일 발송 완료: email={}, inquiryNumber={}", email, inquiryNumber);
    }

    @Override
    public void sendReplyNotification(String email, String inquiryNumber, String title, String replyContent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[IGRUS] 문의에 답변이 등록되었습니다");
        message.setText(buildReplyNotificationContent(inquiryNumber, title, replyContent));

        sendEmail(message);
        log.info("문의 답변 알림 이메일 발송 완료: email={}, inquiryNumber={}", email, inquiryNumber);
    }

    private void sendEmail(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("이메일 발송 실패: to={}, error={}", message.getTo(), e.getMessage());
            throw new EmailSendFailedException();
        }
    }

    private String buildInquiryConfirmationContent(String inquiryNumber, String title) {
        return """
            안녕하세요, IGRUS입니다.

            문의가 정상적으로 접수되었습니다.

            문의 번호: %s
            문의 제목: %s

            답변이 등록되면 이메일로 알려드리겠습니다.

            감사합니다.
            IGRUS 드림
            """.formatted(inquiryNumber, title);
    }

    private String buildReplyNotificationContent(String inquiryNumber, String title, String replyContent) {
        return """
            안녕하세요, IGRUS입니다.

            문의에 답변이 등록되었습니다.

            문의 번호: %s
            문의 제목: %s

            [답변 내용]
            %s

            추가 문의 사항이 있으시면 언제든 문의해 주세요.

            감사합니다.
            IGRUS 드림
            """.formatted(inquiryNumber, title, replyContent);
    }
}
