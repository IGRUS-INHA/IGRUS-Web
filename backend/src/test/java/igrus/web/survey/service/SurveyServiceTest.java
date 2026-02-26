package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.dto.request.CreateSurveyRequest;
import igrus.web.survey.dto.request.UpdateSurveyRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyListResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.exception.SurveyPublishValidationException;
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
 * SurveyService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyService 단위 테스트")
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurveyService surveyService;

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

    // ==================== 설문 생성 ====================

    @Nested
    @DisplayName("설문 생성")
    class CreateSurvey {

        @DisplayName("운영진 설문 생성 성공")
        @Test
        void createSurvey_ByOperator_Success() {
            // given
            CreateSurveyRequest request = new CreateSurveyRequest(
                    DEFAULT_SURVEY_TITLE, DEFAULT_SURVEY_DESCRIPTION,
                    SurveyAccessLevel.PUBLIC, null);

            Survey savedSurvey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.save(any(Survey.class))).willReturn(savedSurvey);

            // when
            SurveyDetailResponse response = surveyService.createSurvey(request, operatorAuth);

            // then
            assertThat(response.title()).isEqualTo(DEFAULT_SURVEY_TITLE);
            assertThat(response.description()).isEqualTo(DEFAULT_SURVEY_DESCRIPTION);
            assertThat(response.visibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(response.responseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
            verify(surveyRepository).save(any(Survey.class));
        }

        @DisplayName("일반 회원(MEMBER) 생성 시 SurveyAccessDeniedException")
        @Test
        void createSurvey_ByMember_ThrowsAccessDenied() {
            // given
            CreateSurveyRequest request = new CreateSurveyRequest(
                    DEFAULT_SURVEY_TITLE, DEFAULT_SURVEY_DESCRIPTION,
                    SurveyAccessLevel.PUBLIC, null);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.createSurvey(request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }

        @DisplayName("존재하지 않는 사용자 ID면 UserNotFoundException")
        @Test
        void createSurvey_UserNotFound_ThrowsException() {
            // given
            CreateSurveyRequest request = new CreateSurveyRequest(
                    DEFAULT_SURVEY_TITLE, DEFAULT_SURVEY_DESCRIPTION,
                    SurveyAccessLevel.PUBLIC, null);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.createSurvey(request, operatorAuth))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== 설문 수정 ====================

    @Nested
    @DisplayName("설문 수정")
    class UpdateSurvey {

        @DisplayName("운영진 설문 수정 성공")
        @Test
        void updateSurvey_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            UpdateSurveyRequest request = new UpdateSurveyRequest(
                    "수정 제목", "수정 설명", SurveyAccessLevel.MEMBER, null);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.updateSurvey(DEFAULT_SURVEY_ID, request, operatorAuth);

            // then
            assertThat(response.title()).isEqualTo("수정 제목");
            assertThat(response.description()).isEqualTo("수정 설명");
            assertThat(response.accessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
        }

        @DisplayName("설문 미존재 시 SurveyNotFoundException")
        @Test
        void updateSurvey_SurveyNotFound_ThrowsException() {
            // given
            UpdateSurveyRequest request = new UpdateSurveyRequest(
                    "수정 제목", "수정 설명", SurveyAccessLevel.PUBLIC, null);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.updateSurvey(DEFAULT_SURVEY_ID, request, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 수정 시 SurveyAccessDeniedException")
        @Test
        void updateSurvey_ByMember_ThrowsAccessDenied() {
            // given
            UpdateSurveyRequest request = new UpdateSurveyRequest(
                    "수정 제목", "수정 설명", SurveyAccessLevel.PUBLIC, null);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.updateSurvey(DEFAULT_SURVEY_ID, request, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 휴지통 이동 ====================

    @Nested
    @DisplayName("휴지통 이동")
    class TrashSurvey {

        @DisplayName("운영진 휴지통 이동 성공")
        @Test
        void trashSurvey_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            surveyService.trashSurvey(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(survey.isTrashed()).isTrue();
        }

        @DisplayName("설문 미존재 시 SurveyNotFoundException")
        @Test
        void trashSurvey_SurveyNotFound_ThrowsException() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.trashSurvey(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void trashSurvey_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.trashSurvey(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 휴지통 복원 ====================

    @Nested
    @DisplayName("휴지통 복원")
    class RestoreSurvey {

        @DisplayName("운영진 복원 성공")
        @Test
        void restoreSurvey_ByOperator_Success() {
            // given
            Survey survey = withId(createTrashedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            surveyService.restoreSurvey(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(survey.isTrashed()).isFalse();
        }

        @DisplayName("설문 미존재(또는 휴지통에 없음) 시 SurveyNotFoundException")
        @Test
        void restoreSurvey_SurveyNotFound_ThrowsException() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.restoreSurvey(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void restoreSurvey_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.restoreSurvey(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 영구 삭제 ====================

    @Nested
    @DisplayName("영구 삭제")
    class PermanentDeleteSurvey {

        @DisplayName("운영진 영구 삭제 성공")
        @Test
        void permanentDeleteSurvey_ByOperator_Success() {
            // given
            Survey survey = withId(createTrashedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            surveyService.permanentDeleteSurvey(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(survey.isDeleted()).isTrue();
        }

        @DisplayName("설문 미존재(또는 휴지통에 없음) 시 SurveyNotFoundException")
        @Test
        void permanentDeleteSurvey_SurveyNotFound_ThrowsException() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.permanentDeleteSurvey(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void permanentDeleteSurvey_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.permanentDeleteSurvey(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 설문 공개 ====================

    @Nested
    @DisplayName("설문 공개")
    class PublishSurvey {

        @DisplayName("운영진 공개 성공 (질문 1개 이상 존재)")
        @Test
        void publishSurvey_ByOperator_WithQuestions_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            survey.getQuestions().add(question);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.publishSurvey(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.visibility()).isEqualTo(SurveyVisibility.PUBLISHED);
        }

        @DisplayName("질문 없을 때 SurveyPublishValidationException")
        @Test
        void publishSurvey_NoQuestions_ThrowsValidationException() {
            // given
            Survey survey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when & then
            assertThatThrownBy(() -> surveyService.publishSurvey(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyPublishValidationException.class);
        }

        @DisplayName("모든 질문이 soft delete된 경우 SurveyPublishValidationException")
        @Test
        void publishSurvey_AllQuestionsDeleted_ThrowsValidationException() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion deletedQuestion = createShortAnswerQuestion(survey, 1);
            deletedQuestion.delete(operatorAuth.userId());
            survey.getQuestions().add(deletedQuestion);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when & then
            assertThatThrownBy(() -> surveyService.publishSurvey(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyPublishValidationException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void publishSurvey_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.publishSurvey(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 설문 비공개 ====================

    @Nested
    @DisplayName("설문 비공개")
    class UnpublishSurvey {

        @DisplayName("운영진 비공개 성공")
        @Test
        void unpublishSurvey_ByOperator_Success() {
            // given
            Survey survey = withId(createPublishedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.unpublishSurvey(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.visibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void unpublishSurvey_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.unpublishSurvey(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 응답 수집 시작 ====================

    @Nested
    @DisplayName("응답 수집 시작")
    class OpenResponse {

        @DisplayName("운영진 응답 시작 성공")
        @Test
        void openResponse_ByOperator_Success() {
            // given
            Survey survey = withId(createPublishedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.openResponse(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.responseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void openResponse_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.openResponse(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 응답 수집 마감 ====================

    @Nested
    @DisplayName("응답 수집 마감")
    class CloseResponse {

        @DisplayName("운영진 응답 마감 성공")
        @Test
        void closeResponse_ByOperator_Success() {
            // given
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.closeResponse(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.responseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void closeResponse_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.closeResponse(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 공개+응답 시작 ====================

    @Nested
    @DisplayName("공개+응답 시작")
    class PublishAndOpen {

        @DisplayName("운영진 공개+응답 시작 성공")
        @Test
        void publishAndOpen_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            survey.getQuestions().add(question);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.publishAndOpen(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.visibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(response.responseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("질문 없을 때 SurveyPublishValidationException")
        @Test
        void publishAndOpen_NoQuestions_ThrowsValidationException() {
            // given
            Survey survey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when & then
            assertThatThrownBy(() -> surveyService.publishAndOpen(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyPublishValidationException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void publishAndOpen_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.publishAndOpen(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 설문 단건 조회 ====================

    @Nested
    @DisplayName("설문 단건 조회")
    class GetSurveyDetail {

        @DisplayName("운영진 단건 조회 성공")
        @Test
        void getSurveyDetail_ByOperator_Success() {
            // given
            Survey survey = createSurveyWithId();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(survey));

            // when
            SurveyDetailResponse response = surveyService.getSurveyDetail(DEFAULT_SURVEY_ID, operatorAuth);

            // then
            assertThat(response.id()).isEqualTo(DEFAULT_SURVEY_ID);
            assertThat(response.title()).isEqualTo(DEFAULT_SURVEY_TITLE);
        }

        @DisplayName("설문 미존재 시 SurveyNotFoundException")
        @Test
        void getSurveyDetail_SurveyNotFound_ThrowsException() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyService.getSurveyDetail(DEFAULT_SURVEY_ID, operatorAuth))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getSurveyDetail_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.getSurveyDetail(DEFAULT_SURVEY_ID, memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 설문 목록 조회 ====================

    @Nested
    @DisplayName("설문 목록 조회")
    class GetSurveyList {

        @DisplayName("운영진 목록 조회 성공 - 빈 목록")
        @Test
        void getSurveyList_ByOperator_EmptyList_Success() {
            // given
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByDeletedFalseAndTrashedAtIsNull()).willReturn(List.of());

            // when
            List<SurveyListResponse> result = surveyService.getSurveyList(operatorAuth);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("운영진 목록 조회 성공 - 복수 설문")
        @Test
        void getSurveyList_ByOperator_MultipleSurveys_Success() {
            // given
            Survey survey1 = createSurveyWithId(10L);
            Survey survey2 = createSurveyWithId(11L);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByDeletedFalseAndTrashedAtIsNull()).willReturn(List.of(survey1, survey2));

            // when
            List<SurveyListResponse> result = surveyService.getSurveyList(operatorAuth);

            // then
            assertThat(result).hasSize(2);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getSurveyList_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.getSurveyList(memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }

    // ==================== 휴지통 목록 조회 ====================

    @Nested
    @DisplayName("휴지통 목록 조회")
    class GetTrashedSurveyList {

        @DisplayName("운영진 휴지통 목록 조회 성공")
        @Test
        void getTrashedSurveyList_ByOperator_Success() {
            // given
            Survey trashedSurvey = withId(createTrashedSurvey(), DEFAULT_SURVEY_ID);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(surveyRepository.findByDeletedFalseAndTrashedAtIsNotNull()).willReturn(List.of(trashedSurvey));

            // when
            List<SurveyListResponse> result = surveyService.getTrashedSurveyList(operatorAuth);

            // then
            assertThat(result).hasSize(1);
        }

        @DisplayName("일반 회원 시 SurveyAccessDeniedException")
        @Test
        void getTrashedSurveyList_ByMember_ThrowsAccessDenied() {
            // given
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> surveyService.getTrashedSurveyList(memberAuth))
                    .isInstanceOf(SurveyAccessDeniedException.class);
        }
    }
}
