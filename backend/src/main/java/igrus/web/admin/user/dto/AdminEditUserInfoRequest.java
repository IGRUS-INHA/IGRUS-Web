package igrus.web.admin.user.dto;

import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.Wish;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "관리자 회원 정보 수정 요청 (부분 업데이트: null 필드는 기존 값 유지)")
public record AdminEditUserInfoRequest(

        @Email
        @Schema(description = "이메일 (null이면 변경하지 않음)", example = "user@inha.edu")
        String email,

        @Schema(description = "이름 (null이면 변경하지 않음)", example = "홍길동")
        @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이내여야 합니다")
        String name,

        @Schema(description = "전화번호 (null이면 변경하지 않음)", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "학과 (null이면 변경하지 않음)", example = "컴퓨터공학과")
        @Size(max = 50, message = "학과는 50자 이내여야 합니다")
        String department,

        @Schema(description = "성별 (null이면 변경하지 않음)")
        Gender gender,

        @Schema(description = "학년 (null이면 변경하지 않음)", example = "2")
        @Min(value = 1, message = "학년은 1 이상이어야 합니다")
        Integer grade,

        @Schema(description = "재학 상태 (null이면 변경하지 않음)")
        EnrollmentStatus enrollmentStatus,

        @Schema(description = "가입 동기 (null이면 변경하지 않음)")
        String motivation,

        @Schema(description = "가입 목적 (null이면 변경하지 않음)")
        List<Wish> wishes,

        @Schema(description = "관심 분야 (null이면 변경하지 않음)")
        List<Interest> interests,

        @Schema(description = "기타 관심 분야 (null이면 변경하지 않음)")
        @Size(max = 100, message = "기타 관심 분야는 100자 이내여야 합니다")
        String customInterest,

        @Schema(description = "가입 경로 (null이면 변경하지 않음)")
        JoinRoute joinRoute,

        @Schema(description = "기타 가입 경로 (null이면 변경하지 않음)")
        @Size(max = 100, message = "기타 가입 경로는 100자 이내여야 합니다")
        String customJoinRoute
) {}
