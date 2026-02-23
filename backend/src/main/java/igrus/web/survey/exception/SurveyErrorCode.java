package igrus.web.survey.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SurveyErrorCode implements ErrorCode {

    SURVEY_NOT_FOUND(404, "설문을 찾을 수 없습니다"),
    SURVEY_ACCESS_DENIED(403, "설문에 대한 접근 권한이 없습니다"),
    SURVEY_ALREADY_TRASHED(400, "이미 휴지통에 있는 설문입니다"),
    SURVEY_NOT_TRASHED(400, "휴지통에 있는 설문이 아닙니다"),
    SURVEY_INVALID_STATE_TRANSITION(400, "허용되지 않는 상태 전이입니다");

    private final int status;
    private final String message;

    SurveyErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
