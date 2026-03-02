package igrus.web.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PoC 검증: RequestContextHolder를 통한 HttpServletRequest/Response 접근 패턴.
 *
 * <p>openapi-generator가 생성하는 인터페이스에는 Servlet API 파라미터가 없으므로,
 * 컨트롤러 메서드 바디에서 {@code RequestContextHolder}를 통해 Servlet 객체를 획득하는
 * 패턴이 정상 동작하는지 검증한다.</p>
 *
 * <h3>검증 항목</h3>
 * <ol>
 *   <li>{@code attrs.getResponse()}가 {@code null}이 아닌지</li>
 *   <li>획득한 {@code HttpServletResponse}에 Set-Cookie 헤더 추가 시 클라이언트에 전달되는지</li>
 *   <li>{@code extractIpAddress}의 X-Forwarded-For, X-Real-IP, remoteAddr 우선순위 동작</li>
 * </ol>
 */
@WebMvcTest(controllers = ServletContextUtilPocTest.PocController.class)
@ContextConfiguration(classes = {
        ServletContextUtilPocTest.PocController.class,
        ServletContextUtilPocTest.PocSecurityConfig.class
})
class ServletContextUtilPocTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * PoC 테스트 전용 컨트롤러.
     * openapi-generator 인터페이스와 동일하게 Servlet API 파라미터 없이 메서드를 정의하고,
     * 내부에서 {@link ServletContextUtil}을 통해 Servlet 객체를 사용한다.
     */
    @RestController
    static class PocController {

        @GetMapping("/poc/cookie-test")
        public ResponseEntity<Map<String, String>> cookieTest() {
            HttpServletResponse response = ServletContextUtil.getCurrentResponse();
            response.addHeader(HttpHeaders.SET_COOKIE,
                    "testCookie=testValue; Path=/; HttpOnly; Secure; SameSite=Lax");

            Map<String, String> body = new HashMap<>();
            body.put("status", "cookie_set");
            return ResponseEntity.ok(body);
        }

        @GetMapping("/poc/ip-test")
        public ResponseEntity<Map<String, String>> ipTest() {
            HttpServletRequest request = ServletContextUtil.getCurrentRequest();

            Map<String, String> body = new HashMap<>();
            body.put("extractedIp", ServletContextUtil.extractIpAddress(request));
            body.put("remoteAddr", request.getRemoteAddr());
            body.put("xForwardedFor", request.getHeader("X-Forwarded-For"));
            body.put("xRealIp", request.getHeader("X-Real-IP"));
            body.put("userAgent", ServletContextUtil.getCurrentUserAgent());
            return ResponseEntity.ok(body);
        }

        @GetMapping("/poc/response-not-null")
        public ResponseEntity<Map<String, Object>> responseNotNull() {
            HttpServletResponse response = ServletContextUtil.getCurrentResponse();

            Map<String, Object> body = new HashMap<>();
            body.put("responseNotNull", response != null);
            body.put("responseClass", response.getClass().getName());
            return ResponseEntity.ok(body);
        }
    }

    /**
     * PoC 테스트용 Security 설정 - 모든 경로 허용.
     */
    @Configuration
    static class PocSecurityConfig {
        @Bean
        SecurityFilterChain pocSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }
    }

    // ==================== 검증 항목 1: getResponse()가 null이 아닌지 ====================

    @Nested
    @DisplayName("검증 1: HttpServletResponse 획득")
    class ResponseNotNull {

        @Test
        @DisplayName("RequestContextHolder로 획득한 HttpServletResponse는 null이 아니어야 한다")
        void getResponse_ShouldNotReturnNull() throws Exception {
            mockMvc.perform(get("/poc/response-not-null"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responseNotNull").value(true));
        }
    }

    // ==================== 검증 항목 2: Set-Cookie 헤더 전달 ====================

    @Nested
    @DisplayName("검증 2: Set-Cookie 헤더 전달")
    class SetCookieDelivery {

        @Test
        @DisplayName("RequestContextHolder로 획득한 Response에 Set-Cookie 설정 시 클라이언트에 전달되어야 한다")
        void addSetCookieHeader_ShouldBeDeliveredToClient() throws Exception {
            mockMvc.perform(get("/poc/cookie-test"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(result -> {
                        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
                        assertThat(setCookie).as("Set-Cookie header should not be null").isNotNull();
                        assertThat(setCookie).as("Cookie name/value").contains("testCookie=testValue");
                        assertThat(setCookie).as("Path attribute").contains("Path=/");
                        assertThat(setCookie).as("HttpOnly attribute").contains("HttpOnly");
                        assertThat(setCookie).as("Secure attribute").contains("Secure");
                        assertThat(setCookie).as("SameSite attribute").contains("SameSite=Lax");
                    })
                    .andExpect(jsonPath("$.status").value("cookie_set"));
        }
    }

    // ==================== 검증 항목 3: IP 추출 우선순위 ====================

    @Nested
    @DisplayName("검증 3: IP 추출 우선순위")
    class IpExtraction {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP를 반환해야 한다")
        void extractIpAddress_WithXForwardedFor_ReturnsFirstIp() throws Exception {
            mockMvc.perform(get("/poc/ip-test")
                            .header("X-Forwarded-For", "203.0.113.50, 70.41.3.18, 150.172.238.178"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.extractedIp").value("203.0.113.50"));
        }

        @Test
        @DisplayName("X-Forwarded-For 없고 X-Real-IP가 있으면 X-Real-IP를 반환해야 한다")
        void extractIpAddress_WithXRealIp_ReturnsXRealIp() throws Exception {
            mockMvc.perform(get("/poc/ip-test")
                            .header("X-Real-IP", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.extractedIp").value("192.168.1.100"));
        }

        @Test
        @DisplayName("프록시 헤더가 없으면 remoteAddr를 반환해야 한다")
        void extractIpAddress_WithoutProxyHeaders_ReturnsRemoteAddr() throws Exception {
            mockMvc.perform(get("/poc/ip-test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.extractedIp").value("127.0.0.1"));
        }

        @Test
        @DisplayName("User-Agent 헤더가 정상적으로 추출되어야 한다")
        void getCurrentUserAgent_ReturnsUserAgentHeader() throws Exception {
            mockMvc.perform(get("/poc/ip-test")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userAgent").value("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
        }
    }
}
