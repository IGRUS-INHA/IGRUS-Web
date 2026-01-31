package igrus.web.user.semester.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.semester.dto.request.RegisterSemesterMembersRequest;
import igrus.web.user.semester.dto.request.RemoveSemesterMembersRequest;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
import igrus.web.user.semester.service.SemesterMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/semesters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Semester Member", description = "학기별 회원 관리 API (ADMIN 전용)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminSemesterMemberController {

    private final SemesterMemberService semesterMemberService;

    @Operation(summary = "등록 후보 회원 목록 조회",
            description = "특정 학기에 등록 가능한 회원 목록을 조회합니다. ASSOCIATE 이상 + ACTIVE 상태 회원이 대상입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 연도 또는 학기", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)", content = @Content)
    })
    @GetMapping("/{year}/{semester}/candidates")
    public ResponseEntity<List<CandidateMemberResponse>> getCandidateMembers(
            @Parameter(description = "연도", example = "2026") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)", example = "1") @PathVariable int semester,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<CandidateMemberResponse> candidates = semesterMemberService.getCandidateMembers(year, semester);
        return ResponseEntity.ok(candidates);
    }

    @Operation(summary = "회원 일괄 등록",
            description = "선택된 회원들을 특정 학기에 일괄 등록합니다. 이미 등록된 회원은 건너뜁니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "등록 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 목록, 잘못된 연도/학기)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)", content = @Content)
    })
    @PostMapping("/{year}/{semester}/members")
    public ResponseEntity<RegisterSemesterMembersResponse> registerMembers(
            @Parameter(description = "연도", example = "2026") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)", example = "1") @PathVariable int semester,
            @Valid @RequestBody RegisterSemesterMembersRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        RegisterSemesterMembersResponse response = semesterMemberService.registerMembers(year, semester, request.userIds());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원 일괄 제외",
            description = "선택된 회원들을 특정 학기에서 일괄 제외합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "제외 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 목록, 잘못된 연도/학기)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)", content = @Content)
    })
    @DeleteMapping("/{year}/{semester}/members")
    public ResponseEntity<Integer> removeMembers(
            @Parameter(description = "연도", example = "2026") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)", example = "1") @PathVariable int semester,
            @Valid @RequestBody RemoveSemesterMembersRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        int removedCount = semesterMemberService.removeMembers(year, semester, request.userIds());
        return ResponseEntity.ok(removedCount);
    }
}
