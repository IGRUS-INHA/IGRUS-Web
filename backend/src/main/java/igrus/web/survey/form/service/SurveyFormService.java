package igrus.web.survey.form.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.exception.SurveyAnonymousNotAllowedException;
import igrus.web.survey.response.exception.SurveyNotAcceptingResponsesException;
import igrus.web.survey.response.exception.SurveyResponseAccessDeniedException;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 설문 양식 조회 서비스.
 * 응답자가 설문 양식(질문·선택지·행)을 조회하기 위한 비즈니스 로직을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyFormService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    /**
     * 회원용 설문 양식을 조회합니다.
     * PUBLISHED + OPEN 상태이며 accessLevel 조건을 충족해야 합니다.
     *
     * @param surveyId 설문 ID
     * @param auth     인증된 사용자 정보
     * @return 설문 상세 (양식 포함)
     */
    @Transactional
    public SurveyDetailResponse getSurveyForm(Long surveyId, AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        validateAccessLevel(survey.getAccessLevel(), user);

        return SurveyDetailResponse.from(survey);
    }

    /**
     * 비회원용 설문 양식을 조회합니다.
     * PUBLIC + PUBLISHED + OPEN 상태에서만 조회 가능합니다.
     *
     * @param surveyId 설문 ID
     * @return 설문 상세 (양식 포함)
     */
    @Transactional
    public SurveyDetailResponse getAnonymousSurveyForm(Long surveyId) {
        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (survey.getAccessLevel() != SurveyAccessLevel.PUBLIC) {
            throw new SurveyAnonymousNotAllowedException();
        }

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        return SurveyDetailResponse.from(survey);
    }

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
