package igrus.web.admin.user.controller;

import igrus.web.admin.user.dto.AdminEditUserInfoRequest;
import igrus.web.admin.user.dto.UserDetailResponse;
import igrus.web.admin.user.dto.UserListResponse;
import igrus.web.admin.user.dto.UserRoleHistoryResponse;
import igrus.web.admin.user.service.AdminEditUserInfoService;
import igrus.web.admin.user.service.ChangeUserRoleService;
import igrus.web.admin.user.service.ChangeUserStatusService;
import igrus.web.admin.user.service.ForceActivateService;
import igrus.web.admin.user.service.ForceWithdrawService;
import igrus.web.admin.user.service.GetUserDetailService;
import igrus.web.admin.user.service.GetUserListService;
import igrus.web.admin.user.service.GetUserRoleHistoryService;
import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.AdminUserManagementApi;
import igrus.web.generated.model.GetRoleHistories200Response;
import igrus.web.generated.model.GetRoleHistories200ResponseContentInner;
import igrus.web.generated.model.GetUserDetail200Response;
import igrus.web.generated.model.GetUserList200Response;
import igrus.web.generated.model.GetUserList200ResponseUsersInner;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.domain.Wish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 회원 관리 컨트롤러.
 * 회원 목록/상세 조회, 권한/상태 변경, 강제 탈퇴/활성화, 정보 수정 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class AdminUserController implements AdminUserManagementApi {

    private final GetUserListService getUserListService;
    private final GetUserDetailService getUserDetailService;
    private final ChangeUserRoleService changeUserRoleService;
    private final ChangeUserStatusService changeUserStatusService;
    private final ForceWithdrawService forceWithdrawService;
    private final ForceActivateService forceActivateService;
    private final AdminEditUserInfoService adminEditUserInfoService;
    private final GetUserRoleHistoryService getUserRoleHistoryService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetRoleHistories200Response> getRoleHistories(
            Long userId,
            String previousRole,
            String newRole,
            Long changedBy,
            Instant startDate,
            Instant endDate,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("권한 변경 이력 조회 요청 - userId: {}, previousRole: {}, newRole: {}, changedBy: {}, page: {}, size: {}",
                userId, previousRole, newRole, changedBy, pageable.getPageNumber(), pageable.getPageSize());

        UserRole prevRole = EnumUtils.fromStringOrNull(UserRole.class, previousRole);
        UserRole nextRole = EnumUtils.fromStringOrNull(UserRole.class, newRole);

        Page<UserRoleHistoryResponse> resultPage = getUserRoleHistoryService.getUserRoleHistories(
                userId, prevRole, nextRole, changedBy, startDate, endDate, pageable);

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                h -> new GetRoleHistories200ResponseContentInner()
                        .id(h.id())
                        .userId(h.userId())
                        .userName(h.userName())
                        .studentId(h.studentId())
                        .previousRole(h.previousRole() != null
                                ? GetRoleHistories200ResponseContentInner.PreviousRoleEnum.fromValue(h.previousRole().name()) : null)
                        .newRole(h.newRole() != null
                                ? GetRoleHistories200ResponseContentInner.NewRoleEnum.fromValue(h.newRole().name()) : null)
                        .reason(h.reason())
                        .changedBy(h.changedBy())
                        .changedAt(h.changedAt()),
                GetRoleHistories200Response::new,
                (r, content, meta) -> r
                        .content(content)
                        .totalElements(meta.totalElements())
                        .totalPages(meta.totalPages())
                        .number(meta.number())
                        .size(meta.size())
                        .numberOfElements(meta.numberOfElements())
                        .first(meta.first())
                        .last(meta.last())
                        .empty(meta.empty())
                        .pageable(meta.pageable())
                        .sort(meta.sort())
        ));
    }

    @Override
    public ResponseEntity<GetUserList200Response> getUserList(
            String keyword,
            String role,
            String status,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("회원 목록 조회 요청 - keyword: {}, role: {}, status: {}, page: {}, size: {}",
                keyword, role, status, pageable.getPageNumber(), pageable.getPageSize());

        UserRole userRole = EnumUtils.fromStringOrNull(UserRole.class, role);
        UserStatus userStatus = EnumUtils.fromStringOrNull(UserStatus.class, status);

        Page<UserListResponse> resultPage = getUserListService.getUserList(keyword, userRole, userStatus, pageable);
        return ResponseEntity.ok(new GetUserList200Response()
                .users(resultPage.getContent().stream()
                        .map(u -> new GetUserList200ResponseUsersInner()
                                .userId(u.userId())
                                .studentId(u.studentId())
                                .name(u.name())
                                .email(u.email())
                                .role(GetUserList200ResponseUsersInner.RoleEnum.fromValue(u.role().name()))
                                .status(GetUserList200ResponseUsersInner.StatusEnum.fromValue(u.status().name()))
                                .createdAt(u.createdAt()))
                        .toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .hasNext(resultPage.hasNext()));
    }

    @Override
    public ResponseEntity<GetUserDetail200Response> getUserDetail(Long userId) {
        log.info("회원 상세 조회 요청 - userId: {}", userId);

        UserDetailResponse result = getUserDetailService.getUserDetail(userId);
        return ResponseEntity.ok(new GetUserDetail200Response()
                .userId(result.userId())
                .studentId(result.studentId())
                .name(result.name())
                .email(result.email())
                .phoneNumber(result.phoneNumber())
                .department(result.department())
                .motivation(result.motivation())
                .wishes(result.wishes() != null
                        ? result.wishes().stream()
                                .map(w -> GetUserDetail200Response.WishesEnum.fromValue(w.name()))
                                .toList()
                        : null)
                .interests(result.interests() != null
                        ? result.interests().stream()
                                .map(i -> GetUserDetail200Response.InterestsEnum.fromValue(i.name()))
                                .toList()
                        : null)
                .customInterest(result.customInterest())
                .joinRoute(result.joinRoute() != null
                        ? GetUserDetail200Response.JoinRouteEnum.fromValue(result.joinRoute().name()) : null)
                .customJoinRoute(result.customJoinRoute())
                .gender(result.gender() != null
                        ? GetUserDetail200Response.GenderEnum.fromValue(result.gender().name()) : null)
                .grade(result.grade())
                .enrollmentStatus(result.enrollmentStatus() != null
                        ? GetUserDetail200Response.EnrollmentStatusEnum.fromValue(result.enrollmentStatus().name()) : null)
                .role(GetUserDetail200Response.RoleEnum.fromValue(result.role().name()))
                .status(GetUserDetail200Response.StatusEnum.fromValue(result.status().name()))
                .createdAt(result.createdAt()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(
            Long userId,
            igrus.web.generated.model.ChangeUserRoleRequest changeUserRoleRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 권한 변경 요청 - targetUserId: {}, newRole: {}, performedBy: {}",
                userId, changeUserRoleRequest.getRole(), user.userId());

        UserRole newRole = UserRole.valueOf(changeUserRoleRequest.getRole().name());
        changeUserRoleService.changeUserRole(userId, newRole, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserStatus(
            Long userId,
            igrus.web.generated.model.ChangeUserStatusRequest changeUserStatusRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 상태 변경 요청 - targetUserId: {}, action: {}, performedBy: {}",
                userId, changeUserStatusRequest.getAction(), user.userId());

        igrus.web.admin.user.dto.ChangeUserStatusRequest internalRequest =
                new igrus.web.admin.user.dto.ChangeUserStatusRequest(
                        igrus.web.admin.user.dto.ChangeUserStatusRequest.Action.valueOf(
                                changeUserStatusRequest.getAction().name()),
                        changeUserStatusRequest.getReason(),
                        changeUserStatusRequest.getSuspendedUntil()
                );

        changeUserStatusService.changeUserStatus(userId, internalRequest, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceWithdrawUser(
            Long userId,
            igrus.web.generated.model.ForceWithdrawUserRequest forceWithdrawUserRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 강제 탈퇴 요청 - targetUserId: {}, performedBy: {}", userId, user.userId());

        forceWithdrawService.forceWithdraw(userId, forceWithdrawUserRequest.getReason(), user.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceActivateUser(Long userId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 강제 활성화 요청 - targetUserId: {}, performedBy: {}", userId, user.userId());

        forceActivateService.forceActivate(userId, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> editUserInfo(
            Long userId,
            igrus.web.generated.model.EditUserInfoRequest editUserInfoRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 정보 수정 요청 - targetUserId: {}, performedBy: {}", userId, user.userId());

        AdminEditUserInfoRequest internalRequest = new AdminEditUserInfoRequest(
                editUserInfoRequest.getStudentId(),
                editUserInfoRequest.getEmail(),
                editUserInfoRequest.getName(),
                editUserInfoRequest.getPhoneNumber(),
                editUserInfoRequest.getDepartment(),
                editUserInfoRequest.getGender() != null
                        ? Gender.valueOf(editUserInfoRequest.getGender().name()) : null,
                editUserInfoRequest.getGrade(),
                editUserInfoRequest.getEnrollmentStatus() != null
                        ? EnrollmentStatus.valueOf(editUserInfoRequest.getEnrollmentStatus().name()) : null,
                editUserInfoRequest.getMotivation(),
                editUserInfoRequest.getWishes() != null
                        ? editUserInfoRequest.getWishes().stream()
                                .map(w -> Wish.valueOf(w.name()))
                                .toList()
                        : null,
                editUserInfoRequest.getInterests() != null
                        ? editUserInfoRequest.getInterests().stream()
                                .map(i -> Interest.valueOf(i.name()))
                                .toList()
                        : null,
                editUserInfoRequest.getCustomInterest(),
                editUserInfoRequest.getJoinRoute() != null
                        ? JoinRoute.valueOf(editUserInfoRequest.getJoinRoute().name()) : null,
                editUserInfoRequest.getCustomJoinRoute()
        );

        adminEditUserInfoService.editUserInfo(userId, internalRequest, user.userId());
        return ResponseEntity.noContent().build();
    }
}
