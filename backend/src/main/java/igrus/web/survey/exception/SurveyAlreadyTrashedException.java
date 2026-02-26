package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

/** 이미 휴지통에 있는 설문에 대해 휴지통 이동을 시도할 때 발생합니다. */
public class SurveyAlreadyTrashedException extends CustomBaseException {

    public SurveyAlreadyTrashedException() {
        super(SurveyErrorCode.SURVEY_ALREADY_TRASHED);
    }

    public SurveyAlreadyTrashedException(String message) {
        super(SurveyErrorCode.SURVEY_ALREADY_TRASHED, message);
    }
}
