package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyResponseDuplicateException extends CustomBaseException {

    public SurveyResponseDuplicateException() {
        super(SurveyErrorCode.SURVEY_RESPONSE_DUPLICATE);
    }

    public SurveyResponseDuplicateException(String message) {
        super(SurveyErrorCode.SURVEY_RESPONSE_DUPLICATE, message);
    }
}
