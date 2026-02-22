package igrus.web.survey.domain;

/**
 * 설문 질문 유형.
 * 구글 폼과 유사한 11가지 질문 유형을 지원합니다.
 */
public enum SurveyQuestionType {

    /** 단답형 - 한 줄 텍스트 입력 */
    SHORT_ANSWER,

    /** 서술형 - 여러 줄 텍스트 입력 */
    PARAGRAPH,

    /** 객관식 (단일) - 보기 중 1개 선택 */
    MULTIPLE_CHOICE,

    /** 체크박스 (복수) - 보기 중 여러 개 선택 */
    CHECKBOX,

    /** 드롭다운 - 드롭다운에서 1개 선택 */
    DROPDOWN,

    /** 선형 배율 - 1~N점 척도 선택 */
    LINEAR_SCALE,

    /** 객관식 그리드 - 행×열 그리드에서 행마다 1개 선택 */
    MULTIPLE_CHOICE_GRID,

    /** 체크박스 그리드 - 행×열 그리드에서 행마다 여러 개 선택 */
    CHECKBOX_GRID,

    /** 날짜 - 날짜 입력 */
    DATE,

    /** 시간 - 시간 입력 */
    TIME,

    /** 파일 업로드 - 파일 첨부 */
    FILE_UPLOAD
}
