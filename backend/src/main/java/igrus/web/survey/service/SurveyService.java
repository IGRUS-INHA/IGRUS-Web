package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.dto.request.CreateSurveyRequest;
import igrus.web.survey.dto.request.UpdateSurveyRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyListResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 설문 서비스.
 * 설문 CRUD 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createSurvey} - 설문 생성 (운영진 이상)</li>
 *   <li>{@link #updateSurvey} - 설문 수정 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #trashSurvey} - 설문 휴지통 이동 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #getSurveyDetail} - 설문 단건 조회 (운영진 이상)</li>
 *   <li>{@link #getSurveyList} - 설문 목록 조회 (운영진 이상)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    /**
     * 설문을 생성합니다. 초기 상태는 DRAFT입니다.
     *
     * @param request           설문 생성 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 생성된 설문 상세 응답
     */
    public SurveyDetailResponse createSurvey(CreateSurveyRequest request, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = Survey.create(
                request.title(),
                request.description(),
                request.accessLevel(),
                request.deadline()
        );

        Survey savedSurvey = surveyRepository.save(survey);

        return SurveyDetailResponse.from(savedSurvey);
    }

    /**
     * 설문을 수정합니다. 모든 상태에서 수정 가능합니다. (구글폼 방식)
     *
     * @param surveyId          설문 ID
     * @param request           설문 수정 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse updateSurvey(Long surveyId, UpdateSurveyRequest request, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        survey.update(
                request.title(),
                request.description(),
                request.accessLevel(),
                request.deadline()
        );

        return SurveyDetailResponse.from(survey);
    }

    /**
     * 설문을 휴지통으로 이동합니다. 모든 상태에서 가능합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void trashSurvey(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.trash();
    }

    /**
     * 설문 단건을 조회합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 상세 응답
     */
    @Transactional(readOnly = true)
    public SurveyDetailResponse getSurveyDetail(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        return SurveyDetailResponse.from(survey);
    }

    /**
     * 설문 목록을 조회합니다.
     *
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 목록 응답
     */
    @Transactional(readOnly = true)
    public List<SurveyListResponse> getSurveyList(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        return surveyRepository.findByDeletedFalseAndTrashedAtIsNull().stream()
                .map(SurveyListResponse::from)
                .toList();
    }

    // === Private helper methods ===

    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new SurveyAccessDeniedException("운영진 이상만 설문을 관리할 수 있습니다");
        }
    }
}
