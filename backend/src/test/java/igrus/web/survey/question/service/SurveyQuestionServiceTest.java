package igrus.web.survey.question.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.dto.request.CreateQuestionRequest;
import igrus.web.survey.question.dto.request.UpdateQuestionRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionLimitExceededException;
import igrus.web.survey.question.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * SurveyQuestionService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyQuestionService 단위 테스트")
class SurveyQuestionServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyQuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurveyQuestionService surveyQuestionService;

    private static final Long QUESTION_ID = 100L;

    private User operatorUser;
    private User memberUser;
    private AuthenticatedUser operatorAuth;
    private AuthenticatedUser memberAuth;

    @BeforeEach
    void setUp() {
        operatorUser = createOperatorWithId();
        memberUser = createMemberWithId();
        operatorAuth = operatorAuth();
        memberAuth = memberAuth();
    }

    // ==================== 질문 생성 ====================

    @Nested
    @DisplayName("질문 생성")
    class CreateQuestion {

        @DisplayName("SVC-QST-001: 운영진 질문 생성 성공")
        @Test
        void createQuestion_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            CreateQuestionRequest request = new CreateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "단답형 질문", null, false, 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.countBySurveyIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(0L);
            given(questionRepository.save(any(SurveyQuestion.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            SurveyDetailResponse response = surveyQuestionService.createQuestion(DEFAULT_SURVEY_ID, request, operatorAuth);

            // then
            assertThat(response.questions()).hasSize(1);
            verify(questionRepository).save(any(SurveyQuestion.class));
        }

        @DisplayName("SVC-QST-002: 질문 50개 초과 시 SurveyQuestionLimitExceededException")
        @Test
        void createQuestion_ExceedsLimit_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            CreateQuestionRequest request = new CreateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "51번째 질문", null, false, 51);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.countBySurveyIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(50L);

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.createQuestion(DEFAULT_SURVEY_ID, request, operatorAuth))
                    .isInstanceOf(SurveyQuestionLimitExceededException.class);
        }

        @DisplayName("SVC-QST-003: 존재하지 않는 설문 시 SurveyNotFoundException")
        @Test
        void createQuestion_SurveyNotFound_ThrowsException() {
            // given
            CreateQuestionRequest request = new CreateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "질문", null, false, 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.createQuestion(DEFAULT_SURVEY_ID, request, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("SVC-QST-004: 일반 회원(MEMBER) 생성 시 SurveyAccessDeniedException")
        @Test
        void createQuestion_ByMember_ThrowsAccessDenied() {
            // given
            CreateQuestionRequest request = new CreateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "질문", null, false, 1);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.createQuestion(DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("SVC-QST-012: 존재하지 않는 사용자 시 UserNotFoundException")
        @Test
        void createQuestion_UserNotFound_ThrowsException() {
            // given
            CreateQuestionRequest request = new CreateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "질문", null, false, 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.createQuestion(DEFAULT_SURVEY_ID, request, operatorAuth))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== 질문 수정 ====================

    @Nested
    @DisplayName("질문 수정")
    class UpdateQuestion {

        @DisplayName("SVC-QST-005: 운영진 질문 수정 성공")
        @Test
        void updateQuestion_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER, "원본 제목", null, false, 1),
                    QUESTION_ID);
            survey.getQuestions().add(question);

            UpdateQuestionRequest request = new UpdateQuestionRequest(
                    SurveyQuestionType.PARAGRAPH, "수정 제목", "수정 설명", true, 2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            SurveyDetailResponse response = surveyQuestionService.updateQuestion(
                    DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth);

            // then
            assertThat(response.questions()).hasSize(1);
            assertThat(response.questions().getFirst().title()).isEqualTo("수정 제목");
            assertThat(response.questions().getFirst().questionType()).isEqualTo(SurveyQuestionType.PARAGRAPH);
        }

        @DisplayName("SVC-QST-006: 존재하지 않는 질문 시 SurveyQuestionNotFoundException")
        @Test
        void updateQuestion_QuestionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            UpdateQuestionRequest request = new UpdateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "수정 제목", null, false, 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.updateQuestion(
                    DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyQuestionNotFoundException.class);
        }

        @DisplayName("SVC-QST-007: 질문이 다른 설문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void updateQuestion_QuestionBelongsToDifferentSurvey_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            Survey otherSurvey = createSurveyWithId(99L);
            SurveyQuestion question = withId(
                    TextSurveyQuestion.create(otherSurvey, SurveyQuestionType.SHORT_ANSWER, "다른 설문의 질문", null, false, 1),
                    QUESTION_ID);

            UpdateQuestionRequest request = new UpdateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "수정 제목", null, false, 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.updateQuestion(
                    DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("일반 회원 수정 시 SurveyAccessDeniedException")
        @Test
        void updateQuestion_ByMember_ThrowsAccessDenied() {
            // given
            UpdateQuestionRequest request = new UpdateQuestionRequest(
                    SurveyQuestionType.SHORT_ANSWER, "수정 제목", null, false, 1);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.updateQuestion(
                    DEFAULT_SURVEY_ID, QUESTION_ID, request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 질문 삭제 ====================

    @Nested
    @DisplayName("질문 삭제")
    class DeleteQuestion {

        @DisplayName("SVC-QST-008: 운영진 질문 삭제(soft delete) 성공")
        @Test
        void deleteQuestion_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER, "질문", null, false, 1),
                    QUESTION_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            surveyQuestionService.deleteQuestion(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth);

            // then
            assertThat(question.isDeleted()).isTrue();
        }

        @DisplayName("SVC-QST-009: 질문이 다른 설문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void deleteQuestion_QuestionBelongsToDifferentSurvey_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            Survey otherSurvey = createSurveyWithId(99L);
            SurveyQuestion question = withId(
                    TextSurveyQuestion.create(otherSurvey, SurveyQuestionType.SHORT_ANSWER, "다른 설문의 질문", null, false, 1),
                    QUESTION_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.deleteQuestion(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("존재하지 않는 질문 삭제 시 SurveyQuestionNotFoundException")
        @Test
        void deleteQuestion_QuestionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.deleteQuestion(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth))
                    .isInstanceOf(SurveyQuestionNotFoundException.class);
        }

        @DisplayName("일반 회원 삭제 시 SurveyAccessDeniedException")
        @Test
        void deleteQuestion_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.deleteQuestion(DEFAULT_SURVEY_ID, QUESTION_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 질문 목록 조회 ====================

    @Nested
    @DisplayName("질문 목록 조회")
    class GetQuestionList {

        @DisplayName("SVC-QST-010: 운영진 질문 목록 조회 성공")
        @Test
        void getQuestionList_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question1 = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "질문 1", null, false, 1);
            SurveyQuestion question2 = TextSurveyQuestion.create(survey, SurveyQuestionType.PARAGRAPH,
                    "질문 2", null, false, 2);
            survey.getQuestions().add(question1);
            survey.getQuestions().add(question2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            List<SurveyDetailResponse.QuestionResponse> result =
                    surveyQuestionService.getQuestionList(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(result).hasSize(2);
        }

        @DisplayName("SVC-QST-011: 삭제된 질문은 목록에서 제외")
        @Test
        void getQuestionList_ExcludesDeletedQuestions() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion activeQuestion = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "활성 질문", null, false, 1);
            SurveyQuestion deletedQuestion = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "삭제된 질문", null, false, 2);
            deletedQuestion.delete(operatorAuth.userId());
            survey.getQuestions().add(activeQuestion);
            survey.getQuestions().add(deletedQuestion);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            List<SurveyDetailResponse.QuestionResponse> result =
                    surveyQuestionService.getQuestionList(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().title()).isEqualTo("활성 질문");
        }

        @DisplayName("설문 미존재 시 SurveyNotFoundException")
        @Test
        void getQuestionList_SurveyNotFound_ThrowsException() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.getQuestionList(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getQuestionList_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyQuestionService.getQuestionList(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }
}
