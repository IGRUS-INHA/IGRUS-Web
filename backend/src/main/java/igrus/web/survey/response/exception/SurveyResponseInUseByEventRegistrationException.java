package igrus.web.survey.response.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.survey.exception.SurveyErrorCode;

/**
 * 연결된 행사에 유효한 신청이 있는 설문 응답을 삭제하려 할 때 발생하는 예외.
 * 행사 신청 취소는 운영진만 처리할 수 있으므로, 응답 삭제로 신청이 취소되어서는 안 된다.
 */
public class SurveyResponseInUseByEventRegistrationException extends CustomBaseException {

    public SurveyResponseInUseByEventRegistrationException() {
        super(SurveyErrorCode.SURVEY_RESPONSE_IN_USE_BY_EVENT_REGISTRATION);
    }
}
