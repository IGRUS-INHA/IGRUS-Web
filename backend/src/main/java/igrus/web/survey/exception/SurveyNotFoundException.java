package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyNotFoundException extends CustomBaseException {

    public SurveyNotFoundException() {
        super(SurveyErrorCode.SURVEY_NOT_FOUND);
    }

    public SurveyNotFoundException(Long surveyId) {
        super(SurveyErrorCode.SURVEY_NOT_FOUND, "설문을 찾을 수 없습니다: id=" + surveyId);
    }
}
