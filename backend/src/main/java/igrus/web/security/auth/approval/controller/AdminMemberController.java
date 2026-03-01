package igrus.web.security.auth.approval.controller;

import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.AdminAssociateApprovalApi;
import igrus.web.generated.model.ApproveBulk200Response;
import igrus.web.generated.model.GetDemotedAssociates200Response;
import igrus.web.generated.model.GetDemotedAssociates200ResponseContentInner;
import igrus.web.generated.model.GetPendingAssociates200Response;
import igrus.web.generated.model.GetPendingAssociates200ResponseAssociatesInner;
import igrus.web.generated.model.GetRejectedAssociates200Response;
import igrus.web.generated.model.GetRejectedAssociates200ResponseContentInner;
import igrus.web.generated.model.RejectBulk200Response;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.DemotedAssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.RejectedAssociateInfoResponse;
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
    public ResponseEntity<GetPendingAssociates200Response> getPendingAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("승인 대기 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<AssociateInfoResponse> resultPage = getPendingAssociatesService.getPendingAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(new GetPendingAssociates200Response()
                .associates(resultPage.getContent().stream()
                        .map(a -> new GetPendingAssociates200ResponseAssociatesInner()
                                .userId(a.userId())
                                .studentId(a.studentId())
                                .name(a.name())
                                .department(a.department())
                                .motivation(a.motivation())
                                .wishes(a.wishes() != null
                                        ? a.wishes().stream()
                                                .map(w -> GetPendingAssociates200ResponseAssociatesInner.WishesEnum.fromValue(w.name()))
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
    public ResponseEntity<ApproveBulk200Response> approveBulk(
            igrus.web.generated.model.ApproveBulkRequest approveBulkRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("준회원 일괄 승인 요청 - count: {}, performedBy: {}",
                approveBulkRequest.getUserIds().size(), user.userId());

        int approvedCount = bulkApproveAssociatesService.approveBulk(
                approveBulkRequest.getUserIds(), user.userId());

        int totalRequested = approveBulkRequest.getUserIds().size();
        int failedCount = totalRequested - approvedCount;

        return ResponseEntity.ok(new ApproveBulk200Response()
                .approvedCount(approvedCount)
                .failedCount(failedCount)
                .totalRequested(totalRequested));
    }

    @Override
    public ResponseEntity<Void> rejectAssociate(
            Long id,
            igrus.web.generated.model.RejectAssociateRequest rejectAssociateRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("개별 준회원 거절 요청 - targetUserId: {}, performedBy: {}", id, user.userId());

        rejectAssociateService.rejectAssociate(id, user.userId(), rejectAssociateRequest.getReason());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<RejectBulk200Response> rejectBulk(
            igrus.web.generated.model.RejectBulkRequest rejectBulkRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("준회원 일괄 거절 요청 - count: {}, performedBy: {}",
                rejectBulkRequest.getUserIds().size(), user.userId());

        int rejectedCount = bulkRejectAssociatesService.rejectBulk(
                rejectBulkRequest.getUserIds(), user.userId(), rejectBulkRequest.getReason());

        int totalRequested = rejectBulkRequest.getUserIds().size();
        int failedCount = totalRequested - rejectedCount;

        return ResponseEntity.ok(new RejectBulk200Response()
                .rejectedCount(rejectedCount)
                .failedCount(failedCount)
                .totalRequested(totalRequested));
    }

    @Override
    public ResponseEntity<GetRejectedAssociates200Response> getRejectedAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("거절된 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<RejectedAssociateInfoResponse> resultPage = getRejectedAssociatesService.getRejectedAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                r -> new GetRejectedAssociates200ResponseContentInner()
                        .userId(r.userId())
                        .studentId(r.studentId())
                        .name(r.name())
                        .department(r.department())
                        .motivation(r.motivation())
                        .wishes(r.wishes() != null
                                ? r.wishes().stream()
                                        .map(w -> GetRejectedAssociates200ResponseContentInner.WishesEnum.fromValue(w.name()))
                                        .toList()
                                : null)
                        .createdAt(r.createdAt())
                        .rejectionReason(r.rejectionReason())
                        .rejectedAt(r.rejectedAt())
                        .rejectedBy(r.rejectedBy()),
                GetRejectedAssociates200Response::new,
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
    public ResponseEntity<GetDemotedAssociates200Response> getDemotedAssociates(
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("강등된 준회원 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<DemotedAssociateInfoResponse> resultPage = getDemotedAssociatesService.getDemotedAssociates(
                pageable, user.userId());

        return ResponseEntity.ok(PageResponseMapper.toSpringPageResponse(
                resultPage,
                d -> new GetDemotedAssociates200ResponseContentInner()
                        .userId(d.userId())
                        .studentId(d.studentId())
                        .name(d.name())
                        .department(d.department())
                        .demotedAt(d.demotedAt())
                        .demotedBy(d.demotedBy()),
                GetDemotedAssociates200Response::new,
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
