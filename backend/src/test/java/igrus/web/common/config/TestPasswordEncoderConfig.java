package igrus.web.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 테스트 전용 PasswordEncoder 설정.
 *
 * <p>운영 환경의 기본 BCrypt strength(10)는 의도적으로 느리게 설계되어 있어
 * 테스트 실행 속도에 영향을 줍니다. 테스트에서는 strength를 4로 낮추어
 * 해싱 비용을 줄이되, BCrypt 동작 자체는 동일하게 유지합니다.</p>
 */
@TestConfiguration
public class TestPasswordEncoderConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4);
    }
}
