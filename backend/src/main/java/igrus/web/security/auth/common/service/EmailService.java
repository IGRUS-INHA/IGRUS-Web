package igrus.web.security.auth.common.service;

/**
 * 이메일 발송 서비스 인터페이스.
 * 동기/비동기, 재시도 여부에 따른 메서드를 제공합니다.
 */
public interface EmailService {

    /**
     * 인증 코드 이메일 발송 (동기, 재시도 없음)
     *
     * @param to   수신자 이메일 주소
     * @param code 인증 코드
     */
    void sendVerificationEmail(String to, String code);

    /**
     * 비밀번호 재설정 이메일 발송 (동기, 재시도 없음)
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크
     */
    void sendPasswordResetEmail(String to, String resetLink);

    /**
     * 환영 이메일 발송 (동기, 재시도 없음)
     *
     * @param to   수신자 이메일 주소
     * @param name 회원 이름
     */
    void sendWelcomeEmail(String to, String name);

    /**
     * 인증 코드 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to   수신자 이메일 주소
     * @param code 인증 코드
     */
    void sendVerificationEmailWithRetry(String to, String code);

    /**
     * 비밀번호 재설정 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크
     */
    void sendPasswordResetEmailWithRetry(String to, String resetLink);

    /**
     * 환영 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to   수신자 이메일 주소
     * @param name 회원 이름
     */
    void sendWelcomeEmailWithRetry(String to, String name);
}
