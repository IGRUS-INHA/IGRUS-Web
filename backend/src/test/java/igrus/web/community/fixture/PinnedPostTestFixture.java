package igrus.web.community.fixture;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest;
import igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * PinnedPost 도메인 관련 테스트 픽스처 클래스.
 */
public final class PinnedPostTestFixture {

    public static final Long DEFAULT_PINNED_POST_ID = 100L;

    private PinnedPostTestFixture() {
    }

    // ==================== PinnedPost 생성 (ID 없음) ====================

    public static PinnedPost createPinnedPost(Post post, User pinnedBy, int displayOrder) {
        return PinnedPost.create(post, pinnedBy, displayOrder);
    }

    // ==================== PinnedPost 생성 (ID 포함) ====================

    public static PinnedPost pinnedPost(Post post, User pinnedBy, int displayOrder) {
        return withId(createPinnedPost(post, pinnedBy, displayOrder), DEFAULT_PINNED_POST_ID);
    }

    public static PinnedPost pinnedPost(Post post, User pinnedBy, int displayOrder, Long id) {
        return withId(createPinnedPost(post, pinnedBy, displayOrder), id);
    }

    // ==================== Request DTO 생성 ====================

    public static CreatePinnedPostRequest createRequest(Long postId, int displayOrder) {
        return new CreatePinnedPostRequest(postId, displayOrder);
    }

    public static UpdateDisplayOrderRequest updateOrderRequest(int displayOrder) {
        return new UpdateDisplayOrderRequest(displayOrder);
    }
}
