package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.exception.PinnedPostNotFoundException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
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

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PinnedPostTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePinnedPostDisplayOrderService 단위 테스트")
class UpdatePinnedPostDisplayOrderServiceTest {

    @Mock
    private PinnedPostRepository pinnedPostRepository;

    @InjectMocks
    private UpdatePinnedPostDisplayOrderService updatePinnedPostDisplayOrderService;

    private Board board;
    private User operator;
    private Post post;

    @BeforeEach
    void setUp() {
        board = generalBoard();
        operator = createOperatorWithId();
        post = normalPost(board, createMemberWithId());
    }

    @Nested
    @DisplayName("표시 순서 변경 성공")
    class UpdateSuccess {

        @Test
        @DisplayName("유효한 순서로 변경 성공")
        void updateDisplayOrder_WithValidOrder_Success() {
            // given
            PinnedPost pinnedPost = pinnedPost(post, operator, 1);
            given(pinnedPostRepository.findByIdAndDeletedFalse(pinnedPost.getId()))
                    .willReturn(Optional.of(pinnedPost));

            UpdateDisplayOrderRequest request = updateOrderRequest(5);

            // when
            PinnedPostDetailResponse response = updatePinnedPostDisplayOrderService
                    .updateDisplayOrder(pinnedPost.getId(), request);

            // then
            assertThat(response.displayOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("표시 순서 변경 실패")
    class UpdateFailure {

        @Test
        @DisplayName("존재하지 않는 고정 게시글이면 예외 발생")
        void updateDisplayOrder_NotFound_ThrowsException() {
            // given
            given(pinnedPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            UpdateDisplayOrderRequest request = updateOrderRequest(5);

            // when & then
            assertThatThrownBy(() -> updatePinnedPostDisplayOrderService.updateDisplayOrder(999L, request))
                    .isInstanceOf(PinnedPostNotFoundException.class);
        }
    }
}
