package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyQuestionLimitExceededException extends CustomBaseException {

    public SurveyQuestionLimitExceededException() {
        super(SurveyErrorCode.SURVEY_QUESTION_LIMIT_EXCEEDED);
    }
}
