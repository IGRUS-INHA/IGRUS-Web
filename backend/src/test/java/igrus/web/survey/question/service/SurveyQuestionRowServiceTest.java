package igrus.web.survey.question.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.dto.request.SaveQuestionRowRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.question.exception.SurveyRowNotFoundException;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.question.repository.SurveyQuestionRowRepository;
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
 * SurveyQuestionRowService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyQuestionRowService 단위 테스트")
class SurveyQuestionRowServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyQuestionRepository questionRepository;

    @Mock
    private SurveyQuestionRowRepository rowRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurveyQuestionRowService rowService;

    private static final Long QUESTION_ID = 100L;
    private static final Long ROW_ID = 300L;

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

    // ==================== 행 생성 ====================

    @Nested
    @DisplayName("행 생성")
    class CreateRow {

        @DisplayName("운영진 행 생성 성공")
        @Test
        void createRow_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.save(any(SurveyQuestionRow.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<SurveyDetailResponse.RowResponse> result =
                    rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            verify(rowRepository).save(any(SurveyQuestionRow.class));
        }

        @DisplayName("존재하지 않는 설문 시 SurveyNotFoundException")
        @Test
        void createRow_SurveyNotFound_ThrowsException() {
            // given
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("존재하지 않는 질문 시 SurveyQuestionNotFoundException")
        @Test
        void createRow_QuestionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyQuestionNotFoundException.class);
        }

        @DisplayName("질문이 다른 설문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void createRow_QuestionBelongsToDifferentSurvey_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            Survey otherSurvey = createSurveyWithId(99L);
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(otherSurvey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "다른 설문의 질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when & then
            assertThatThrownBy(() -> rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void createRow_ByMember_ThrowsAccessDenied() {
            // given
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("존재하지 않는 사용자 시 UserNotFoundException")
        @Test
        void createRow_UserNotFound_ThrowsException() {
            // given
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("행 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rowService.createRow(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== 행 수정 ====================

    @Nested
    @DisplayName("행 수정")
    class UpdateRow {

        @DisplayName("운영진 행 수정 성공")
        @Test
        void updateRow_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            GridSurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionRow row = withId(
                    SurveyQuestionRow.create(question, "원본 행", 1),
                    ROW_ID);
            question.addRow(row);
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("수정된 행", 2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.findByIdAndDeletedFalse(ROW_ID))
                    .willReturn(Optional.of(row));

            // when
            List<SurveyDetailResponse.RowResponse> result =
                    rowService.updateRow(DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, request, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            assertThat(row.getLabel()).isEqualTo("수정된 행");
            assertThat(row.getDisplayOrder()).isEqualTo(2);
        }

        @DisplayName("존재하지 않는 행 시 SurveyRowNotFoundException")
        @Test
        void updateRow_RowNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("수정된 행", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.findByIdAndDeletedFalse(ROW_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rowService.updateRow(
                    DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, request, operatorAuth))
                    .isInstanceOf(SurveyRowNotFoundException.class);
        }

        @DisplayName("행이 다른 질문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void updateRow_RowBelongsToDifferentQuestion_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestion otherQuestion = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "다른 질문", null, false, 2),
                    999L);
            SurveyQuestionRow row = withId(
                    SurveyQuestionRow.create(otherQuestion, "다른 질문의 행", 1),
                    ROW_ID);
            SaveQuestionRowRequest request = new SaveQuestionRowRequest("수정된 행", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.findByIdAndDeletedFalse(ROW_ID))
                    .willReturn(Optional.of(row));

            // when & then
            assertThatThrownBy(() -> rowService.updateRow(
                    DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, request, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 행 삭제 ====================

    @Nested
    @DisplayName("행 삭제")
    class DeleteRow {

        @DisplayName("운영진 행 삭제(soft delete) 성공")
        @Test
        void deleteRow_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionRow row = withId(
                    SurveyQuestionRow.create(question, "행", 1),
                    ROW_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.findByIdAndDeletedFalse(ROW_ID))
                    .willReturn(Optional.of(row));

            // when
            rowService.deleteRow(DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, operatorAuth);

            // then
            assertThat(row.isDeleted()).isTrue();
        }

        @DisplayName("존재하지 않는 행 삭제 시 SurveyRowNotFoundException")
        @Test
        void deleteRow_RowNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(rowRepository.findByIdAndDeletedFalse(ROW_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rowService.deleteRow(DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, operatorAuth))
                    .isInstanceOf(SurveyRowNotFoundException.class);
        }

        @DisplayName("일반 회원 삭제 시 SurveyAccessDeniedException")
        @Test
        void deleteRow_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> rowService.deleteRow(DEFAULT_SURVEY_ID, QUESTION_ID, ROW_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 행 목록 조회 ====================

    @Nested
    @DisplayName("행 목록 조회")
    class GetRowList {

        @DisplayName("운영진 행 목록 조회 성공")
        @Test
        void getRowList_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            GridSurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionRow row1 = SurveyQuestionRow.create(question, "행 1", 1);
            SurveyQuestionRow row2 = SurveyQuestionRow.create(question, "행 2", 2);
            question.addRow(row1);
            question.addRow(row2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            List<SurveyDetailResponse.RowResponse> result =
                    rowService.getRowList(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth);

            // then
            assertThat(result).hasSize(2);
        }

        @DisplayName("삭제된 행은 목록에서 제외")
        @Test
        void getRowList_ExcludesDeletedRows() {
            // given
            Survey survey = createSurveyWithId();
            GridSurveyQuestion question = withId(
                    GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionRow activeRow = SurveyQuestionRow.create(question, "활성 행", 1);
            SurveyQuestionRow deletedRow = SurveyQuestionRow.create(question, "삭제된 행", 2);
            deletedRow.delete(operatorAuth.userId());
            question.addRow(activeRow);
            question.addRow(deletedRow);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            List<SurveyDetailResponse.RowResponse> result =
                    rowService.getRowList(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().label()).isEqualTo("활성 행");
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getRowList_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> rowService.getRowList(DEFAULT_SURVEY_ID, QUESTION_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }
}
