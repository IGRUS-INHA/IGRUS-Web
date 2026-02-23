package igrus.web.user.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum UserErrorCode implements ErrorCode {

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "이미 존재하는 이메일입니다"),
    DUPLICATE_PHONE_NUMBER(409, "이미 등록된 전화번호입니다"),
    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다"),
    SAME_ROLE_CHANGE(400, "이전 역할과 새 역할이 동일합니다"),
    INVALID_STUDENT_ID(400, "학번은 8자리 숫자여야 합니다"),
    INVALID_EMAIL_FORMAT(400, "유효하지 않은 이메일 형식입니다"),
    INVALID_GRADE(400, "학년은 1 이상이어야 합니다"),
    INVALID_PHONE_NUMBER_FORMAT(400, "전화번호는 000-0000-0000 형식이어야 합니다"),
    SAME_EMAIL(400, "현재 이메일과 다른 이메일을 입력해주세요"),
    SAME_PHONE_NUMBER(400, "현재 전화번호와 다른 전화번호를 입력해주세요"),

    // Suspension
    SUSPENSION_INVALID_PERIOD(400, "정지 종료일은 정지 시작일 이후여야 합니다"),
    SUSPENSION_ALREADY_LIFTED(400, "이미 해제된 정지입니다"),
    SUSPENSION_REASON_REQUIRED(400, "정지 사유는 필수입니다"),
    SUSPENSION_CANNOT_EXTEND(400, "해제된 정지는 연장할 수 없습니다"),
    SUSPENSION_EXTEND_INVALID_DATE(400, "새로운 종료일은 기존 종료일 이후여야 합니다"),
    SUSPENSION_END_DATE_MUST_BE_FUTURE(400, "정지 종료일은 현재 시간 이후여야 합니다"),
    LAST_ADMIN_CANNOT_SUSPEND(400, "마지막 관리자는 정지할 수 없습니다"),

    // Semester Member
    SEMESTER_MEMBER_NOT_FOUND(404, "해당 학기에 등록된 회원을 찾을 수 없습니다"),
    SEMESTER_MEMBER_ALREADY_EXISTS(409, "이미 해당 학기에 등록된 회원입니다"),
    SEMESTER_INVALID_SEMESTER(400, "학기는 1 또는 2만 가능합니다"),
    SEMESTER_INVALID_YEAR(400, "유효하지 않은 연도입니다"),

    // Temporary Student ID
    TEMP_STUDENT_ID_NOT_AVAILABLE(400, "임시 학번 발급은 1월~2월에만 가능합니다"),
    TEMP_STUDENT_ID_EXHAUSTED(500, "임시 학번이 모두 소진되었습니다"),
    STUDENT_ID_NOT_TEMPORARY(400, "임시 학번이 아닌 경우 학번을 변경할 수 없습니다");

    private final int status;
    private final String message;

    UserErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
