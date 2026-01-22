package igrus.web.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 칭호 (기술부, 기술부장, 회장 등)
@Entity
@Table(name = "titles")
@Getter
@Setter
@NoArgsConstructor
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 기본키

    @Column(unique = true, nullable = false, length = 20)
    private String name;
    // 칭호명 (기술부, 기술부장, 회장 등)

    @Column
    private String imageUrl;
    // 칭호 이미지 경로

    @Column
    private Integer displayOrder;
    // 표시 순서

}
