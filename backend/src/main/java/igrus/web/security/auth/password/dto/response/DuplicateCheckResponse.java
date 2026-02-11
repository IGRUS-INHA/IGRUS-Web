package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "중복 체크 응답")
public record DuplicateCheckResponse(
        @Schema(description = "사용 가능 여부", example = "true")
        boolean available,

        @Schema(description = "안내 메시지", example = "사용 가능한 학번입니다")
        String message
) {
    public static DuplicateCheckResponse studentIdAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 학번입니다");
    }

    public static DuplicateCheckResponse emailAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 이메일입니다");
    }

    public static DuplicateCheckResponse phoneNumberAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 전화번호입니다");
    }
}
