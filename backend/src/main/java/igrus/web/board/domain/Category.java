package igrus.web.board.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 게시판 카테고리 (정보게시판 등 특정 게시판의 하위 분류)
@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board; // 소속 게시판

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 카테고리 이름

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
