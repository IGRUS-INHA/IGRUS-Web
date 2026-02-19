package igrus.web.common.filter;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC(Mapped Diagnostic Context)에 요청 추적 정보를 설정하는 필터.
 * 모든 로그 라인에 traceId와 userId가 자동으로 포함됩니다.
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_HEADER_TRACE_ID = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String traceId = request.getHeader(REQUEST_HEADER_TRACE_ID);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            MDC.put(TRACE_ID, traceId);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                MDC.put(USER_ID, String.valueOf(user.userId()));
            }

            response.setHeader("X-Trace-ID", traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
