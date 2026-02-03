package igrus.web.community.comment.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.exception.CommentAccessDeniedException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.service.support.CommentFinder;
import igrus.web.community.comment.service.support.CommentValidator;
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

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

/**
 * DeleteCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-030: 본인 댓글 삭제</li>
 *     <li>CMT-033: 타인 댓글 삭제 API 접근 거부</li>
 *     <li>CMT-034: 관리자 타인 댓글 삭제</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCommentService 단위 테스트")
class DeleteCommentServiceTest {

    @Mock
    private CommentFinder commentFinder;

    @Mock
    private CommentValidator commentValidator;

    @InjectMocks
    private DeleteCommentService deleteCommentService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMember;
    private User operatorUser;
    private Post post;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        operatorUser = createOperatorWithId();
        post = normalPost(generalBoard, memberUser);
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("CMT-030: 본인 댓글 삭제 성공 (Soft Delete)")
        void deleteComment_byAuthor_success() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentFinder.findCommentById(targetComment.getId())).willReturn(targetComment);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);

            // when
            deleteCommentService.deleteComment(post.getId(), targetComment.getId(), memberUser.getId());

            // then
            assertThat(targetComment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("CMT-033: 타인 댓글 삭제 시 CommentAccessDeniedException 발생")
        void deleteComment_byOther_denied() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentFinder.findCommentById(targetComment.getId())).willReturn(targetComment);
            given(commentFinder.findUserById(anotherMember.getId())).willReturn(anotherMember);
            doThrow(new CommentAccessDeniedException())
                    .when(commentValidator).validateCanDelete(targetComment, anotherMember);

            // when & then
            assertThatThrownBy(() -> deleteCommentService.deleteComment(
                    post.getId(), targetComment.getId(), anotherMember.getId()))
                    .isInstanceOf(CommentAccessDeniedException.class);
        }

        @Test
        @DisplayName("CMT-034: 관리자가 타인 댓글 삭제 성공")
        void deleteComment_byOperator_success() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentFinder.findCommentById(targetComment.getId())).willReturn(targetComment);
            given(commentFinder.findUserById(operatorUser.getId())).willReturn(operatorUser);

            // when
            deleteCommentService.deleteComment(post.getId(), targetComment.getId(), operatorUser.getId());

            // then
            assertThat(targetComment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 CommentNotFoundException 발생")
        void deleteComment_notFound() {
            // given
            given(commentFinder.findCommentById(999L)).willThrow(new CommentNotFoundException(999L));

            // when & then
            assertThatThrownBy(() -> deleteCommentService.deleteComment(
                    post.getId(), 999L, memberUser.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}
