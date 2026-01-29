package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 코드 재발송 응답")
public record VerificationResendResponse(
    @Schema(description = "처리 결과 메시지", example = "인증 코드가 재발송되었습니다.")
    String message,

    @Schema(description = "인증 코드가 발송된 이메일 주소", example = "user@inha.edu")
    String email
) {
    public static VerificationResendResponse success(String email) {
        return new VerificationResendResponse(
            "인증 코드가 재발송되었습니다.",
            email
        );
    }
}