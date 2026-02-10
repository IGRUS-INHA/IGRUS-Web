package igrus.web.community.pinnedpost.dto.response;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.user.domain.User;

import java.time.Instant;

public record PinnedPostDetailResponse(
        Long id,
        Long postId,
        String postTitle,
        String boardCode,
        Integer displayOrder,
        PinnedByInfo pinnedBy,
        Instant createdAt
) {
    public static PinnedPostDetailResponse from(PinnedPost pinnedPost) {
        return new PinnedPostDetailResponse(
                pinnedPost.getId(),
                pinnedPost.getPost().getId(),
                pinnedPost.getPost().getTitle(),
                pinnedPost.getPost().getBoard().getCode().name(),
                pinnedPost.getDisplayOrder(),
                PinnedByInfo.from(pinnedPost.getPinnedBy()),
                pinnedPost.getCreatedAt()
        );
    }

    public record PinnedByInfo(Long id, String name) {
        public static PinnedByInfo from(User user) {
            return new PinnedByInfo(user.getId(), user.getDisplayName());
        }
    }
}
