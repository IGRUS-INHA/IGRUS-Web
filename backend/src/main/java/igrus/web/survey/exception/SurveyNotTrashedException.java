package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

/** 휴지통에 있지 않은 설문에 대해 복원/영구 삭제를 시도할 때 발생합니다. */
public class SurveyNotTrashedException extends CustomBaseException {

    public SurveyNotTrashedException() {
        super(SurveyErrorCode.SURVEY_NOT_TRASHED);
    }

    public SurveyNotTrashedException(String message) {
        super(SurveyErrorCode.SURVEY_NOT_TRASHED, message);
    }
}
