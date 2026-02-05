package igrus.web.community.like.comment_like.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * GetCommentLikeCountService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>좋아요 수 조회 성공</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetCommentLikeCountService 단위 테스트")
class GetCommentLikeCountServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentLikeValidator commentLikeValidator;

    @InjectMocks
    private GetCommentLikeCountService getCommentLikeCountService;

    private Board generalBoard;
    private User memberUser;
    private Post post;
    private Comment targetComment;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        post = normalPost(generalBoard, memberUser);
        targetComment = comment(post, memberUser);
    }

    @Test
    @DisplayName("좋아요 수 조회 성공")
    void getLikeCount_success() {
        // given
        given(commentLikeRepository.countByCommentId(targetComment.getId())).willReturn(5L);

        // when
        long count = getCommentLikeCountService.getLikeCount(targetComment.getId());

        // then
        verify(commentLikeValidator).validateCommentExists(targetComment.getId());
        assertThat(count).isEqualTo(5L);
    }
}
