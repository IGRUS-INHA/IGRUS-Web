package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
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

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * DeletePostService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>PST-040: 본인 게시글 삭제 (Soft Delete)</li>
 *     <li>PST-043: 관리자 타인 게시글 삭제</li>
 *     <li>PST-046: 익명 게시글 본인 삭제</li>
 *     <li>PST-047: 익명 게시글 타인 삭제 시도</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePostService 단위 테스트")
class DeletePostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetBoardEntityService getBoardEntityService;

    @InjectMocks
    private DeletePostService deletePostService;

    private Board generalBoard;
    private User memberUser;
    private User operatorUser;
    private AuthenticatedUser memberAuth;
    private AuthenticatedUser operatorAuth;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();

        memberUser = createMemberWithId();
        operatorUser = createOperatorWithId();

        memberAuth = memberAuth();
        operatorAuth = operatorAuth();
    }

    @Nested
    @DisplayName("PST: 게시글 삭제 테스트")
    class PstDeletePostTest {

        @DisplayName("PST-040: 본인 게시글 삭제 (Soft Delete)")
        @Test
        void deletePost_ByAuthor_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            Post post = normalPost(generalBoard, memberUser, postId);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            deletePostService.deletePost(boardCode, postId, memberAuth);

            // then
            assertThat(post.isDeleted()).isTrue();
        }

        @DisplayName("PST-046: 익명 게시글 본인 삭제")
        @Test
        void deletePost_AnonymousPost_ByAuthor_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            Post anonymousPost = anonymousPost(generalBoard, memberUser, postId);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when
            deletePostService.deletePost(boardCode, postId, memberAuth);

            // then
            assertThat(anonymousPost.isDeleted()).isTrue();
        }

        @DisplayName("PST-047: 익명 게시글 타인 삭제 시도")
        @Test
        void deletePost_AnonymousPost_ByOther_ThrowsException() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            // operatorUser가 작성한 익명 게시글
            Post anonymousPost = anonymousPost(generalBoard, operatorUser, postId);

            // memberUser가 삭제 시도 (OPERATOR가 아닌 일반 MEMBER이므로 삭제 불가)
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when & then
            assertThatThrownBy(() -> deletePostService.deletePost(boardCode, postId, memberAuth))
                    .isInstanceOf(PostAccessDeniedException.class)
                    .hasMessageContaining("권한");
        }

        @DisplayName("PST-043: 관리자 타인 게시글 삭제")
        @Test
        void deletePost_ByOperator_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            // memberUser가 작성한 게시글
            Post post = normalPost(generalBoard, memberUser, postId);

            // operatorUser가 삭제 (OPERATOR 권한으로 타인 게시글 삭제 가능)
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            deletePostService.deletePost(boardCode, postId, operatorAuth);

            // then
            assertThat(post.isDeleted()).isTrue();
        }
    }
}
