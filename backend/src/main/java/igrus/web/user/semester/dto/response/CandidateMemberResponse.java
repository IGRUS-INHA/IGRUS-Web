package igrus.web.user.semester.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.Wish;

import java.util.List;

public record CandidateMemberResponse(
        Long userId,
        String studentId,
        String name,
        String department,
        UserRole role,
        boolean alreadyRegistered,
        String motivation,
        List<Wish> wishes
) {
    public static CandidateMemberResponse from(User user, boolean alreadyRegistered) {
        return new CandidateMemberResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getRole(),
                alreadyRegistered,
                user.getMotivation(),
                user.getWishes()
        );
    }
}
