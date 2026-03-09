package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventAttachment;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.EventAttachmentValidationException;
import igrus.web.event.exception.EventErrorCode;
import igrus.web.event.repository.EventAttachmentRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.exception.FileOwnershipMismatchException;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.OptionSurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.domain.TextSurveyQuestion;
import igrus.web.survey.question.exception.SurveyQuestionValidationException;
import igrus.web.survey.question.repository.SurveyQuestionOptionRepository;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 행사 + 설문 원자적 생성 서비스.
 * 행사와 설문(질문+선택지)을 하나의 트랜잭션으로 생성하여 고아 데이터를 방지합니다.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class EventWithSurveyService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyQuestionOptionRepository optionRepository;
    private final EventAttachmentRepository eventAttachmentRepository;
    private final FileMetadataRepository fileMetadataRepository;

    private static final int MAX_QUESTIONS = 50;

    /**
     * 행사와 설문을 원자적으로 생성합니다.
     *
     * @param request 행사+설문 생성 요청
     * @param userId  생성자(운영진) ID
     * @return 생성된 행사 응답 DTO
     */
    public EventCreateResponse createEventWithSurvey(CreateEventWithSurveyRequest request, Long userId) {
        // 1. 사용자 조회 및 권한 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        validateOperatorPermission(user);

        // 2. 행사 날짜 유효성 검증
        EventDateValidator.validate(request.eventStartAt(), request.eventEndAt(),
                request.registrationStartAt(), request.registrationEndAt());

        // 3. 설문 생성
        SurveyAccessLevel accessLevel = Boolean.TRUE.equals(request.allowExternal())
                ? SurveyAccessLevel.PUBLIC
                : SurveyAccessLevel.MEMBER;

        Survey survey = Survey.create(
                request.surveyTitle(),
                request.surveyDescription(),
                accessLevel,
                null // deadline은 행사 연동 설문에서는 사용하지 않음
        );
        Survey savedSurvey = surveyRepository.save(survey);

        // 4. 질문 생성
        validateQuestions(request.questions());
        for (CreateEventWithSurveyRequest.QuestionData questionData : request.questions()) {
            SurveyQuestion question = createQuestionByType(
                    savedSurvey,
                    questionData.questionType(),
                    questionData.title(),
                    null, // description
                    questionData.required(),
                    questionData.displayOrder()
            );
            savedSurvey.getQuestions().add(question);
            SurveyQuestion savedQuestion = questionRepository.save(question);

            // 5. 옵션 타입 질문의 선택지 생성
            if (questionData.options() != null && savedQuestion instanceof OptionSurveyQuestion optionQuestion) {
                List<String> options = questionData.options().stream()
                        .filter(o -> o != null && !o.trim().isEmpty())
                        .toList();
                if (options.isEmpty()) {
                    throw new SurveyQuestionValidationException(
                            "선택형 질문에는 최소 1개의 선택지가 필요합니다: " + questionData.title());
                }
                for (int i = 0; i < options.size(); i++) {
                    SurveyQuestionOption option = SurveyQuestionOption.create(
                            optionQuestion, options.get(i), i + 1);
                    optionQuestion.getOptions().add(option);
                    optionRepository.save(option);
                }
            }
        }

        // 6. 행사 생성
        Event event = Event.create(
                user,
                request.title(),
                request.description(),
                request.location(),
                request.eventStartAt(),
                request.eventEndAt(),
                request.registrationStartAt(),
                request.registrationEndAt(),
                request.capacity(),
                request.registrationType(),
                savedSurvey,
                request.allowExternal()
        );
        Event savedEvent = eventRepository.save(event);

        // 7. 첨부파일 처리
        List<String> attachmentObjectKeys = request.attachmentObjectKeys() == null
                ? List.of() : request.attachmentObjectKeys();
        if (!attachmentObjectKeys.isEmpty()) {
            validateAndCreateAttachments(savedEvent, attachmentObjectKeys, userId);
            log.info("행사+설문 원자적 생성: eventId={}, attachmentCount={}",
                    savedEvent.getId(), attachmentObjectKeys.size());
        }

        log.info("행사+설문 원자적 생성 완료 - userId: {}, eventId: {}, surveyId: {}",
                userId, savedEvent.getId(), savedSurvey.getId());

        return EventCreateResponse.from(savedEvent);
    }

    // === Private helper methods ===

    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new EventAccessDeniedException("행사 생성은 운영진 이상만 가능합니다");
        }
    }

    private void validateQuestions(List<CreateEventWithSurveyRequest.QuestionData> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new SurveyQuestionValidationException("최소 1개의 질문이 필요합니다");
        }
        if (questions.size() > MAX_QUESTIONS) {
            throw new SurveyQuestionValidationException("질문은 최대 " + MAX_QUESTIONS + "개까지 가능합니다");
        }
    }

    private SurveyQuestion createQuestionByType(Survey survey, SurveyQuestionType questionType,
                                                 String title, String description,
                                                 boolean required, int displayOrder) {
        if (questionType == null) {
            throw new SurveyQuestionValidationException("질문 유형은 필수입니다");
        }
        return switch (questionType.getCategory()) {
            case "TEXT" -> TextSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            case "OPTION" -> OptionSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            default -> throw new SurveyQuestionValidationException("지원하지 않는 질문 카테고리: " + questionType.getCategory());
        };
    }

    private void validateAndCreateAttachments(Event event, List<String> objectKeys, Long userId) {
        validateNoDuplicateObjectKeys(objectKeys);
        List<FileMetadata> files = validateAndFetchFilesByObjectKeys(objectKeys, userId);

        List<EventAttachment> attachments = files.stream()
                .map(file -> EventAttachment.create(event, file))
                .toList();

        eventAttachmentRepository.saveAll(attachments);
    }

    private void validateNoDuplicateObjectKeys(List<String> objectKeys) {
        Set<String> uniqueKeys = new HashSet<>(objectKeys);
        if (uniqueKeys.size() != objectKeys.size()) {
            throw new EventAttachmentValidationException(EventErrorCode.EVENT_ATTACHMENT_DUPLICATE_FILE);
        }
    }

    private List<FileMetadata> validateAndFetchFilesByObjectKeys(List<String> objectKeys, Long userId) {
        List<FileMetadata> files = new ArrayList<>();
        for (String objectKey : objectKeys) {
            FileMetadata file = fileMetadataRepository.findByObjectKeyAndDeletedFalse(objectKey)
                    .orElseThrow(() -> new EventAttachmentValidationException(
                            EventErrorCode.EVENT_ATTACHMENT_FILE_NOT_FOUND,
                            "파일을 찾을 수 없습니다: objectKey=" + objectKey));

            if (file.getStatus() != FileUploadStatus.COMPLETED) {
                throw new EventAttachmentValidationException(
                        EventErrorCode.EVENT_ATTACHMENT_FILE_NOT_COMPLETED,
                        "업로드가 완료되지 않은 파일입니다: objectKey=" + objectKey);
            }

            if (!file.getUploaderUserId().equals(userId)) {
                throw new FileOwnershipMismatchException();
            }

            files.add(file);
        }
        return files;
    }

    /**
     * 행사+설문 원자적 생성 요청 DTO.
     */
    public record CreateEventWithSurveyRequest(
            String title,
            String description,
            String location,
            Instant eventStartAt,
            Instant eventEndAt,
            Instant registrationStartAt,
            Instant registrationEndAt,
            Integer capacity,
            EventRegistrationType registrationType,
            List<String> attachmentObjectKeys,
            Boolean allowExternal,
            String surveyTitle,
            String surveyDescription,
            List<QuestionData> questions
    ) {
        public record QuestionData(
                SurveyQuestionType questionType,
                String title,
                boolean required,
                int displayOrder,
                List<String> options
        ) {}
    }
}
