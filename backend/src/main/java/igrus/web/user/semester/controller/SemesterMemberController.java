package igrus.web.user.semester.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
import igrus.web.user.semester.service.SemesterMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Semester Member", description = "학기별 회원 명단 조회 API (운영진 이상)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class SemesterMemberController {

    private final SemesterMemberService semesterMemberService;

    @Operation(summary = "학기 목록 조회",
            description = "멤버십 기록이 존재하는 학기 목록을 회원 수와 함께 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<SemesterSummaryResponse>> getSemesterList(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<SemesterSummaryResponse> semesters = semesterMemberService.getSemesterList();
        return ResponseEntity.ok(semesters);
    }

    @Operation(summary = "학기별 회원 명단 조회",
            description = "특정 학기의 회원 명단을 조회합니다. 탈퇴 회원도 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 연도 또는 학기", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)", content = @Content)
    })
    @GetMapping("/{year}/{semester}/members")
    public ResponseEntity<List<SemesterMemberListResponse>> getMemberList(
            @Parameter(description = "연도", example = "2026") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)", example = "1") @PathVariable int semester,
            @Parameter(description = "검색 키워드 (학번, 이름)") @RequestParam(required = false) String keyword,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<SemesterMemberListResponse> members = semesterMemberService.getMemberList(year, semester, keyword);
        return ResponseEntity.ok(members);
    }
}
