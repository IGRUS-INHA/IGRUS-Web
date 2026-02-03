package igrus.web.community.like.comment_like.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.like.comment_like.domain.CommentLike;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * LikeCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-040: 댓글 좋아요 추가</li>
 *     <li>CMT-042: 본인 댓글 좋아요 불가</li>
 *     <li>중복 좋아요 불가</li>
 *     <li>존재하지 않는 댓글 좋아요 시 실패</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LikeCommentService 단위 테스트")
class LikeCommentServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentLikeValidator commentLikeValidator;

    @InjectMocks
    private LikeCommentService likeCommentService;

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
    @DisplayName("CMT-040: 댓글 좋아요 성공")
    void likeComment_success() {
        // given
        given(commentLikeValidator.findCommentById(targetComment.getId())).willReturn(targetComment);
        given(commentLikeValidator.findUserById(anotherMember.getId())).willReturn(anotherMember);

        // when
        likeCommentService.likeComment(targetComment.getId(), anotherMember.getId());

        // then
        verify(commentLikeValidator).validateNotOwnComment(targetComment, anotherMember);
        verify(commentLikeValidator).validateNotAlreadyLiked(targetComment.getId(), anotherMember.getId());
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("CMT-042: 본인 댓글에 좋아요 시 CommentLikeException 발생")
    void likeComment_ownComment_fails() {
        // given
        given(commentLikeValidator.findCommentById(targetComment.getId())).willReturn(targetComment);
        given(commentLikeValidator.findUserById(memberUser.getId())).willReturn(memberUser);
        willThrow(CommentLikeException.cannotLikeOwnComment())
                .given(commentLikeValidator).validateNotOwnComment(targetComment, memberUser);

        // when & then
        assertThatThrownBy(() -> likeCommentService.likeComment(targetComment.getId(), memberUser.getId()))
                .isInstanceOf(CommentLikeException.class);
    }

    @Test
    @DisplayName("이미 좋아요한 댓글에 중복 좋아요 시 CommentLikeException 발생")
    void likeComment_alreadyLiked_fails() {
        // given
        given(commentLikeValidator.findCommentById(targetComment.getId())).willReturn(targetComment);
        given(commentLikeValidator.findUserById(anotherMember.getId())).willReturn(anotherMember);
        willThrow(CommentLikeException.alreadyLiked())
                .given(commentLikeValidator).validateNotAlreadyLiked(targetComment.getId(), anotherMember.getId());

        // when & then
        assertThatThrownBy(() -> likeCommentService.likeComment(targetComment.getId(), anotherMember.getId()))
                .isInstanceOf(CommentLikeException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요 시 CommentNotFoundException 발생")
    void likeComment_notFound() {
        // given
        given(commentLikeValidator.findCommentById(999L))
                .willThrow(new CommentNotFoundException(999L));

        // when & then
        assertThatThrownBy(() -> likeCommentService.likeComment(999L, anotherMember.getId()))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
