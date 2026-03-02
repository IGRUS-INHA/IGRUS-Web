package igrus.web.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class PublicResourceSecurityConfig {

    private final SecurityConfigUtil securityConfigUtil;

    @Bean
    @Order(2)
    public SecurityFilterChain publicResourceSecurityFilterChain(HttpSecurity http) throws Exception {

        // 공개 리소스 경로 담당
        http.securityMatcher(
                "/",
                "/index.html",
                "/favicon.svg",
                "/webjars/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/openapi.yaml"
        );

        // 모든 요청 허용 (인증 불필요)
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
        );

        securityConfigUtil.disableSessionManagement(http);
        securityConfigUtil.disableCsrf(http);
        securityConfigUtil.configCors(http);

        return http.build();
    }
}
