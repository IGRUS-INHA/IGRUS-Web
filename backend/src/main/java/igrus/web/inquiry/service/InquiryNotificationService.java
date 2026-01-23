package igrus.web.inquiry.service;

public interface InquiryNotificationService {

    void sendInquiryConfirmation(String email, String inquiryNumber, String title);

    void sendReplyNotification(String email, String inquiryNumber, String title, String replyContent);
}