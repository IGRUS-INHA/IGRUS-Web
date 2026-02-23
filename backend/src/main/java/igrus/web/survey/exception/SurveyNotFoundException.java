package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SurveyNotFoundException extends CustomBaseException {

    public SurveyNotFoundException() {
        super(ErrorCode.SURVEY_NOT_FOUND);
    }

    public SurveyNotFoundException(Long surveyId) {
        super(ErrorCode.SURVEY_NOT_FOUND, "설문을 찾을 수 없습니다: id=" + surveyId);
    }
}
