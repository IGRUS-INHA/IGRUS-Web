package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// User-Position 다대다 관계를 위한 중간 테이블
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_positions_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_positions_user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_positions_position_id", nullable = false)
    private Position position;

    // 직책 부여일
    @Column(name = "user_positions_assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    // === 정적 팩토리 메서드 ===

    public static UserPosition create(User user, Position position) {
        UserPosition userPosition = new UserPosition();
        userPosition.user = user;
        userPosition.position = position;
        userPosition.assignedAt = LocalDateTime.now();
        return userPosition;
    }

    public static UserPosition create(User user, Position position, LocalDateTime assignedAt) {
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
