package igrus.web.community.board.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "boards_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "boards_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "boards_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "boards_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

    /** 게시판 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "boards_id")
    private Long id;

    /** 게시판 코드 (NOTICES: 공지사항, GENERAL: 자유게시판, INSIGHT: 인사이트) */
    @Enumerated(EnumType.STRING)
    @Column(name = "boards_code", unique = true, nullable = false, length = 20)
    private BoardCode code;

    /** 게시판 이름 (최대 50자) */
    @Column(name = "boards_name", nullable = false, length = 50)
    private String name;

    /** 게시판 설명 (최대 200자, nullable) */
    @Column(name = "boards_description", length = 200)
    private String description;

    /** 익명 게시 허용 여부. true인 게시판에서만 익명 글 작성 가능 */
    @Column(name = "boards_allows_anonymous", nullable = false)
    private Boolean allowsAnonymous;

    /** 질문 태그 허용 여부. true인 게시판에서만 질문글 표시 가능 */
    @Column(name = "boards_allows_question_tag", nullable = false)
    private Boolean allowsQuestionTag;

    /** 게시판 표시 순서 (낮을수록 상단에 표시) */
    @Column(name = "boards_display_order", nullable = false)
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
