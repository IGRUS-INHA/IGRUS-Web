package igrus.web.community.comment.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentLike;
import igrus.web.community.comment.exception.CommentLikeException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.repository.CommentLikeRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
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

import java.util.Optional;

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
 * CommentLikeService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-040: 댓글 좋아요 추가</li>
 *     <li>CMT-041: 댓글 좋아요 취소</li>
 *     <li>CMT-042: 본인 댓글 좋아요 불가</li>
 *     <li>중복 좋아요 불가</li>
 *     <li>좋아요하지 않은 댓글 취소 시 실패</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentLikeService 단위 테스트")
class CommentLikeServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentLikeService commentLikeService;

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

    @Nested
    @DisplayName("댓글 좋아요")
    class LikeComment {

        @Test
        @DisplayName("CMT-040: 댓글 좋아요 성공")
        void likeComment_success() {
            // given
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(anotherMember.getId())).willReturn(Optional.of(anotherMember));
            given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(false);

            // when
            commentLikeService.likeComment(targetComment.getId(), anotherMember.getId());

            // then
            verify(commentLikeRepository).save(any(CommentLike.class));
        }

        @Test
        @DisplayName("CMT-042: 본인 댓글에 좋아요 시 CommentLikeException 발생")
        void likeComment_ownComment_fails() {
            // given
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> commentLikeService.likeComment(targetComment.getId(), memberUser.getId()))
                    .isInstanceOf(CommentLikeException.class);
        }

        @Test
        @DisplayName("이미 좋아요한 댓글에 중복 좋아요 시 CommentLikeException 발생")
        void likeComment_alreadyLiked_fails() {
            // given
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(anotherMember.getId())).willReturn(Optional.of(anotherMember));
            given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentLikeService.likeComment(targetComment.getId(), anotherMember.getId()))
                    .isInstanceOf(CommentLikeException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 좋아요 시 CommentNotFoundException 발생")
        void likeComment_notFound() {
            // given
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentLikeService.likeComment(999L, anotherMember.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소")
    class UnlikeComment {

        @Test
        @DisplayName("CMT-041: 좋아요 취소 성공")
        void unlikeComment_success() {
            // given
            given(commentRepository.existsById(targetComment.getId())).willReturn(true);
            given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(true);

            // when
            commentLikeService.unlikeComment(targetComment.getId(), anotherMember.getId());

            // then
            verify(commentLikeRepository).deleteByCommentIdAndUserId(targetComment.getId(), anotherMember.getId());
        }

        @Test
        @DisplayName("좋아요하지 않은 댓글 취소 시 CommentLikeException 발생")
        void unlikeComment_notLiked_fails() {
            // given
            given(commentRepository.existsById(targetComment.getId())).willReturn(true);
            given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(targetComment.getId(), anotherMember.getId()))
                    .isInstanceOf(CommentLikeException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 좋아요 취소 시 CommentNotFoundException 발생")
        void unlikeComment_commentNotFound() {
            // given
            given(commentRepository.existsById(anyLong())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentLikeService.unlikeComment(999L, anotherMember.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("좋아요 수 조회")
    class GetLikeCount {

        @Test
        @DisplayName("좋아요 수 조회 성공")
        void getLikeCount_success() {
            // given
            given(commentRepository.existsById(targetComment.getId())).willReturn(true);
            given(commentLikeRepository.countByCommentId(targetComment.getId())).willReturn(5L);

            // when
            long count = commentLikeService.getLikeCount(targetComment.getId());

            // then
            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("좋아요 여부 확인")
    class HasUserLiked {

        @Test
        @DisplayName("좋아요 여부 확인 - 좋아요한 경우")
        void hasUserLiked_true() {
            // given
            given(commentLikeRepository.existsByCommentIdAndUserId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(true);

            // when
            boolean result = commentLikeService.hasUserLiked(targetComment.getId(), anotherMember.getId());

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
            boolean result = commentLikeService.hasUserLiked(targetComment.getId(), anotherMember.getId());

            // then
            assertThat(result).isFalse();
        }
    }
}
