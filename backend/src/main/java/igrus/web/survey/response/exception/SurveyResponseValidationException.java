package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyResponseValidationException extends CustomBaseException {

    public SurveyResponseValidationException() {
        super(SurveyErrorCode.SURVEY_RESPONSE_VALIDATION_FAILED);
    }

    public SurveyResponseValidationException(String message) {
        super(SurveyErrorCode.SURVEY_RESPONSE_VALIDATION_FAILED, message);
    }
}
