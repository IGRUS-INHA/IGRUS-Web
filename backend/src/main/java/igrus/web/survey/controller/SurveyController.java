package igrus.web.survey.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.request.CreateSurveyRequest;
import igrus.web.survey.dto.request.UpdateSurveyRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyListResponse;
import igrus.web.survey.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 설문 컨트롤러.
 * 설문 CRUD 및 상태 관리 API를 제공합니다.
 */
@Tag(name = "Survey", description = "설문 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    // ===== 설문 CRUD =====

    @Operation(summary = "설문 생성", description = "새로운 설문을 생성합니다. 초기 상태: UNPUBLISHED + NOT_STARTED. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "설문 생성 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ResponseEntity<SurveyDetailResponse> createSurvey(
            @Valid @RequestBody CreateSurveyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 생성 요청 - userId: {}, title: {}", user.userId(), request.title());
        SurveyDetailResponse response = surveyService.createSurvey(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "설문 목록 조회", description = "활성 설문 목록을 조회합니다. 휴지통 설문은 제외됩니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ResponseEntity<List<SurveyListResponse>> getSurveyList(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 목록 조회 요청 - userId: {}", user.userId());
        List<SurveyListResponse> response = surveyService.getSurveyList(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 상세 조회", description = "설문의 상세 정보를 조회합니다. 질문·선택지·행을 포함합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 상세 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @GetMapping("/{surveyId}")
    public ResponseEntity<SurveyDetailResponse> getSurveyDetail(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 상세 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.getSurveyDetail(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 수정", description = "설문 정보를 수정합니다. 모든 상태에서 수정 가능합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 수정 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PutMapping("/{surveyId}")
    public ResponseEntity<SurveyDetailResponse> updateSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Valid @RequestBody UpdateSurveyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 수정 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.updateSurvey(surveyId, request, user);
        return ResponseEntity.ok(response);
    }

    // ===== 휴지통 =====

    @Operation(summary = "휴지통 목록 조회", description = "휴지통에 있는 설문 목록을 조회합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "휴지통 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/trash")
    public ResponseEntity<List<SurveyListResponse>> getTrashedSurveyList(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("휴지통 목록 조회 요청 - userId: {}", user.userId());
        List<SurveyListResponse> response = surveyService.getTrashedSurveyList(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 휴지통 이동", description = "설문을 휴지통으로 이동합니다. 모든 상태에서 가능합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "휴지통 이동 성공"),
            @ApiResponse(responseCode = "400", description = "이미 휴지통에 있는 설문"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/trash")
    public ResponseEntity<Void> trashSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 휴지통 이동 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.trashSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "설문 휴지통 복원", description = "휴지통에서 설문을 복원합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복원 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/restore")
    public ResponseEntity<Void> restoreSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 휴지통 복원 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.restoreSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "설문 영구 삭제", description = "설문을 영구 삭제합니다. 휴지통에 있는 설문만 가능합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "영구 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "휴지통에 있는 설문이 아님"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> permanentDeleteSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 영구 삭제 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        surveyService.permanentDeleteSurvey(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    // ===== 상태 전이 =====

    @Operation(summary = "설문 공개", description = "설문을 공개합니다. UNPUBLISHED → PUBLISHED. 질문 구조 유효성 검증을 수행합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 공개 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "공개 조건 미충족 또는 이미 공개 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/publish")
    public ResponseEntity<SurveyDetailResponse> publishSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 공개 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.publishSurvey(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 비공개", description = "설문을 비공개로 전환합니다. PUBLISHED → UNPUBLISHED. 응답 수집 중(OPEN)이면 자동 마감됩니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 비공개 전환 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 비공개 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/unpublish")
    public ResponseEntity<SurveyDetailResponse> unpublishSurvey(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 비공개 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.unpublishSurvey(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "응답 수집 시작", description = "응답 수집을 시작(또는 재개)합니다. NOT_STARTED/CLOSED → OPEN. PUBLISHED 상태에서만 가능하며 마감일이 경과하지 않아야 합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 수집 시작 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "시작 불가능한 상태 또는 마감일 경과"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/open")
    public ResponseEntity<SurveyDetailResponse> openResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("응답 수집 시작 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.openResponse(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "응답 수집 마감", description = "응답 수집을 마감합니다. OPEN → CLOSED. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 수집 마감 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "마감 불가능한 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/close")
    public ResponseEntity<SurveyDetailResponse> closeResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("응답 수집 마감 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.closeResponse(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 공개 + 응답 수집 시작", description = "설문을 공개하고 동시에 응답 수집을 시작합니다. UNPUBLISHED 상태에서만 가능합니다. 질문 구조 유효성 검증을 수행합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공개 + 응답 수집 시작 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "공개 조건 미충족 또는 마감일 경과"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping("/{surveyId}/publish-and-open")
    public ResponseEntity<SurveyDetailResponse> publishAndOpen(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 공개+응답 시작 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyDetailResponse response = surveyService.publishAndOpen(surveyId, user);
        return ResponseEntity.ok(response);
    }
}
