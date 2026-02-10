package igrus.web.community.pinnedpost.domain;

import igrus.web.community.board.domain.Board;
import igrus.web.community.pinnedpost.exception.InvalidDisplayOrderException;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PinnedPost 엔티티 단위 테스트")
class PinnedPostTest {

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
    @DisplayName("고정 게시글 생성")
    class Create {

        @Test
        @DisplayName("유효한 입력으로 고정 게시글 생성 성공")
        void create_WithValidInputs_Success() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 1);

            assertThat(pinnedPost.getPost()).isEqualTo(post);
            assertThat(pinnedPost.getPinnedBy()).isEqualTo(operator);
            assertThat(pinnedPost.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("displayOrder가 0이면 예외 발생")
        void create_WithZeroOrder_ThrowsException() {
            assertThatThrownBy(() -> PinnedPost.create(post, operator, 0))
                    .isInstanceOf(InvalidDisplayOrderException.class);
        }

        @Test
        @DisplayName("displayOrder가 음수이면 예외 발생")
        void create_WithNegativeOrder_ThrowsException() {
            assertThatThrownBy(() -> PinnedPost.create(post, operator, -1))
                    .isInstanceOf(InvalidDisplayOrderException.class);
        }

        @Test
        @DisplayName("displayOrder가 null이면 예외 발생")
        void create_WithNullOrder_ThrowsException() {
            assertThatThrownBy(() -> PinnedPost.create(post, operator, null))
                    .isInstanceOf(InvalidDisplayOrderException.class);
        }

        @Test
        @DisplayName("큰 displayOrder 값으로 생성 성공")
        void create_WithLargeOrder_Success() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 999);

            assertThat(pinnedPost.getDisplayOrder()).isEqualTo(999);
        }
    }

    @Nested
    @DisplayName("표시 순서 변경")
    class UpdateDisplayOrder {

        @Test
        @DisplayName("유효한 순서로 변경 성공")
        void updateDisplayOrder_WithValidOrder_Success() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 1);

            pinnedPost.updateDisplayOrder(5);

            assertThat(pinnedPost.getDisplayOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("0으로 변경 시 예외 발생")
        void updateDisplayOrder_WithZero_ThrowsException() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 1);

            assertThatThrownBy(() -> pinnedPost.updateDisplayOrder(0))
                    .isInstanceOf(InvalidDisplayOrderException.class);
        }

        @Test
        @DisplayName("음수로 변경 시 예외 발생")
        void updateDisplayOrder_WithNegative_ThrowsException() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 1);

            assertThatThrownBy(() -> pinnedPost.updateDisplayOrder(-1))
                    .isInstanceOf(InvalidDisplayOrderException.class);
        }
    }

    @Nested
    @DisplayName("Soft Delete")
    class SoftDelete {

        @Test
        @DisplayName("삭제 시 deleted 플래그가 true로 변경")
        void delete_SetsDeletedFlag() {
            PinnedPost pinnedPost = PinnedPost.create(post, operator, 1);

            pinnedPost.delete(operator.getId());

            assertThat(pinnedPost.isDeleted()).isTrue();
            assertThat(pinnedPost.getDeletedBy()).isEqualTo(operator.getId());
            assertThat(pinnedPost.getDeletedAt()).isNotNull();
        }
    }
}
