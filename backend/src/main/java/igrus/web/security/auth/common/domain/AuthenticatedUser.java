package igrus.web.security.auth.common.domain;

/**
 * 인증된 사용자 정보를 담는 Principal 객체.
 * 모든 인증 방식(Password, OAuth2 등)에서 공통으로 사용됩니다.
 */
public record AuthenticatedUser(
    Long userId,
    String studentId,
    String role
) {}
