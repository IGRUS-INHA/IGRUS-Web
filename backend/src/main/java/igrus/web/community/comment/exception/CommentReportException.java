package igrus.web.community.comment.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 댓글 신고 관련 예외.
 */
public class CommentReportException extends CustomBaseException {

    private CommentReportException(ErrorCode errorCode) {
        super(errorCode);
    }

    private CommentReportException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static CommentReportException alreadyReported() {
        return new CommentReportException(ErrorCode.ALREADY_REPORTED_COMMENT);
    }

    public static CommentReportException invalidReason() {
        return new CommentReportException(ErrorCode.INVALID_REPORT_REASON);
    }

    public static CommentReportException reportNotFound() {
        return new CommentReportException(ErrorCode.COMMENT_REPORT_NOT_FOUND);
    }
}
