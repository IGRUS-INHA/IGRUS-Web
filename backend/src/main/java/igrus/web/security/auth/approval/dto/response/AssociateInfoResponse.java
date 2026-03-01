package igrus.web.security.auth.approval.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.Wish;

import java.time.Instant;
import java.util.List;

public record AssociateInfoResponse(
        Long userId,

        String studentId,

        String name,

        String department,

        String motivation,

        List<Wish> wishes,

        Instant createdAt,

        boolean demoted
) {
    public static AssociateInfoResponse from(User user, boolean demoted) {
        return new AssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getMotivation(),
                user.getWishes(),
                user.getCreatedAt(),
                demoted
        );
    }
}
