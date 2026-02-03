package igrus.web.community.like.comment_like.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
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

/**
 * HasUserLikedCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>좋아요 여부 확인 - 좋아요한 경우</li>
 *     <li>좋아요 여부 확인 - 좋아요하지 않은 경우</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HasUserLikedCommentService 단위 테스트")
class HasUserLikedCommentServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private HasUserLikedCommentService hasUserLikedCommentService;

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
    @DisplayName("좋아요 여부 확인 - 좋아요한 경우")
    void hasUserLiked_true() {
        // given
        given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                .willReturn(true);

        // when
        boolean result = hasUserLikedCommentService.hasUserLiked(targetComment.getId(), anotherMember.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("좋아요 여부 확인 - 좋아요하지 않은 경우")
    void hasUserLiked_false() {
        // given
        given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                .willReturn(false);

        // when
        boolean result = hasUserLikedCommentService.hasUserLiked(targetComment.getId(), anotherMember.getId());

        // then
        assertThat(result).isFalse();
    }
}
