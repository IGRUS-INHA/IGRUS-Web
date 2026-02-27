package igrus.web.survey.question.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.dto.request.SaveQuestionOptionRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.exception.SurveyOptionNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.question.repository.SurveyQuestionOptionRepository;
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
 * SurveyQuestionOptionService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyQuestionOptionService 단위 테스트")
class SurveyQuestionOptionServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyQuestionRepository questionRepository;

    @Mock
    private SurveyQuestionOptionRepository optionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurveyQuestionOptionService optionService;

    private static final Long QUESTION_ID = 100L;
    private static final Long OPTION_ID = 200L;

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

    // ==================== 선택지 생성 ====================

    @Nested
    @DisplayName("선택지 생성")
    class CreateOption {

        @DisplayName("운영진 선택지 생성 성공")
        @Test
        void createOption_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.save(any(SurveyQuestionOption.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<SurveyDetailResponse.OptionResponse> result =
                    optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            verify(optionRepository).save(any(SurveyQuestionOption.class));
        }

        @DisplayName("존재하지 않는 설문 시 SurveyNotFoundException")
        @Test
        void createOption_SurveyNotFound_ThrowsException() {
            // given
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("존재하지 않는 질문 시 SurveyQuestionNotFoundException")
        @Test
        void createOption_QuestionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyQuestionNotFoundException.class);
        }

        @DisplayName("질문이 다른 설문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void createOption_QuestionBelongsToDifferentSurvey_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            Survey otherSurvey = createSurveyWithId(99L);
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(otherSurvey, SurveyQuestionType.MULTIPLE_CHOICE, "다른 설문의 질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when & then
            assertThatThrownBy(() -> optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void createOption_ByMember_ThrowsAccessDenied() {
            // given
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("존재하지 않는 사용자 시 UserNotFoundException")
        @Test
        void createOption_UserNotFound_ThrowsException() {
            // given
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("선택지 1", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> optionService.createOption(DEFAULT_SURVEY_ID, QUESTION_ID, request, operatorAuth))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== 선택지 수정 ====================

    @Nested
    @DisplayName("선택지 수정")
    class UpdateOption {

        @DisplayName("운영진 선택지 수정 성공")
        @Test
        void updateOption_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            OptionSurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionOption option = withId(
                    SurveyQuestionOption.create(question, "원본 선택지", 1),
                    OPTION_ID);
            question.addOption(option);
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("수정된 선택지", 2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.findByIdAndDeletedFalse(OPTION_ID))
                    .willReturn(Optional.of(option));

            // when
            List<SurveyDetailResponse.OptionResponse> result =
                    optionService.updateOption(DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, request, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            assertThat(option.getText()).isEqualTo("수정된 선택지");
            assertThat(option.getDisplayOrder()).isEqualTo(2);
        }

        @DisplayName("존재하지 않는 선택지 시 SurveyOptionNotFoundException")
        @Test
        void updateOption_OptionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("수정된 선택지", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.findByIdAndDeletedFalse(OPTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> optionService.updateOption(
                    DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyOptionNotFoundException.class);
        }

        @DisplayName("선택지가 다른 질문에 소속된 경우 SurveyAccessDeniedException")
        @Test
        void updateOption_OptionBelongsToDifferentQuestion_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestion otherQuestion = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "다른 질문", null, false, 2),
                    999L);
            SurveyQuestionOption option = withId(
                    SurveyQuestionOption.create(otherQuestion, "다른 질문의 선택지", 1),
                    OPTION_ID);
            SaveQuestionOptionRequest request = new SaveQuestionOptionRequest("수정된 선택지", 1);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.findByIdAndDeletedFalse(OPTION_ID))
                    .willReturn(Optional.of(option));

            // when & then
            assertThatThrownBy(() -> optionService.updateOption(
                    DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, request, operatorAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 선택지 삭제 ====================

    @Nested
    @DisplayName("선택지 삭제")
    class DeleteOption {

        @DisplayName("운영진 선택지 삭제(soft delete) 성공")
        @Test
        void deleteOption_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionOption option = withId(
                    SurveyQuestionOption.create(question, "선택지", 1),
                    OPTION_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.findByIdAndDeletedFalse(OPTION_ID))
                    .willReturn(Optional.of(option));

            // when
            optionService.deleteOption(DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, operatorAuth);

            // then
            assertThat(option.isDeleted()).isTrue();
        }

        @DisplayName("존재하지 않는 선택지 삭제 시 SurveyOptionNotFoundException")
        @Test
        void deleteOption_OptionNotFound_ThrowsException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));
            given(optionRepository.findByIdAndDeletedFalse(OPTION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> optionService.deleteOption(
                    DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, operatorAuth))
                    .isInstanceOf(SurveyOptionNotFoundException.class);
        }

        @DisplayName("일반 회원 삭제 시 SurveyAccessDeniedException")
        @Test
        void deleteOption_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> optionService.deleteOption(
                    DEFAULT_SURVEY_ID, QUESTION_ID, OPTION_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 선택지 목록 조회 ====================

    @Nested
    @DisplayName("선택지 목록 조회")
    class GetOptionList {

        @DisplayName("운영진 선택지 목록 조회 성공")
        @Test
        void getOptionList_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            OptionSurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "선택지 1", 1);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "선택지 2", 2);
            question.addOption(option1);
            question.addOption(option2);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            List<SurveyDetailResponse.OptionResponse> result =
                    optionService.getOptionList(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth);

            // then
            assertThat(result).hasSize(2);
        }

        @DisplayName("삭제된 선택지는 목록에서 제외")
        @Test
        void getOptionList_ExcludesDeletedOptions() {
            // given
            Survey survey = createSurveyWithId();
            OptionSurveyQuestion question = withId(
                    OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE, "질문", null, false, 1),
                    QUESTION_ID);
            SurveyQuestionOption activeOption = SurveyQuestionOption.create(question, "활성 선택지", 1);
            SurveyQuestionOption deletedOption = SurveyQuestionOption.create(question, "삭제된 선택지", 2);
            deletedOption.delete(operatorAuth.userId());
            question.addOption(activeOption);
            question.addOption(deletedOption);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(questionRepository.findByIdAndDeletedFalse(QUESTION_ID))
                    .willReturn(Optional.of(question));

            // when
            List<SurveyDetailResponse.OptionResponse> result =
                    optionService.getOptionList(DEFAULT_SURVEY_ID, QUESTION_ID, operatorAuth);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().text()).isEqualTo("활성 선택지");
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getOptionList_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> optionService.getOptionList(DEFAULT_SURVEY_ID, QUESTION_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }
}
