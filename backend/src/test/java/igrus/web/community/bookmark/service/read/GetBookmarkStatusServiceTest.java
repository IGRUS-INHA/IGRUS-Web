package igrus.web.community.bookmark.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * GetBookmarkStatusService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetBookmarkStatusService 단위 테스트")
class GetBookmarkStatusServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private GetBookmarkStatusService getBookmarkStatusService;

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
    @DisplayName("북마크 상태 응답 조회 테스트")
    class GetBookmarkStatusTest {

        @DisplayName("북마크한 게시글의 상태 조회 시 bookmarked=true 응답")
        @Test
        void getBookmarkStatus_WhenBookmarked_ReturnsBookmarkedTrue() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();

            given(postRepository.existsById(postId)).willReturn(true);
            given(bookmarkRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

            // when
            BookmarkStatusResponse response = getBookmarkStatusService.getBookmarkStatus(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isTrue();
        }

        @DisplayName("북마크하지 않은 게시글의 상태 조회 시 bookmarked=false 응답")
        @Test
        void getBookmarkStatus_WhenNotBookmarked_ReturnsBookmarkedFalse() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();

            given(postRepository.existsById(postId)).willReturn(true);
            given(bookmarkRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);

            // when
            BookmarkStatusResponse response = getBookmarkStatusService.getBookmarkStatus(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isFalse();
        }

        @DisplayName("존재하지 않는 게시글 상태 조회 시 PostNotFoundException 발생")
        @Test
        void getBookmarkStatus_WhenPostNotFound_ThrowsPostNotFoundException() {
            // given
            Long nonExistentPostId = 999L;
            Long userId = memberUser.getId();

            given(postRepository.existsById(nonExistentPostId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> getBookmarkStatusService.getBookmarkStatus(nonExistentPostId, userId))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }
}
