package igrus.web.survey.statistics.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.domain.SurveyVisibility;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.repository.SurveyAnswerRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.survey.statistics.dto.response.*;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SurveyStatisticsService 통합 테스트.
 *
 * <p>실제 H2 DB 환경에서 통계 정합성을 검증합니다.
 * 단위 테스트로는 확인하기 어려운 데이터 연관 관계 및 실제 쿼리 동작을 검증합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>TC-STAT-110~114: 복합 시나리오 통합 테스트</li>
 *     <li>TC-STAT-120: N+1 쿼리 방지 성능 통합 테스트</li>
 *     <li>TC-STAT-121: 대량 응답 성능 테스트 (선택)</li>
 * </ul>
 */
@DisplayName("SurveyStatisticsService 통합 테스트")
class SurveyStatisticsServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private SurveyStatisticsService surveyStatisticsService;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    @Autowired
    private SurveyResponseRepository surveyResponseRepository;

    @Autowired
    private SurveyAnswerRepository surveyAnswerRepository;

    private User operatorUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operatorUser = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            return null;
        });
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * PUBLISHED + CLOSED 설문을 생성하고 저장합니다.
     */
    private Survey createAndSavePublishedClosedSurvey() {
        Survey survey = Survey.create("통합 테스트 설문", "설명", SurveyAccessLevel.PUBLIC, null);
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.CLOSED);
        return surveyRepository.save(survey);
    }

    /**
     * PUBLISHED + OPEN 설문을 생성하고 저장합니다.
     */
    private Survey createAndSavePublishedOpenSurvey() {
        Survey survey = Survey.create("오픈 설문", "설명", SurveyAccessLevel.PUBLIC, null);
        ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.OPEN);
        return surveyRepository.save(survey);
    }

    /**
     * 회원 응답을 생성하고 저장합니다.
     */
    private SurveyResponse createAndSaveResponse(Survey survey, User user) {
        SurveyResponse response = SurveyResponse.create(survey, user);
        return surveyResponseRepository.save(response);
    }

    /**
     * 비회원(익명) 응답을 생성하고 저장합니다.
     */
    private SurveyResponse createAndSaveAnonymousResponse(Survey survey) {
        SurveyResponse response = SurveyResponse.createAnonymous(survey);
        return surveyResponseRepository.save(response);
    }

    // ==================== TASK-022: 복합 시나리오 통합 테스트 ====================

    @Nested
    @DisplayName("복합 시나리오 통합 테스트")
    class ComplexScenarioTest {

        @DisplayName("TC-STAT-110: 5개 질문 유형 혼합 설문 통계 전체 정합성")
        @Test
        void getSurveyStatistics_MixedQuestionTypes_AllStatisticsCorrect() {
            // given: 5개 질문 유형이 혼합된 설문 생성
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedClosedSurvey();

                // 1. SHORT_ANSWER 질문
                TextSurveyQuestion textQ = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "단답형 질문", null, true, 0);
                textQ = surveyQuestionRepository.save(textQ);

                // 2. MULTIPLE_CHOICE 질문
                OptionSurveyQuestion mcQ = OptionSurveyQuestion.create(
                        s, SurveyQuestionType.MULTIPLE_CHOICE, "객관식 질문", null, true, 1);
                SurveyQuestionOption mcOptA = SurveyQuestionOption.create(mcQ, "A", 0);
                SurveyQuestionOption mcOptB = SurveyQuestionOption.create(mcQ, "B", 1);
                mcQ.addOption(mcOptA);
                mcQ.addOption(mcOptB);
                mcQ = surveyQuestionRepository.save(mcQ);

                // 3. CHECKBOX 질문
                OptionSurveyQuestion cbQ = OptionSurveyQuestion.create(
                        s, SurveyQuestionType.CHECKBOX, "체크박스 질문", null, true, 2);
                SurveyQuestionOption cbOptX = SurveyQuestionOption.create(cbQ, "X", 0);
                SurveyQuestionOption cbOptY = SurveyQuestionOption.create(cbQ, "Y", 1);
                cbQ.addOption(cbOptX);
                cbQ.addOption(cbOptY);
                cbQ = surveyQuestionRepository.save(cbQ);

                // 4. LINEAR_SCALE 질문
                LinearScaleSurveyQuestion scaleQ = LinearScaleSurveyQuestion.create(
                        s, SurveyQuestionType.LINEAR_SCALE, "척도 질문", null, true, 3);
                scaleQ.setScaleRange(1, 5);
                scaleQ = surveyQuestionRepository.save(scaleQ);

                // 5. MC_GRID 질문
                GridSurveyQuestion gridQ = GridSurveyQuestion.create(
                        s, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, true, 4);
                SurveyQuestionOption gridOptP = SurveyQuestionOption.create(gridQ, "P", 0);
                SurveyQuestionOption gridOptQ = SurveyQuestionOption.create(gridQ, "Q", 1);
                gridQ.addOption(gridOptP);
                gridQ.addOption(gridOptQ);
                SurveyQuestionRow gridRow1 = SurveyQuestionRow.create(gridQ, "행1", 0);
                gridQ.addRow(gridRow1);
                gridQ = surveyQuestionRepository.save(gridQ);

                // 응답 2건 생성
                User member1 = createAndSaveUser("20230010", "m1@inha.edu", UserRole.MEMBER);
                User member2 = createAndSaveUser("20230011", "m2@inha.edu", UserRole.MEMBER);

                // 응답자 1
                SurveyResponse r1 = createAndSaveResponse(s, member1);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r1, textQ, "답변1"));
                surveyAnswerRepository.save(OptionSurveyAnswer.create(r1, mcQ, mcOptA));
                surveyAnswerRepository.save(OptionSurveyAnswer.create(r1, cbQ, cbOptX));
                surveyAnswerRepository.save(OptionSurveyAnswer.create(r1, cbQ, cbOptY));
                surveyAnswerRepository.save(NumericSurveyAnswer.create(r1, scaleQ, 3));
                surveyAnswerRepository.save(GridSurveyAnswer.create(r1, gridQ, gridRow1, gridOptP));

                // 응답자 2
                SurveyResponse r2 = createAndSaveResponse(s, member2);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r2, textQ, "답변2"));
                surveyAnswerRepository.save(OptionSurveyAnswer.create(r2, mcQ, mcOptB));
                surveyAnswerRepository.save(OptionSurveyAnswer.create(r2, cbQ, cbOptX));
                surveyAnswerRepository.save(NumericSurveyAnswer.create(r2, scaleQ, 5));
                surveyAnswerRepository.save(GridSurveyAnswer.create(r2, gridQ, gridRow1, gridOptQ));

                return s;
            });

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());

            // then
            assertThat(result.totalResponseCount()).isEqualTo(2);
            assertThat(result.questionStatistics()).hasSize(5);

            // TEXT 검증
            QuestionStatisticsResponse textStat = result.questionStatistics().get(0);
            assertThat(textStat.responseCount()).isEqualTo(2);
            assertThat(textStat.textStatistics()).isNotNull();
            assertThat(textStat.textStatistics().textResponses()).hasSize(2);

            // MC 검증
            QuestionStatisticsResponse mcStat = result.questionStatistics().get(1);
            assertThat(mcStat.responseCount()).isEqualTo(2);
            assertThat(mcStat.optionStatistics()).isNotNull();
            assertThat(mcStat.optionStatistics().options()).hasSize(2);

            // CHECKBOX 검증
            QuestionStatisticsResponse cbStat = result.questionStatistics().get(2);
            assertThat(cbStat.responseCount()).isEqualTo(2); // countDistinctResponses 적용
            assertThat(cbStat.optionStatistics()).isNotNull();

            // SCALE 검증
            QuestionStatisticsResponse scaleStat = result.questionStatistics().get(3);
            assertThat(scaleStat.responseCount()).isEqualTo(2);
            assertThat(scaleStat.scaleStatistics()).isNotNull();
            assertThat(scaleStat.scaleStatistics().average()).isEqualByComparingTo(new BigDecimal("4.0"));
            assertThat(scaleStat.scaleStatistics().min()).isEqualTo(3);
            assertThat(scaleStat.scaleStatistics().max()).isEqualTo(5);

            // GRID 검증
            QuestionStatisticsResponse gridStat = result.questionStatistics().get(4);
            assertThat(gridStat.responseCount()).isEqualTo(2); // countDistinctResponses 적용
            assertThat(gridStat.gridStatistics()).isNotNull();
            assertThat(gridStat.gridStatistics().rows()).hasSize(1);
        }

        @DisplayName("TC-STAT-111: 응답 수정(PUT) 후 통계 반영 - 수정 후 최종값만 반영")
        @Test
        void getSurveyStatistics_AfterResponseUpdate_ReflectsLatestOnly() {
            // given: MC 질문 + 응답자가 A를 선택 후 B로 수정
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedOpenSurvey();

                OptionSurveyQuestion mcQ = OptionSurveyQuestion.create(
                        s, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 0);
                SurveyQuestionOption optA = SurveyQuestionOption.create(mcQ, "A", 0);
                SurveyQuestionOption optB = SurveyQuestionOption.create(mcQ, "B", 1);
                mcQ.addOption(optA);
                mcQ.addOption(optB);
                mcQ = surveyQuestionRepository.save(mcQ);

                User member = createAndSaveUser("20230020", "m20@inha.edu", UserRole.MEMBER);

                // 초기 응답: A 선택
                SurveyResponse response = createAndSaveResponse(s, member);
                OptionSurveyAnswer initialAnswer = OptionSurveyAnswer.create(response, mcQ, optA);
                surveyAnswerRepository.save(initialAnswer);

                // 수정: 기존 답변 삭제 후 B로 새 답변 생성 (orphanRemoval 시뮬레이션)
                surveyAnswerRepository.delete(initialAnswer);
                surveyAnswerRepository.flush();
                OptionSurveyAnswer updatedAnswer = OptionSurveyAnswer.create(response, mcQ, optB);
                surveyAnswerRepository.save(updatedAnswer);

                return s;
            });

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());

            // then: 수정 후 최종 선택(B)만 반영
            assertThat(result.totalResponseCount()).isEqualTo(1);
            QuestionStatisticsResponse mcStat = result.questionStatistics().getFirst();
            List<OptionStatisticsItem> options = mcStat.optionStatistics().options();

            OptionStatisticsItem optAItem = options.stream()
                    .filter(o -> o.optionText().equals("A")).findFirst().orElseThrow();
            OptionStatisticsItem optBItem = options.stream()
                    .filter(o -> o.optionText().equals("B")).findFirst().orElseThrow();

            assertThat(optAItem.count()).isZero();
            assertThat(optBItem.count()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-112: 질문 추가 후 기존 응답자 미응답 질문 통계")
        @Test
        void getSurveyStatistics_QuestionAddedAfterResponses_NewQuestionHasZeroResponses() {
            // given: 질문 1개로 응답 2건 수집 후, 질문 1개 추가
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedOpenSurvey();

                // 기존 질문
                TextSurveyQuestion existingQ = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "기존 질문", null, true, 0);
                existingQ = surveyQuestionRepository.save(existingQ);

                User member1 = createAndSaveUser("20230030", "m30@inha.edu", UserRole.MEMBER);
                User member2 = createAndSaveUser("20230031", "m31@inha.edu", UserRole.MEMBER);

                SurveyResponse r1 = createAndSaveResponse(s, member1);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r1, existingQ, "응답1"));

                SurveyResponse r2 = createAndSaveResponse(s, member2);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r2, existingQ, "응답2"));

                // 나중에 추가된 질문
                TextSurveyQuestion newQ = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "새 질문", null, false, 1);
                surveyQuestionRepository.save(newQ);

                return s;
            });

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());

            // then
            assertThat(result.totalResponseCount()).isEqualTo(2);
            assertThat(result.questionStatistics()).hasSize(2);

            QuestionStatisticsResponse existingQStat = result.questionStatistics().get(0);
            assertThat(existingQStat.responseCount()).isEqualTo(2);
            assertThat(existingQStat.textStatistics().textResponses()).hasSize(2);

            QuestionStatisticsResponse newQStat = result.questionStatistics().get(1);
            assertThat(newQStat.responseCount()).isZero();
            assertThat(newQStat.textStatistics().textResponses()).isEmpty();
        }

        @DisplayName("TC-STAT-113: 비회원(PUBLIC) 응답 포함 통계 - user=null 응답도 카운트")
        @Test
        void getSurveyStatistics_PublicSurveyWithAnonymousResponses_IncludedInStats() {
            // given: PUBLIC 설문, 회원 응답 2건 + 비회원 응답 1건
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedOpenSurvey();

                TextSurveyQuestion textQ = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "PUBLIC 질문", null, true, 0);
                textQ = surveyQuestionRepository.save(textQ);

                User member1 = createAndSaveUser("20230040", "m40@inha.edu", UserRole.MEMBER);
                User member2 = createAndSaveUser("20230041", "m41@inha.edu", UserRole.MEMBER);

                // 회원 응답 2건
                SurveyResponse r1 = createAndSaveResponse(s, member1);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r1, textQ, "회원 응답1"));

                SurveyResponse r2 = createAndSaveResponse(s, member2);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r2, textQ, "회원 응답2"));

                // 비회원 응답 1건
                SurveyResponse r3 = createAndSaveAnonymousResponse(s);
                surveyAnswerRepository.save(TextSurveyAnswer.create(r3, textQ, "비회원 응답"));

                return s;
            });

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());

            // then
            assertThat(result.totalResponseCount()).isEqualTo(3);
            assertThat(result.questionStatistics().getFirst().responseCount()).isEqualTo(3);
            assertThat(result.questionStatistics().getFirst().textStatistics().textResponses())
                    .containsExactly("회원 응답1", "회원 응답2", "비회원 응답");
        }

        @DisplayName("TC-STAT-114: 질문 displayOrder 순서대로 통계 반환")
        @Test
        void getSurveyStatistics_QuestionsOrderedByDisplayOrder() {
            // given: 질문 A(displayOrder=2), B(displayOrder=0), C(displayOrder=1)
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedClosedSurvey();

                TextSurveyQuestion qA = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "질문 A", null, false, 2);
                TextSurveyQuestion qB = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "질문 B", null, false, 0);
                TextSurveyQuestion qC = TextSurveyQuestion.create(
                        s, SurveyQuestionType.SHORT_ANSWER, "질문 C", null, false, 1);

                surveyQuestionRepository.save(qA);
                surveyQuestionRepository.save(qB);
                surveyQuestionRepository.save(qC);

                return s;
            });

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());

            // then: displayOrder 오름차순: B(0), C(1), A(2)
            assertThat(result.questionStatistics()).hasSize(3);
            assertThat(result.questionStatistics().get(0).questionTitle()).isEqualTo("질문 B");
            assertThat(result.questionStatistics().get(1).questionTitle()).isEqualTo("질문 C");
            assertThat(result.questionStatistics().get(2).questionTitle()).isEqualTo("질문 A");
        }
    }

    // ==================== TASK-023: N+1 쿼리 방지 성능 통합 테스트 ====================

    @Nested
    @DisplayName("N+1 쿼리 방지 성능 테스트")
    class QueryPerformanceTest {

        @DisplayName("TC-STAT-120: 질문 10개, 선택지 50개, 응답 100건에서 쿼리 수 합리적 범위 확인")
        @Test
        void getSurveyStatistics_LargeDataSet_CompletesWithoutN1Problem() {
            // given: 질문 10개 (MC 각 5개 선택지), 응답 100건
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedClosedSurvey();

                // MC 질문 10개 생성, 각 5개 선택지
                OptionSurveyQuestion[] questions = new OptionSurveyQuestion[10];
                SurveyQuestionOption[][] options = new SurveyQuestionOption[10][5];

                for (int qi = 0; qi < 10; qi++) {
                    OptionSurveyQuestion q = OptionSurveyQuestion.create(
                            s, SurveyQuestionType.MULTIPLE_CHOICE, "질문 " + qi, null, true, qi);
                    for (int oi = 0; oi < 5; oi++) {
                        SurveyQuestionOption opt = SurveyQuestionOption.create(q, "옵션 " + qi + "-" + oi, oi);
                        q.addOption(opt);
                    }
                    questions[qi] = surveyQuestionRepository.save(q);
                    for (int oi = 0; oi < 5; oi++) {
                        options[qi][oi] = questions[qi].getOptions().get(oi);
                    }
                }

                // 응답 100건 생성
                for (int ri = 0; ri < 100; ri++) {
                    String studentId = String.format("202400%02d", ri);
                    User member = createAndSaveUser(studentId, "m" + ri + "@inha.edu", UserRole.MEMBER);
                    SurveyResponse response = createAndSaveResponse(s, member);

                    // 각 질문에 대해 랜덤 옵션 선택
                    for (int qi = 0; qi < 10; qi++) {
                        int optIdx = ri % 5;
                        surveyAnswerRepository.save(
                                OptionSurveyAnswer.create(response, questions[qi], options[qi][optIdx]));
                    }
                }

                return s;
            });

            // when: 성능 측정 (N+1 문제 없이 완료되는지)
            long startTime = System.currentTimeMillis();
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());
            long duration = System.currentTimeMillis() - startTime;

            // then: 정상 완료, 결과 정합성
            assertThat(result.totalResponseCount()).isEqualTo(100);
            assertThat(result.questionStatistics()).hasSize(10);
            for (QuestionStatisticsResponse qStat : result.questionStatistics()) {
                assertThat(qStat.responseCount()).isEqualTo(100);
                assertThat(qStat.optionStatistics().options()).hasSize(5);
            }

            // N+1 미발생 시 합리적 시간 내 완료 (3초 이내)
            assertThat(duration).isLessThan(3000L);
        }
    }

    // ==================== TASK-024: 대량 응답 성능 테스트 (선택) ====================

    @Nested
    @DisplayName("대량 응답 성능 테스트")
    class LargeScalePerformanceTest {

        @DisplayName("TC-STAT-121: 질문 50개, 응답 1000건 -> 응답 시간 3초 이내")
        @Test
        void getSurveyStatistics_VeryLargeDataSet_CompletesWithin3Seconds() {
            // given: 질문 50개 (MC 각 3개 선택지), 응답 1000건
            Survey survey = transactionTemplate.execute(status -> {
                Survey s = createAndSavePublishedClosedSurvey();

                // MC 질문 50개 생성 (선택지 3개씩 -> 총 150개 선택지)
                OptionSurveyQuestion[] questions = new OptionSurveyQuestion[50];
                SurveyQuestionOption[][] options = new SurveyQuestionOption[50][3];

                for (int qi = 0; qi < 50; qi++) {
                    OptionSurveyQuestion q = OptionSurveyQuestion.create(
                            s, SurveyQuestionType.MULTIPLE_CHOICE, "질문 " + qi, null, true, qi);
                    for (int oi = 0; oi < 3; oi++) {
                        SurveyQuestionOption opt = SurveyQuestionOption.create(q, "옵션 " + qi + "-" + oi, oi);
                        q.addOption(opt);
                    }
                    questions[qi] = surveyQuestionRepository.save(q);
                    for (int oi = 0; oi < 3; oi++) {
                        options[qi][oi] = questions[qi].getOptions().get(oi);
                    }
                }

                // 응답 1000건 생성
                for (int ri = 0; ri < 1000; ri++) {
                    String studentId = String.format("203%05d", ri);
                    User member = createAndSaveUser(studentId, "perf" + ri + "@inha.edu", UserRole.MEMBER);
                    SurveyResponse response = createAndSaveResponse(s, member);

                    for (int qi = 0; qi < 50; qi++) {
                        int optIdx = ri % 3;
                        surveyAnswerRepository.save(
                                OptionSurveyAnswer.create(response, questions[qi], options[qi][optIdx]));
                    }
                }

                return s;
            });

            // when: 응답 시간 측정
            long startTime = System.currentTimeMillis();
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    survey.getId(), operatorUser.getId());
            long duration = System.currentTimeMillis() - startTime;

            // then
            assertThat(result.totalResponseCount()).isEqualTo(1000);
            assertThat(result.questionStatistics()).hasSize(50);

            // 응답 시간 < 3초
            assertThat(duration).isLessThan(3000L);
        }
    }
}
