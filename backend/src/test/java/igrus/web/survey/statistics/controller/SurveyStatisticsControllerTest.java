package igrus.web.survey.statistics.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.domain.SurveyVisibility;
import igrus.web.survey.question.domain.OptionSurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.OptionSurveyAnswer;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.repository.SurveyAnswerRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SurveyStatisticsController 통합 테스트.
 *
 * <p>HTTP 인증/인가, 응답 구조, 상태 코드를 MockMvc 기반으로 검증합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>TC-STAT-001~005: 권한/보안 테스트</li>
 *     <li>TC-STAT-100~102: 부정 시나리오 테스트</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("SurveyStatisticsController 통합 테스트")
class SurveyStatisticsControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    @Autowired
    private SurveyResponseRepository surveyResponseRepository;

    @Autowired
    private SurveyAnswerRepository surveyAnswerRepository;

    private static final String STATISTICS_URL = "/api/v1/surveys/{surveyId}/statistics";

    private User operatorUser;
    private User adminUser;
    private User memberUser;
    private User associateUser;
    private Survey survey;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operatorUser = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            adminUser = createAndSaveUser("20230004", "admin@inha.edu", UserRole.ADMIN);
            memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            associateUser = createAndSaveUser("20230003", "associate@inha.edu", UserRole.ASSOCIATE);
            survey = createAndSavePublishedClosedSurveyWithData();
            return null;
        });
    }

    private String generateToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getStudentId(), user.getRole().name());
    }

    /**
     * PUBLISHED + CLOSED 설문에 MC 질문 1개 + 응답 2건을 생성합니다.
     */
    private Survey createAndSavePublishedClosedSurveyWithData() {
        Survey s = Survey.create("통계 테스트 설문", "설문 설명", SurveyAccessLevel.MEMBER, null);
        ReflectionTestUtils.setField(s, "visibility", SurveyVisibility.PUBLISHED);
        ReflectionTestUtils.setField(s, "responseStatus", SurveyResponseStatus.CLOSED);
        s = surveyRepository.save(s);

        OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                s, SurveyQuestionType.MULTIPLE_CHOICE, "객관식 질문", null, true, 0);
        SurveyQuestionOption optionA = SurveyQuestionOption.create(mcQuestion, "A", 0);
        SurveyQuestionOption optionB = SurveyQuestionOption.create(mcQuestion, "B", 1);
        mcQuestion.addOption(optionA);
        mcQuestion.addOption(optionB);
        surveyQuestionRepository.save(mcQuestion);

        // 응답 1: optionA 선택
        SurveyResponse response1 = SurveyResponse.create(s, operatorUser);
        response1 = surveyResponseRepository.save(response1);
        OptionSurveyAnswer answer1 = OptionSurveyAnswer.create(response1, mcQuestion, optionA);
        surveyAnswerRepository.save(answer1);

        // 응답 2: optionB 선택
        SurveyResponse response2 = SurveyResponse.create(s, memberUser);
        response2 = surveyResponseRepository.save(response2);
        OptionSurveyAnswer answer2 = OptionSurveyAnswer.create(response2, mcQuestion, optionB);
        surveyAnswerRepository.save(answer2);

        return s;
    }

    // ==================== TASK-020: 권한/보안 컨트롤러 통합 테스트 ====================

    @Nested
    @DisplayName("권한/보안 테스트")
    class AuthorizationTest {

        @DisplayName("TC-STAT-001: 비인증 사용자 통계 조회 시 401 Unauthorized")
        @Test
        void getSurveyStatistics_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(STATISTICS_URL, survey.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @DisplayName("TC-STAT-002: ASSOCIATE 통계 조회 시 403 Forbidden")
        @Test
        void getSurveyStatistics_AsAssociate_Returns403() throws Exception {
            String token = generateToken(associateUser);

            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @DisplayName("TC-STAT-003: MEMBER 통계 조회 시 403 Forbidden")
        @Test
        void getSurveyStatistics_AsMember_Returns403() throws Exception {
            String token = generateToken(memberUser);

            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @DisplayName("TC-STAT-004: OPERATOR 통계 조회 시 200 OK + 통계 데이터 반환")
        @Test
        void getSurveyStatistics_AsOperator_Returns200WithData() throws Exception {
            String token = generateToken(operatorUser);

            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResponseCount").value(2))
                    .andExpect(jsonPath("$.respondents").isArray())
                    .andExpect(jsonPath("$.respondents.length()").value(2))
                    .andExpect(jsonPath("$.questionStatistics").isArray())
                    .andExpect(jsonPath("$.questionStatistics.length()").value(1))
                    .andExpect(jsonPath("$.questionStatistics[0].responseCount").value(2));
        }

        @DisplayName("TC-STAT-006: ADMIN 통계 조회 시 200 OK")
        @Test
        void getSurveyStatistics_AsAdmin_Returns200() throws Exception {
            String token = generateToken(adminUser);

            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResponseCount").value(2));
        }

        @DisplayName("TC-STAT-005: MEMBER 접근 시 부작용 없음 - 403 후 데이터 변경 없음")
        @Test
        void getSurveyStatistics_MemberAccess_NoSideEffect() throws Exception {
            String memberToken = generateToken(memberUser);
            String operatorToken = generateToken(operatorUser);

            // MEMBER로 접근 시도 -> 403
            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isForbidden());

            // 이후 OPERATOR로 동일 데이터 정상 조회 -> 데이터 변경 없음
            mockMvc.perform(get(STATISTICS_URL, survey.getId())
                            .header("Authorization", "Bearer " + operatorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResponseCount").value(2));
        }
    }

    // ==================== TASK-021: 부정 시나리오 컨트롤러 통합 테스트 ====================

    @Nested
    @DisplayName("부정 시나리오 테스트")
    class NegativeScenarioTest {

        @DisplayName("TC-STAT-100: 존재하지 않는 설문 ID로 통계 조회 시 404 Not Found")
        @Test
        void getSurveyStatistics_NonExistentSurvey_Returns404() throws Exception {
            String token = generateToken(operatorUser);

            mockMvc.perform(get(STATISTICS_URL, 999999L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @DisplayName("TC-STAT-101: 음수 surveyId로 통계 조회 시 400 Bad Request")
        @Test
        void getSurveyStatistics_NegativeSurveyId_Returns400() throws Exception {
            String token = generateToken(operatorUser);

            mockMvc.perform(get("/api/v1/surveys/-1/statistics")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("TC-STAT-102: 비숫자 surveyId로 통계 조회 시 400 Bad Request")
        @Test
        void getSurveyStatistics_NonNumericSurveyId_Returns400() throws Exception {
            String token = generateToken(operatorUser);

            mockMvc.perform(get("/api/v1/surveys/abc/statistics")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }
}
