package igrus.web.community.like.post_like.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.PostLikeStatusResponse;
import igrus.web.community.like.post_like.dto.response.PostLikeToggleResponse;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
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
import static org.mockito.Mockito.*;

/**
 * PostLikeService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-001: 게시글 좋아요 추가</li>
 *     <li>LKB-002: 게시글 좋아요 취소 (토글)</li>
 *     <li>LKB-003: 본인 게시글 좋아요 가능</li>
 *     <li>LKB-006: 좋아요 1인 1회 제한 (토글로 동작)</li>
 *     <li>LKB-030: 좋아요 목록 조회 (최신순)</li>
 *     <li>LKB-040: 삭제된 게시글 좋아요 시도 시 PostDeletedException 발생</li>
 *     <li>LKB-092: 좋아요 취소 시 Hard Delete</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostLikeService 단위 테스트")
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostLikeService postLikeService;

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
    @DisplayName("좋아요 토글 테스트")
    class ToggleLikeTest {

        @DisplayName("LKB-001: 게시글 좋아요 추가 - 좋아요가 없을 때 토글하면 좋아요가 추가된다")
        @Test
        void toggleLike_WhenNoExistingLike_AddsLike() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.empty());
            given(postLikeRepository.save(any(PostLike.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isTrue();
            assertThat(response.likeCount()).isEqualTo(1);
            verify(postLikeRepository).save(any(PostLike.class));
            verify(postRepository).save(normalPost);
        }

        @DisplayName("LKB-002: 게시글 좋아요 취소 (토글) - 좋아요가 있을 때 토글하면 좋아요가 취소된다")
        @Test
        void toggleLike_WhenExistingLike_RemovesLike() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            PostLike existingLike = PostLike.create(normalPost, memberUser);
            withId(existingLike, 1L);

            // 좋아요가 1개 있는 상태로 설정
            normalPost.incrementLikeCount();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingLike));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isFalse();
            assertThat(response.likeCount()).isEqualTo(0);
            verify(postLikeRepository).delete(existingLike);
            verify(postRepository).save(normalPost);
        }

        @DisplayName("LKB-003: 본인 게시글 좋아요 가능 - 작성자도 본인 게시글에 좋아요를 할 수 있다")
        @Test
        void toggleLike_OnOwnPost_Success() {
            // given
            // 본인이 작성한 게시글
            Post ownPost = normalPost(generalBoard, memberUser);
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            given(postRepository.findById(postId)).willReturn(Optional.of(ownPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(ownPost, memberUser)).willReturn(Optional.empty());
            given(postLikeRepository.save(any(PostLike.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(postRepository.save(any(Post.class))).willReturn(ownPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isTrue();
            assertThat(response.likeCount()).isEqualTo(1);
            verify(postLikeRepository).save(any(PostLike.class));
        }

        @DisplayName("LKB-006: 좋아요 1인 1회 제한 (토글로 동작) - 이미 좋아요한 상태에서 다시 요청하면 취소된다")
        @Test
        void toggleLike_WhenAlreadyLiked_TogglesOff() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            PostLike existingLike = PostLike.create(normalPost, memberUser);
            withId(existingLike, 1L);

            normalPost.incrementLikeCount();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingLike));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isFalse();
            verify(postLikeRepository).delete(existingLike);
        }

        @DisplayName("LKB-040: 삭제된 게시글 좋아요 시도 시 PostDeletedException 발생")
        @Test
        void toggleLike_OnDeletedPost_ThrowsPostDeletedException() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            // 삭제된 게시글 설정
            Post deletedPost = normalPost(generalBoard, anotherMemberUser);
            deletedPost.delete(ANOTHER_MEMBER_ID);

            given(postRepository.findById(postId)).willReturn(Optional.of(deletedPost));

            // when & then
            assertThatThrownBy(() -> postLikeService.toggleLike(postId, userId))
                    .isInstanceOf(PostDeletedException.class);

            verify(postLikeRepository, never()).save(any(PostLike.class));
            verify(postLikeRepository, never()).delete(any(PostLike.class));
        }

        @DisplayName("LKB-092: 좋아요 취소 시 Hard Delete - 좋아요 취소 시 DB에서 완전히 삭제된다")
        @Test
        void toggleLike_WhenCancellingLike_PerformsHardDelete() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            PostLike existingLike = PostLike.create(normalPost, memberUser);
            withId(existingLike, 1L);

            normalPost.incrementLikeCount();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingLike));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            postLikeService.toggleLike(postId, userId);

            // then
            // Hard Delete 확인: postLikeRepository.delete()가 호출되어야 함
            verify(postLikeRepository).delete(existingLike);
            // Soft Delete가 아닌 것 확인: 엔티티의 메서드 호출 없이 바로 delete
            verify(postLikeRepository, never()).save(existingLike);
        }

        @DisplayName("존재하지 않는 게시글에 좋아요 시도 시 PostNotFoundException 발생")
        @Test
        void toggleLike_OnNonExistentPost_ThrowsPostNotFoundException() {
            // given
            Long postId = 999L;
            Long userId = DEFAULT_MEMBER_ID;

            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.toggleLike(postId, userId))
                    .isInstanceOf(PostNotFoundException.class);

            verify(postLikeRepository, never()).save(any(PostLike.class));
        }

        @DisplayName("존재하지 않는 사용자로 좋아요 시도 시 UserNotFoundException 발생")
        @Test
        void toggleLike_WithNonExistentUser_ThrowsUserNotFoundException() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = 999L;

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.toggleLike(postId, userId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(postLikeRepository, never()).save(any(PostLike.class));
        }

        @DisplayName("좋아요 추가 시 게시글의 좋아요 수가 증가한다")
        @Test
        void toggleLike_WhenAddingLike_IncrementsLikeCount() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            int initialLikeCount = normalPost.getLikeCount();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.empty());
            given(postLikeRepository.save(any(PostLike.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response.likeCount()).isEqualTo(initialLikeCount + 1);
            verify(postRepository).save(normalPost);
        }

        @DisplayName("좋아요 취소 시 게시글의 좋아요 수가 감소한다")
        @Test
        void toggleLike_WhenRemovingLike_DecrementsLikeCount() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            PostLike existingLike = PostLike.create(normalPost, memberUser);
            withId(existingLike, 1L);

            normalPost.incrementLikeCount();
            normalPost.incrementLikeCount(); // 좋아요 2개 상태
            int initialLikeCount = normalPost.getLikeCount();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingLike));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response.likeCount()).isEqualTo(initialLikeCount - 1);
            verify(postRepository).save(normalPost);
        }
    }

    @Nested
    @DisplayName("좋아요 여부 확인 테스트")
    class IsLikedByUserTest {

        @DisplayName("사용자가 좋아요한 게시글인 경우 true를 반환한다")
        @Test
        void isLikedByUser_WhenLiked_ReturnsTrue() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

            // when
            boolean result = postLikeService.isLikedByUser(postId, userId);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("사용자가 좋아요하지 않은 게시글인 경우 false를 반환한다")
        @Test
        void isLikedByUser_WhenNotLiked_ReturnsFalse() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);

            // when
            boolean result = postLikeService.isLikedByUser(postId, userId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("좋아요 상태 조회 테스트")
    class GetLikeStatusTest {

        @DisplayName("좋아요 상태와 총 좋아요 수를 조회한다")
        @Test
        void getLikeStatus_ReturnsLikeStatusWithCount() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            normalPost.incrementLikeCount();
            normalPost.incrementLikeCount();
            normalPost.incrementLikeCount(); // 좋아요 3개

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

            // when
            PostLikeStatusResponse response = postLikeService.getLikeStatus(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isTrue();
            assertThat(response.likeCount()).isEqualTo(3);
        }

        @DisplayName("좋아요하지 않은 상태에서 좋아요 상태 조회")
        @Test
        void getLikeStatus_WhenNotLiked_ReturnsNotLikedStatus() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;

            normalPost.incrementLikeCount(); // 다른 사용자가 좋아요

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);

            // when
            PostLikeStatusResponse response = postLikeService.getLikeStatus(postId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.liked()).isFalse();
            assertThat(response.likeCount()).isEqualTo(1);
        }

        @DisplayName("존재하지 않는 게시글의 좋아요 상태 조회 시 PostNotFoundException 발생")
        @Test
        void getLikeStatus_OnNonExistentPost_ThrowsPostNotFoundException() {
            // given
            Long postId = 999L;
            Long userId = DEFAULT_MEMBER_ID;

            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.getLikeStatus(postId, userId))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("좋아요 목록 조회 테스트")
    class GetMyLikesTest {

        @DisplayName("LKB-030: 좋아요 목록 조회 (최신순) - 사용자가 좋아요한 게시글 목록을 최신순으로 조회한다")
        @Test
        void getMyLikes_ReturnsLikedPostsInDescendingOrder() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post post1 = normalPost(generalBoard, anotherMemberUser, 1L);
            Post post2 = normalPost(generalBoard, anotherMemberUser, 2L);
            Post post3 = normalPost(generalBoard, anotherMemberUser, 3L);

            PostLike like1 = PostLike.create(post1, memberUser);
            PostLike like2 = PostLike.create(post2, memberUser);
            PostLike like3 = PostLike.create(post3, memberUser);
            withId(like1, 1L);
            withId(like2, 2L);
            withId(like3, 3L);

            // 최신순 정렬 (like3 -> like2 -> like1)
            List<PostLike> likes = List.of(like3, like2, like1);
            Page<PostLike> likePage = new PageImpl<>(likes, pageable, likes.size());

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = postLikeService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).postId()).isEqualTo(3L);
            assertThat(result.getContent().get(1).postId()).isEqualTo(2L);
            assertThat(result.getContent().get(2).postId()).isEqualTo(1L);
        }

        @DisplayName("좋아요한 게시글이 없는 경우 빈 목록을 반환한다")
        @Test
        void getMyLikes_WhenNoLikes_ReturnsEmptyPage() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Page<PostLike> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(emptyPage);

            // when
            Page<LikedPostResponse> result = postLikeService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @DisplayName("삭제된 게시글도 좋아요 목록에 포함된다")
        @Test
        void getMyLikes_IncludesDeletedPosts() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post deletedPost = normalPost(generalBoard, anotherMemberUser, 1L);
            deletedPost.delete(ANOTHER_MEMBER_ID);

            PostLike likeOnDeletedPost = PostLike.create(deletedPost, memberUser);
            withId(likeOnDeletedPost, 1L);

            Page<PostLike> likePage = new PageImpl<>(List.of(likeOnDeletedPost), pageable, 1);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = postLikeService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            LikedPostResponse response = result.getContent().get(0);
            assertThat(response.isDeleted()).isTrue();
            assertThat(response.title()).isNull();
            assertThat(response.deletedMessage()).isEqualTo("삭제된 게시글입니다");
        }

        @DisplayName("존재하지 않는 사용자의 좋아요 목록 조회 시 UserNotFoundException 발생")
        @Test
        void getMyLikes_WithNonExistentUser_ThrowsUserNotFoundException() {
            // given
            Long userId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.getMyLikes(userId, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("페이징이 올바르게 적용된다")
        @Test
        void getMyLikes_AppliesPagingCorrectly() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 페이지당 2개

            Post post1 = normalPost(generalBoard, anotherMemberUser, 3L);
            PostLike like1 = PostLike.create(post1, memberUser);
            withId(like1, 3L);

            // 전체 5개 중 두 번째 페이지 (인덱스 2, 3)
            List<PostLike> secondPageLikes = List.of(like1);
            Page<PostLike> likePage = new PageImpl<>(secondPageLikes, pageable, 5);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = postLikeService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1); // 두 번째 페이지 (0-indexed)
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3); // 5개를 2개씩 나누면 3페이지
        }

        @DisplayName("익명 게시글의 좋아요 목록 조회 시 작성자 이름이 '익명'으로 표시된다")
        @Test
        void getMyLikes_AnonymousPostShowsAnonymousAuthor() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post anonymousPost = anonymousPost(generalBoard, anotherMemberUser, 1L);
            PostLike likeOnAnonymousPost = PostLike.create(anonymousPost, memberUser);
            withId(likeOnAnonymousPost, 1L);

            Page<PostLike> likePage = new PageImpl<>(List.of(likeOnAnonymousPost), pageable, 1);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = postLikeService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            LikedPostResponse response = result.getContent().get(0);
            assertThat(response.authorName()).isEqualTo("익명");
        }
    }

    @Nested
    @DisplayName("좋아요 수 경계값 테스트")
    class LikeCountEdgeCaseTest {

        @DisplayName("좋아요 수가 0일 때 취소해도 음수가 되지 않는다")
        @Test
        void toggleLike_WhenLikeCountIsZero_DoesNotGoNegative() {
            // given
            Long postId = DEFAULT_POST_ID;
            Long userId = DEFAULT_MEMBER_ID;
            PostLike existingLike = PostLike.create(normalPost, memberUser);
            withId(existingLike, 1L);

            // 좋아요 수가 0인 상태 (비정상적인 상황이지만 방어 코드 테스트)
            assertThat(normalPost.getLikeCount()).isZero();

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findByPostAndUser(normalPost, memberUser)).willReturn(Optional.of(existingLike));
            given(postRepository.save(any(Post.class))).willReturn(normalPost);

            // when
            PostLikeToggleResponse response = postLikeService.toggleLike(postId, userId);

            // then
            assertThat(response.likeCount()).isGreaterThanOrEqualTo(0);
        }
    }
}
