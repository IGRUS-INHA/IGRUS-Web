package igrus.web.survey.question.exception;

import igrus.web.survey.exception.SurveyErrorCode;

import igrus.web.common.exception.CustomBaseException;

public class SurveyOptionNotFoundException extends CustomBaseException {

    public SurveyOptionNotFoundException() {
        super(SurveyErrorCode.SURVEY_OPTION_NOT_FOUND);
    }

    public SurveyOptionNotFoundException(Long optionId) {
        super(SurveyErrorCode.SURVEY_OPTION_NOT_FOUND, "선택지를 찾을 수 없습니다: id=" + optionId);
    }
}
