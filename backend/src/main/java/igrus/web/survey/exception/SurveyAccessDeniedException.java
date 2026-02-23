package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SurveyAccessDeniedException extends CustomBaseException {

    public SurveyAccessDeniedException() {
        super(ErrorCode.SURVEY_ACCESS_DENIED);
    }

    public SurveyAccessDeniedException(String message) {
        super(ErrorCode.SURVEY_ACCESS_DENIED, message);
    }
}
