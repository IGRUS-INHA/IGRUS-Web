package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyResponseAccessDeniedException extends CustomBaseException {

    public SurveyResponseAccessDeniedException() {
        super(SurveyErrorCode.SURVEY_RESPONSE_ACCESS_DENIED);
    }

    public SurveyResponseAccessDeniedException(String message) {
        super(SurveyErrorCode.SURVEY_RESPONSE_ACCESS_DENIED, message);
    }
}
