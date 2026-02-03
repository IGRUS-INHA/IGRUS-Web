package igrus.web.community.bookmark.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ToggleBookmarkService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-010: 게시글 북마크 추가</li>
 *     <li>LKB-011: 게시글 북마크 취소 (토글)</li>
 *     <li>LKB-013: 북마크 1인 1회 제한 (토글로 동작)</li>
 *     <li>LKB-041: 삭제된 게시글 북마크 시도 시 PostDeletedException 발생</li>
 *     <li>LKB-093: 북마크 취소 시 Hard Delete</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ToggleBookmarkService 단위 테스트")
class ToggleBookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ToggleBookmarkService toggleBookmarkService;

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
    @DisplayName("북마크 토글 테스트")
    class ToggleBookmarkTest {

        @DisplayName("LKB-010: 게시글 북마크 추가")
        @Test
        void toggleBookmark_WhenNotBookmarked_AddsBookmark() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.empty());
            given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> {
                Bookmark bookmark = invocation.getArgument(0);
                return withId(bookmark, 1L);
            });

            // when
            BookmarkToggleResponse response = toggleBookmarkService.toggleBookmark(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isTrue();
            verify(bookmarkRepository).save(any(Bookmark.class));
            verify(bookmarkRepository, never()).delete(any(Bookmark.class));
        }

        @DisplayName("LKB-011: 게시글 북마크 취소 (토글)")
        @Test
        void toggleBookmark_WhenAlreadyBookmarked_RemovesBookmark() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();
            Bookmark existingBookmark = withId(Bookmark.create(normalPost, memberUser), 1L);

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingBookmark));

            // when
            BookmarkToggleResponse response = toggleBookmarkService.toggleBookmark(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isFalse();
            verify(bookmarkRepository).delete(existingBookmark);
            verify(bookmarkRepository, never()).save(any(Bookmark.class));
        }

        @DisplayName("LKB-013: 북마크 1인 1회 제한 (토글로 동작) - 이미 북마크한 경우 취소됨")
        @Test
        void toggleBookmark_MultipleCallsToggle_BookmarkStateToggles() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();
            Bookmark existingBookmark = withId(Bookmark.create(normalPost, memberUser), 1L);

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingBookmark));

            // when
            BookmarkToggleResponse response = toggleBookmarkService.toggleBookmark(postId, userId);

            // then
            assertThat(response.bookmarked()).isFalse();
            verify(bookmarkRepository).delete(existingBookmark);
        }

        @DisplayName("LKB-093: 북마크 취소 시 Hard Delete")
        @Test
        void toggleBookmark_WhenCanceling_PerformsHardDelete() {
            // given
            Long postId = normalPost.getId();
            Long userId = memberUser.getId();
            Bookmark existingBookmark = withId(Bookmark.create(normalPost, memberUser), 1L);

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingBookmark));

            // when
            toggleBookmarkService.toggleBookmark(postId, userId);

            // then
            verify(bookmarkRepository).delete(existingBookmark);
        }

        @DisplayName("LKB-041: 삭제된 게시글 북마크 시도 시 PostDeletedException 발생")
        @Test
        void toggleBookmark_WhenPostDeleted_ThrowsPostDeletedException() {
            // given
            Post deletedPost = normalPost(generalBoard, anotherMemberUser, 2L);
            deletedPost.delete(anotherMemberUser.getId());

            Long postId = deletedPost.getId();
            Long userId = memberUser.getId();

            given(postRepository.findById(postId)).willReturn(Optional.of(deletedPost));

            // when & then
            assertThatThrownBy(() -> toggleBookmarkService.toggleBookmark(postId, userId))
                    .isInstanceOf(PostDeletedException.class);

            verify(bookmarkRepository, never()).save(any(Bookmark.class));
            verify(bookmarkRepository, never()).delete(any(Bookmark.class));
        }

        @DisplayName("존재하지 않는 게시글 북마크 시도 시 PostNotFoundException 발생")
        @Test
        void toggleBookmark_WhenPostNotFound_ThrowsPostNotFoundException() {
            // given
            Long nonExistentPostId = 999L;
            Long userId = memberUser.getId();

            given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toggleBookmarkService.toggleBookmark(nonExistentPostId, userId))
                    .isInstanceOf(PostNotFoundException.class);

            verify(bookmarkRepository, never()).save(any(Bookmark.class));
        }

        @DisplayName("존재하지 않는 사용자 북마크 시도 시 UserNotFoundException 발생")
        @Test
        void toggleBookmark_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long postId = normalPost.getId();
            Long nonExistentUserId = 999L;

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toggleBookmarkService.toggleBookmark(postId, nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(bookmarkRepository, never()).save(any(Bookmark.class));
        }
    }

    @Nested
    @DisplayName("본인 게시글 북마크 테스트")
    class SelfBookmarkTest {

        @DisplayName("본인 게시글 북마크 가능")
        @Test
        void toggleBookmark_OwnPost_Success() {
            // given
            Post ownPost = normalPost(generalBoard, memberUser, 10L);
            Long postId = ownPost.getId();
            Long userId = memberUser.getId();

            given(postRepository.findById(postId)).willReturn(Optional.of(ownPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findByPostAndUser(ownPost, memberUser)).willReturn(Optional.empty());
            given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> {
                Bookmark bookmark = invocation.getArgument(0);
                return withId(bookmark, 1L);
            });

            // when
            BookmarkToggleResponse response = toggleBookmarkService.toggleBookmark(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isTrue();
            verify(bookmarkRepository).save(any(Bookmark.class));
        }
    }
}
