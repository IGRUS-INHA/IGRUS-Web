package igrus.web.survey.domain;

/**
 * 설문 응답 대상 권한.
 * 설문 생성 시 운영진이 지정하며, 응답자의 최소 권한을 결정합니다.
 */
public enum SurveyAccessLevel {

    /** 전체 공개 - 비회원 포함 누구나 응답 가능 */
    PUBLIC,

    /** 준회원 이상 - 로그인한 준회원(ASSOCIATE) 이상 응답 가능 */
    ASSOCIATE,

    /** 정회원 이상 - 정회원(MEMBER) 이상만 응답 가능 */
    MEMBER,

    /** 운영진 이상 - 운영진(OPERATOR) 이상만 응답 가능 */
    OPERATOR
}
