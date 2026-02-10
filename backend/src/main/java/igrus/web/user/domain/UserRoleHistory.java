package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.exception.SameRoleChangeException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

// 사용자 역할 변경 이력
@Entity
@Table(name = "user_role_histories", indexes = {
        @Index(name = "idx_user_role_histories_user_id", columnList = "user_role_histories_user_id"),
        @Index(name = "idx_user_role_histories_new_role", columnList = "user_role_histories_new_role"),
        @Index(name = "idx_user_role_histories_created_at", columnList = "user_role_histories_created_at"),
        @Index(name = "idx_user_role_histories_created_by", columnList = "user_role_histories_created_by")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "user_role_histories_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "user_role_histories_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "user_role_histories_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "user_role_histories_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoleHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_histories_id")
    private Long id;

    /**
     * 대상 사용자.
     * <p>User 엔티티의 {@code @SQLRestriction("users_deleted = false")}로 인해
     * 탈퇴 사용자는 로딩 불가하므로 {@code @NotFound(IGNORE)}로 null 허용.</p>
     * <p>{@code @NotFound}는 EAGER 로딩을 강제하므로 FetchType.EAGER를 명시적으로 사용.
     * findByFilters()는 LEFT JOIN FETCH 사용으로 추가 쿼리 발생 없음.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_role_histories_user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    /** 대상 사용자 ID (raw FK). 탈퇴 사용자도 원본 ID 유지. */
    @Column(name = "user_role_histories_user_id", insertable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role_histories_previous_role", nullable = false)
    private UserRole previousRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role_histories_new_role", nullable = false)
    private UserRole newRole;

    @Column(name = "user_role_histories_reason")
    private String reason;

    private UserRoleHistory(User user, UserRole previousRole, UserRole newRole, String reason) {
        this.user = user;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.reason = reason;
    }

    public static UserRoleHistory create(User user, UserRole previousRole, UserRole newRole, String reason) {
        validateRoleChange(previousRole, newRole);
        return new UserRoleHistory(user, previousRole, newRole, reason);
    }

    private static void validateRoleChange(UserRole previousRole, UserRole newRole) {
        if (previousRole == newRole) {
            throw new SameRoleChangeException(previousRole);
        }
    }

    // === 역할 변경 유형 확인 ===

    /**
     * 승급 여부 확인 (ASSOCIATE → MEMBER → OPERATOR → ADMIN 순서 기준)
     */
    public boolean isPromotion() {
        return newRole.ordinal() > previousRole.ordinal();
    }

    /**
     * 강등 여부 확인
     */
    public boolean isDemotion() {
        return newRole.ordinal() < previousRole.ordinal();
    }

    /**
     * 관리자로 승급인지 확인
     */
    public boolean isPromotionToAdmin() {
        return newRole == UserRole.ADMIN && previousRole != UserRole.ADMIN;
    }

    /**
     * 운영진으로 승급인지 확인
     */
    public boolean isPromotionToOperator() {
        return newRole == UserRole.OPERATOR && previousRole.ordinal() < UserRole.OPERATOR.ordinal();
    }

    /**
     * 정회원으로 승급인지 확인 (준회원 → 정회원)
     */
    public boolean isPromotionToMember() {
        return newRole == UserRole.MEMBER && previousRole == UserRole.ASSOCIATE;
    }

    /**
     * 관리자에서 강등인지 확인
     */
    public boolean isDemotionFromAdmin() {
        return previousRole == UserRole.ADMIN && newRole != UserRole.ADMIN;
    }

    /**
     * 특정 역할로 변경인지 확인
     */
    public boolean isChangeTo(UserRole targetRole) {
        return newRole == targetRole;
    }

    /**
     * 특정 역할에서 변경인지 확인
     */
    public boolean isChangeFrom(UserRole sourceRole) {
        return previousRole == sourceRole;
    }

    // === 사유(reason) 관리 ===

    /**
     * 사유 업데이트
     */
    public void updateReason(String reason) {
        this.reason = reason;
    }

    /**
     * 사유 존재 여부 확인
     */
    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }
}
