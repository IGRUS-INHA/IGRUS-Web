package igrus.web.user.semester.controller;

import igrus.web.generated.api.AdminSemesterMemberApi;
import igrus.web.generated.model.ApiCandidateMemberResponse;
import igrus.web.generated.model.ApiRegisterSemesterMembersRequest;
import igrus.web.generated.model.ApiRegisterSemesterMembersResponse;
import igrus.web.generated.model.ApiRemoveSemesterMembersRequest;
import igrus.web.user.semester.service.read.GetCandidateMembersService;
import igrus.web.user.semester.service.write.RegisterSemesterMembersService;
import igrus.web.user.semester.service.write.RemoveSemesterMembersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSemesterMemberController implements AdminSemesterMemberApi {

    private final GetCandidateMembersService getCandidateMembersService;
    private final RegisterSemesterMembersService registerSemesterMembersService;
    private final RemoveSemesterMembersService removeSemesterMembersService;

    @Override
    public ResponseEntity<List<ApiCandidateMemberResponse>> getCandidateMembers(
            Integer year, Integer semester) {
        var candidates = getCandidateMembersService.getCandidateMembers(year, semester);

        List<ApiCandidateMemberResponse> response = candidates.stream()
                .map(c -> new ApiCandidateMemberResponse()
                        .userId(c.userId())
                        .studentId(c.studentId())
                        .name(c.name())
                        .department(c.department())
                        .role(ApiCandidateMemberResponse.RoleEnum.fromValue(c.role().name()))
                        .alreadyRegistered(c.alreadyRegistered())
                        .motivation(c.motivation())
                        .wishes(c.wishes().stream()
                                .map(w -> ApiCandidateMemberResponse.WishesEnum.fromValue(w.name()))
                                .toList()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiRegisterSemesterMembersResponse> registerMembers(
            Integer year, Integer semester, ApiRegisterSemesterMembersRequest registerSemesterMembersRequest) {
        var internal = registerSemesterMembersService.registerMembers(year, semester, registerSemesterMembersRequest.getUserIds());

        ApiRegisterSemesterMembersResponse response = new ApiRegisterSemesterMembersResponse()
                .registeredCount(internal.registeredCount())
                .skippedCount(internal.skippedCount())
                .totalRequested(internal.totalRequested());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Integer> removeMembers(
            Integer year, Integer semester, ApiRemoveSemesterMembersRequest removeSemesterMembersRequest) {
        int removedCount = removeSemesterMembersService.removeMembers(
                year, semester, removeSemesterMembersRequest.getUserIds());
        return ResponseEntity.ok(removedCount);
    }
}
