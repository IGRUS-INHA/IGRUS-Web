package igrus.web.security.jwt;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import igrus.web.security.config.SecurityPaths;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return Arrays.stream(SecurityPaths.PUBLIC_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 요청에서 토큰 추출 및 인증 처리
        extractToken(request).ifPresent(token -> {
            try {
                // Access Token 유효성 검증 및 Claims 추출 (한 번에 수행)
                Claims claims = jwtTokenProvider.validateAccessTokenAndGetClaims(token);

                // Claims에서 정보 추출 (토큰 재파싱 없이 재사용)
                Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
                String studentId = jwtTokenProvider.getStudentIdFromClaims(claims);
                String role = jwtTokenProvider.getRoleFromClaims(claims);

                // AuthenticatedUser 생성
                AuthenticatedUser principal = new AuthenticatedUser(userId, studentId, role);

                // 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("인증 성공: userId={}, studentId={}", userId, studentId);

            } catch (CustomBaseException e) {
                log.warn("인증 실패 - {}: path={}", e.getMessage(), request.getRequestURI());
            } catch (Exception e) {
                log.error("인증 처리 중 예외 발생: path={}, error={}", request.getRequestURI(), e.getMessage());
            }
        });

        // 2. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private Optional<String> extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7));
        }
        return Optional.empty();
    }
}
