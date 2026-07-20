package igrus.web.user.mypage.dto.response;

import igrus.web.user.domain.ProfileLink;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;

import java.time.Instant;
import java.util.List;

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

        boolean hasTemporaryStudentId,

        String nickname,

        String introduction,

        List<ProfileLink> links
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
                user.isHasTemporaryStudentId(),
                user.getNickname(),
                user.getIntroduction(),
                user.getLinks()
        );
    }
}
