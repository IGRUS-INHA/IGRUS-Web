package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

public class SurveyNotAcceptingResponsesException extends CustomBaseException {

    public SurveyNotAcceptingResponsesException() {
        super(SurveyErrorCode.SURVEY_NOT_ACCEPTING_RESPONSES);
    }

    public SurveyNotAcceptingResponsesException(String message) {
        super(SurveyErrorCode.SURVEY_NOT_ACCEPTING_RESPONSES, message);
    }
}
