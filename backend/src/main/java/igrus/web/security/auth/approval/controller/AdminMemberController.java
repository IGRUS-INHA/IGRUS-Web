package igrus.web.security.auth.approval.controller;

import igrus.web.security.auth.approval.dto.request.BulkApprovalRequest;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.BulkApprovalResultResponse;
import igrus.web.security.auth.approval.service.MemberApprovalService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController implements AdminMemberControllerApi {

    private final MemberApprovalService memberApprovalService;

    @GetMapping("/pending")
    public ResponseEntity<Page<AssociateInfoResponse>> getPendingAssociates(
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Page<AssociateInfoResponse> pendingAssociates = memberApprovalService.getPendingAssociates(
                pageable,
                authenticatedUser.userId()
        );
        return ResponseEntity.ok(pendingAssociates);
    }

    @Override
    public ResponseEntity<Page<AssociateInfoResponse>> getPendingAssociates(Pageable pageable) {
        // This method signature is required by the interface but won't be called directly.
        // The actual implementation uses the overloaded method with AuthenticatedUser.
        throw new UnsupportedOperationException("Use the method with AuthenticatedUser parameter");
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveAssociate(
            @PathVariable("id") Long userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        memberApprovalService.approveAssociate(userId, authenticatedUser.userId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> approveAssociate(Long userId) {
        // This method signature is required by the interface but won't be called directly.
        // The actual implementation uses the overloaded method with AuthenticatedUser.
        throw new UnsupportedOperationException("Use the method with AuthenticatedUser parameter");
    }

    @PostMapping("/approve/bulk")
    public ResponseEntity<BulkApprovalResultResponse> approveBulk(
            @Valid @RequestBody BulkApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        int approvedCount = memberApprovalService.approveBulk(
                request.userIds(),
                authenticatedUser.userId()
        );

        int totalRequested = request.userIds().size();
        int failedCount = totalRequested - approvedCount;

        BulkApprovalResultResponse response = new BulkApprovalResultResponse(
                approvedCount,
                failedCount,
                totalRequested
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BulkApprovalResultResponse> approveBulk(BulkApprovalRequest request) {
        // This method signature is required by the interface but won't be called directly.
        // The actual implementation uses the overloaded method with AuthenticatedUser.
        throw new UnsupportedOperationException("Use the method with AuthenticatedUser parameter");
    }
}
