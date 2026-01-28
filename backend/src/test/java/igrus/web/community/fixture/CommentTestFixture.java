package igrus.web.community.fixture;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.like.comment_like.domain.CommentLike;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * Comment 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 Comment 엔티티와 관련 DTO를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class CommentTestFixture {

    public static final String DEFAULT_COMMENT_CONTENT = "테스트 댓글 내용입니다.";
    public static final String DEFAULT_REPLY_CONTENT = "테스트 대댓글 내용입니다.";
    public static final Long DEFAULT_COMMENT_ID = 100L;
    public static final Long DEFAULT_REPLY_ID = 101L;

    private CommentTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== Comment 생성 (ID 없음) ====================

    /**
     * 일반 댓글을 생성합니다.
     *
     * @param post   게시글
     * @param author 작성자
     * @return 일반 댓글
     */
    public static Comment createComment(Post post, User author) {
        return Comment.createComment(post, author, DEFAULT_COMMENT_CONTENT, false);
    }

    /**
     * 지정된 내용으로 댓글을 생성합니다.
     *
     * @param post    게시글
     * @param author  작성자
     * @param content 내용
     * @return 댓글
     */
    public static Comment createComment(Post post, User author, String content) {
        return Comment.createComment(post, author, content, false);
    }

    /**
     * 익명 댓글을 생성합니다.
     *
     * @param post   게시글
     * @param author 작성자
     * @return 익명 댓글
     */
    public static Comment createAnonymousComment(Post post, User author) {
        return Comment.createComment(post, author, DEFAULT_COMMENT_CONTENT, true);
    }

    /**
     * 대댓글을 생성합니다.
     *
     * @param post          게시글
     * @param parentComment 부모 댓글
     * @param author        작성자
     * @return 대댓글
     */
    public static Comment createReply(Post post, Comment parentComment, User author) {
        return Comment.createReply(post, parentComment, author, DEFAULT_REPLY_CONTENT, false);
    }

    /**
     * 익명 대댓글을 생성합니다.
     *
     * @param post          게시글
     * @param parentComment 부모 댓글
     * @param author        작성자
     * @return 익명 대댓글
     */
    public static Comment createAnonymousReply(Post post, Comment parentComment, User author) {
        return Comment.createReply(post, parentComment, author, DEFAULT_REPLY_CONTENT, true);
    }

    // ==================== Comment 생성 (ID 포함) ====================

    /**
     * ID가 설정된 일반 댓글을 생성합니다.
     *
     * @param post   게시글
     * @param author 작성자
     * @return ID가 설정된 일반 댓글
     */
    public static Comment comment(Post post, User author) {
        return withId(createComment(post, author), DEFAULT_COMMENT_ID);
    }

    /**
     * 지정된 ID가 설정된 일반 댓글을 생성합니다.
     *
     * @param post   게시글
     * @param author 작성자
     * @param id     설정할 ID
     * @return ID가 설정된 일반 댓글
     */
    public static Comment comment(Post post, User author, Long id) {
        return withId(createComment(post, author), id);
    }

    /**
     * ID가 설정된 익명 댓글을 생성합니다.
     *
     * @param post   게시글
     * @param author 작성자
     * @return ID가 설정된 익명 댓글
     */
    public static Comment anonymousComment(Post post, User author) {
        return withId(createAnonymousComment(post, author), DEFAULT_COMMENT_ID);
    }

    /**
     * ID가 설정된 대댓글을 생성합니다.
     *
     * @param post          게시글
     * @param parentComment 부모 댓글
     * @param author        작성자
     * @return ID가 설정된 대댓글
     */
    public static Comment reply(Post post, Comment parentComment, User author) {
        return withId(createReply(post, parentComment, author), DEFAULT_REPLY_ID);
    }

    // ==================== CommentLike 생성 ====================

    /**
     * 댓글 좋아요를 생성합니다.
     *
     * @param comment 댓글
     * @param user    좋아요한 사용자
     * @return 댓글 좋아요
     */
    public static CommentLike createCommentLike(Comment comment, User user) {
        return CommentLike.create(comment, user);
    }

    // ==================== CommentReport 생성 ====================

    /**
     * 댓글 신고를 생성합니다.
     *
     * @param comment  댓글
     * @param reporter 신고자
     * @return 댓글 신고
     */
    public static CommentReport createCommentReport(Comment comment, User reporter) {
        return CommentReport.create(comment, reporter, "신고 사유입니다.");
    }

    // ==================== CreateCommentRequest 생성 ====================

    /**
     * 기본 댓글 작성 요청을 생성합니다.
     *
     * @return 기본 댓글 작성 요청
     */
    public static CreateCommentRequest createCommentRequest() {
        return new CreateCommentRequest(DEFAULT_COMMENT_CONTENT, false);
    }

    /**
     * 지정된 내용으로 댓글 작성 요청을 생성합니다.
     *
     * @param content 내용
     * @return 댓글 작성 요청
     */
    public static CreateCommentRequest createCommentRequest(String content) {
        return new CreateCommentRequest(content, false);
    }

    /**
     * 익명 댓글 작성 요청을 생성합니다.
     *
     * @return 익명 댓글 작성 요청
     */
    public static CreateCommentRequest anonymousCommentRequest() {
        return new CreateCommentRequest(DEFAULT_COMMENT_CONTENT, true);
    }

    /**
     * 지정된 길이의 내용을 가진 댓글 작성 요청을 생성합니다.
     *
     * @param length 내용 길이
     * @return 댓글 작성 요청
     */
    public static CreateCommentRequest createCommentRequestWithLength(int length) {
        return new CreateCommentRequest("가".repeat(length), false);
    }
}
