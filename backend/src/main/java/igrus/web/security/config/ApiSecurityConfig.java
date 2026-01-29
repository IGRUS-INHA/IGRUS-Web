package igrus.web.security.config;

import igrus.web.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityConfigUtil securityConfigUtil;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {

        // API 경로만 담당
        http.securityMatcher("/api/**");

        // URL별 인가 설정
        http.authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능 (SecurityPaths에서 중앙 관리)
                .requestMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()

                // 운영진 이상 (더 구체적인 경로를 먼저 배치)
                .requestMatchers(
                        "/api/admin/dashboard",
                        "/api/events/*/registrations",
                        "/api/v1/admin/comment-reports/**"
                ).hasAnyRole("OPERATOR", "ADMIN")

                // 관리자 전용
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 댓글 API - MEMBER 이상 (정회원)
                .requestMatchers("/api/v1/posts/*/comments/**").hasAnyRole("MEMBER", "OPERATOR", "ADMIN")
                .requestMatchers("/api/v1/comments/**").hasAnyRole("MEMBER", "OPERATOR", "ADMIN")

                // 게시글 API - ASSOCIATE 이상
                .requestMatchers("/api/v1/boards/*/posts/**").hasAnyRole("ASSOCIATE", "MEMBER", "OPERATOR", "ADMIN")

                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
        );

        // JWT 필터 등록
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        securityConfigUtil.disableSessionManagement(http);
        securityConfigUtil.disableDefaultLoginOption(http);
        securityConfigUtil.disableCsrf(http);
        securityConfigUtil.configCors(http);
        securityConfigUtil.configSecurityHeaders(http);

        return http.build();
    }

    // 비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 기본 UserDetailsService 비활성화 (JWT 인증 사용)
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
