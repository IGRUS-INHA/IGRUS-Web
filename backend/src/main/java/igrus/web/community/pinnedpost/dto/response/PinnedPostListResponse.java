package igrus.web.community.pinnedpost.dto.response;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;

import java.time.Instant;

public record PinnedPostListResponse(
        Long id,
        PostInfo post,
        Integer displayOrder,
        PinnedByInfo pinnedBy,
        Instant pinnedAt
) {
    public static PinnedPostListResponse from(PinnedPost pinnedPost) {
        return new PinnedPostListResponse(
                pinnedPost.getId(),
                PostInfo.from(pinnedPost.getPost()),
                pinnedPost.getDisplayOrder(),
                PinnedByInfo.from(pinnedPost.getPinnedBy()),
                pinnedPost.getCreatedAt()
        );
    }

    public record PostInfo(
            Long id,
            String title,
            String contentPreview,
            String boardCode,
            String boardName,
            AuthorInfo author,
            int viewCount,
            int likeCount,
            int commentCount,
            Instant createdAt
    ) {
        private static final int PREVIEW_MAX_LENGTH = 200;

        public static PostInfo from(Post post) {
            return new PostInfo(
                    post.getId(),
                    post.getTitle(),
                    truncate(post.getContent(), PREVIEW_MAX_LENGTH),
                    post.getBoard().getCode().name(),
                    post.getBoard().getName(),
                    AuthorInfo.from(post.getAuthor()),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getCreatedAt()
            );
        }

        private static String truncate(String text, int maxLength) {
            if (text == null || text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength);
        }
    }

    public record AuthorInfo(Long id, String name) {
        public static AuthorInfo from(User author) {
            if (author == null) {
                return new AuthorInfo(null, User.WITHDRAWN_DISPLAY_NAME);
            }
            return new AuthorInfo(author.getId(), author.getDisplayName());
        }
    }

    public record PinnedByInfo(Long id, String name) {
        public static PinnedByInfo from(User user) {
            return new PinnedByInfo(user.getId(), user.getDisplayName());
        }
    }
}
