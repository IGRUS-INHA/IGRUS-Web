package igrus.web.community.comment.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.exception.CommentAccessDeniedException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
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

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * CommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-001: 일반 댓글 작성</li>
 *     <li>CMT-002: 익명 댓글 작성 (자유게시판)</li>
 *     <li>CMT-003: 익명 옵션 없음 (정보공유)</li>
 *     <li>CMT-004: 500자 초과 댓글 거부</li>
 *     <li>CMT-007: 삭제된 게시글에 댓글 작성 불가</li>
 *     <li>CMT-010: 대댓글 작성</li>
 *     <li>CMT-011: 대댓글에 대댓글 불가</li>
 *     <li>CMT-020: 댓글 계층 구조 표시</li>
 *     <li>CMT-030: 본인 댓글 삭제</li>
 *     <li>CMT-033: 타인 댓글 삭제 API 접근 거부</li>
 *     <li>CMT-034: 관리자 타인 댓글 삭제</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 단위 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private Board generalBoard;
    private Board insightBoard;
    private User memberUser;
    private User anotherMember;
    private User operatorUser;
    private Post post;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        insightBoard = insightBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        operatorUser = createOperatorWithId();
        post = normalPost(generalBoard, memberUser);
    }

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("CMT-001: 정회원이 일반 댓글 작성 성공")
        void createComment_success() {
            // given
            CreateCommentRequest request = createCommentRequest();
            given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                return withId(comment, DEFAULT_COMMENT_ID);
            });

            // when
            CommentResponse response = commentService.createComment(post.getId(), request, memberUser.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(DEFAULT_COMMENT_CONTENT);
            assertThat(response.isAnonymous()).isFalse();
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("CMT-002: 익명 허용 게시판에서 익명 댓글 작성 성공")
        void createAnonymousComment_success() {
            // given
            CreateCommentRequest request = anonymousCommentRequest();
            given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                return withId(comment, DEFAULT_COMMENT_ID);
            });

            // when
            CommentResponse response = commentService.createComment(post.getId(), request, memberUser.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.isAnonymous()).isTrue();
            assertThat(response.getAuthorName()).isEqualTo("익명");
        }

        @Test
        @DisplayName("CMT-003: 익명 비허용 게시판에서 익명 댓글 작성 시 실패")
        void createAnonymousComment_notAllowed() {
            // given
            Post insightPost = normalPost(insightBoard, memberUser);
            CreateCommentRequest request = anonymousCommentRequest();
            given(postRepository.findById(insightPost.getId())).willReturn(Optional.of(insightPost));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> commentService.createComment(insightPost.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("CMT-007: 삭제된 게시글에 댓글 작성 시 실패")
        void createComment_deletedPost() {
            // given
            Post deletedPost = normalPost(generalBoard, memberUser);
            deletedPost.delete(memberUser.getId());
            CreateCommentRequest request = createCommentRequest();
            given(postRepository.findById(deletedPost.getId())).willReturn(Optional.of(deletedPost));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> commentService.createComment(deletedPost.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 댓글 작성 시 PostNotFoundException 발생")
        void createComment_postNotFound() {
            // given
            CreateCommentRequest request = createCommentRequest();
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(999L, request, memberUser.getId()))
                    .isInstanceOf(PostNotFoundException.class);
        }
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
            given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
            given(commentRepository.findById(parentComment.getId())).willReturn(Optional.of(parentComment));
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment reply = invocation.getArgument(0);
                return withId(reply, DEFAULT_REPLY_ID);
            });

            // when
            CommentResponse response = commentService.createReply(
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
            given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
            given(commentRepository.findById(replyComment.getId())).willReturn(Optional.of(replyComment));

            // when & then
            assertThatThrownBy(() -> commentService.createReply(
                    post.getId(), replyComment.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 CommentNotFoundException 발생")
        void createReply_parentNotFound() {
            // given
            CreateCommentRequest request = createCommentRequest();
            given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createReply(
                    post.getId(), 999L, request, memberUser.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 조회")
    class GetComments {

        @Test
        @DisplayName("CMT-020: 게시글의 댓글 계층 구조로 조회")
        void getComments_hierarchical() {
            // given
            Comment comment1 = comment(post, memberUser, 1L);
            Comment comment2 = comment(post, anotherMember, 2L);
            Comment reply1 = reply(post, comment1, anotherMember);

            given(postRepository.existsById(post.getId())).willReturn(true);
            given(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()))
                    .willReturn(List.of(comment1, comment2, reply1));
            given(commentLikeRepository.countByCommentId(anyLong())).willReturn(0L);
            given(commentLikeRepository.existsByCommentIdAndUserId(anyLong(), anyLong())).willReturn(false);
            given(commentRepository.countByPostIdAndNotDeleted(post.getId())).willReturn(3L);

            // when
            CommentListResponse response = commentService.getCommentsByPostId(post.getId(), memberUser.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getComments()).hasSize(2); // 부모 댓글 2개
            assertThat(response.getTotalCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("존재하지 않는 게시글 댓글 조회 시 PostNotFoundException 발생")
        void getComments_postNotFound() {
            // given
            given(postRepository.existsById(anyLong())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.getCommentsByPostId(999L, memberUser.getId()))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("CMT-030: 본인 댓글 삭제 성공 (Soft Delete)")
        void deleteComment_byAuthor_success() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));

            // when
            commentService.deleteComment(post.getId(), targetComment.getId(), memberUser.getId());

            // then
            assertThat(targetComment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("CMT-033: 타인 댓글 삭제 시 CommentAccessDeniedException 발생")
        void deleteComment_byOther_denied() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(anotherMember.getId())).willReturn(Optional.of(anotherMember));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(
                    post.getId(), targetComment.getId(), anotherMember.getId()))
                    .isInstanceOf(CommentAccessDeniedException.class);
        }

        @Test
        @DisplayName("CMT-034: 관리자가 타인 댓글 삭제 성공")
        void deleteComment_byOperator_success() {
            // given
            Comment targetComment = comment(post, memberUser);
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(operatorUser.getId())).willReturn(Optional.of(operatorUser));

            // when
            commentService.deleteComment(post.getId(), targetComment.getId(), operatorUser.getId());

            // then
            assertThat(targetComment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 CommentNotFoundException 발생")
        void deleteComment_notFound() {
            // given
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(
                    post.getId(), 999L, memberUser.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}
