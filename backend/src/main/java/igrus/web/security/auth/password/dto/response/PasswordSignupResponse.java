package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record PasswordSignupResponse(
    @Schema(description = "처리 결과 메시지", example = "회원가입 요청이 완료되었습니다. 이메일로 발송된 인증 코드를 입력해주세요.")
    String message,

    @Schema(description = "가입 요청한 이메일 주소", example = "user@inha.edu")
    String email,

    @Schema(description = "이메일 인증 필요 여부", example = "true")
    boolean requiresVerification,

    @Schema(description = "임시 학번 (임시 학번 가입 시에만 포함)", example = "99260001")
    String temporaryStudentId
) {
    public static PasswordSignupResponse pendingVerification(String email) {
        return new PasswordSignupResponse(
            "회원가입 요청이 완료되었습니다. 이메일로 발송된 인증 코드를 입력해주세요.",
            email,
            true,
            null
        );
    }

    public static PasswordSignupResponse pendingVerificationWithTempId(String email, String tempStudentId) {
        return new PasswordSignupResponse(
            "회원가입 요청이 완료되었습니다. 이메일로 발송된 인증 코드를 입력해주세요.",
            email,
            true,
            tempStudentId
        );
    }

    public static PasswordSignupResponse verified(String email) {
        return new PasswordSignupResponse(
            "이메일 인증이 완료되었습니다. 관리자 승인 후 로그인 가능합니다.",
            email,
            false,
            null
        );
    }
}
