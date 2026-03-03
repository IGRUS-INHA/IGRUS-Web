package igrus.web.survey.response.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.exception.*;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설문 응답 서비스.
 * 설문 응답 제출, 수정, 조회 비즈니스 로직을 처리합니다.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyResponseService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final UserRepository userRepository;
    private final SurveyAnswerValidator answerValidator;
    private final SurveyAnswerFactory surveyAnswerFactory;

    /**
     * 회원 응답을 제출합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 제출 요청
     * @param auth     인증된 사용자 정보
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitResponse(Long surveyId,
                                                        SubmitSurveyResponseRequest request,
                                                        AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        validateAccessLevel(survey.getAccessLevel(), user);

        if (surveyResponseRepository.existsBySurveyIdAndUserId(surveyId, user.getId())) {
            throw new SurveyResponseDuplicateException();
        }

        answerValidator.validate(survey, request.answers());

        SurveyResponse response = SurveyResponse.create(survey, user);
        surveyAnswerFactory.createAnswers(response, survey, request.answers());

        SurveyResponse savedResponse;
        try {
            savedResponse = surveyResponseRepository.save(response);
        } catch (DataIntegrityViolationException e) {
            throw new SurveyResponseDuplicateException();
        }

        log.info("설문 응답 제출 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 비회원(익명) 응답을 제출합니다. PUBLIC 설문에서만 가능합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 제출 요청
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitAnonymousResponse(Long surveyId,
                                                                 SubmitSurveyResponseRequest request) {
        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        if (survey.getAccessLevel() != SurveyAccessLevel.PUBLIC) {
            throw new SurveyAnonymousNotAllowedException();
        }

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        answerValidator.validate(survey, request.answers());

        SurveyResponse response = SurveyResponse.createAnonymous(survey);
        surveyAnswerFactory.createAnswers(response, survey, request.answers());
        SurveyResponse savedResponse = surveyResponseRepository.save(response);

        log.info("비회원 설문 응답 제출 - surveyId: {}", surveyId);
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 본인 응답을 수정합니다. OPEN 상태 + 현재 accessLevel 충족 시에만 가능합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 수정 요청
     * @param auth     인증된 사용자 정보
     * @return 수정된 응답 상세
     */
    public SurveyResponseDetailResponse updateMyResponse(Long surveyId,
                                                          SubmitSurveyResponseRequest request,
                                                          AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        validateAccessLevel(survey.getAccessLevel(), user);

        SurveyResponse response = surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        answerValidator.validate(survey, request.answers());

        response.getAnswers().clear();
        surveyAnswerFactory.createAnswers(response, survey, request.answers());
        SurveyResponse savedResponse = surveyResponseRepository.save(response);

        log.info("설문 응답 수정 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 본인 응답을 조회합니다.
     *
     * @param surveyId 설문 ID
     * @param auth     인증된 사용자 정보
     * @return 본인 응답 상세
     */
    @Transactional(readOnly = true)
    public SurveyResponseDetailResponse getMyResponse(Long surveyId, AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        SurveyResponse response = surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        log.info("본인 응답 조회 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(response);
    }

    // ==================== Private helpers ====================

    /**
     * 설문의 accessLevel과 사용자 역할을 비교하여 응답 권한을 검증합니다.
     */
    private void validateAccessLevel(SurveyAccessLevel accessLevel, User user) {
        boolean allowed = switch (accessLevel) {
            case PUBLIC, ASSOCIATE -> true;
            case MEMBER -> !user.isAssociate();
            case OPERATOR -> user.isOperatorOrAbove();
        };
        if (!allowed) {
            throw new SurveyResponseAccessDeniedException();
        }
    }
}
