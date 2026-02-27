package igrus.web.common.fixture;

import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * Survey 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 Survey 엔티티와 관련 객체를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class SurveyTestFixture {

    private SurveyTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== Survey 생성 (기본: UNPUBLISHED + NOT_STARTED) ====================

    /**
     * 기본 설문을 생성합니다. (UNPUBLISHED + NOT_STARTED)
     *
     * @return 기본 설문
     */
    public static Survey createSurvey() {
        return Survey.create(DEFAULT_SURVEY_TITLE, DEFAULT_SURVEY_DESCRIPTION,
                SurveyAccessLevel.PUBLIC, null);
    }

    /**
     * 커스텀 제목/설명의 설문을 생성합니다.
     *
     * @param title       설문 제목
     * @param description 설문 설명
     * @return 생성된 설문
     */
    public static Survey createSurvey(String title, String description) {
        return Survey.create(title, description, SurveyAccessLevel.PUBLIC, null);
    }

    /**
     * ID가 설정된 기본 설문을 생성합니다. (DEFAULT_SURVEY_ID)
     *
     * @return ID가 설정된 설문
     */
    public static Survey createSurveyWithId() {
        return withId(createSurvey(), DEFAULT_SURVEY_ID);
    }

    /**
     * 지정된 ID가 설정된 설문을 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 설문
     */
    public static Survey createSurveyWithId(Long id) {
        return withId(createSurvey(), id);
    }

    // ==================== 특정 상태의 Survey 생성 ====================

    /**
     * PUBLISHED + NOT_STARTED 상태의 설문을 생성합니다.
     *
     * @return 공개 상태 설문
     */
    public static Survey createPublishedSurvey() {
        Survey survey = createSurvey();
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        return survey;
    }

    /**
     * PUBLISHED + OPEN 상태의 설문을 생성합니다.
     *
     * @return 공개 + 응답 수집 중 설문
     */
    public static Survey createPublishedAndOpenSurvey() {
        Survey survey = createSurvey();
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.OPEN);
        return survey;
    }

    /**
     * PUBLISHED + CLOSED 상태의 설문을 생성합니다.
     *
     * @return 공개 + 응답 마감 설문
     */
    public static Survey createClosedSurvey() {
        Survey survey = createSurvey();
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.CLOSED);
        return survey;
    }

    /**
     * 휴지통에 있는 설문을 생성합니다. (trashedAt 설정됨)
     *
     * @return 휴지통 설문
     */
    public static Survey createTrashedSurvey() {
        Survey survey = createSurvey();
        ReflectionTestUtils.setField(survey, "trashedAt", Instant.now());
        return survey;
    }

    // ==================== SurveyQuestion 생성 (publish 검증용) ====================

    /**
     * 단답형 질문을 생성합니다.
     *
     * @param survey 소속 설문
     * @param order  표시 순서
     * @return 단답형 질문
     */
    public static TextSurveyQuestion createShortAnswerQuestion(Survey survey, int order) {
        return TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                "단답형 질문 " + order, null, false, order);
    }

    /**
     * 객관식 질문을 생성합니다. (선택지 1개 포함)
     *
     * @param survey 소속 설문
     * @param order  표시 순서
     * @return 객관식 질문
     */
    public static OptionSurveyQuestion createMultipleChoiceQuestion(Survey survey, int order) {
        OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                "객관식 질문 " + order, null, false, order);
        SurveyQuestionOption option = SurveyQuestionOption.create(question, "선택지 1", 1);
        question.addOption(option);
        return question;
    }

    /**
     * 그리드 질문을 생성합니다. (선택지 1개 + 행 1개 포함)
     *
     * @param survey 소속 설문
     * @param order  표시 순서
     * @return 그리드 질문
     */
    public static GridSurveyQuestion createGridQuestion(Survey survey, int order) {
        GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                "그리드 질문 " + order, null, false, order);
        SurveyQuestionOption option = SurveyQuestionOption.create(question, "열 1", 1);
        question.addOption(option);
        SurveyQuestionRow row = SurveyQuestionRow.create(question, "행 1", 1);
        question.addRow(row);
        return question;
    }

    /**
     * 선형 배율 질문을 생성합니다. (scaleMin=1, scaleMax=5)
     *
     * @param survey 소속 설문
     * @param order  표시 순서
     * @return 선형 배율 질문
     */
    public static LinearScaleSurveyQuestion createLinearScaleQuestion(Survey survey, int order) {
        LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                "선형 배율 질문 " + order, null, false, order);
        question.setScaleRange(1, 5);
        return question;
    }

    // ==================== accessLevel이 설정된 Survey 생성 ====================

    /**
     * 지정된 accessLevel의 PUBLISHED + OPEN 설문을 생성합니다.
     *
     * @param accessLevel 응답 대상 권한
     * @return 공개 + 응답 수집 중 설문
     */
    public static Survey createPublishedAndOpenSurvey(SurveyAccessLevel accessLevel) {
        Survey survey = Survey.create(DEFAULT_SURVEY_TITLE, DEFAULT_SURVEY_DESCRIPTION, accessLevel, null);
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.OPEN);
        return survey;
    }
}
