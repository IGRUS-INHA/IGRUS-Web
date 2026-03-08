package igrus.web.event.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum EventErrorCode implements ErrorCode {

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
    EVENT_NOT_CANCELABLE(400, "취소할 수 없는 상태의 행사입니다"),
    EVENT_NOT_REACTIVATABLE(400, "재활성화할 수 없는 상태의 행사입니다"),
    EVENT_REGISTRATION_NOT_REOPENABLE(400, "등록을 재오픈할 수 없습니다"),
    EVENT_REOPEN_REASON_REQUIRED(400, "재오픈 사유는 필수입니다"),
    EVENT_NOT_DELETABLE(400, "신청자가 있는 행사는 삭제할 수 없습니다"),
    EVENT_SURVEY_RESPONSE_REQUIRED(400, "설문 응답이 필요합니다"),
    EVENT_SURVEY_NOT_READY(400, "설문이 아직 시작되지 않았습니다"),
    EVENT_ATTACHMENT_FILE_NOT_FOUND(404, "첨부파일을 찾을 수 없습니다"),
    EVENT_ATTACHMENT_FILE_NOT_COMPLETED(400, "업로드가 완료되지 않은 파일입니다"),
    EVENT_ATTACHMENT_DUPLICATE_FILE(400, "중복된 첨부파일 ID가 포함되어 있습니다"),
    EXTERNAL_REGISTRATION_NOT_ALLOWED(400, "외부인 신청이 허용되지 않은 행사입니다"),
    EXTERNAL_ALREADY_REGISTERED(409, "이미 신청한 외부인입니다"),
    REGISTERED_MEMBER_EXISTS(400, "해당 학번으로 가입된 회원이 존재하므로 로그인 후 신청하세요"),
    SURVEY_RESPONSE_SERIALIZATION_FAILED(500, "설문 응답 직렬화에 실패했습니다");

    private final int status;
    private final String message;

    EventErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
