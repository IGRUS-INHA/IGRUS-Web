package igrus.web.user.mypage.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import igrus.web.user.mypage.dto.response.MyCommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GetMyCommentsService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-006: 내 댓글 목록 조회 성공</li>
 *     <li>MP-007: 댓글 없는 경우 빈 페이지 반환</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyCommentsService 단위 테스트")
class GetMyCommentsServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private GetMyCommentsService getMyCommentsService;

    private User memberUser;
    private Board generalBoard;
    private Post post;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
        generalBoard = generalBoard();
        post = normalPost(generalBoard, memberUser, 1L);
    }

    @Nested
    @DisplayName("내 댓글 목록 조회 테스트")
    class GetMyCommentsTest {

        @DisplayName("MP-006: 내 댓글 목록 조회 성공")
        @Test
        void getMyComments_ReturnsComments() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Comment comment1 = comment(post, memberUser, 1L);
            Comment comment2 = comment(post, memberUser, 2L);

            Page<Comment> commentPage = new PageImpl<>(List.of(comment1, comment2), pageable, 2);

            given(commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(commentPage);

            // when
            Page<MyCommentResponse> result = getMyCommentsService.getMyComments(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).postId()).isEqualTo(post.getId());
            assertThat(result.getContent().get(0).postTitle()).isEqualTo(post.getTitle());
        }

        @DisplayName("MP-007: 댓글 없는 경우 빈 페이지 반환")
        @Test
        void getMyComments_WhenEmpty_ReturnsEmptyPage() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Page<Comment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<MyCommentResponse> result = getMyCommentsService.getMyComments(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
