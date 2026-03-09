package igrus.web.event.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.InvalidEventDateException;
import igrus.web.event.repository.EventRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.OptionSurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.domain.TextSurveyQuestion;
import igrus.web.survey.question.exception.SurveyQuestionValidationException;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventWithSurveyService - 행사+설문 원자적 생성")
class EventWithSurveyServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private EventWithSurveyService eventWithSurveyService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository questionRepository;

    private User operator;
    private User member;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            member = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            return null;
        });
    }

    private EventWithSurveyService.CreateEventWithSurveyRequest createValidRequest() {
        return createValidRequest(false);
    }

    private EventWithSurveyService.CreateEventWithSurveyRequest createValidRequest(boolean allowExternal) {
        Instant now = Instant.now();
        return new EventWithSurveyService.CreateEventWithSurveyRequest(
                "테스트 행사",
                "행사 설명",
                "인하대학교 5호관",
                now.plus(3, ChronoUnit.DAYS),   // eventStartAt
                now.plus(4, ChronoUnit.DAYS),   // eventEndAt
                now.plus(1, ChronoUnit.HOURS),  // registrationStartAt
                now.plus(2, ChronoUnit.DAYS),   // registrationEndAt
                30,
                EventRegistrationType.AUTO_APPROVE,
                null, // attachmentObjectKeys
                allowExternal,
                "테스트 행사 신청 설문",
                null, // surveyDescription
                List.of(
                        new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                SurveyQuestionType.SHORT_ANSWER, "이름을 입력하세요", true, 1, null
                        ),
                        new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                SurveyQuestionType.MULTIPLE_CHOICE, "참여 동기", false, 2,
                                List.of("관심", "친구 추천", "기타")
                        )
                )
        );
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("행사와 설문이 원자적으로 생성된다")
        void createEventWithSurvey_Success() {
            // when
            EventCreateResponse response = eventWithSurveyService.createEventWithSurvey(
                    createValidRequest(), operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 행사");
            assertThat(response.surveyId()).isNotNull();

            // 행사가 DB에 저장되었는지 확인
            assertThat(eventRepository.findById(response.id())).isPresent();

            // 설문이 DB에 저장되었는지 확인
            assertThat(surveyRepository.findById(response.surveyId())).isPresent();

            // 질문 2개가 생성되었는지 확인
            assertThat(questionRepository.findBySurveyIdAndDeletedFalseOrderByDisplayOrderAsc(response.surveyId())).hasSize(2);
        }

        @Test
        @DisplayName("텍스트 질문과 옵션 질문이 각각 올바른 타입으로 생성된다")
        void createEventWithSurvey_QuestionTypes() {
            // when
            EventCreateResponse response = eventWithSurveyService.createEventWithSurvey(
                    createValidRequest(), operator.getId());

            // then - 옵션 lazy loading을 위해 트랜잭션 내에서 검증
            transactionTemplate.execute(status -> {
                var questions = questionRepository.findBySurveyIdAndDeletedFalseOrderByDisplayOrderAsc(response.surveyId());
                assertThat(questions).hasSize(2);

                // 첫 번째 질문: 단답형 (TEXT)
                var textQuestion = questions.stream()
                        .filter(q -> q instanceof TextSurveyQuestion)
                        .findFirst().orElseThrow();
                assertThat(textQuestion.getTitle()).isEqualTo("이름을 입력하세요");
                assertThat(textQuestion.isRequired()).isTrue();
                assertThat(textQuestion.getDisplayOrder()).isEqualTo(1);

                // 두 번째 질문: 객관식 (OPTION) + 선택지 3개
                var optionQuestion = questions.stream()
                        .filter(q -> q instanceof OptionSurveyQuestion)
                        .findFirst().orElseThrow();
                assertThat(optionQuestion.getTitle()).isEqualTo("참여 동기");
                assertThat(optionQuestion.isRequired()).isFalse();
                assertThat(((OptionSurveyQuestion) optionQuestion).getOptions()).hasSize(3);
                return null;
            });
        }

        @Test
        @DisplayName("allowExternal=true이면 설문 accessLevel이 PUBLIC으로 설정된다")
        void createEventWithSurvey_AllowExternal_PublicAccessLevel() {
            // when
            EventCreateResponse response = eventWithSurveyService.createEventWithSurvey(
                    createValidRequest(true), operator.getId());

            // then
            assertThat(response.allowExternal()).isTrue();
            Survey survey = surveyRepository.findById(response.surveyId()).orElseThrow();
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.PUBLIC);
        }

        @Test
        @DisplayName("allowExternal=false이면 설문 accessLevel이 MEMBER로 설정된다")
        void createEventWithSurvey_NoExternal_MemberAccessLevel() {
            // when
            EventCreateResponse response = eventWithSurveyService.createEventWithSurvey(
                    createValidRequest(false), operator.getId());

            // then
            assertThat(response.allowExternal()).isFalse();
            Survey survey = surveyRepository.findById(response.surveyId()).orElseThrow();
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
        }

        @Test
        @DisplayName("ADMIN 역할도 행사+설문을 생성할 수 있다")
        void createEventWithSurvey_AdminCanCreate() {
            // given
            User admin = transactionTemplate.execute(status ->
                    createAndSaveUser("20230003", "admin@inha.edu", UserRole.ADMIN));

            // when
            EventCreateResponse response = eventWithSurveyService.createEventWithSurvey(
                    createValidRequest(), admin.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
        }
    }

    @Nested
    @DisplayName("권한 검증 실패")
    class PermissionDenied {

        @Test
        @DisplayName("MEMBER 역할은 행사를 생성할 수 없다")
        void createEventWithSurvey_MemberDenied() {
            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(createValidRequest(), member.getId()))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        @Test
        @DisplayName("권한 검증 실패 시 설문도 생성되지 않는다 (롤백)")
        void createEventWithSurvey_MemberDenied_NoSurveyCreated() {
            long surveyCountBefore = surveyRepository.count();

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(createValidRequest(), member.getId()))
                    .isInstanceOf(EventAccessDeniedException.class);

            assertThat(surveyRepository.count()).isEqualTo(surveyCountBefore);
        }
    }

    @Nested
    @DisplayName("날짜 유효성 검증 실패")
    class DateValidationFailure {

        @Test
        @DisplayName("신청 시작일이 현재 이전이면 실패한다")
        void createEventWithSurvey_RegistrationStartInPast() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.HOURS), // 과거
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                            SurveyQuestionType.SHORT_ANSWER, "질문", true, 1, null))
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        @Test
        @DisplayName("날짜 검증 실패 시 설문도 생성되지 않는다 (롤백)")
        void createEventWithSurvey_DateFail_NoSurveyCreated() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.HOURS), // 과거
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                            SurveyQuestionType.SHORT_ANSWER, "질문", true, 1, null))
            );

            long surveyCountBefore = surveyRepository.count();

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(InvalidEventDateException.class);

            assertThat(surveyRepository.count()).isEqualTo(surveyCountBefore);
        }

        @Test
        @DisplayName("신청 마감일이 신청 시작일 이전이면 실패한다")
        void createEventWithSurvey_RegEndBeforeRegStart() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(2, ChronoUnit.DAYS),    // regStart
                    now.plus(1, ChronoUnit.DAYS),    // regEnd < regStart
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                            SurveyQuestionType.SHORT_ANSWER, "질문", true, 1, null))
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(InvalidEventDateException.class);
        }
    }

    @Nested
    @DisplayName("질문 유효성 검증 실패")
    class QuestionValidationFailure {

        @Test
        @DisplayName("질문이 없으면 실패한다")
        void createEventWithSurvey_NoQuestions() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of() // 빈 질문 목록
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }

        @Test
        @DisplayName("질문이 50개를 초과하면 실패한다")
        void createEventWithSurvey_TooManyQuestions() {
            Instant now = Instant.now();
            List<EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData> questions =
                    IntStream.rangeClosed(1, 51)
                            .mapToObj(i -> new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                    SurveyQuestionType.SHORT_ANSWER, "질문 " + i, false, i, null))
                            .toList();

            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    questions
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }

        @Test
        @DisplayName("질문 50개 초과 시 설문도 생성되지 않는다 (롤백)")
        void createEventWithSurvey_TooManyQuestions_NoSurveyCreated() {
            Instant now = Instant.now();
            List<EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData> questions =
                    IntStream.rangeClosed(1, 51)
                            .mapToObj(i -> new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                    SurveyQuestionType.SHORT_ANSWER, "질문 " + i, false, i, null))
                            .toList();

            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    questions
            );

            long surveyCountBefore = surveyRepository.count();

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);

            assertThat(surveyRepository.count()).isEqualTo(surveyCountBefore);
        }

        @Test
        @DisplayName("옵션 타입 질문에 선택지가 없으면 실패한다")
        void createEventWithSurvey_OptionQuestionWithoutOptions() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                            SurveyQuestionType.MULTIPLE_CHOICE, "선택 질문", true, 1,
                            List.of() // 빈 옵션
                    ))
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }

        @Test
        @DisplayName("옵션 타입 질문에 공백 선택지만 있으면 실패한다")
        void createEventWithSurvey_OptionQuestionWithBlankOptions() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                            SurveyQuestionType.CHECKBOX, "선택 질문", true, 1,
                            List.of("  ", "", " ")  // 공백만 있는 옵션
                    ))
            );

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }

        @Test
        @DisplayName("옵션 질문 실패 시 이미 생성된 설문과 질문도 롤백된다")
        void createEventWithSurvey_OptionFail_FullRollback() {
            Instant now = Instant.now();
            var request = new EventWithSurveyService.CreateEventWithSurveyRequest(
                    "테스트 행사", "설명", "장소",
                    now.plus(3, ChronoUnit.DAYS),
                    now.plus(4, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.HOURS),
                    now.plus(2, ChronoUnit.DAYS),
                    30, EventRegistrationType.AUTO_APPROVE,
                    null, false,
                    "설문 제목", null,
                    List.of(
                            // 첫 번째 질문: 정상 텍스트
                            new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                    SurveyQuestionType.SHORT_ANSWER, "정상 질문", true, 1, null),
                            // 두 번째 질문: 옵션 없는 객관식 → 실패
                            new EventWithSurveyService.CreateEventWithSurveyRequest.QuestionData(
                                    SurveyQuestionType.MULTIPLE_CHOICE, "실패 질문", true, 2,
                                    List.of()) // 빈 옵션
                    )
            );

            long surveyCountBefore = surveyRepository.count();
            long questionCountBefore = questionRepository.count();
            long eventCountBefore = eventRepository.count();

            assertThatThrownBy(() ->
                    eventWithSurveyService.createEventWithSurvey(request, operator.getId()))
                    .isInstanceOf(SurveyQuestionValidationException.class);

            // 모든 것이 롤백됨
            assertThat(surveyRepository.count()).isEqualTo(surveyCountBefore);
            assertThat(questionRepository.count()).isEqualTo(questionCountBefore);
            assertThat(eventRepository.count()).isEqualTo(eventCountBefore);
        }
    }
}
