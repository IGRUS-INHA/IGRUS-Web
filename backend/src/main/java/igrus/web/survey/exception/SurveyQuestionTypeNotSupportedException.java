package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

/** 해당 질문 유형에서 지원하지 않는 작업을 시도할 때 발생합니다. (예: 텍스트 질문에 선택지 추가) */
public class SurveyQuestionTypeNotSupportedException extends CustomBaseException {

    public SurveyQuestionTypeNotSupportedException() {
        super(SurveyErrorCode.SURVEY_QUESTION_TYPE_NOT_SUPPORTED);
    }

    public SurveyQuestionTypeNotSupportedException(String message) {
        super(SurveyErrorCode.SURVEY_QUESTION_TYPE_NOT_SUPPORTED, message);
    }
}
