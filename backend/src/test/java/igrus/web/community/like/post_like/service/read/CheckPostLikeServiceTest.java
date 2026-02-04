package igrus.web.community.like.post_like.service.read;

import igrus.web.community.like.post_like.repository.PostLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static igrus.web.common.fixture.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * CheckPostLikeService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckPostLikeService 단위 테스트")
class CheckPostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private CheckPostLikeService checkPostLikeService;

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
            boolean result = checkPostLikeService.isLikedByUser(postId, userId);

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
            boolean result = checkPostLikeService.isLikedByUser(postId, userId);

            // then
            assertThat(result).isFalse();
        }
    }
}
