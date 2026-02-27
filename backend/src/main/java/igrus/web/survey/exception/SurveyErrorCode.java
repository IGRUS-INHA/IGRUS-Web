package igrus.web.survey.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SurveyErrorCode implements ErrorCode {

    SURVEY_NOT_FOUND(404, "설문을 찾을 수 없습니다"),
    SURVEY_ACCESS_DENIED(403, "설문에 대한 접근 권한이 없습니다"),
    SURVEY_ALREADY_TRASHED(400, "이미 휴지통에 있는 설문입니다"),
    SURVEY_NOT_TRASHED(400, "휴지통에 있는 설문이 아닙니다"),
    SURVEY_INVALID_STATE_TRANSITION(400, "허용되지 않는 상태 전이입니다"),
    SURVEY_PUBLISH_VALIDATION_FAILED(400, "설문 공개 조건을 충족하지 않습니다"),

    SURVEY_QUESTION_NOT_FOUND(404, "질문을 찾을 수 없습니다"),
    SURVEY_QUESTION_LIMIT_EXCEEDED(400, "질문은 최대 50개까지 가능합니다"),
    SURVEY_QUESTION_NOT_BELONGS(403, "해당 설문의 질문이 아닙니다"),
    SURVEY_QUESTION_VALIDATION_FAILED(400, "질문 유효성 검증에 실패했습니다"),
    SURVEY_QUESTION_TYPE_NOT_SUPPORTED(400, "해당 질문 유형에서는 지원하지 않는 작업입니다"),

    SURVEY_OPTION_NOT_FOUND(404, "선택지를 찾을 수 없습니다"),

    SURVEY_ROW_NOT_FOUND(404, "행을 찾을 수 없습니다"),

    SURVEY_RESPONSE_NOT_FOUND(404, "설문 응답을 찾을 수 없습니다"),
    SURVEY_RESPONSE_DUPLICATE(409, "이미 응답한 설문입니다"),
    SURVEY_RESPONSE_ACCESS_DENIED(403, "설문 응답에 대한 접근 권한이 없습니다"),
    SURVEY_RESPONSE_VALIDATION_FAILED(400, "응답 데이터 유효성 검증에 실패했습니다"),
    SURVEY_NOT_ACCEPTING_RESPONSES(400, "현재 응답을 받을 수 없는 설문입니다"),
    SURVEY_ANONYMOUS_NOT_ALLOWED(403, "비회원 응답이 허용되지 않는 설문입니다"),

    SURVEY_STATISTICS_AGGREGATION_FAILED(500, "설문 통계 집계 중 오류가 발생했습니다");

    private final int status;
    private final String message;

    SurveyErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
