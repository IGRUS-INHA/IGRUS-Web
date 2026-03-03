package igrus.web.user.mypage.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;

import java.time.Instant;

/**
 * 내 프로필 조회 응답 DTO.
 */
public record MyProfileResponse(
        String studentId,

        String name,

        String email,

        String phoneNumber,

        String department,

        UserRole role,

        Instant createdAt,

        boolean hasTemporaryStudentId
) {
    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDepartment(),
                user.getRole(),
                user.getCreatedAt(),
                user.isHasTemporaryStudentId()
        );
    }
}
