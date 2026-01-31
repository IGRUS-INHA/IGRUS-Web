package igrus.web.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(405, "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(400, "잘못된 타입입니다"),
    ACCESS_DENIED(403, "접근이 거부되었습니다"),

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다"),
    SAME_ROLE_CHANGE(400, "이전 역할과 새 역할이 동일합니다"),
    INVALID_STUDENT_ID(400, "학번은 8자리 숫자여야 합니다"),
    INVALID_EMAIL_FORMAT(400, "유효하지 않은 이메일 형식입니다"),
    INVALID_GRADE(400, "학년은 1 이상이어야 합니다"),

    // Inquiry
    INQUIRY_NOT_FOUND(404, "문의를 찾을 수 없습니다"),
    INQUIRY_ACCESS_DENIED(403, "문의에 대한 접근 권한이 없습니다"),
    INQUIRY_ALREADY_REPLIED(409, "이미 답변이 작성된 문의입니다"),
    INQUIRY_INVALID_PASSWORD(401, "문의 비밀번호가 일치하지 않습니다"),
    INQUIRY_MAX_ATTACHMENTS_EXCEEDED(400, "첨부파일은 최대 3개까지 가능합니다"),
    INQUIRY_NUMBER_GENERATION_FAILED(500, "문의 번호 생성에 실패했습니다"),
    GUEST_INQUIRY_EMAIL_REQUIRED(400, "비회원 문의 시 이메일은 필수입니다"),
    GUEST_INQUIRY_NAME_REQUIRED(400, "비회원 문의 시 이름은 필수입니다"),
    GUEST_INQUIRY_PASSWORD_REQUIRED(400, "비회원 문의 시 비밀번호는 필수입니다"),
    INQUIRY_REPLY_NOT_FOUND(404, "답변을 찾을 수 없습니다"),
    INVALID_STATUS_TRANSITION(400, "허용되지 않은 상태 변경입니다"),

    // JWT
    ACCESS_TOKEN_INVALID(401, "유효하지 않은 액세스 토큰입니다"),
    ACCESS_TOKEN_EXPIRED(401, "액세스 토큰이 만료되었습니다"),
    INVALID_TOKEN_TYPE(401, "올바르지 않은 토큰 타입입니다"),

    // Auth
    INVALID_CREDENTIALS(401, "학번 또는 비밀번호가 올바르지 않습니다"),
    EMAIL_NOT_VERIFIED(401, "이메일 인증이 완료되지 않았습니다"),
    EMAIL_ALREADY_VERIFIED(400, "이미 인증된 이메일입니다"),
    VERIFICATION_CODE_EXPIRED(400, "인증 코드가 만료되었습니다"),
    VERIFICATION_CODE_INVALID(400, "유효하지 않은 인증 코드입니다"),
    VERIFICATION_ATTEMPTS_EXCEEDED(429, "인증 시도 횟수를 초과했습니다"),
    DUPLICATE_STUDENT_ID(409, "이미 가입된 학번입니다"),
    DUPLICATE_PHONE_NUMBER(409, "이미 등록된 전화번호입니다"),
    INVALID_PASSWORD_FORMAT(400, "비밀번호는 영문 대/소문자, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다"),
    PRIVACY_CONSENT_REQUIRED(400, "개인정보 처리방침 동의가 필요합니다"),
    ACCOUNT_SUSPENDED(403, "정지된 계정입니다"),
    ACCOUNT_WITHDRAWN(403, "탈퇴한 계정입니다"),
    ACCOUNT_RECOVERABLE(200, "복구 가능한 탈퇴 계정입니다"),
    ACCOUNT_NOT_RECOVERABLE(400, "복구 기간이 만료된 계정입니다"),
    REFRESH_TOKEN_INVALID(401, "유효하지 않은 리프레시 토큰입니다"),
    REFRESH_TOKEN_EXPIRED(401, "리프레시 토큰이 만료되었습니다"),
    PASSWORD_RESET_TOKEN_INVALID(400, "유효하지 않은 비밀번호 재설정 토큰입니다"),
    PASSWORD_RESET_TOKEN_EXPIRED(400, "비밀번호 재설정 토큰이 만료되었습니다"),
    EMAIL_SEND_FAILED(500, "이메일 발송에 실패했습니다"),
    RECENT_WITHDRAWAL_EXISTS(400, "최근 탈퇴 이력이 있어 재가입이 불가합니다"),
    VERIFICATION_RESEND_RATE_LIMITED(429, "인증 코드 재발송은 5분에 1회만 가능합니다"),
    ACCOUNT_LOCKED(423, "로그인 시도 횟수 초과로 계정이 잠겼습니다"),

    // Member Approval
    ADMIN_REQUIRED(403, "관리자 권한이 필요합니다"),
    USER_NOT_ASSOCIATE(400, "해당 사용자는 준회원이 아닙니다"),
    LAST_ADMIN_CANNOT_CHANGE(400, "마지막 관리자는 권한을 변경할 수 없습니다"),
    BULK_APPROVAL_EMPTY(400, "승인할 사용자를 선택해주세요"),

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

    // Semester Member
    SEMESTER_MEMBER_NOT_FOUND(404, "해당 학기에 등록된 회원을 찾을 수 없습니다"),
    SEMESTER_MEMBER_ALREADY_EXISTS(409, "이미 해당 학기에 등록된 회원입니다"),
    SEMESTER_INVALID_SEMESTER(400, "학기는 1 또는 2만 가능합니다"),
    SEMESTER_INVALID_YEAR(400, "유효하지 않은 연도입니다");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return this.name();
    }
}
