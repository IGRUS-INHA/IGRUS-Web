package igrus.web.user.semester.controller;

import igrus.web.generated.api.AdminSemesterMemberApi;
import igrus.web.generated.model.GetCandidateMembers200ResponseInner;
import igrus.web.generated.model.RegisterMembers200Response;
import igrus.web.generated.model.RegisterMembersRequest;
import igrus.web.generated.model.RemoveMembersRequest;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
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
    public ResponseEntity<List<GetCandidateMembers200ResponseInner>> getCandidateMembers(
            Integer year, Integer semester) {
        List<CandidateMemberResponse> candidates =
                getCandidateMembersService.getCandidateMembers(year, semester);

        List<GetCandidateMembers200ResponseInner> response = candidates.stream()
                .map(c -> new GetCandidateMembers200ResponseInner()
                        .userId(c.userId())
                        .studentId(c.studentId())
                        .name(c.name())
                        .department(c.department())
                        .role(GetCandidateMembers200ResponseInner.RoleEnum.fromValue(c.role().name()))
                        .alreadyRegistered(c.alreadyRegistered())
                        .motivation(c.motivation())
                        .wishes(c.wishes().stream()
                                .map(w -> GetCandidateMembers200ResponseInner.WishesEnum.fromValue(w.name()))
                                .toList()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RegisterMembers200Response> registerMembers(
            Integer year, Integer semester, RegisterMembersRequest registerMembersRequest) {
        RegisterSemesterMembersResponse internal =
                registerSemesterMembersService.registerMembers(year, semester, registerMembersRequest.getUserIds());

        RegisterMembers200Response response = new RegisterMembers200Response()
                .registeredCount(internal.registeredCount())
                .skippedCount(internal.skippedCount())
                .totalRequested(internal.totalRequested());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Integer> removeMembers(
            Integer year, Integer semester, RemoveMembersRequest removeMembersRequest) {
        int removedCount = removeSemesterMembersService.removeMembers(
                year, semester, removeMembersRequest.getUserIds());
        return ResponseEntity.ok(removedCount);
    }
}
