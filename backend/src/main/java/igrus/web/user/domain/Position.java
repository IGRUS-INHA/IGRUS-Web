package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// 직책 (기술부, 기술부장, 회장 등)
@Entity
@Table(name = "positions")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "positions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "positions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "positions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "positions_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "positions_id")
    private Long id;

    // 직책명 (기술부, 기술부장, 회장 등)
    @Column(name = "positions_name", unique = true, nullable = false, length = 20)
    private String name;

    // 직책 이미지 경로
    @Column(name = "positions_image_url")
    private String imageUrl;

    // 표시 순서
    @Column(name = "positions_display_order")
    private Integer displayOrder;

    // 이 직책을 가진 사용자들 (양방향)
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
