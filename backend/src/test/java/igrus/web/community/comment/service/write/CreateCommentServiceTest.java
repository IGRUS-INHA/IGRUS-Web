package igrus.web.community.comment.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.comment.service.support.CommentFinder;
import igrus.web.community.comment.service.support.CommentValidator;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
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
 * CreateCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-001: 일반 댓글 작성</li>
 *     <li>CMT-002: 익명 댓글 작성 (자유게시판)</li>
 *     <li>CMT-003: 익명 옵션 없음 (정보공유)</li>
 *     <li>CMT-007: 삭제된 게시글에 댓글 작성 불가</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCommentService 단위 테스트")
class CreateCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentFinder commentFinder;

    @Mock
    private CommentValidator commentValidator;

    @InjectMocks
    private CreateCommentService createCommentService;

    private Board generalBoard;
    private Board insightBoard;
    private User memberUser;
    private Post post;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        insightBoard = insightBoard();
        memberUser = createMemberWithId();
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
            given(commentFinder.findPostById(post.getId())).willReturn(post);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                return withId(comment, DEFAULT_COMMENT_ID);
            });

            // when
            CommentResponse response = createCommentService.createComment(post.getId(), request, memberUser.getId());

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
            given(commentFinder.findPostById(post.getId())).willReturn(post);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                return withId(comment, DEFAULT_COMMENT_ID);
            });

            // when
            CommentResponse response = createCommentService.createComment(post.getId(), request, memberUser.getId());

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
            given(commentFinder.findPostById(insightPost.getId())).willReturn(insightPost);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            doThrow(InvalidCommentException.anonymousNotAllowed())
                    .when(commentValidator).validateAnonymousOption(insightPost, true);

            // when & then
            assertThatThrownBy(() -> createCommentService.createComment(insightPost.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("CMT-007: 삭제된 게시글에 댓글 작성 시 실패")
        void createComment_deletedPost() {
            // given
            Post deletedPost = normalPost(generalBoard, memberUser);
            deletedPost.delete(memberUser.getId());
            CreateCommentRequest request = createCommentRequest();
            given(commentFinder.findPostById(deletedPost.getId())).willReturn(deletedPost);
            given(commentFinder.findUserById(memberUser.getId())).willReturn(memberUser);
            doThrow(InvalidCommentException.postDeletedCannotComment())
                    .when(commentValidator).validatePostNotDeleted(deletedPost);

            // when & then
            assertThatThrownBy(() -> createCommentService.createComment(deletedPost.getId(), request, memberUser.getId()))
                    .isInstanceOf(InvalidCommentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 댓글 작성 시 PostNotFoundException 발생")
        void createComment_postNotFound() {
            // given
            CreateCommentRequest request = createCommentRequest();
            given(commentFinder.findPostById(999L)).willThrow(new PostNotFoundException(999L));

            // when & then
            assertThatThrownBy(() -> createCommentService.createComment(999L, request, memberUser.getId()))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }
}
