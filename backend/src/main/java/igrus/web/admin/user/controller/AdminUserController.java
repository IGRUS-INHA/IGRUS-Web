package igrus.web.admin.user.controller;

import igrus.web.admin.user.dto.AdminEditUserInfoRequest;
import igrus.web.admin.user.dto.ChangeUserStatusRequest;
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
import igrus.web.generated.model.ApiAdminEditUserInfoRequest;
import igrus.web.generated.model.ApiChangeUserRoleRequest;
import igrus.web.generated.model.ApiChangeUserStatusRequest;
import igrus.web.generated.model.ApiForceWithdrawRequest;
import igrus.web.generated.model.ApiPageUserRoleHistoryResponse;
import igrus.web.generated.model.ApiUserDetailResponse;
import igrus.web.generated.model.ApiUserListPageResponse;
import igrus.web.generated.model.ApiUserListResponse;
import igrus.web.generated.model.ApiUserRoleHistoryResponse;
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
    public ResponseEntity<ApiPageUserRoleHistoryResponse> getRoleHistories(
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

        var resultPage = getUserRoleHistoryService.getUserRoleHistories(
                userId, prevRole, nextRole, changedBy, startDate, endDate, pageable);

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                h -> new ApiUserRoleHistoryResponse()
                        .id(h.id())
                        .userId(h.userId())
                        .userName(h.userName())
                        .studentId(h.studentId())
                        .previousRole(h.previousRole() != null
                                ? ApiUserRoleHistoryResponse.PreviousRoleEnum.fromValue(h.previousRole().name()) : null)
                        .newRole(h.newRole() != null
                                ? ApiUserRoleHistoryResponse.NewRoleEnum.fromValue(h.newRole().name()) : null)
                        .reason(h.reason())
                        .changedBy(h.changedBy())
                        .changedAt(h.changedAt()),
                ApiPageUserRoleHistoryResponse::new,
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
    public ResponseEntity<ApiUserListPageResponse> getUserList(
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

        var resultPage = getUserListService.getUserList(keyword, userRole, userStatus, pageable);
        return ResponseEntity.ok(new ApiUserListPageResponse()
                .users(resultPage.getContent().stream()
                        .map(u -> new ApiUserListResponse()
                                .userId(u.userId())
                                .studentId(u.studentId())
                                .name(u.name())
                                .email(u.email())
                                .role(ApiUserListResponse.RoleEnum.fromValue(u.role().name()))
                                .status(ApiUserListResponse.StatusEnum.fromValue(u.status().name()))
                                .createdAt(u.createdAt()))
                        .toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .hasNext(resultPage.hasNext()));
    }

    @Override
    public ResponseEntity<ApiUserDetailResponse> getUserDetail(Long userId) {
        log.info("회원 상세 조회 요청 - userId: {}", userId);

        var result = getUserDetailService.getUserDetail(userId);
        return ResponseEntity.ok(new ApiUserDetailResponse()
                .userId(result.userId())
                .studentId(result.studentId())
                .name(result.name())
                .email(result.email())
                .phoneNumber(result.phoneNumber())
                .department(result.department())
                .motivation(result.motivation())
                .wishes(result.wishes() != null
                        ? result.wishes().stream()
                                .map(w -> ApiUserDetailResponse.WishesEnum.fromValue(w.name()))
                                .toList()
                        : null)
                .interests(result.interests() != null
                        ? result.interests().stream()
                                .map(i -> ApiUserDetailResponse.InterestsEnum.fromValue(i.name()))
                                .toList()
                        : null)
                .customInterest(result.customInterest())
                .joinRoute(result.joinRoute() != null
                        ? ApiUserDetailResponse.JoinRouteEnum.fromValue(result.joinRoute().name()) : null)
                .customJoinRoute(result.customJoinRoute())
                .gender(result.gender() != null
                        ? ApiUserDetailResponse.GenderEnum.fromValue(result.gender().name()) : null)
                .grade(result.grade())
                .enrollmentStatus(result.enrollmentStatus() != null
                        ? ApiUserDetailResponse.EnrollmentStatusEnum.fromValue(result.enrollmentStatus().name()) : null)
                .role(ApiUserDetailResponse.RoleEnum.fromValue(result.role().name()))
                .status(ApiUserDetailResponse.StatusEnum.fromValue(result.status().name()))
                .createdAt(result.createdAt()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(
            Long userId,
            ApiChangeUserRoleRequest changeUserRoleRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 권한 변경 요청 - targetUserId: {}, newRole: {}, performedBy: {}",
                userId, changeUserRoleRequest.getRole(), user.userId());

        UserRole newRole = EnumUtils.fromStringOrNull(UserRole.class, changeUserRoleRequest.getRole().name());
        changeUserRoleService.changeUserRole(userId, newRole, user.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserStatus(
            Long userId,
            ApiChangeUserStatusRequest changeUserStatusRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 상태 변경 요청 - targetUserId: {}, action: {}, performedBy: {}",
                userId, changeUserStatusRequest.getAction(), user.userId());

        var internalRequest =
                new ChangeUserStatusRequest(
                        EnumUtils.fromStringOrNull(ChangeUserStatusRequest.Action.class,
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
            ApiForceWithdrawRequest forceWithdrawRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 강제 탈퇴 요청 - targetUserId: {}, performedBy: {}", userId, user.userId());

        forceWithdrawService.forceWithdraw(userId, forceWithdrawRequest.getReason(), user.userId());
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
            ApiAdminEditUserInfoRequest adminEditUserInfoRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("회원 정보 수정 요청 - targetUserId: {}, performedBy: {}", userId, user.userId());

        var internalRequest = new AdminEditUserInfoRequest(
                adminEditUserInfoRequest.getStudentId(),
                adminEditUserInfoRequest.getEmail(),
                adminEditUserInfoRequest.getName(),
                adminEditUserInfoRequest.getPhoneNumber(),
                adminEditUserInfoRequest.getDepartment(),
                adminEditUserInfoRequest.getGender() != null
                        ? EnumUtils.fromStringOrNull(Gender.class, adminEditUserInfoRequest.getGender().name()) : null,
                adminEditUserInfoRequest.getGrade(),
                adminEditUserInfoRequest.getEnrollmentStatus() != null
                        ? EnumUtils.fromStringOrNull(EnrollmentStatus.class, adminEditUserInfoRequest.getEnrollmentStatus().name()) : null,
                adminEditUserInfoRequest.getMotivation(),
                adminEditUserInfoRequest.getWishes() != null
                        ? adminEditUserInfoRequest.getWishes().stream()
                                .map(w -> EnumUtils.fromStringOrNull(Wish.class, w.name()))
                                .toList()
                        : null,
                adminEditUserInfoRequest.getInterests() != null
                        ? adminEditUserInfoRequest.getInterests().stream()
                                .map(i -> EnumUtils.fromStringOrNull(Interest.class, i.name()))
                                .toList()
                        : null,
                adminEditUserInfoRequest.getCustomInterest(),
                adminEditUserInfoRequest.getJoinRoute() != null
                        ? EnumUtils.fromStringOrNull(JoinRoute.class, adminEditUserInfoRequest.getJoinRoute().name()) : null,
                adminEditUserInfoRequest.getCustomJoinRoute()
        );

        adminEditUserInfoService.editUserInfo(userId, internalRequest, user.userId());
        return ResponseEntity.noContent().build();
    }
}
