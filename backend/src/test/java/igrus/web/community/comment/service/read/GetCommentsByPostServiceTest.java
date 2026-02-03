package igrus.web.community.comment.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * GetCommentsByPostService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-020: 댓글 계층 구조 표시</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetCommentsByPostService 단위 테스트")
class GetCommentsByPostServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private GetCommentsByPostService getCommentsByPostService;

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
            CommentListResponse response = getCommentsByPostService.getCommentsByPostId(post.getId(), memberUser.getId());

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
            assertThatThrownBy(() -> getCommentsByPostService.getCommentsByPostId(999L, memberUser.getId()))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }
}
