package igrus.web.survey.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SurveyErrorCode implements ErrorCode {

    SURVEY_NOT_FOUND(404, "설문을 찾을 수 없습니다"),
    SURVEY_ACCESS_DENIED(403, "설문에 대한 접근 권한이 없습니다");

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
