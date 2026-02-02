package igrus.web.security.auth.password.service.support;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    /**
     * 6자리 랜덤 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드
     */
    public String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
