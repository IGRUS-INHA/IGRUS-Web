package igrus.web.user.semester.controller;

import igrus.web.generated.api.SemesterMemberApi;
import igrus.web.generated.model.GetMemberList200ResponseInner;
import igrus.web.generated.model.GetSemesterList200ResponseInner;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
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
    public ResponseEntity<List<GetSemesterList200ResponseInner>> getSemesterList() {
        List<SemesterSummaryResponse> semesters = getSemesterListService.getSemesterList();

        List<GetSemesterList200ResponseInner> response = semesters.stream()
                .map(s -> new GetSemesterList200ResponseInner()
                        .year(s.year())
                        .semester(s.semester())
                        .memberCount(s.memberCount())
                        .displayName(s.displayName()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<GetMemberList200ResponseInner>> getMemberList(
            Integer year, Integer semester, String keyword) {
        List<SemesterMemberListResponse> members =
                getSemesterMemberListService.getMemberList(year, semester, keyword);

        List<GetMemberList200ResponseInner> response = members.stream()
                .map(m -> new GetMemberList200ResponseInner()
                        .userId(m.userId())
                        .studentId(m.studentId())
                        .name(m.name())
                        .department(m.department())
                        .email(m.email())
                        .phoneNumber(m.phoneNumber())
                        .role(GetMemberList200ResponseInner.RoleEnum.fromValue(m.role().name()))
                        .isWithdrawn(m.isWithdrawn())
                        .grade(m.grade())
                        .motivation(m.motivation()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
