package igrus.web.security.auth.common.service;

/**
 * 인증 관련 이메일 발송 서비스 인터페이스.
 * 비동기 + 재시도 포함 메서드만 제공합니다.
 */
public interface AuthEmailService {

    /**
     * 인증 코드 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to   수신자 이메일 주소
     * @param code 인증 코드
     */
    void sendVerificationEmail(String to, String code);

    /**
     * 비밀번호 재설정 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크
     */
    void sendPasswordResetEmail(String to, String resetLink);

    /**
     * 환영 이메일 발송 (비동기, 재시도 포함)
     * 지수 백오프 재시도: 1분 → 3분 → 9분 (최대 4회 시도)
     *
     * @param to   수신자 이메일 주소
     * @param name 회원 이름
     */
    void sendWelcomeEmail(String to, String name);
}