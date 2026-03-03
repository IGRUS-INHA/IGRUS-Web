package igrus.web.security.auth.approval.dto.response;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.user.domain.User;
import igrus.web.user.domain.Wish;

import java.time.Instant;
import java.util.List;

public record RejectedAssociateInfoResponse(
        Long userId,

        String studentId,

        String name,

        String department,

        String motivation,

        List<Wish> wishes,

        Instant createdAt,

        String rejectionReason,

        Instant rejectedAt,

        Long rejectedBy
) {
    public static RejectedAssociateInfoResponse from(AssociateDecision decision) {
        User user = decision.getUser();
        return new RejectedAssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getMotivation(),
                user.getWishes(),
                user.getCreatedAt(),
                decision.getReason(),
                decision.getDecidedAt(),
                decision.getDecidedBy()
        );
    }
}
