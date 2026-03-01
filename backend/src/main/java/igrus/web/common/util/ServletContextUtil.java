package igrus.web.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link RequestContextHolder}를 통해 현재 HTTP 요청/응답 객체에 접근하는 유틸리티.
 *
 * <p>openapi-generator가 생성하는 인터페이스 시그니처에는
 * {@link HttpServletRequest}/{@link HttpServletResponse} 파라미터가 포함되지 않으므로,
 * 쿠키 설정, IP 추출 등 Servlet API가 필요한 경우 이 유틸리티를 사용한다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * HttpServletRequest request = ServletContextUtil.getCurrentRequest();
 * HttpServletResponse response = ServletContextUtil.getCurrentResponse();
 *
 * String ip = ServletContextUtil.extractIpAddress(request);
 * response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
 * }</pre>
 *
 * <p>Spring MVC의 {@code DispatcherServlet}이 요청 처리 시 자동으로
 * {@link RequestContextHolder}에 {@link ServletRequestAttributes}를 등록하므로,
 * 별도의 {@code RequestContextListener} 설정 없이도 동작한다.</p>
 */
public final class ServletContextUtil {

    private ServletContextUtil() {
    }

    /**
     * 현재 HTTP 요청의 {@link ServletRequestAttributes}를 반환한다.
     *
     * @return 현재 요청의 ServletRequestAttributes
     * @throws IllegalStateException 요청 컨텍스트 밖에서 호출된 경우
     */
    public static ServletRequestAttributes currentAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }

    /**
     * 현재 HTTP 요청 객체를 반환한다.
     *
     * @return 현재 HttpServletRequest
     * @throws IllegalStateException 요청 컨텍스트 밖에서 호출된 경우
     */
    public static HttpServletRequest getCurrentRequest() {
        return currentAttributes().getRequest();
    }

    /**
     * 현재 HTTP 응답 객체를 반환한다.
     *
     * @return 현재 HttpServletResponse
     * @throws IllegalStateException 요청 컨텍스트 밖에서 호출되거나, 응답 객체가 없는 경우
     */
    public static HttpServletResponse getCurrentResponse() {
        HttpServletResponse response = currentAttributes().getResponse();
        if (response == null) {
            throw new IllegalStateException(
                    "HttpServletResponse is not available in the current request context. "
                            + "Ensure this method is called within a DispatcherServlet-managed request.");
        }
        return response;
    }

    /**
     * 클라이언트의 실제 IP 주소를 추출한다.
     * 프록시/로드밸런서를 고려하여 X-Forwarded-For, X-Real-IP 헤더를 우선 확인한다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public static String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 현재 요청에서 클라이언트 IP 주소를 추출한다.
     *
     * @return 클라이언트 IP 주소
     * @throws IllegalStateException 요청 컨텍스트 밖에서 호출된 경우
     */
    public static String extractCurrentIpAddress() {
        return extractIpAddress(getCurrentRequest());
    }

    /**
     * 현재 요청의 User-Agent 헤더를 반환한다.
     *
     * @return User-Agent 문자열, 없으면 null
     * @throws IllegalStateException 요청 컨텍스트 밖에서 호출된 경우
     */
    public static String getCurrentUserAgent() {
        return getCurrentRequest().getHeader("User-Agent");
    }
}
