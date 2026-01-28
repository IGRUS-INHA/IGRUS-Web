package igrus.web.user.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * 직책 엔티티.
 * 동아리 내 직책(기술부, 기술부장, 회장 등) 정보를 관리합니다.
 */
@Entity
@Table(name = "positions")
@SQLRestriction("positions_deleted = false")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "positions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "positions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "positions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "positions_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "positions_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "positions_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "positions_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position extends SoftDeletableEntity {

    /** 직책 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "positions_id")
    private Long id;

    /** 직책명 (기술부, 기술부장, 회장 등, 최대 20자, 고유값) */
    @Column(name = "positions_name", unique = true, nullable = false, length = 20)
    private String name;

    /** 직책 이미지 URL */
    @Column(name = "positions_image_url")
    private String imageUrl;

    /** 표시 순서 (낮을수록 상단에 표시) */
    @Column(name = "positions_display_order")
    private Integer displayOrder;

    /** 이 직책을 보유한 사용자 목록 (양방향 관계) */
    @OneToMany(mappedBy = "position")
    private List<UserPosition> userPositions = new ArrayList<>();

    // === 정적 팩토리 메서드 ===

    public static Position create(String name, String imageUrl, Integer displayOrder) {
        Position position = new Position();
        position.name = name;
        position.imageUrl = imageUrl;
        position.displayOrder = displayOrder;
        return position;
    }

    // === 수정 메서드 ===

    public void updateName(String name) {
        this.name = name;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void update(String name, String imageUrl, Integer displayOrder) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    // === 조회 메서드 ===

    public List<User> getUsers() {
        return this.userPositions.stream()
                .map(UserPosition::getUser)
                .toList();
    }
}
