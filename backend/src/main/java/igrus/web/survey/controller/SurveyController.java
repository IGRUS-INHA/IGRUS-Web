package igrus.web.survey.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyApi;
import igrus.web.generated.model.ApiCreateSurveyRequest;
import igrus.web.generated.model.ApiSurveyDetailResponse;
import igrus.web.generated.model.ApiSurveyListResponse;
import igrus.web.generated.model.ApiUpdateSurveyRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.dto.request.CreateSurveyRequest;
import igrus.web.survey.dto.request.UpdateSurveyRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyDetailResponseMapper;
import igrus.web.survey.dto.response.SurveyListResponse;
import igrus.web.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 설문 컨트롤러.
 * 설문 CRUD 및 상태 관리 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyController implements SurveyApi {

    private final SurveyService surveyService;

    // ===== 설문 CRUD =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> createSurvey(
            ApiCreateSurveyRequest createSurveyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 생성 요청 - userId: {}, title: {}", user.userId(), createSurveyRequest.getTitle());

        CreateSurveyRequest request = new CreateSurveyRequest(
                createSurveyRequest.getTitle(),
                createSurveyRequest.getDescription(),
                createSurveyRequest.getAccessLevel() != null
                        ? EnumUtils.fromStringOrNull(SurveyAccessLevel.class, createSurveyRequest.getAccessLevel().getValue()) : null,
                createSurveyRequest.getDeadline()
        );

        SurveyDetailResponse response = surveyService.createSurvey(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiSurveyListResponse>> getSurveyList() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 목록 조회 요청 - userId: {}", user.userId());
        List<SurveyListResponse> response = surveyService.getSurveyList(user);
        return ResponseEntity.ok(response.stream()
                .map(this::mapToSurveyListInner)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> getSurveyDetail(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 상세 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.getSurveyDetail(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> updateSurvey(
            Long surveyId,
            ApiUpdateSurveyRequest updateSurveyRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 수정 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        UpdateSurveyRequest request = new UpdateSurveyRequest(
                updateSurveyRequest.getTitle(),
                updateSurveyRequest.getDescription(),
                updateSurveyRequest.getAccessLevel() != null
                        ? EnumUtils.fromStringOrNull(SurveyAccessLevel.class, updateSurveyRequest.getAccessLevel().getValue()) : null,
                updateSurveyRequest.getDeadline()
        );

        SurveyDetailResponse response = surveyService.updateSurvey(surveyId, request, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    // ===== 휴지통 =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiSurveyListResponse>> getTrashedSurveyList() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("휴지통 목록 조회 요청 - userId: {}", user.userId());
        List<SurveyListResponse> response = surveyService.getTrashedSurveyList(user);
        return ResponseEntity.ok(response.stream()
                .map(this::mapToSurveyListInner)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> trashSurvey(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 휴지통 이동 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.trashSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> restoreSurvey(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 휴지통 복원 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.restoreSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> permanentDeleteSurvey(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 영구 삭제 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.permanentDeleteSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    // ===== 상태 전이 =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> publishSurvey(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 공개 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.publishSurvey(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> unpublishSurvey(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 비공개 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.unpublishSurvey(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> openResponse(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("응답 수집 시작 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.openResponse(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> closeResponse(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("응답 수집 마감 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.closeResponse(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> publishAndOpen(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 공개+응답 시작 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.publishAndOpen(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    // === Private helper methods ===

    private ApiSurveyListResponse mapToSurveyListInner(SurveyListResponse s) {
        return new ApiSurveyListResponse()
                .id(s.id())
                .title(s.title())
                .visibility(s.visibility() != null
                        ? ApiSurveyListResponse.VisibilityEnum.fromValue(s.visibility().name()) : null)
                .responseStatus(s.responseStatus() != null
                        ? ApiSurveyListResponse.ResponseStatusEnum.fromValue(s.responseStatus().name()) : null)
                .accessLevel(s.accessLevel() != null
                        ? ApiSurveyListResponse.AccessLevelEnum.fromValue(s.accessLevel().name()) : null)
                .deadline(s.deadline())
                .createdAt(s.createdAt());
    }
}
