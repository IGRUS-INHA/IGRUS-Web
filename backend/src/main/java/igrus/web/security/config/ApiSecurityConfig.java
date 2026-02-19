package igrus.web.security.config;

import igrus.web.common.filter.MdcLoggingFilter;
import igrus.web.security.jwt.JwtAuthenticationEntryPoint;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final MdcLoggingFilter mdcLoggingFilter;
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

                // 고정 게시글 목록 조회 (GET만 공개, POST/PUT/DELETE는 인증 필요)
                .requestMatchers(HttpMethod.GET, "/api/v1/pinned-posts").permitAll()

                // 학기별 회원 명단 조회 (운영진 이상)
                .requestMatchers("/api/v1/semesters/**").hasAnyRole("OPERATOR", "ADMIN")

                // 학기별 회원 관리 (관리자 전용)
                .requestMatchers("/api/v1/admin/semesters/**").hasRole("ADMIN")

                // 운영진 이상 (더 구체적인 경로를 먼저 배치)
                .requestMatchers(
                        "/api/v1/admin/dashboard",
                        "/api/v1/admin/users/**",
                        "/api/events/*/registrations",
                        "/api/v1/admin/comment-reports/**"
                ).hasAnyRole("OPERATOR", "ADMIN")

                // 계정 상태 변경 감사 이력 (관리자 전용)
                .requestMatchers("/api/v1/admin/account-status-histories/**").hasRole("ADMIN")

                // 관리자 전용
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 댓글 API - ASSOCIATE 이상 (접근 가능한 게시글에 한함)
                .requestMatchers("/api/v1/posts/*/comments/**").hasAnyRole("ASSOCIATE", "MEMBER", "OPERATOR", "ADMIN")
                .requestMatchers("/api/v1/comments/**").hasAnyRole("ASSOCIATE", "MEMBER", "OPERATOR", "ADMIN")

                // 게시글 API - ASSOCIATE 이상
                .requestMatchers("/api/v1/boards/*/posts/**").hasAnyRole("ASSOCIATE", "MEMBER", "OPERATOR", "ADMIN")

                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
        );

        // JWT 필터 등록
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // MDC 로깅 필터 등록 (JWT 인증 이후 실행)
        http.addFilterAfter(mdcLoggingFilter, JwtAuthenticationFilter.class);

        // 인증되지 않은 요청에 대해 401 반환
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        );

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
