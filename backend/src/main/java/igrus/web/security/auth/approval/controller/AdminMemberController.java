package igrus.web.security.auth.approval.controller;

import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.AdminAssociateApprovalApi;
import igrus.web.generated.model.ApiBulkApprovalResultResponse;
import igrus.web.generated.model.ApiPageDemotedAssociateInfoResponse;
import igrus.web.generated.model.ApiDemotedAssociateInfoResponse;
import igrus.web.generated.model.ApiAssociateInfoPageResponse;
import igrus.web.generated.model.ApiAssociateInfoResponse;
import igrus.web.generated.model.ApiPageRejectedAssociateInfoResponse;
import igrus.web.generated.model.ApiRejectedAssociateInfoResponse;
import igrus.web.generated.model.ApiBulkRejectionResultResponse;
import igrus.web.generated.model.ApiBulkApprovalRequest;
import igrus.web.generated.model.ApiRejectAssociateRequest;
import igrus.web.generated.model.ApiBulkRejectionRequest;
import igrus.web.security.auth.approval.service.read.GetDemotedAssociatesService;
import igrus.web.security.auth.approval.service.read.GetPendingAssociatesService;
import igrus.web.security.auth.approval.service.read.GetRejectedAssociatesService;
import igrus.web.security.auth.approval.service.write.ApproveAssociateService;
import igrus.web.security.auth.approval.service.write.BulkApproveAssociatesService;
import igrus.web.security.auth.approval.service.write.BulkRejectAssociatesService;
import igrus.web.security.auth.approval.service.write.RejectAssociateService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 준회원 승인 컨트롤러.
 * 준회원 승인/거절, 일괄 처리, 거절/강등 목록 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController implements AdminAssociateApprovalApi {

    private final GetPendingAssociatesService getPendingAssociatesService;
    private final GetRejectedAssociatesService getRejectedAssociatesService;
    private final GetDemotedAssociatesService getDemotedAssociatesService;
    private final ApproveAssociateService approveAssociateService;
    private final BulkApproveAssociatesService bulkApproveAssociatesService;
    private final RejectAssociateService rejectAssociateService;
    private final BulkRejectAssociatesService bulkRejectAssociatesService;

    @Override
    public ResponseEntity<ApiAssociateInfoPageResponse> getPendingAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("승인 대기 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<igrus.web.security.auth.approval.dto.response.AssociateInfoResponse> resultPage = getPendingAssociatesService.getPendingAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(new ApiAssociateInfoPageResponse()
                .associates(resultPage.getContent().stream()
                        .map(a -> new ApiAssociateInfoResponse()
                                .userId(a.userId())
                                .studentId(a.studentId())
                                .name(a.name())
                                .department(a.department())
                                .motivation(a.motivation())
                                .wishes(a.wishes() != null
                                        ? a.wishes().stream()
                                                .map(w -> ApiAssociateInfoResponse.WishesEnum.fromValue(w.name()))
                                                .toList()
                                        : null)
                                .createdAt(a.createdAt())
                                .demoted(a.demoted()))
                        .toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .hasNext(resultPage.hasNext()));
    }

    @Override
    public ResponseEntity<Void> approveAssociate(Long id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("개별 준회원 승인 요청 - targetUserId: {}, performedBy: {}", id, user.userId());

        approveAssociateService.approveAssociate(id, user.userId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ApiBulkApprovalResultResponse> approveBulk(
            ApiBulkApprovalRequest bulkApprovalRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("준회원 일괄 승인 요청 - count: {}, performedBy: {}",
                bulkApprovalRequest.getUserIds().size(), user.userId());

        int approvedCount = bulkApproveAssociatesService.approveBulk(
                bulkApprovalRequest.getUserIds(), user.userId());

        int totalRequested = bulkApprovalRequest.getUserIds().size();
        int failedCount = totalRequested - approvedCount;

        return ResponseEntity.ok(new ApiBulkApprovalResultResponse()
                .approvedCount(approvedCount)
                .failedCount(failedCount)
                .totalRequested(totalRequested));
    }

    @Override
    public ResponseEntity<Void> rejectAssociate(
            Long id,
            ApiRejectAssociateRequest rejectAssociateRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("개별 준회원 거절 요청 - targetUserId: {}, performedBy: {}", id, user.userId());

        rejectAssociateService.rejectAssociate(id, user.userId(), rejectAssociateRequest.getReason());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ApiBulkRejectionResultResponse> rejectBulk(
            ApiBulkRejectionRequest bulkRejectionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("준회원 일괄 거절 요청 - count: {}, performedBy: {}",
                bulkRejectionRequest.getUserIds().size(), user.userId());

        int rejectedCount = bulkRejectAssociatesService.rejectBulk(
                bulkRejectionRequest.getUserIds(), user.userId(), bulkRejectionRequest.getReason());

        int totalRequested = bulkRejectionRequest.getUserIds().size();
        int failedCount = totalRequested - rejectedCount;

        return ResponseEntity.ok(new ApiBulkRejectionResultResponse()
                .rejectedCount(rejectedCount)
                .failedCount(failedCount)
                .totalRequested(totalRequested));
    }

    @Override
    public ResponseEntity<ApiPageRejectedAssociateInfoResponse> getRejectedAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("거절된 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<igrus.web.security.auth.approval.dto.response.RejectedAssociateInfoResponse> resultPage = getRejectedAssociatesService.getRejectedAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                r -> new ApiRejectedAssociateInfoResponse()
                        .userId(r.userId())
                        .studentId(r.studentId())
                        .name(r.name())
                        .department(r.department())
                        .motivation(r.motivation())
                        .wishes(r.wishes() != null
                                ? r.wishes().stream()
                                        .map(w -> ApiRejectedAssociateInfoResponse.WishesEnum.fromValue(w.name()))
                                        .toList()
                                : null)
                        .createdAt(r.createdAt())
                        .rejectionReason(r.rejectionReason())
                        .rejectedAt(r.rejectedAt())
                        .rejectedBy(r.rejectedBy()),
                ApiPageRejectedAssociateInfoResponse::new,
                (resp, content, meta) -> resp
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
    public ResponseEntity<ApiPageDemotedAssociateInfoResponse> getDemotedAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("강등된 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<igrus.web.security.auth.approval.dto.response.DemotedAssociateInfoResponse> resultPage = getDemotedAssociatesService.getDemotedAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                d -> new ApiDemotedAssociateInfoResponse()
                        .userId(d.userId())
                        .studentId(d.studentId())
                        .name(d.name())
                        .department(d.department())
                        .demotedAt(d.demotedAt())
                        .demotedBy(d.demotedBy()),
                ApiPageDemotedAssociateInfoResponse::new,
                (resp, content, meta) -> resp
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
}
