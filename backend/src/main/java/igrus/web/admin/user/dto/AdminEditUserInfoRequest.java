package igrus.web.admin.user.dto;

import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.Wish;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminEditUserInfoRequest(

        @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
        String studentId,

        @Email
        String email,

        @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이내여야 합니다")
        String name,

        String phoneNumber,

        @Size(max = 50, message = "학과는 50자 이내여야 합니다")
        String department,

        Gender gender,

        @Min(value = 1, message = "학년은 1 이상이어야 합니다")
        @Max(value = 4, message = "학년은 4 이하여야 합니다")
        Integer grade,

        EnrollmentStatus enrollmentStatus,

        String motivation,

        List<Wish> wishes,

        List<Interest> interests,

        @Size(max = 100, message = "기타 관심 분야는 100자 이내여야 합니다")
        String customInterest,

        JoinRoute joinRoute,

        @Size(max = 100, message = "기타 가입 경로는 100자 이내여야 합니다")
        String customJoinRoute
) {}
