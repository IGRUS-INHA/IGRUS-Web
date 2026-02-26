package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

/** 허용되지 않는 상태 전이를 시도할 때 발생합니다. (예: 다른 카테고리로 질문 유형 변경) */
public class SurveyInvalidStateTransitionException extends CustomBaseException {

    public SurveyInvalidStateTransitionException() {
        super(SurveyErrorCode.SURVEY_INVALID_STATE_TRANSITION);
    }

    public SurveyInvalidStateTransitionException(String message) {
        super(SurveyErrorCode.SURVEY_INVALID_STATE_TRANSITION, message);
    }
}
