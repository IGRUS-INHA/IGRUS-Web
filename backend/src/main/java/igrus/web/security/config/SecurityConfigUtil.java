package igrus.web.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

@Component
public class SecurityConfigUtil {

    public void disableSessionManagement(HttpSecurity http) {
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    }

    public void disableCsrf(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
    }

    public void disableDefaultLoginOption(HttpSecurity http) {
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);
    }

    public void configCors(HttpSecurity http) {
        http.cors(
                corsCustomizer ->
                        corsCustomizer.configurationSource(
                                request -> {
                                    CorsConfiguration configuration = new CorsConfiguration();

                                    configuration.setAllowedOriginPatterns(
                                            Collections.singletonList("*"));
                                    configuration.setAllowedMethods(Collections.singletonList("*"));
                                    configuration.setAllowCredentials(true);
                                    configuration.setAllowedHeaders(Collections.singletonList("*"));
                                    configuration.setMaxAge(3600L);

                                    configuration.setExposedHeaders(
                                            Arrays.asList("Set-Cookie", "Authorization", "X-Token-Expired", "X-Access-Token"));

                                    return configuration;
                                }));
    }

    /**
     * 보안 헤더 설정
     * - XSS Protection
     * - X-Frame-Options: DENY
     * - X-Content-Type-Options: nosniff
     * - Content-Security-Policy
     * - Referrer-Policy: STRICT_ORIGIN_WHEN_CROSS_ORIGIN
     */
    public void configSecurityHeaders(HttpSecurity http) {
        http.headers(headers -> headers
                // XSS Protection (레거시 브라우저 호환성)
                .xssProtection(xss -> xss.headerValue(
                        org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // X-Frame-Options: DENY (클릭재킹 방지)
                .frameOptions(frame -> frame.deny())
                // X-Content-Type-Options: nosniff (MIME 스니핑 방지)
                .contentTypeOptions(contentType -> {})
                // Content-Security-Policy
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; frame-ancestors 'none'; form-action 'self'"))
                // Referrer-Policy
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        );
    }
}
