package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.exception.PinnedPostNotFoundException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
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
@DisplayName("DeletePinnedPostService 단위 테스트")
class DeletePinnedPostServiceTest {

    @Mock
    private PinnedPostRepository pinnedPostRepository;

    @InjectMocks
    private DeletePinnedPostService deletePinnedPostService;

    private Board board;
    private User operator;
    private Post post;
    private AuthenticatedUser operatorAuth;

    @BeforeEach
    void setUp() {
        board = generalBoard();
        operator = createOperatorWithId();
        post = normalPost(board, createMemberWithId());
        operatorAuth = operatorAuth();
    }

    @Nested
    @DisplayName("고정 해제 성공")
    class DeleteSuccess {

        @Test
        @DisplayName("고정 게시글 soft delete 성공")
        void deletePinnedPost_Success() {
            // given
            PinnedPost pinnedPost = pinnedPost(post, operator, 1);
            given(pinnedPostRepository.findByIdAndDeletedFalse(pinnedPost.getId()))
                    .willReturn(Optional.of(pinnedPost));

            // when
            deletePinnedPostService.deletePinnedPost(pinnedPost.getId(), operatorAuth);

            // then
            assertThat(pinnedPost.isDeleted()).isTrue();
            assertThat(pinnedPost.getDeletedBy()).isEqualTo(operatorAuth.userId());
        }
    }

    @Nested
    @DisplayName("고정 해제 실패")
    class DeleteFailure {

        @Test
        @DisplayName("존재하지 않는 고정 게시글이면 예외 발생")
        void deletePinnedPost_NotFound_ThrowsException() {
            // given
            given(pinnedPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deletePinnedPostService.deletePinnedPost(999L, operatorAuth))
                    .isInstanceOf(PinnedPostNotFoundException.class);
        }
    }
}
