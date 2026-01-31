package igrus.web.community.comment.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 댓글 신고 엔티티.
 * 부적절한 댓글에 대한 신고 정보를 관리합니다.
 */
@Entity
@Table(name = "comment_reports")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "comment_reports_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "comment_reports_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "comment_reports_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "comment_reports_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport extends BaseEntity {

    /** 신고 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_reports_id")
    private Long id;

    /** 신고된 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_reports_comment_id", nullable = false)
    private Comment comment;

    /** 신고자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_reports_reporter_id", nullable = false)
    private User reporter;

    /** 신고 사유 */
    @Column(name = "comment_reports_reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** 신고 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_reports_status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    /** 처리 완료 시각 */
    @Column(name = "comment_reports_resolved_at")
    private Instant resolvedAt;

    /** 처리자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_reports_resolved_by")
    private User resolvedBy;

    private CommentReport(Comment comment, User reporter, String reason) {
        validateReason(reason);
        this.comment = comment;
        this.reporter = reporter;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    /**
     * 댓글 신고를 생성합니다.
     *
     * @param comment  신고할 댓글
     * @param reporter 신고자
     * @param reason   신고 사유
     * @return 생성된 댓글 신고
     * @throws IllegalArgumentException 신고 사유가 비어있는 경우
     */
    public static CommentReport create(Comment comment, User reporter, String reason) {
        return new CommentReport(comment, reporter, reason);
    }

    // === 비즈니스 메서드 ===

    /**
     * 신고를 처리 완료(승인)합니다.
     *
     * @param admin 처리하는 관리자
     */
    public void resolve(User admin) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = admin;
    }

    /**
     * 신고를 반려합니다.
     *
     * @param admin 처리하는 관리자
     */
    public void dismiss(User admin) {
        this.status = ReportStatus.DISMISSED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = admin;
    }

    /**
     * 신고가 대기 상태인지 확인합니다.
     *
     * @return 대기 상태이면 true
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    // === Private 검증 메서드 ===

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("신고 사유를 입력해 주세요");
        }
    }
}
