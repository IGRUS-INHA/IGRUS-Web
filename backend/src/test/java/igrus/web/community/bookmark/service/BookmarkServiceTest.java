package igrus.web.community.bookmark.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.TestConstants.*;
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
 * BookmarkService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-010: 게시글 북마크 추가</li>
 *     <li>LKB-011: 게시글 북마크 취소 (토글)</li>
 *     <li>LKB-013: 북마크 1인 1회 제한 (토글로 동작)</li>
 *     <li>LKB-020: 북마크 목록 조회</li>
 *     <li>LKB-021: 삭제된 게시글 북마크 표시</li>
 *     <li>LKB-041: 삭제된 게시글 북마크 시도 시 PostDeletedException 발생</li>
 *     <li>LKB-093: 북마크 취소 시 Hard Delete</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkService 단위 테스트")
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

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
            BookmarkToggleResponse response = bookmarkService.toggleBookmark(postId, userId);

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
            BookmarkToggleResponse response = bookmarkService.toggleBookmark(postId, userId);

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
            BookmarkToggleResponse response = bookmarkService.toggleBookmark(postId, userId);

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
            bookmarkService.toggleBookmark(postId, userId);

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
            assertThatThrownBy(() -> bookmarkService.toggleBookmark(postId, userId))
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
            assertThatThrownBy(() -> bookmarkService.toggleBookmark(nonExistentPostId, userId))
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
            assertThatThrownBy(() -> bookmarkService.toggleBookmark(postId, nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(bookmarkRepository, never()).save(any(Bookmark.class));
        }
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
            boolean result = bookmarkService.isBookmarkedByUser(postId, userId);

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
            boolean result = bookmarkService.isBookmarkedByUser(postId, userId);

            // then
            assertThat(result).isFalse();
        }
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
            BookmarkStatusResponse response = bookmarkService.getBookmarkStatus(postId, userId);

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
            BookmarkStatusResponse response = bookmarkService.getBookmarkStatus(postId, userId);

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
            assertThatThrownBy(() -> bookmarkService.getBookmarkStatus(nonExistentPostId, userId))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("북마크 목록 조회 테스트")
    class GetMyBookmarksTest {

        @DisplayName("LKB-020: 북마크 목록 조회")
        @Test
        void getMyBookmarks_ReturnsBookmarkedPosts() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Post post1 = normalPost(generalBoard, anotherMemberUser, 1L);
            Post post2 = normalPost(generalBoard, anotherMemberUser, 2L);

            Bookmark bookmark1 = withId(Bookmark.create(post1, memberUser), 1L);
            Bookmark bookmark2 = withId(Bookmark.create(post2, memberUser), 2L);

            Page<Bookmark> bookmarkPage = new PageImpl<>(
                    List.of(bookmark1, bookmark2),
                    pageable,
                    2
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).postId()).isEqualTo(1L);
            assertThat(result.getContent().get(1).postId()).isEqualTo(2L);
            assertThat(result.getContent().get(0).isDeleted()).isFalse();
            assertThat(result.getContent().get(1).isDeleted()).isFalse();
        }

        @DisplayName("LKB-021: 삭제된 게시글 북마크 표시")
        @Test
        void getMyBookmarks_WithDeletedPost_ShowsDeletedMessage() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Post deletedPost = normalPost(generalBoard, anotherMemberUser, 1L);
            deletedPost.delete(anotherMemberUser.getId());

            Post normalPostEntity = normalPost(generalBoard, anotherMemberUser, 2L);

            Bookmark deletedPostBookmark = withId(Bookmark.create(deletedPost, memberUser), 1L);
            Bookmark normalPostBookmark = withId(Bookmark.create(normalPostEntity, memberUser), 2L);

            Page<Bookmark> bookmarkPage = new PageImpl<>(
                    List.of(deletedPostBookmark, normalPostBookmark),
                    pageable,
                    2
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            // 삭제된 게시글 확인
            BookmarkedPostResponse deletedResponse = result.getContent().get(0);
            assertThat(deletedResponse.isDeleted()).isTrue();
            assertThat(deletedResponse.title()).isNull();
            assertThat(deletedResponse.authorName()).isNull();
            assertThat(deletedResponse.deletedMessage()).isEqualTo("삭제된 게시글입니다");

            // 정상 게시글 확인
            BookmarkedPostResponse normalResponse = result.getContent().get(1);
            assertThat(normalResponse.isDeleted()).isFalse();
            assertThat(normalResponse.title()).isNotNull();
            assertThat(normalResponse.deletedMessage()).isNull();
        }

        @DisplayName("북마크 목록이 비어있는 경우 빈 페이지 반환")
        @Test
        void getMyBookmarks_WhenEmpty_ReturnsEmptyPage() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Page<Bookmark> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(emptyPage);

            // when
            Page<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @DisplayName("존재하지 않는 사용자 북마크 목록 조회 시 UserNotFoundException 발생")
        @Test
        void getMyBookmarks_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            Pageable pageable = PageRequest.of(0, 20);

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookmarkService.getMyBookmarks(nonExistentUserId, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("북마크 목록 페이지네이션 테스트")
        @Test
        void getMyBookmarks_WithPagination_ReturnsCorrectPage() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(1, 10); // 두 번째 페이지, 페이지당 10개

            Post post1 = normalPost(generalBoard, anotherMemberUser, 11L);
            Bookmark bookmark1 = withId(Bookmark.create(post1, memberUser), 11L);

            Page<Bookmark> bookmarkPage = new PageImpl<>(
                    List.of(bookmark1),
                    pageable,
                    21 // 전체 21개 중 두 번째 페이지 (페이지당 10개이므로 3페이지)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1); // 두 번째 페이지
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(21);
            assertThat(result.getTotalPages()).isEqualTo(3);
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
            BookmarkToggleResponse response = bookmarkService.toggleBookmark(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.bookmarked()).isTrue();
            verify(bookmarkRepository).save(any(Bookmark.class));
        }
    }

    @Nested
    @DisplayName("익명 게시글 북마크 테스트")
    class AnonymousPostBookmarkTest {

        @DisplayName("익명 게시글 북마크 목록 조회 시 작성자 '익명' 표시")
        @Test
        void getMyBookmarks_WithAnonymousPost_ShowsAnonymousAuthor() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Post anonymousPostEntity = anonymousPost(generalBoard, anotherMemberUser, 1L);

            Bookmark anonymousPostBookmark = withId(Bookmark.create(anonymousPostEntity, memberUser), 1L);

            Page<Bookmark> bookmarkPage = new PageImpl<>(
                    List.of(anonymousPostBookmark),
                    pageable,
                    1
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(bookmarkPage);

            // when
            Page<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).authorName()).isEqualTo("익명");
        }
    }
}
