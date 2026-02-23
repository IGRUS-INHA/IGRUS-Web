package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyAccessDeniedException extends CustomBaseException {

    public SurveyAccessDeniedException() {
        super(SurveyErrorCode.SURVEY_ACCESS_DENIED);
    }

    public SurveyAccessDeniedException(String message) {
        super(SurveyErrorCode.SURVEY_ACCESS_DENIED, message);
    }
}
