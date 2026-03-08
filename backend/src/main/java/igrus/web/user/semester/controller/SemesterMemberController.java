package igrus.web.user.semester.controller;

import igrus.web.generated.api.SemesterMemberApi;
import igrus.web.generated.model.ApiSemesterMemberListResponse;
import igrus.web.generated.model.ApiSemesterSummaryResponse;
import igrus.web.user.semester.service.read.GetSemesterListService;
import igrus.web.user.semester.service.read.GetSemesterMemberListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class SemesterMemberController implements SemesterMemberApi {

    private final GetSemesterListService getSemesterListService;
    private final GetSemesterMemberListService getSemesterMemberListService;

    @Override
    public ResponseEntity<List<ApiSemesterSummaryResponse>> getSemesterList() {
        var semesters = getSemesterListService.getSemesterList();

        List<ApiSemesterSummaryResponse> response = semesters.stream()
                .map(s -> new ApiSemesterSummaryResponse()
                        .year(s.year())
                        .semester(s.semester())
                        .memberCount(s.memberCount())
                        .displayName(s.displayName()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ApiSemesterMemberListResponse>> getMemberList(
            Integer year, Integer semester, String keyword) {
        var members = getSemesterMemberListService.getMemberList(year, semester, keyword);

        List<ApiSemesterMemberListResponse> response = members.stream()
                .map(m -> new ApiSemesterMemberListResponse()
                        .userId(m.userId())
                        .studentId(m.studentId())
                        .name(m.name())
                        .department(m.department())
                        .email(m.email())
                        .phoneNumber(m.phoneNumber())
                        .role(ApiSemesterMemberListResponse.RoleEnum.fromValue(m.role().name()))
                        .isWithdrawn(m.isWithdrawn())
                        .grade(m.grade())
                        .motivation(m.motivation()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
