package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyAlreadyLinkedToEventException extends CustomBaseException {

    public SurveyAlreadyLinkedToEventException() {
        super(EventErrorCode.SURVEY_ALREADY_LINKED_TO_EVENT);
    }

    public SurveyAlreadyLinkedToEventException(Long surveyId) {
        super(EventErrorCode.SURVEY_ALREADY_LINKED_TO_EVENT,
                "이미 다른 행사에 연결된 설문입니다: surveyId=" + surveyId);
    }
}
