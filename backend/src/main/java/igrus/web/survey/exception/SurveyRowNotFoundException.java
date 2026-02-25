package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyRowNotFoundException extends CustomBaseException {

    public SurveyRowNotFoundException() {
        super(SurveyErrorCode.SURVEY_ROW_NOT_FOUND);
    }

    public SurveyRowNotFoundException(Long rowId) {
        super(SurveyErrorCode.SURVEY_ROW_NOT_FOUND, "행을 찾을 수 없습니다: id=" + rowId);
    }
}
