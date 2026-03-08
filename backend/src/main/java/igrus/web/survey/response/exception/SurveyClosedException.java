package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyClosedException extends CustomBaseException {

    public SurveyClosedException() {
        super(SurveyErrorCode.SURVEY_CLOSED);
    }

    public SurveyClosedException(String message) {
        super(SurveyErrorCode.SURVEY_CLOSED, message);
    }
}
