package igrus.web.community.board.domain;

import igrus.web.user.domain.UserRole;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 게시판 권한 엔티티.
 * 각 게시판에 대한 역할별 읽기/쓰기 권한을 관리합니다.
 */
@Entity
@Table(name = "board_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"board_id", "role"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPermission {

    /** 게시판 권한 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 권한이 적용되는 게시판 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /** 권한이 부여된 사용자 역할 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /** 읽기 권한 여부 */
    @Column(name = "can_read", nullable = false)
    private Boolean canRead;

    /** 쓰기 권한 여부 */
    @Column(name = "can_write", nullable = false)
    private Boolean canWrite;

    /** 권한 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private BoardPermission(Board board, UserRole role, Boolean canRead, Boolean canWrite) {
        this.board = board;
        this.role = role;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.createdAt = Instant.now();
    }

    public static BoardPermission create(Board board, UserRole role, Boolean canRead, Boolean canWrite) {
        return new BoardPermission(board, role, canRead, canWrite);
    }

    public boolean hasReadPermission() {
        return Boolean.TRUE.equals(this.canRead);
    }

    public boolean hasWritePermission() {
        return Boolean.TRUE.equals(this.canWrite);
    }
}
