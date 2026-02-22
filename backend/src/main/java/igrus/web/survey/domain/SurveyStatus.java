package igrus.web.survey.domain;

/**
 * 설문 상태.
 * 상태 흐름: DRAFT → PUBLISHED ⇄ CLOSED (CLOSED에서 재발행 가능)
 */
public enum SurveyStatus {

    /** 초안 - 작성 중, 응답 불가 */
    DRAFT,

    /** 발행됨 - 응답 수집 중 */
    PUBLISHED,

    /** 마감 - 응답 종료, 재발행으로 PUBLISHED 전환 가능 */
    CLOSED
}
