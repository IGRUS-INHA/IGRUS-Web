package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyPublishValidationException extends CustomBaseException {

    public SurveyPublishValidationException() {
        super(SurveyErrorCode.SURVEY_PUBLISH_VALIDATION_FAILED);
    }

    public SurveyPublishValidationException(String message) {
        super(SurveyErrorCode.SURVEY_PUBLISH_VALIDATION_FAILED, message);
    }
}
