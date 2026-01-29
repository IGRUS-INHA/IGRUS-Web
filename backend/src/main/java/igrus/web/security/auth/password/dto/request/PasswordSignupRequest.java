package igrus.web.security.auth.password.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "비밀번호 기반 회원가입 요청")
public record PasswordSignupRequest(
    @Schema(description = "학번 (8자리 숫자)", example = "12345678")
    @NotBlank(message = "학번은 필수입니다")
    @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
    String studentId,

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    String name,

    @Schema(description = "이메일 주소", example = "user@inha.edu", format = "email")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @Schema(description = "비밀번호 (영문 대/소문자, 숫자, 특수문자 포함 8~72자)", example = "Password1!", format = "password")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,72}$",
        message = "비밀번호는 영문 대/소문자, 숫자, 특수문자를 포함하여 8자 이상 72자 이하여야 합니다"
    )
    String password,

    @Schema(description = "전화번호", example = "010-1234-5678")
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "유효한 전화번호 형식이 아닙니다")
    String phoneNumber,

    @Schema(description = "학과명", example = "컴퓨터공학과")
    @NotBlank(message = "학과는 필수입니다")
    @Size(max = 50, message = "학과명은 50자 이내여야 합니다")
    String department,

    @Schema(description = "동아리 가입 동기", example = "웹 개발 역량을 키우고 싶습니다.")
    @NotBlank(message = "가입 동기는 필수입니다")
    String motivation,

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    @NotNull(message = "개인정보 동의는 필수입니다")
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
    Boolean privacyConsent
) {}
