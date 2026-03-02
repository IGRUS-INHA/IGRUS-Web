package igrus.web.security.auth.approval.dto.response;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.user.domain.User;

import java.time.Instant;

public record DemotedAssociateInfoResponse(
        Long userId,

        String studentId,

        String name,

        String department,

        Instant demotedAt,

        Long demotedBy
) {
    public static DemotedAssociateInfoResponse from(AssociateDecision decision) {
        User user = decision.getUser();
        return new DemotedAssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                decision.getDecidedAt(),
                decision.getDecidedBy()
        );
    }
}
