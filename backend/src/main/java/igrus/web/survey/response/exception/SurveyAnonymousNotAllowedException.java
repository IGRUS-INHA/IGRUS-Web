package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyAnonymousNotAllowedException extends CustomBaseException {

    public SurveyAnonymousNotAllowedException() {
        super(SurveyErrorCode.SURVEY_ANONYMOUS_NOT_ALLOWED);
    }

    public SurveyAnonymousNotAllowedException(String message) {
        super(SurveyErrorCode.SURVEY_ANONYMOUS_NOT_ALLOWED, message);
    }
}
