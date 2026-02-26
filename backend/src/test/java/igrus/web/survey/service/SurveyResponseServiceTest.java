package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.dto.request.SubmitAnswerRequest;
import igrus.web.survey.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.exception.*;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.repository.SurveyResponseRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

    private static final Long QUESTION_ID = 100L;

    private User memberUser;
    private User associateUser;
    private AuthenticatedUser memberAuth;
    private AuthenticatedUser associateAuth;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
        associateUser = createAssociateWithId();
        memberAuth = memberAuth();
        associateAuth = associateAuth();
    }

    // ==================== 회원 응답 제출 ====================

    @Nested
    @DisplayName("회원 응답 제출")
    class SubmitResponse {

        @DisplayName("PUBLISHED+OPEN 상태에서 정상 제출하면 성공한다")
        @Test
        void submitResponse_WithValidRequest_ReturnsResponse() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, QUESTION_ID);
            survey.getQuestions().add(question);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(
                    List.of(new SubmitAnswerRequest(QUESTION_ID, "답변입니다", null, null, null, null)));

            // when
            SurveyResponseDetailResponse response = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.surveyId()).isEqualTo(DEFAULT_SURVEY_ID);
            assertThat(response.userId()).isEqualTo(DEFAULT_MEMBER_ID);
            assertThat(response.answers()).hasSize(1);
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
        }

        @DisplayName("존재하지 않는 사용자 ID이면 UserNotFoundException 발생")
        @Test
        void submitResponse_WithNonExistentUser_ThrowsException() {
            // given
            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.empty());

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("존재하지 않는 설문 ID이면 SurveyNotFoundException 발생")
        @Test
        void submitResponse_WithNonExistentSurvey_ThrowsException() {
            // given
            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("UNPUBLISHED 설문에 응답하면 SurveyNotAcceptingResponsesException 발생 (INV-09)")
        @Test
        void submitResponse_ToUnpublishedSurvey_ThrowsException() {
            // given
            Survey survey = createSurveyWithId(DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("CLOSED 설문에 응답하면 SurveyNotAcceptingResponsesException 발생 (INV-09)")
        @Test
        void submitResponse_ToClosedSurvey_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("이미 응답한 설문에 다시 응답하면 SurveyResponseDuplicateException 발생 (INV-01)")
        @Test
        void submitResponse_DuplicateResponse_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(true);

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseDuplicateException.class);
        }

        @DisplayName("동시 중복 제출 시 DataIntegrityViolationException이 SurveyResponseDuplicateException으로 변환된다")
        @Test
        void submitResponse_ConcurrentDuplicate_ThrowsDuplicateException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint violation"));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseDuplicateException.class);
        }
    }

    // ==================== 접근 권한 검증 ====================

    @Nested
    @DisplayName("접근 권한 검증")
    class AccessLevelValidation {

        @DisplayName("MEMBER 설문에 준회원(ASSOCIATE)이 응답하면 SurveyResponseAccessDeniedException 발생")
        @Test
        void submitResponse_AssociateToMemberSurvey_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.MEMBER);
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_ASSOCIATE_ID)).willReturn(Optional.of(associateUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, associateAuth))
                    .isInstanceOf(SurveyResponseAccessDeniedException.class);
        }

        @DisplayName("ASSOCIATE 설문에 준회원이 응답하면 성공한다")
        @Test
        void submitResponse_AssociateToAssociateSurvey_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.ASSOCIATE);
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_ASSOCIATE_ID)).willReturn(Optional.of(associateUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_ASSOCIATE_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when
            SurveyResponseDetailResponse response = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, associateAuth);

            // then
            assertThat(response).isNotNull();
        }

        @DisplayName("PUBLIC 설문에 정회원이 응답하면 성공한다")
        @Test
        void submitResponse_MemberToPublicSurvey_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.PUBLIC);
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when
            SurveyResponseDetailResponse response = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(response).isNotNull();
        }
    }

    // ==================== 비회원 응답 제출 ====================

    @Nested
    @DisplayName("비회원 응답 제출")
    class SubmitAnonymousResponse {

        @DisplayName("PUBLIC 설문에 비회원이 정상 제출하면 성공한다")
        @Test
        void submitAnonymousResponse_ToPublicSurvey_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.PUBLIC);
            withId(survey, DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when
            SurveyResponseDetailResponse response = surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isNull();
        }

        @DisplayName("ASSOCIATE 설문에 비회원이 제출하면 SurveyAnonymousNotAllowedException 발생")
        @Test
        void submitAnonymousResponse_ToAssociateSurvey_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.ASSOCIATE);
            withId(survey, DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyAnonymousNotAllowedException.class);
        }

        @DisplayName("MEMBER 설문에 비회원이 제출하면 SurveyAnonymousNotAllowedException 발생")
        @Test
        void submitAnonymousResponse_ToMemberSurvey_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.MEMBER);
            withId(survey, DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyAnonymousNotAllowedException.class);
        }

        @DisplayName("응답 불가 설문에 비회원이 제출하면 SurveyNotAcceptingResponsesException 발생")
        @Test
        void submitAnonymousResponse_ToClosedSurvey_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitAnonymousResponse(
                    DEFAULT_SURVEY_ID, request))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }
    }

    // ==================== 응답 수정 ====================

    @Nested
    @DisplayName("응답 수정")
    class UpdateResponse {

        @DisplayName("OPEN 중인 설문의 응답을 수정하면 성공한다")
        @Test
        void updateResponse_WhileOpen_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, QUESTION_ID);
            survey.getQuestions().add(question);

            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(
                    List.of(new SubmitAnswerRequest(QUESTION_ID, "수정된 답변", null, null, null, null)));

            // when
            SurveyResponseDetailResponse response = surveyResponseService.updateResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answers()).hasSize(1);
        }

        @DisplayName("CLOSED 설문의 응답을 수정하면 SurveyNotAcceptingResponsesException 발생")
        @Test
        void updateResponse_WhenClosed_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyNotAcceptingResponsesException.class);
        }

        @DisplayName("기존 응답이 없으면 SurveyResponseNotFoundException 발생")
        @Test
        void updateResponse_WithNoExistingResponse_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.empty());

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.updateResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseNotFoundException.class);
        }
    }

    // ==================== 본인 응답 조회 ====================

    @Nested
    @DisplayName("본인 응답 조회")
    class GetMyResponse {

        @DisplayName("기존 응답이 있으면 정상 조회된다")
        @Test
        void getMyResponse_WithExistingResponse_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);

            SurveyResponse existingResponse = SurveyResponse.create(survey, memberUser);
            withId(existingResponse, 1L);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.of(existingResponse));

            // when
            SurveyResponseDetailResponse response = surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.responseId()).isEqualTo(1L);
            assertThat(response.userId()).isEqualTo(DEFAULT_MEMBER_ID);
        }

        @DisplayName("응답이 없으면 SurveyResponseNotFoundException 발생")
        @Test
        void getMyResponse_WithNoResponse_ThrowsException() {
            // given
            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyResponseNotFoundException.class);
        }

        @DisplayName("accessLevel이 변경되어도 기존 응답 조회가 가능하다 (INV-19)")
        @Test
        void getMyResponse_AfterAccessLevelChange_StillReturnsResponse() {
            // given: ASSOCIATE로 응답한 뒤, accessLevel이 MEMBER로 변경된 경우
            // (조회 시 accessLevel 검증을 하지 않으므로 기존 응답이 반환된다)
            Survey survey = createPublishedAndOpenSurvey(SurveyAccessLevel.MEMBER);
            withId(survey, DEFAULT_SURVEY_ID);

            // associateUser가 이전에 ASSOCIATE 설문일 때 응답함
            SurveyResponse existingResponse = SurveyResponse.create(survey, associateUser);
            withId(existingResponse, 1L);

            given(userRepository.findById(DEFAULT_ASSOCIATE_ID)).willReturn(Optional.of(associateUser));
            given(surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_ASSOCIATE_ID))
                    .willReturn(Optional.of(existingResponse));

            // when
            SurveyResponseDetailResponse response = surveyResponseService.getMyResponse(
                    DEFAULT_SURVEY_ID, associateAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.responseId()).isEqualTo(1L);
        }
    }

    // ==================== 필수 질문 검증 (답변 유효성) ====================

    @Nested
    @DisplayName("필수 질문 답변 검증 (INV-12)")
    class RequiredQuestionValidation {

        @DisplayName("필수 질문 답변이 누락되면 SurveyResponseValidationException 발생")
        @Test
        void submitResponse_MissingRequiredQuestion_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);
            SurveyQuestion requiredQuestion = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "필수 질문", null, true, 1);
            withId(requiredQuestion, QUESTION_ID);
            survey.getQuestions().add(requiredQuestion);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);

            // 필수 질문에 대한 답변 누락
            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when & then
            assertThatThrownBy(() -> surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("선택 질문만 있는 설문에 빈 답변을 제출하면 성공한다")
        @Test
        void submitResponse_AllOptionalQuestions_WithEmptyAnswers_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            withId(survey, DEFAULT_SURVEY_ID);
            SurveyQuestion optionalQuestion = createShortAnswerQuestion(survey, 1);
            withId(optionalQuestion, QUESTION_ID);
            survey.getQuestions().add(optionalQuestion);

            given(userRepository.findById(DEFAULT_MEMBER_ID)).willReturn(Optional.of(memberUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));
            given(surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(DEFAULT_SURVEY_ID, DEFAULT_MEMBER_ID))
                    .willReturn(false);
            given(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .willAnswer(invocation -> {
                        SurveyResponse resp = invocation.getArgument(0);
                        withId(resp, 1L);
                        return resp;
                    });

            SubmitSurveyResponseRequest request = new SubmitSurveyResponseRequest(List.of());

            // when
            SurveyResponseDetailResponse response = surveyResponseService.submitResponse(
                    DEFAULT_SURVEY_ID, request, memberAuth);

            // then
            assertThat(response).isNotNull();
        }
    }
}
