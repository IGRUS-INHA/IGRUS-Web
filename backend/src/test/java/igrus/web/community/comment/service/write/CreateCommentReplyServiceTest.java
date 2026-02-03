package igrus.web.community.comment.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.comment.repository.CommentRepository;
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

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * CreateCommentReplyService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-010: 대댓글 작성</li>
 *     <li>CMT-011: 대댓글에 대댓글 불가</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCommentReplyService 단위 테스트")
class CreateCommentReplyServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentFinder commentFinder;

    @Mock
    private CommentValidator commentValidator;

    @InjectMocks
    private CreateCommentReplyService createCommentReplyService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMember;
    private Post post;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        post = normalPost(generalBoard, memberUser);
    }

    @Nested
    @DisplayName("대댓글 작성")
    class CreateReply {

        @Test
        @DisplayName("CMT-010: 댓글에 대댓글 작성 성공")
        void createReply_success() {
            // given
            Comment parentComment = comment(post, memberUser);
            CreateCommentRequest request = createCommentRequest("대댓글 내용입니다.");
            given(commentFinder.findPostById(post.getId())).willReturn(post);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            given(commentFinder.findCommentById(parentComment.getId())).willReturn(parentComment);
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment reply = invocation.getArgument(0);
                return withId(reply, DEFAULT_REPLY_ID);
            });

            // when
            CommentResponse response = createCommentReplyService.createReply(
                    post.getId(), parentComment.getId(), request, memberUser.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getParentCommentId()).isEqualTo(parentComment.getId());
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("CMT-011/CMT-014: 대댓글에 답글 작성 시 InvalidCommentException 발생")
        void createReply_toReply_fails() {
            // given
            Comment parentComment = comment(post, memberUser);
            Comment replyComment = reply(post, parentComment, anotherMember);
            CreateCommentRequest request = createCommentRequest();
            given(commentFinder.findPostById(post.getId())).willReturn(post);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            given(commentFinder.findCommentById(replyComment.getId())).willReturn(replyComment);
            doThrow(InvalidCommentException.replyToReplyNotAllowed())
                    .when(commentValidator).validateCanReplyTo(replyComment);

            // when & then
            assertThatThrownBy(() -> createCommentReplyService.createReply(
                    post.getId(), replyComment.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 CommentNotFoundException 발생")
        void createReply_parentNotFound() {
            // given
            CreateCommentRequest request = createCommentRequest();
            given(commentFinder.findPostById(post.getId())).willReturn(post);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            given(commentFinder.findCommentById(999L)).willThrow(new CommentNotFoundException(999L));

            // when & then
            assertThatThrownBy(() -> createCommentReplyService.createReply(
                    post.getId(), 999L, request, memberUser.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}
