package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyResponseNotFoundException extends CustomBaseException {

    public SurveyResponseNotFoundException() {
        super(SurveyErrorCode.SURVEY_RESPONSE_NOT_FOUND);
    }

    public SurveyResponseNotFoundException(String message) {
        super(SurveyErrorCode.SURVEY_RESPONSE_NOT_FOUND, message);
    }
}
