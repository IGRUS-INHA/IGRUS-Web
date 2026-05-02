package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.dto.request.CreateSurveyRequest;
import igrus.web.survey.dto.request.UpdateSurveyRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyListResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.exception.SurveyPublishValidationException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 설문 서비스.
 * 설문 CRUD 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createSurvey} - 설문 생성 (운영진 이상)</li>
 *   <li>{@link #updateSurvey} - 설문 수정 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #trashSurvey} - 설문 휴지통 이동 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #restoreSurvey} - 설문 휴지통 복원 (운영진 이상)</li>
 *   <li>{@link #permanentDeleteSurvey} - 설문 영구 삭제 (운영진 이상, 휴지통 상태만)</li>
 *   <li>{@link #publishSurvey} - 설문 공개 (운영진 이상, UNPUBLISHED → PUBLISHED)</li>
 *   <li>{@link #unpublishSurvey} - 설문 비공개 (운영진 이상, PUBLISHED → UNPUBLISHED)</li>
 *   <li>{@link #openResponse} - 응답 수집 시작 (운영진 이상, PUBLISHED 상태에서)</li>
 *   <li>{@link #closeResponse} - 응답 수집 마감 (운영진 이상, OPEN → CLOSED)</li>
 *   <li>{@link #publishAndOpen} - 공개+응답 시작 (운영진 이상, 편의 메서드)</li>
 *   <li>{@link #getSurveyDetail} - 설문 단건 조회 (운영진 이상)</li>
 *   <li>{@link #getSurveyList} - 설문 목록 조회 (운영진 이상)</li>
 *   <li>{@link #getTrashedSurveyList} - 휴지통 목록 조회 (운영진 이상)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final UserRepository userRepository;

    /**
     * 설문을 생성합니다. 초기 상태는 UNPUBLISHED입니다.
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

        return SurveyDetailResponse.from(savedSurvey, 0);
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
        survey.updateStatusIfNeeded(Instant.now());

        survey.update(
                request.title(),
                request.description(),
                request.accessLevel(),
                request.deadline()
        );

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
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
        survey.updateStatusIfNeeded(Instant.now());
        survey.trash();
    }

    /**
     * 휴지통에서 설문을 복원합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void restoreSurvey(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.restoreFromTrash();
    }

    /**
     * 설문을 영구 삭제합니다. 휴지통에 있는 설문만 가능합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void permanentDeleteSurvey(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNotNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.permanentDelete(authenticatedUser.userId());
    }

    /**
     * 설문 단건을 조회합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 상세 응답
     */
    public SurveyDetailResponse getSurveyDetail(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());
        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    /**
     * 설문 목록을 조회합니다.
     *
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 목록 응답
     */
    public List<SurveyListResponse> getSurveyList(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Instant now = Instant.now();
        List<Survey> surveys = surveyRepository.findByDeletedFalseAndTrashedAtIsNull();
        surveys.forEach(s -> s.updateStatusIfNeeded(now));

        Map<Long, Integer> responseCountMap = getResponseCountMap(
                surveys.stream().map(Survey::getId).toList());

        return surveys.stream()
                .map(s -> SurveyListResponse.from(s, responseCountMap.getOrDefault(s.getId(), 0)))
                .toList();
    }

    /**
     * 휴지통에 있는 설문 목록을 조회합니다.
     *
     * @param authenticatedUser 인증된 사용자 정보
     * @return 휴지통 설문 목록 응답
     */
    @Transactional(readOnly = true)
    public List<SurveyListResponse> getTrashedSurveyList(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        List<Survey> trashedSurveys = surveyRepository.findByDeletedFalseAndTrashedAtIsNotNull();

        Map<Long, Integer> responseCountMap = getResponseCountMap(
                trashedSurveys.stream().map(Survey::getId).toList());

        return trashedSurveys.stream()
                .map(s -> SurveyListResponse.from(s, responseCountMap.getOrDefault(s.getId(), 0)))
                .toList();
    }

    // === 상태 전이 메서드 ===

    /**
     * 설문을 공개합니다. UNPUBLISHED → PUBLISHED
     * 발행 전 질문 구조 유효성을 검증합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse publishSurvey(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        validatePublishPreConditions(survey);
        survey.publish();

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    /**
     * 설문을 비공개로 전환합니다. PUBLISHED → UNPUBLISHED
     * OPEN 상태이면 자동으로 응답을 마감합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse unpublishSurvey(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        survey.unpublish();

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    /**
     * 응답 수집을 시작(또는 재개)합니다. NOT_STARTED/CLOSED → OPEN
     * 사전조건: PUBLISHED 상태, 마감일 미경과
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse openResponse(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        survey.openResponse();

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    /**
     * 응답 수집을 마감합니다. OPEN → CLOSED
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse closeResponse(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        survey.closeResponse();

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    /**
     * 설문을 공개하고 동시에 응답 수집을 시작합니다.
     * UNPUBLISHED 상태에서 한 번에 공개 + 응답 시작 (편의 메서드)
     * 발행 전 질문 구조 유효성을 검증합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 설문 상세 응답
     */
    public SurveyDetailResponse publishAndOpen(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        validatePublishPreConditions(survey);
        survey.publishAndOpen();

        return SurveyDetailResponse.from(survey, getResponseCount(surveyId));
    }

    // === Private helper methods ===

    /**
     * 단건 설문의 유효 응답 수를 조회합니다.
     *
     * @param surveyId 설문 ID
     * @return 유효 응답 수 (int)
     */
    private int getResponseCount(Long surveyId) {
        return (int) surveyResponseRepository.countBySurveyIdAndDeletedFalse(surveyId);
    }

    /**
     * 여러 설문의 유효 응답 수를 배치로 조회합니다. N+1 방지용.
     *
     * @param surveyIds 설문 ID 목록
     * @return 설문 ID → 응답 수 맵
     */
    private Map<Long, Integer> getResponseCountMap(List<Long> surveyIds) {
        if (surveyIds.isEmpty()) {
            return Map.of();
        }
        return surveyResponseRepository.countBySurveyIdInAndDeletedFalse(surveyIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new SurveyAccessDeniedException("운영진 이상만 설문을 관리할 수 있습니다");
        }
    }

    /**
     * 설문 발행 전 질문 구조 유효성을 검증합니다.
     * - 활성 질문 수 1~50개
     * - 질문 유형별 필수 구성요소
     * - 그리드 질문의 최소 구성 (행 + 선택지)
     */
    private void validatePublishPreConditions(Survey survey) {
        List<SurveyQuestion> activeQuestions = survey.getQuestions().stream()
                .filter(q -> !q.isArchived())
                .toList();

        if (activeQuestions.isEmpty()) {
            throw new SurveyPublishValidationException("설문에 최소 1개 이상의 질문이 필요합니다.");
        }
        if (activeQuestions.size() > 50) {
            throw new SurveyPublishValidationException("설문 질문은 최대 50개까지 가능합니다.");
        }

        for (SurveyQuestion question : activeQuestions) {
            validateQuestionStructure(question);
        }
    }

    /**
     * 질문 유형별 필수 구성요소를 검증합니다.
     */
    private void validateQuestionStructure(SurveyQuestion question) {
        if (question instanceof LinearScaleSurveyQuestion scaleQ) {
            if (scaleQ.getScaleMin() == null || scaleQ.getScaleMax() == null) {
                throw new SurveyPublishValidationException(
                        String.format("질문 '%s'에 배율 범위(최솟값, 최댓값)가 설정되어야 합니다.", question.getTitle()));
            }
            if (scaleQ.getScaleMin() >= scaleQ.getScaleMax()) {
                throw new SurveyPublishValidationException(
                        String.format("질문 '%s'의 최솟값은 최댓값보다 작아야 합니다.", question.getTitle()));
            }
        } else if (question instanceof GridSurveyQuestion gridQ) {
            long activeOptionCount = gridQ.getOptions().stream().filter(o -> !o.isArchived()).count();
            long activeRowCount = gridQ.getRows().stream().filter(r -> !r.isArchived()).count();
            if (activeRowCount == 0 || activeOptionCount == 0) {
                throw new SurveyPublishValidationException(
                        String.format("질문 '%s'에 행과 선택지가 각각 1개 이상 필요합니다.", question.getTitle()));
            }
        } else if (question instanceof OptionSurveyQuestion optionQ) {
            long activeOptionCount = optionQ.getOptions().stream().filter(o -> !o.isArchived()).count();
            if (activeOptionCount == 0) {
                throw new SurveyPublishValidationException(
                        String.format("질문 '%s'에 선택지가 1개 이상 필요합니다.", question.getTitle()));
            }
        }
        // TextSurveyQuestion: 필수 구성요소 없음
    }
}
