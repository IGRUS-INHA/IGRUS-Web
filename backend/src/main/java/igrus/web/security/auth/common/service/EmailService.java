package igrus.web.security.auth.common.service;

public interface EmailService {
    void sendVerificationEmail(String to, String code);
    void sendPasswordResetEmail(String to, String resetLink);
    void sendWelcomeEmail(String to, String name);
}
