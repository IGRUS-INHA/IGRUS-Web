package igrus.web.admin.user.dto;

import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.domain.Wish;

import java.time.Instant;
import java.util.List;

public record UserDetailResponse(
        Long userId,

        String studentId,

        String name,

        String email,

        String phoneNumber,

        String department,

        String motivation,

        List<Wish> wishes,

        List<Interest> interests,

        String customInterest,

        JoinRoute joinRoute,

        String customJoinRoute,

        Gender gender,

        int grade,

        EnrollmentStatus enrollmentStatus,

        UserRole role,

        UserStatus status,

        Instant createdAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDepartment(),
                user.getMotivation(),
                user.getWishes(),
                user.getInterests(),
                user.getCustomInterest(),
                user.getJoinRoute(),
                user.getCustomJoinRoute(),
                user.getGender(),
                user.getGrade(),
                user.getEnrollmentStatus(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
