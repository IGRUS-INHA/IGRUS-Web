package igrus.web.survey.question.domain;

/**
 * 설문 질문 유형.
 * 구글 폼과 유사한 11가지 질문 유형을 지원합니다.
 *
 * <p>각 유형은 4가지 카테고리 중 하나에 속합니다:</p>
 * <ul>
 *   <li>{@code TEXT} - 텍스트 입력 (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD)</li>
 *   <li>{@code SCALE} - 선형 배율 (LINEAR_SCALE)</li>
 *   <li>{@code OPTION} - 선택지 (MULTIPLE_CHOICE, CHECKBOX, DROPDOWN)</li>
 *   <li>{@code GRID} - 그리드 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID)</li>
 * </ul>
 */
public enum SurveyQuestionType {

    /** 단답형 - 한 줄 텍스트 입력 */
    SHORT_ANSWER("TEXT"),

    /** 서술형 - 여러 줄 텍스트 입력 */
    PARAGRAPH("TEXT"),

    /** 객관식 (단일) - 보기 중 1개 선택 */
    MULTIPLE_CHOICE("OPTION"),

    /** 체크박스 (복수) - 보기 중 여러 개 선택 */
    CHECKBOX("OPTION"),

    /** 드롭다운 - 드롭다운에서 1개 선택 */
    DROPDOWN("OPTION"),

    /** 선형 배율 - 1~N점 척도 선택 */
    LINEAR_SCALE("SCALE"),

    /** 객관식 그리드 - 행×열 그리드에서 행마다 1개 선택 */
    MULTIPLE_CHOICE_GRID("GRID"),

    /** 체크박스 그리드 - 행×열 그리드에서 행마다 여러 개 선택 */
    CHECKBOX_GRID("GRID"),

    /** 날짜 - 날짜 입력 */
    DATE("TEXT"),

    /** 시간 - 시간 입력 */
    TIME("TEXT"),

    /** 파일 업로드 - 파일 첨부 */
    FILE_UPLOAD("TEXT");

    private final String category;

    SurveyQuestionType(String category) {
        this.category = category;
    }

    /**
     * 질문 유형의 카테고리를 반환합니다.
     * STI(Single Table Inheritance) discriminator 값과 일치합니다.
     *
     * @return TEXT, SCALE, OPTION, GRID 중 하나
     */
    public String getCategory() {
        return category;
    }
}
