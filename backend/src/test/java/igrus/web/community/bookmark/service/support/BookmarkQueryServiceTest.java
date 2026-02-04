package igrus.web.community.bookmark.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * BookmarkQueryService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkQueryService 단위 테스트")
class BookmarkQueryServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMemberUser;
    private Post normalPost;

    @BeforeEach
    void setUp() {
        // 게시판 생성 - 픽스처 사용
        generalBoard = generalBoard();

        // 사용자 생성 - 픽스처 사용
        memberUser = createMemberWithId();
        anotherMemberUser = createAnotherMemberWithId();

        // 게시글 생성 - 픽스처 사용
        normalPost = normalPost(generalBoard, anotherMemberUser);
    }

    @Nested
    @DisplayName("북마크 상태 조회 테스트")
    class IsBookmarkedByUserTest {

        @DisplayName("사용자가 북마크한 게시글인 경우 true 반환")
        @Test
        void isBookmarkedByUser_WhenBookmarked_ReturnsTrue() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();

            given(bookmarkRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

            // when
            boolean result = bookmarkQueryService.isBookmarkedByUser(postId, userId);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("사용자가 북마크하지 않은 게시글인 경우 false 반환")
        @Test
        void isBookmarkedByUser_WhenNotBookmarked_ReturnsFalse() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();

            given(bookmarkRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);

            // when
            boolean result = bookmarkQueryService.isBookmarkedByUser(postId, userId);

            // then
            assertThat(result).isFalse();
        }
    }
}
