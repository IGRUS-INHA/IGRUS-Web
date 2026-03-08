package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 설문 응답 JSON 직렬화에 실패할 때 발생하는 예외.
 */
public class SurveyResponseSerializationException extends CustomBaseException {

    public SurveyResponseSerializationException() {
        super(EventErrorCode.SURVEY_RESPONSE_SERIALIZATION_FAILED);
    }

    public SurveyResponseSerializationException(String message) {
        super(EventErrorCode.SURVEY_RESPONSE_SERIALIZATION_FAILED, message);
    }

    public SurveyResponseSerializationException(String message, Throwable cause) {
        super(EventErrorCode.SURVEY_RESPONSE_SERIALIZATION_FAILED, message);
        initCause(cause);
    }
}
