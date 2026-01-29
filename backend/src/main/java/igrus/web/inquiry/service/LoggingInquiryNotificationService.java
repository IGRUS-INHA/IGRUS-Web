package igrus.web.inquiry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "test"})
public class LoggingInquiryNotificationService implements InquiryNotificationService {

    @Override
    public void sendInquiryConfirmation(String email, String inquiryNumber, String title) {
        log.info("[Test] 문의 접수 확인 메일 전송: email={}, inquiryNumber={}, title={}", email, inquiryNumber, title);
    }

    @Override
    public void sendReplyNotification(String email, String inquiryNumber, String title, String replyContent) {
        log.info("[Test] 문의 답변 알림 메일 전송: email={}, inquiryNumber={}, title={}", email, inquiryNumber, title);
    }
}