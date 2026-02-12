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
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다"),

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다"),
    SAME_ROLE_CHANGE(400, "이전 역할과 새 역할이 동일합니다"),
    INVALID_STUDENT_ID(400, "학번은 8자리 숫자여야 합니다"),
    INVALID_EMAIL_FORMAT(400, "유효하지 않은 이메일 형식입니다"),
    INVALID_GRADE(400, "학년은 1 이상이어야 합니다"),
    INVALID_PHONE_NUMBER_FORMAT(400, "전화번호는 000-0000-0000 형식이어야 합니다"),

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

    // User Suspension
    SUSPENSION_INVALID_PERIOD(400, "정지 종료일은 정지 시작일 이후여야 합니다"),
    SUSPENSION_ALREADY_LIFTED(400, "이미 해제된 정지입니다"),
    SUSPENSION_REASON_REQUIRED(400, "정지 사유는 필수입니다"),
    SUSPENSION_CANNOT_EXTEND(400, "해제된 정지는 연장할 수 없습니다"),
    SUSPENSION_EXTEND_INVALID_DATE(400, "새로운 종료일은 기존 종료일 이후여야 합니다"),
    SUSPENSION_END_DATE_MUST_BE_FUTURE(400, "정지 종료일은 현재 시간 이후여야 합니다"),
    LAST_ADMIN_CANNOT_SUSPEND(400, "마지막 관리자는 정지할 수 없습니다"),
    LAST_ADMIN_CANNOT_WITHDRAW(400, "마지막 관리자는 강제 탈퇴할 수 없습니다"),

    // JWT
    JWT_SECRET_KEY_TOO_SHORT(500, "JWT 비밀키 길이가 최소 요구사항을 충족하지 않습니다"),
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
    INVALID_PASSWORD_FORMAT(400, "비밀번호는 영문, 숫자를 포함하여 8자 이상이어야 합니다"),
    SAME_PASSWORD(400, "현재 비밀번호와 다른 비밀번호를 입력해주세요"),
    SAME_EMAIL(400, "현재 이메일과 다른 이메일을 입력해주세요"),
    SAME_PHONE_NUMBER(400, "현재 전화번호와 다른 전화번호를 입력해주세요"),

    PRIVACY_CONSENT_REQUIRED(400, "개인정보 처리방침 동의가 필요합니다"),
    ACCOUNT_SUSPENDED(403, "정지된 계정입니다"),
    ACCOUNT_WITHDRAWN(403, "탈퇴한 계정입니다"),
    ACCOUNT_RECOVERABLE(200, "복구 가능한 탈퇴 계정입니다"),
    ACCOUNT_NOT_RECOVERABLE(400, "복구 기간이 만료된 계정입니다"),
    REFRESH_TOKEN_INVALID(401, "유효하지 않은 리프레시 토큰입니다"),
    REFRESH_TOKEN_EXPIRED(401, "리프레시 토큰이 만료되었습니다"),
    REFRESH_TOKEN_THEFT_DETECTED(401, "토큰 도용이 감지되어 모든 세션이 종료되었습니다"),
    PASSWORD_RESET_TOKEN_INVALID(400, "유효하지 않은 비밀번호 재설정 토큰입니다"),
    PASSWORD_RESET_TOKEN_EXPIRED(400, "비밀번호 재설정 토큰이 만료되었습니다"),
    EMAIL_SEND_FAILED(500, "이메일 발송에 실패했습니다"),
    RECENT_WITHDRAWAL_EXISTS(400, "최근 탈퇴 이력이 있어 재가입이 불가합니다"),
    VERIFICATION_RESEND_RATE_LIMITED(429, "인증 코드 재발송은 5분에 1회만 가능합니다"),
    VERIFICATION_EMAIL_NOT_FOUND(400, "해당 이메일로 가입 요청된 계정을 찾을 수 없습니다"),
    ACCOUNT_LOCKED(423, "로그인 시도 횟수 초과로 계정이 잠겼습니다"),

    // Member Approval
    ADMIN_REQUIRED(403, "관리자 권한이 필요합니다"),
    USER_NOT_ASSOCIATE(400, "해당 사용자는 준회원이 아닙니다"),
    SELF_ROLE_CHANGE_NOT_ALLOWED(400, "자기 자신의 권한은 변경할 수 없습니다"),
    SELF_STATUS_CHANGE_NOT_ALLOWED(400, "자기 자신의 상태는 변경할 수 없습니다"),
    LAST_ADMIN_CANNOT_CHANGE(400, "마지막 관리자는 권한을 변경할 수 없습니다"),
    BULK_APPROVAL_EMPTY(400, "승인할 사용자를 선택해주세요"),
    BULK_REJECTION_EMPTY(400, "거절할 사용자를 선택해주세요"),
    ASSOCIATE_ALREADY_DECIDED(400, "이미 처리된 준회원입니다"),
    INVALID_DATE_RANGE(400, "종료 일시는 시작 일시 이후여야 합니다"),

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
    SEMESTER_INVALID_YEAR(400, "유효하지 않은 연도입니다"),

    // Event
    EVENT_NOT_FOUND(404, "행사를 찾을 수 없습니다"),
    EVENT_ACCESS_DENIED(403, "행사에 대한 접근 권한이 없습니다"),
    EVENT_INVALID_DATE(400, "행사 날짜가 유효하지 않습니다"),
    EVENT_INVALID_CAPACITY(400, "행사 정원이 유효하지 않습니다"),
    EVENT_ALREADY_REGISTERED(409, "이미 신청한 행사입니다"),
    EVENT_REGISTRATION_CLOSED(400, "신청이 마감된 행사입니다"),
    EVENT_REGISTRATION_NOT_FOUND(404, "행사 신청 정보를 찾을 수 없습니다"),
    EVENT_CAPACITY_FULL(400, "정원이 초과되었습니다"),
    EVENT_ASSOCIATE_NOT_ALLOWED(403, "준회원은 행사에 신청할 수 없습니다"),
    EVENT_ALREADY_CANCELED(400, "이미 취소된 신청입니다"),
    EVENT_NOT_MANUAL_APPROVE(400, "수동 승인(선발제) 행사가 아닙니다"),
    EVENT_INVALID_REGISTRATION_STATUS(400, "유효하지 않은 신청 상태입니다"),
    EVENT_OPERATOR_REQUIRED(403, "운영진 이상만 접근할 수 있습니다"),
    EVENT_NOT_OPEN(400, "신청 가능한 상태가 아닙니다"),
    EVENT_NOT_IN_REGISTRATION_PERIOD(400, "신청 기간이 아닙니다"),
    EVENT_NOT_EDITABLE(400, "수정 불가능한 상태의 행사입니다"),
    EVENT_INVALID_STATE_TRANSITION(400, "허용되지 않은 행사 상태 변경입니다"),
    EVENT_TIME_OVERLAP(409, "이미 신청한 다른 행사와 시간이 겹칩니다"),

    // Pinned Post (고정 게시글)
    PINNED_POST_NOT_FOUND(404, "고정 게시글을 찾을 수 없습니다"),
    PINNED_POST_ALREADY_EXISTS(409, "이미 고정된 게시글입니다"),
    INVALID_DISPLAY_ORDER(400, "표시 순서는 1 이상이어야 합니다"),

    // Signup - Custom Field Validation
    INVALID_CUSTOM_FIELD(400, "기타 선택 시 직접 입력 값은 필수입니다");

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
