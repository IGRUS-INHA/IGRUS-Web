package igrus.web.survey.response.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.response.exception.*;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

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
 * SurveyResponseService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyResponseService 단위 테스트")
class SurveyResponseServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyResponseRepository surveyResponseRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SurveyAnswerValidator answerValidator;

    @InjectMocks
    private SurveyResponseService surveyResponseService;

    private User memberUser;
    private AuthenticatedUser memberAuth;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
        memberAuth = memberAuth();
    }

    // ==================== 회원 응답 제출 ====================

    @Nested
    @DisplayName("회원 응답 제출")
    class SubmitResponse {

        @DisplayName("회원 응답 제출 성공")
        @Test
        void submitResponse_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserId(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(result).isNotNull();
            assertThat(result.surveyId()).isEqualTo(DEFAULT_SURVEY_ID);
            assertThat(result.userId()).isEqualTo(DEFAULT_MEMBER_ID);
            assertThat(result.answers()).hasSize(1);
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
        }

        @DisplayName("중복 응답 시 거부")
        @Test
        void submitResponse_Duplicate_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserId(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(true);

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseDuplicateException.class);
        }

        @DisplayName("CLOSED 설문 응답 시 거부")
        @Test
        void submitResponse_ClosedSurvey_ThrowsException() {
            // given
            Survey survey = withId(createClosedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("휴지통 설문 응답 시 거부 (PUBLISHED+OPEN 이지만 trashed)")
        @Test
        void submitResponse_TrashedSurvey_ThrowsException() {
            // given: PUBLISHED+OPEN 상태이지만 휴지통에 있는 설문
            // trashedAt 때문에 isAcceptingResponses()가 false여야 함
            Survey survey = createPublishedAndOpenSurvey();
            ReflectionTestUtils.setField(survey, "trashedAt", Instant.now());
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("MEMBER 설문에 ASSOCIATE 사용자 응답 거부")
        @Test
        void submitResponse_MemberSurveyByAssociate_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.MEMBER), DEFAULT_SURVEY_ID);
            User associateUser = createUserWithRole("20200005", "준회원", UserRole.ASSOCIATE);
            withId(associateUser, 5L);
            AuthenticatedUser associateAuth = new AuthenticatedUser(5L, "20200005", UserRole.ASSOCIATE.name());

            given(userRepository.findById(5L)).willReturn(Optional.of(associateUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, associateAuth))
                    .isInstanceOf(SurveyResponseAccessDeniedException.class);
        }

        @DisplayName("OPERATOR 설문에 MEMBER 사용자 응답 거부")
        @Test
        void submitResponse_OperatorSurveyByMember_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.OPERATOR), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseAccessDeniedException.class);
        }

        @DisplayName("PUBLIC 설문에 ASSOCIATE 사용자 응답 성공")
        @Test
        void submitResponse_PublicSurveyByAssociate_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.PUBLIC), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            User associateUser = createUserWithRole("20200005", "준회원", UserRole.ASSOCIATE);
            withId(associateUser, 5L);
            AuthenticatedUser associateAuth = new AuthenticatedUser(5L, "20200005", UserRole.ASSOCIATE.name());

            given(userRepository.findById(5L)).willReturn(Optional.of(associateUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserId(DEFAULT_SURVEY_ID, 5L))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, associateAuth);

            // then
            assertThat(result).isNotNull();
        }

        @DisplayName("필수 질문 누락 제출 시 Validator 예외 전파")
        @Test
        void submitResponse_MissingRequiredQuestion_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            ReflectionTestUtils.setField(question, "required", true);
            withId(question, 100L);
            survey.getQuestions().add(question);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserId(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);

            // 필수 질문에 대한 답변 없이 빈 목록 제출
            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then - Validator가 던진 예외가 Service를 통해 전파됨
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("필수 질문");
        }
    }

    // ==================== 비회원 응답 제출 ====================

    @Nested
    @DisplayName("비회원 응답 제출")
    class SubmitAnonymousResponse {

        @DisplayName("PUBLIC 설문에 비회원 응답 성공")
        @Test
        void submitAnonymousResponse_PublicSurvey_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.PUBLIC), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isNull();
        }

        @DisplayName("MEMBER 설문에 비회원 응답 거부")
        @Test
        void submitAnonymousResponse_MemberSurvey_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.MEMBER), DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyAnonymousNotAllowedException.class);
        }

        @DisplayName("OPERATOR 설문에 비회원 응답 거부")
        @Test
        void submitAnonymousResponse_OperatorSurvey_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.OPERATOR), DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyAnonymousNotAllowedException.class);
        }

        @DisplayName("CLOSED PUBLIC 설문에 비회원 응답 거부")
        @Test
        void submitAnonymousResponse_ClosedPublicSurvey_ThrowsException() {
            // given
            Survey survey = Survey.create("설문", "설명", SurveyAccessLevel.PUBLIC, null);
            ReflectionTestUtils.setField(survey, "visibility", SurveyVisibility.PUBLISHED);
            ReflectionTestUtils.setField(survey, "responseStatus", SurveyResponseStatus.CLOSED);
            withId(survey, DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }
    }

    // ==================== 응답 수정 ====================

    @Nested
    @DisplayName("응답 수정")
    class UpdateMyResponse {

        @DisplayName("본인 응답 수정 성공")
        @Test
        void updateMyResponse_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "수정된 답변", null, null, null)
            ));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.updateMyResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(result).isNotNull();
            assertThat(result.answers()).hasSize(1);
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
        }

        @DisplayName("CLOSED 상태에서 응답 수정 거부")
        @Test
        void updateMyResponse_ClosedSurvey_ThrowsException() {
            // given
            Survey survey = withId(createClosedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "수정", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateMyResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("OPERATOR 설문에 MEMBER 사용자 수정 시 거부")
        @Test
        void updateMyResponse_OperatorSurveyByMember_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.OPERATOR), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "수정", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateMyResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseAccessDeniedException.class);
        }

        @DisplayName("휴지통 설문에서 응답 수정 시 거부")
        @Test
        void updateMyResponse_TrashedSurvey_ThrowsException() {
            // given: PUBLISHED+OPEN 상태이지만 휴지통에 있는 설문
            Survey survey = createPublishedAndOpenSurvey();
            ReflectionTestUtils.setField(survey, "trashedAt", Instant.now());
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "수정", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateMyResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("응답이 없는 상태에서 수정 시 거부")
        @Test
        void updateMyResponse_NoExistingResponse_ThrowsException() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID)).willReturn(Optional.of(survey));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.empty());

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of(
                    new SubmitAnswerRequest(100L, "수정", null, null, null)
            ));

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateMyResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseNotFoundException.class);
        }
    }

    // ==================== 응답 조회 ====================

    @Nested
    @DisplayName("본인 응답 조회")
    class GetMyResponse {

        @DisplayName("본인 응답 조회 성공")
        @Test
        void getMyResponse_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);
            TextSurveyAnswer textAnswer = TextSurveyAnswer.create(existingResponse, question, "답변");
            existingResponse.addAnswer(textAnswer);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth);

            // then
            assertThat(result).isNotNull();
            assertThat(result.responseId()).isEqualTo(1L);
            assertThat(result.surveyId()).isEqualTo(DEFAULT_SURVEY_ID);
            assertThat(result.userId()).isEqualTo(DEFAULT_MEMBER_ID);
            assertThat(result.answers()).hasSize(1);
        }

        @DisplayName("응답이 없을 때 조회 시 예외")
        @Test
        void getMyResponse_NotFound_ThrowsException() {
            // given
            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyResponseNotFoundException.class);
        }

        @DisplayName("삭제된 질문의 답변은 제외")
        @Test
        void getMyResponse_ExcludesDeletedQuestionAnswers() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question1 = createShortAnswerQuestion(survey, 1);
            withId(question1, 100L);
            survey.getQuestions().add(question1);

            TextSurveyQuestion question2 = createShortAnswerQuestion(survey, 2);
            withId(question2, 101L);
            question2.delete(1L); // soft delete
            survey.getQuestions().add(question2);

            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);
            existingResponse.addAnswer(TextSurveyAnswer.create(existingResponse, question1, "답변1"));
            existingResponse.addAnswer(TextSurveyAnswer.create(existingResponse, question2, "답변2"));

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));

            // when
            SurveyResponseDetailResponse result = surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth);

            // then
            assertThat(result.answers()).hasSize(1);
            assertThat(result.answers().getFirst().questionId()).isEqualTo(100L);
        }

        @DisplayName("accessLevel이 OPERATOR로 변경된 설문에서 MEMBER가 본인 응답 조회 가능")
        @Test
        void getMyResponse_AccessLevelChangedToOperator_MemberCanStillView() {
            // given: 원래 PUBLIC이었던 설문의 accessLevel이 OPERATOR로 변경됨
            Survey survey = withId(createPublishedAndOpenSurvey(SurveyAccessLevel.OPERATOR), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            // MEMBER가 이전에 제출한 응답이 존재
            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);
            existingResponse.addAnswer(TextSurveyAnswer.create(existingResponse, question, "답변"));

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));

            // when: accessLevel이 OPERATOR이지만 본인 응답 조회는 accessLevel과 무관
            SurveyResponseDetailResponse result = surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth);

            // then
            assertThat(result).isNotNull();
            assertThat(result.responseId()).isEqualTo(1L);
            assertThat(result.userId()).isEqualTo(DEFAULT_MEMBER_ID);
        }
    }
}
