package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사용자-직책 중간 테이블 엔티티.
 * User와 Position 간의 다대다 관계를 관리합니다.
 */
@Entity
@Table(name = "user_positions")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "user_positions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "user_positions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "user_positions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "user_positions_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPosition extends BaseEntity {

    /** 사용자-직책 관계 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_positions_id")
    private Long id;

    /** 직책을 보유한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_positions_user_id", nullable = false)
    private User user;

    /** 사용자에게 부여된 직책 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_positions_position_id", nullable = false)
    private Position position;

    /** 직책 부여 시각 */
    @Column(name = "user_positions_assigned_at", nullable = false)
    private Instant assignedAt;

    // === 정적 팩토리 메서드 ===

    public static UserPosition create(User user, Position position) {
        UserPosition userPosition = new UserPosition();
        userPosition.user = user;
        userPosition.position = position;
        userPosition.assignedAt = Instant.now();
        return userPosition;
    }

    public static UserPosition create(User user, Position position, Instant assignedAt) {
        UserPosition userPosition = new UserPosition();
        userPosition.user = user;
        userPosition.position = position;
        userPosition.assignedAt = assignedAt;
        return userPosition;
    }

    // === 연관관계 편의 메서드 (패키지 레벨) ===

    void setUser(User user) {
        this.user = user;
    }
}
