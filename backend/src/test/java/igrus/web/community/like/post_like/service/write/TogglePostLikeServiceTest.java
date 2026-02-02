package igrus.web.community.like.post_like.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.PostLikeToggleResponse;
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
 * TogglePostLikeService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-001: 게시글 좋아요 추가</li>
 *     <li>LKB-002: 게시글 좋아요 취소 (토글)</li>
 *     <li>LKB-003: 본인 게시글 좋아요 가능</li>
 *     <li>LKB-006: 좋아요 1인 1회 제한 (토글로 동작)</li>
 *     <li>LKB-040: 삭제된 게시글 좋아요 시도 시 PostDeletedException 발생</li>
 *     <li>LKB-092: 좋아요 취소 시 Hard Delete</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TogglePostLikeService 단위 테스트")
class TogglePostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TogglePostLikeService togglePostLikeService;

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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

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
            assertThatThrownBy(() -> togglePostLikeService.toggleLike(postId, userId))
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
            togglePostLikeService.toggleLike(postId, userId);

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
            assertThatThrownBy(() -> togglePostLikeService.toggleLike(postId, userId))
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
            assertThatThrownBy(() -> togglePostLikeService.toggleLike(postId, userId))
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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

            // then
            assertThat(response.likeCount()).isEqualTo(initialLikeCount - 1);
            verify(postRepository).save(normalPost);
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
            PostLikeToggleResponse response = togglePostLikeService.toggleLike(postId, userId);

            // then
            assertThat(response.likeCount()).isGreaterThanOrEqualTo(0);
        }
    }
}
