package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사전 이메일 인증 응답")
public record PreSignupVerificationResponse(
    @Schema(description = "처리 결과 메시지", example = "이메일 인증이 완료되었습니다.")
    String message,

    @Schema(description = "인증된 이메일 주소", example = "user@inha.edu")
    String email,

    @Schema(description = "인증 완료 여부", example = "true")
    boolean verified,

    @Schema(description = "인증 토큰 (회원가입 시 필수 제출)", example = "550e8400-e29b-41d4-a716-446655440000")
    String verificationToken
) {
    public static PreSignupVerificationResponse success(String email, String verificationToken) {
        return new PreSignupVerificationResponse(
            "이메일 인증이 완료되었습니다.",
            email,
            true,
            verificationToken
        );
    }
}
