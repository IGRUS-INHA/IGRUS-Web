package igrus.web.community.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum CommunityErrorCode implements ErrorCode {

    // Board
    BOARD_NOT_FOUND(404, "게시판을 찾을 수 없습니다"),
    BOARD_ACCESS_DENIED(403, "게시판 접근이 거부되었습니다"),
    BOARD_READ_DENIED(403, "게시판 읽기 권한이 없습니다"),
    BOARD_WRITE_DENIED(403, "게시판 쓰기 권한이 없습니다"),

    // Post
    POST_NOT_FOUND(404, "게시글을 찾을 수 없습니다"),
    POST_ACCESS_DENIED(403, "게시글에 대한 접근 권한이 없습니다"),
    POST_TITLE_TOO_LONG(400, "제목은 100자 이내여야 합니다"),
    POST_IMAGE_LIMIT_EXCEEDED(400, "이미지는 최대 5개까지 첨부 가능합니다"),
    POST_RATE_LIMIT_EXCEEDED(429, "게시글 작성 제한을 초과했습니다 (시간당 20회)"),
    POST_INVALID_ANONYMOUS_OPTION(400, "익명 옵션은 자유게시판에서만 사용 가능합니다"),
    POST_INVALID_QUESTION_OPTION(400, "질문 옵션은 자유게시판에서만 사용 가능합니다"),
    POST_INVALID_VISIBILITY_OPTION(400, "준회원 공개 옵션은 공지사항에서만 사용 가능합니다"),
    POST_DELETED(410, "삭제된 게시글입니다"),
    POST_ANONYMOUS_UNCHANGEABLE(400, "익명 설정은 변경할 수 없습니다"),

    // Comment
    COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다"),
    COMMENT_ACCESS_DENIED(403, "댓글에 대한 접근 권한이 없습니다"),
    COMMENT_CONTENT_TOO_LONG(400, "댓글은 500자 이내여야 합니다"),
    COMMENT_CONTENT_EMPTY(400, "내용을 입력해 주세요"),
    REPLY_TO_REPLY_NOT_ALLOWED(400, "대댓글에는 답글을 달 수 없습니다"),
    POST_DELETED_CANNOT_COMMENT(400, "삭제된 게시글에는 댓글을 작성할 수 없습니다"),
    ANONYMOUS_NOT_ALLOWED(400, "이 게시판에서는 익명 댓글을 작성할 수 없습니다"),

    // Comment Like
    CANNOT_LIKE_OWN_COMMENT(400, "본인 댓글에는 좋아요를 할 수 없습니다"),
    ALREADY_LIKED_COMMENT(400, "이미 좋아요한 댓글입니다"),
    LIKE_NOT_FOUND(404, "좋아요 정보를 찾을 수 없습니다"),

    // Comment Report
    ALREADY_REPORTED_COMMENT(400, "이미 신고한 댓글입니다"),
    INVALID_REPORT_REASON(400, "신고 사유를 입력해 주세요"),
    COMMENT_REPORT_NOT_FOUND(404, "신고 정보를 찾을 수 없습니다"),

    // Post Like
    POST_LIKE_ALREADY_EXISTS(409, "이미 좋아요한 게시글입니다"),
    POST_LIKE_NOT_FOUND(404, "게시글 좋아요를 찾을 수 없습니다"),

    // Bookmark
    BOOKMARK_ALREADY_EXISTS(409, "이미 북마크한 게시글입니다"),
    BOOKMARK_NOT_FOUND(404, "북마크를 찾을 수 없습니다"),

    // Pinned Post
    PINNED_POST_NOT_FOUND(404, "고정 게시글을 찾을 수 없습니다"),
    PINNED_POST_ALREADY_EXISTS(409, "이미 고정된 게시글입니다"),
    INVALID_DISPLAY_ORDER(400, "표시 순서는 1 이상이어야 합니다");

    private final int status;
    private final String message;

    CommunityErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
