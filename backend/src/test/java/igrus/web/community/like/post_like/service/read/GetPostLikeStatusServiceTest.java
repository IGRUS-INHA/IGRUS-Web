package igrus.web.community.like.post_like.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.like.post_like.dto.response.PostLikeStatusResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
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
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * GetPostLikeStatusService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPostLikeStatusService 단위 테스트")
class GetPostLikeStatusServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private GetPostLikeStatusService getPostLikeStatusService;

    private Post normalPost;

    @BeforeEach
    void setUp() {
        // 게시판 생성 - 픽스처 사용
        Board generalBoard = generalBoard();

        // 게시글 생성 - 픽스처 사용
        normalPost = normalPost(generalBoard, createAnotherMemberWithId());
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

            setField(normalPost, "likeCount", 3); // 좋아요 3개

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

            // when
            PostLikeStatusResponse response = getPostLikeStatusService.getLikeStatus(postId, userId);

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

            setField(normalPost, "likeCount", 1); // 다른 사용자가 좋아요

            given(postRepository.findById(postId)).willReturn(Optional.of(normalPost));
            given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);

            // when
            PostLikeStatusResponse response = getPostLikeStatusService.getLikeStatus(postId, userId);

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
            assertThatThrownBy(() -> getPostLikeStatusService.getLikeStatus(postId, userId))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }
}
