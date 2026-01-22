package igrus.web.board.domain;

import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 게시글 좋아요
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post; // 게시글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 좋아요 누른 사용자

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
