package igrus.web.community.like.comment_like.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.like.comment_like.exception.CommentLikeException;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.like.comment_like.service.support.CommentLikeValidator;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * UnlikeCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-041: 댓글 좋아요 취소</li>
 *     <li>좋아요하지 않은 댓글 취소 시 실패</li>
 *     <li>존재하지 않는 댓글 좋아요 취소 시 실패</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnlikeCommentService 단위 테스트")
class UnlikeCommentServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentLikeValidator commentLikeValidator;

    @InjectMocks
    private UnlikeCommentService unlikeCommentService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMember;
    private Post post;
    private Comment targetComment;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        post = normalPost(generalBoard, memberUser);
        targetComment = comment(post, memberUser);
    }

    @Test
    @DisplayName("CMT-041: 좋아요 취소 성공")
    void unlikeComment_success() {
        // when
        unlikeCommentService.unlikeComment(targetComment.getId(), anotherMember.getId());

        // then
        verify(commentLikeValidator).validateCommentExists(targetComment.getId());
        verify(commentLikeValidator).validateLikeExists(targetComment.getId(), anotherMember.getId());
        verify(commentLikeRepository).deleteByCommentIdAndUserId(targetComment.getId(), anotherMember.getId());
    }

    @Test
    @DisplayName("좋아요하지 않은 댓글 취소 시 CommentLikeException 발생")
    void unlikeComment_notLiked_fails() {
        // given
        willThrow(CommentLikeException.likeNotFound())
                .given(commentLikeValidator).validateLikeExists(targetComment.getId(), anotherMember.getId());

        // when & then
        assertThatThrownBy(() -> unlikeCommentService.unlikeComment(targetComment.getId(), anotherMember.getId()))
                .isInstanceOf(CommentLikeException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 좋아요 취소 시 CommentNotFoundException 발생")
    void unlikeComment_commentNotFound() {
        // given
        willThrow(new CommentNotFoundException(999L))
                .given(commentLikeValidator).validateCommentExists(999L);

        // when & then
        assertThatThrownBy(() -> unlikeCommentService.unlikeComment(999L, anotherMember.getId()))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
