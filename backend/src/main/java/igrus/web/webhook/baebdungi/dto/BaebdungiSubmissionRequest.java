package igrus.web.webhook.baebdungi.dto;

import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;

/**
 * 뱁둥이봇 웹훅 제출 요청 DTO.
 * User 엔티티의 데이터를 웹훅 API 스펙에 맞게 변환합니다.
 */
public record BaebdungiSubmissionRequest(
        String name,
        String studentId,
        String email,
        String department,
        String phone,
        String gender,
        String grade,
        String enrollmentStatus,
        String hasPaid,
        String submittedAt
) {

    /**
     * User 엔티티로부터 웹훅 요청 DTO를 생성합니다.
     *
     * @param user 회원가입 완료된 사용자
     * @return 웹훅 제출 요청 DTO
     */
    public static BaebdungiSubmissionRequest fromUser(User user) {
        return new BaebdungiSubmissionRequest(
                user.getName(),
                user.getStudentId(),
                user.getEmail(),
                user.getDepartment(),
                user.getPhoneNumber(),
                convertGender(user.getGender()),
                convertGrade(user.getGrade()),
                convertEnrollmentStatus(user.getEnrollmentStatus()),
                "네",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }

    private static String convertGender(Gender gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case MALE -> "남";
            case FEMALE -> "여";
        };
    }

    private static String convertEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
        if (enrollmentStatus == null) {
            return null;
        }
        return switch (enrollmentStatus) {
            case ENROLLED -> "재학";
            case GENERAL_LEAVE -> "휴학(일반)";
            case MILITARY_LEAVE -> "휴학(군)";
        };
    }

    private static String convertGrade(Integer grade) {
        if (grade == null) {
            return null;
        }
        return grade + "학년";
    }
}
