package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 설문 응답이 존재하지 않아 행사 신청이 거부될 때 발생하는 예외.
 */
public class SurveyResponseRequiredException extends CustomBaseException {

    public SurveyResponseRequiredException() {
        super(EventErrorCode.EVENT_SURVEY_RESPONSE_REQUIRED);
    }

    public SurveyResponseRequiredException(String message) {
        super(EventErrorCode.EVENT_SURVEY_RESPONSE_REQUIRED, message);
    }
}
