package igrus.web.security.auth.password.dto.request;

import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.Wish;
import jakarta.validation.constraints.*;

import java.util.List;

public record PasswordSignupRequest(
    @NotBlank(message = "학번은 필수입니다")
    @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
    String studentId,

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    String name,

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
        message = "비밀번호는 영문, 숫자를 포함하여 8자 이상 72자 이하여야 합니다"
    )
    String password,

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호는 000-0000-0000 형식이어야 합니다")
    String phoneNumber,

    @NotBlank(message = "학과는 필수입니다")
    @Size(max = 50, message = "학과명은 50자 이내여야 합니다")
    String department,

    String motivation,

    @Size(max = 10, message = "가입 목적은 최대 10개까지 선택 가능합니다")
    List<Wish> wishes,

    @NotNull(message = "관심 분야는 필수입니다")
    @Size(min = 1, message = "관심 분야는 최소 1개 이상 선택해야 합니다")
    List<Interest> interests,

    @Size(max = 100, message = "기타 관심 분야는 100자 이내여야 합니다")
    String customInterest,

    @NotNull(message = "가입 경로는 필수입니다")
    JoinRoute joinRoute,

    @Size(max = 100, message = "기타 가입 경로는 100자 이내여야 합니다")
    String customJoinRoute,

    @NotNull(message = "성별은 필수입니다")
    Gender gender,

    @NotNull(message = "학년은 필수입니다")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다")
    Integer grade,

    @NotNull(message = "재학 상태는 필수입니다")
    EnrollmentStatus enrollmentStatus,

    @NotNull(message = "개인정보 동의는 필수입니다")
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
    Boolean privacyConsent,

    @NotBlank(message = "인증 토큰은 필수입니다")
    String verificationToken
) {}
