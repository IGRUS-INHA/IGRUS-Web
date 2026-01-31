package igrus.web.security.config;

/**
 * 보안 경로 상수를 중앙 관리하는 유틸리티 클래스
 */
public final class SecurityPaths {
    private SecurityPaths() {}

    /**
     * 인증 없이 접근 가능한 API 경로 목록
     */
    public static final String[] PUBLIC_PATHS = {
            "/api/health",               // 헬스체크
            "/api/v1/auth/password/**",  // 비밀번호 기반 인증 (로그인, 회원가입, 토큰 갱신)
            "/api/privacy/policy",       // 개인정보 처리방침
            "/api/v1/inquiries/guest",   // 문의 작성 (비로그인 가능)
            "/api/v1/inquiries/lookup"   // 비회원 문의 조회
    };
}
