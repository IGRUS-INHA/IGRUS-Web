package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyQuestionNotFoundException extends CustomBaseException {

    public SurveyQuestionNotFoundException() {
        super(SurveyErrorCode.SURVEY_QUESTION_NOT_FOUND);
    }

    public SurveyQuestionNotFoundException(Long questionId) {
        super(SurveyErrorCode.SURVEY_QUESTION_NOT_FOUND, "질문을 찾을 수 없습니다: id=" + questionId);
    }
}
