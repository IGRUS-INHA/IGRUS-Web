package igrus.web.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 요청에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰 유효성 검증 및 인증 처리
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // Access Token인지 확인
            String tokenType = jwtTokenProvider.getTokenType(token);
            if ("access".equals(tokenType)) {
                // 토큰에서 정보 추출
                Long userId = jwtTokenProvider.getUserId(token);
                String studentId = jwtTokenProvider.getStudentId(token);
                String role = jwtTokenProvider.getRole(token);

                // 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                    // principal (유저 ID)
                                null,                      // credentials
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 3. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
