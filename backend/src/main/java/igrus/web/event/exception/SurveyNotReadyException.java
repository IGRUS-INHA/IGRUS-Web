package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 설문이 NOT_STARTED 상태일 때 행사 신청이 거부될 때 발생하는 예외.
 */
public class SurveyNotReadyException extends CustomBaseException {

    public SurveyNotReadyException() {
        super(EventErrorCode.EVENT_SURVEY_NOT_READY);
    }

    public SurveyNotReadyException(String message) {
        super(EventErrorCode.EVENT_SURVEY_NOT_READY, message);
    }
}
