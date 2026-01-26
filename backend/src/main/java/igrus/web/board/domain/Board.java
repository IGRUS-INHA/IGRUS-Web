package igrus.web.board.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시판 엔티티.
 * 시스템에서 사용하는 게시판 정보를 관리합니다.
 */
@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", unique = true, nullable = false, length = 20)
    private BoardCode code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "allows_anonymous", nullable = false)
    private Boolean allowsAnonymous;

    @Column(name = "allows_question_tag", nullable = false)
    private Boolean allowsQuestionTag;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    private Board(BoardCode code, String name, String description,
                  Boolean allowsAnonymous, Boolean allowsQuestionTag, Integer displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.allowsAnonymous = allowsAnonymous;
        this.allowsQuestionTag = allowsQuestionTag;
        this.displayOrder = displayOrder;
    }

    public static Board create(BoardCode code, String name, String description,
                               Boolean allowsAnonymous, Boolean allowsQuestionTag, Integer displayOrder) {
        return new Board(code, name, description, allowsAnonymous, allowsQuestionTag, displayOrder);
    }
}
