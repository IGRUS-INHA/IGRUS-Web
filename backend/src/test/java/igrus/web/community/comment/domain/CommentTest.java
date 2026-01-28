package igrus.web.community.comment.domain;

import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comment 도메인 단위 테스트.
 */
@DisplayName("Comment 도메인")
class CommentTest {

    @Nested
    @DisplayName("createComment 정적 팩토리 메서드")
    class CreateCommentTest {

        @Test
        @DisplayName("유효한 내용으로 일반 댓글 생성 성공")
        void createComment_WithValidContent_ReturnsComment() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            String content = "테스트 댓글입니다.";

            // when
            Comment comment = Comment.createComment(post, author, content, false);

            // then
            assertThat(comment).isNotNull();
            assertThat(comment.getPost()).isEqualTo(post);
            assertThat(comment.getAuthor()).isEqualTo(author);
            assertThat(comment.getContent()).isEqualTo(content);
            assertThat(comment.isAnonymous()).isFalse();
            assertThat(comment.isReply()).isFalse();
            assertThat(comment.getParentComment()).isNull();
        }

        @Test
        @DisplayName("익명 댓글 생성 성공")
        void createComment_Anonymous_ReturnsAnonymousComment() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();

            // when
            Comment comment = Comment.createComment(post, author, DEFAULT_COMMENT_CONTENT, true);

            // then
            assertThat(comment.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("내용이 정확히 500자일 때 생성 성공")
        void createComment_WithContentAt500Chars_ReturnsComment() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            String content = "가".repeat(500);

            // when
            Comment comment = Comment.createComment(post, author, content, false);

            // then
            assertThat(comment).isNotNull();
            assertThat(comment.getContent()).hasSize(500);
        }

        @Test
        @DisplayName("내용이 500자를 초과하면 IllegalArgumentException 발생")
        void createComment_WithContentOver500Chars_ThrowsException() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            String content = "가".repeat(501);

            // when & then
            assertThatThrownBy(() -> Comment.createComment(post, author, content, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("내용이 비어있으면 IllegalArgumentException 발생")
        void createComment_WithEmptyContent_ThrowsException() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();

            // when & then
            assertThatThrownBy(() -> Comment.createComment(post, author, "", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용");
        }

        @Test
        @DisplayName("내용이 공백만 있으면 IllegalArgumentException 발생")
        void createComment_WithBlankContent_ThrowsException() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();

            // when & then
            assertThatThrownBy(() -> Comment.createComment(post, author, "   ", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용");
        }

        @Test
        @DisplayName("내용이 null이면 IllegalArgumentException 발생")
        void createComment_WithNullContent_ThrowsException() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();

            // when & then
            assertThatThrownBy(() -> Comment.createComment(post, author, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용");
        }
    }

    @Nested
    @DisplayName("createReply 정적 팩토리 메서드")
    class CreateReplyTest {

        @Test
        @DisplayName("일반 댓글에 대댓글 생성 성공")
        void createReply_ToComment_ReturnsReply() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User commentAuthor = createAnotherMemberWithId();
            Comment parentComment = comment(post, commentAuthor);
            User replyAuthor = createMemberWithId();

            // when
            Comment reply = Comment.createReply(post, parentComment, replyAuthor, DEFAULT_REPLY_CONTENT, false);

            // then
            assertThat(reply).isNotNull();
            assertThat(reply.isReply()).isTrue();
            assertThat(reply.getParentComment()).isEqualTo(parentComment);
            assertThat(reply.getPost()).isEqualTo(post);
        }

        @Test
        @DisplayName("대댓글에 대댓글을 달면 IllegalArgumentException 발생")
        void createReply_ToReply_ThrowsException() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User commentAuthor = createAnotherMemberWithId();
            Comment parentComment = comment(post, commentAuthor);
            Comment existingReply = reply(post, parentComment, createMemberWithId());
            User newReplyAuthor = createOperatorWithId();

            // when & then
            assertThatThrownBy(() -> Comment.createReply(post, existingReply, newReplyAuthor, "대댓글의 대댓글", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("대댓글에는 답글을 달 수 없습니다");
        }
    }

    @Nested
    @DisplayName("canReplyTo 메서드")
    class CanReplyToTest {

        @Test
        @DisplayName("일반 댓글이고 삭제되지 않은 경우 대댓글 가능")
        void canReplyTo_NormalActiveComment_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment parentComment = createComment(post, createAnotherMemberWithId());

            // when
            boolean result = parentComment.canReplyTo();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("대댓글에는 대댓글 불가")
        void canReplyTo_Reply_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment parentComment = comment(post, createAnotherMemberWithId());
            Comment replyComment = reply(post, parentComment, createMemberWithId());

            // when
            boolean result = replyComment.canReplyTo();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("삭제된 댓글에는 대댓글 불가")
        void canReplyTo_DeletedComment_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            Comment parentComment = createComment(post, author);
            parentComment.delete(author.getId());

            // when
            boolean result = parentComment.canReplyTo();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("삭제된 대댓글에도 대댓글 불가 (이중 조건)")
        void canReplyTo_DeletedReply_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment parentComment = comment(post, createAnotherMemberWithId());
            User replyAuthor = createMemberWithId();
            Comment replyComment = reply(post, parentComment, replyAuthor);
            replyComment.delete(replyAuthor.getId());

            // when
            boolean result = replyComment.canReplyTo();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isReply 메서드")
    class IsReplyTest {

        @Test
        @DisplayName("일반 댓글은 false 반환")
        void isReply_NormalComment_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment normalComment = createComment(post, createAnotherMemberWithId());

            // when & then
            assertThat(normalComment.isReply()).isFalse();
        }

        @Test
        @DisplayName("대댓글은 true 반환")
        void isReply_Reply_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment parentComment = comment(post, createAnotherMemberWithId());
            Comment replyComment = reply(post, parentComment, createMemberWithId());

            // when & then
            assertThat(replyComment.isReply()).isTrue();
        }
    }

    @Nested
    @DisplayName("canDelete 메서드")
    class CanDeleteTest {

        @Test
        @DisplayName("작성자는 자신의 댓글 삭제 가능")
        void canDelete_ByAuthor_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            Comment commentEntity = comment(post, author);

            // when
            boolean result = commentEntity.canDelete(author);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("운영자는 다른 사용자의 댓글 삭제 가능")
        void canDelete_ByOperator_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment commentEntity = comment(post, createAnotherMemberWithId());
            User operator = createOperatorWithId();

            // when
            boolean result = commentEntity.canDelete(operator);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("관리자는 다른 사용자의 댓글 삭제 가능")
        void canDelete_ByAdmin_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment commentEntity = comment(post, createAnotherMemberWithId());
            User admin = createAdminWithId();

            // when
            boolean result = commentEntity.canDelete(admin);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작성자가 아닌 일반 회원은 삭제 불가")
        void canDelete_ByNonAuthorMember_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment commentEntity = comment(post, createAnotherMemberWithId());
            User anotherMember = createMemberWithId();

            // when
            boolean result = commentEntity.canDelete(anotherMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 사용자는 삭제 불가")
        void canDelete_ByNullUser_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment commentEntity = comment(post, createAnotherMemberWithId());

            // when
            boolean result = commentEntity.canDelete(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isAuthor 메서드")
    class IsAuthorTest {

        @Test
        @DisplayName("작성자와 동일한 사용자이면 true 반환")
        void isAuthor_SameUser_ReturnsTrue() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            User author = createAnotherMemberWithId();
            Comment commentEntity = comment(post, author);

            // when
            boolean result = commentEntity.isAuthor(author);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작성자와 다른 사용자이면 false 반환")
        void isAuthor_DifferentUser_ReturnsFalse() {
            // given
            Post post = normalPost(generalBoard(), createMemberWithId());
            Comment commentEntity = comment(post, createAnotherMemberWithId());
            User differentUser = createMemberWithId();

            // when
            boolean result = commentEntity.isAuthor(differentUser);

            // then
            assertThat(result).isFalse();
        }
    }
}
