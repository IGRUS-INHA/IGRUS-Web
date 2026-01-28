package igrus.web.community.post.domain;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 게시글 엔티티.
 * 게시판에 작성된 게시글 정보를 관리합니다.
 */
@Entity
@Table(name = "posts")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "posts_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "posts_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "posts_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "posts_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "posts_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "posts_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "posts_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends SoftDeletableEntity {

    /** 게시글 제목 최대 길이 */
    private static final int MAX_TITLE_LENGTH = 100;

    /** 게시글당 첨부 가능한 최대 이미지 개수 */
    private static final int MAX_IMAGE_COUNT = 5;

    /** 게시글 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posts_id")
    private Long id;

    /** 게시글이 속한 게시판 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posts_board_id", nullable = false)
    private Board board;

    /** 게시글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posts_author_id", nullable = false)
    private User author;

    /** 게시글 제목 (최대 100자) */
    @Column(name = "posts_title", nullable = false, length = 100)
    private String title;

    /** 게시글 본문 내용 */
    @Column(name = "posts_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 조회수 (기본값: 0) */
    @Column(name = "posts_view_count", nullable = false)
    private int viewCount = 0;

    /** 낙관적 락을 위한 버전 (동시성 제어) */
    @Version
    @Column(name = "posts_version")
    private Long version;

    /** 익명 여부. 자유게시판(GENERAL)에서만 true 가능 */
    @Column(name = "posts_is_anonymous", nullable = false)
    private boolean isAnonymous = false;

    /** 질문글 여부. 자유게시판(GENERAL)에서만 true 가능 */
    @Column(name = "posts_is_question", nullable = false)
    private boolean isQuestion = false;

    /** 준회원 공개 여부. 공지사항(NOTICES)에서만 사용 */
    @Column(name = "posts_is_visible_to_associate", nullable = false)
    private boolean isVisibleToAssociate = false;

    /** 첨부 이미지 목록 (최대 5개) */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

    private Post(Board board, User author, String title, String content,
                 boolean isAnonymous, boolean isQuestion, boolean isVisibleToAssociate) {
        validateTitle(title);
        this.board = board;
        this.author = author;
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.isQuestion = isQuestion;
        this.isVisibleToAssociate = isVisibleToAssociate;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 일반 게시글을 생성합니다.
     *
     * @param board   게시판
     * @param author  작성자
     * @param title   제목 (최대 100자)
     * @param content 내용
     * @return 생성된 게시글
     * @throws IllegalArgumentException 제목이 100자를 초과하는 경우
     */
    public static Post createPost(Board board, User author, String title, String content) {
        return new Post(board, author, title, content, false, false, false);
    }

    /**
     * 익명 게시글을 생성합니다.
     * 자유게시판(GENERAL)에서만 생성 가능합니다.
     *
     * @param board   게시판
     * @param author  작성자
     * @param title   제목 (최대 100자)
     * @param content 내용
     * @return 생성된 익명 게시글
     * @throws IllegalArgumentException 제목이 100자를 초과하거나 자유게시판이 아닌 경우
     */
    public static Post createAnonymousPost(Board board, User author, String title, String content) {
        validateAnonymousOption(board);
        return new Post(board, author, title, content, true, false, false);
    }

    /**
     * 공지사항을 생성합니다.
     * 공지사항 게시판(NOTICES)에서만 생성 가능합니다.
     *
     * @param board                게시판
     * @param author               작성자
     * @param title                제목 (최대 100자)
     * @param content              내용
     * @param isVisibleToAssociate 준회원 공개 여부
     * @return 생성된 공지사항
     * @throws IllegalArgumentException 제목이 100자를 초과하거나 공지사항 게시판이 아닌 경우
     */
    public static Post createNotice(Board board, User author, String title, String content,
                                    boolean isVisibleToAssociate) {
        validateVisibilityOption(board);
        return new Post(board, author, title, content, false, false, isVisibleToAssociate);
    }

    // === 비즈니스 메서드 ===

    /**
     * 게시글 제목과 내용을 수정합니다.
     *
     * @param title   새 제목 (최대 100자)
     * @param content 새 내용
     * @throws IllegalArgumentException 제목이 100자를 초과하는 경우
     */
    public void updateContent(String title, String content) {
        validateTitle(title);
        this.title = title;
        this.content = content;
    }

    /**
     * 조회수를 1 증가시킵니다.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 조회수를 지정된 값으로 동기화합니다.
     * PostView 테이블의 실제 조회 수와 동기화할 때 사용합니다.
     *
     * @param viewCount 동기화할 조회수
     */
    public void syncViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 해당 사용자가 게시글을 수정할 수 있는지 확인합니다.
     * 작성자 본인 또는 ADMIN만 수정 가능합니다.
     *
     * @param user 확인할 사용자
     * @return 수정 가능 여부
     */
    public boolean canModify(User user) {
        if (user == null) {
            return false;
        }
        return isAuthor(user) || user.isAdmin();
    }

    /**
     * 해당 사용자가 게시글을 삭제할 수 있는지 확인합니다.
     * 작성자 본인 또는 OPERATOR 이상 권한을 가진 사용자만 삭제 가능합니다.
     *
     * @param user 확인할 사용자
     * @return 삭제 가능 여부
     */
    public boolean canDelete(User user) {
        if (user == null) {
            return false;
        }
        return isAuthor(user) || user.isOperatorOrAbove();
    }

    /**
     * 질문 여부를 설정합니다.
     * 자유게시판(GENERAL)에서만 설정 가능합니다.
     *
     * @param isQuestion 질문 여부
     * @throws IllegalArgumentException 자유게시판이 아닌 경우
     */
    public void setQuestion(boolean isQuestion) {
        validateQuestionOption(this.board);
        this.isQuestion = isQuestion;
    }

    /**
     * 이미지를 추가합니다.
     * 최대 5개까지 추가 가능합니다.
     * PostImage 엔티티 생성 시 이 메서드를 통해 추가하면 양방향 관계가 설정됩니다.
     *
     * @param image 추가할 이미지
     * @throws IllegalArgumentException 이미지가 5개를 초과하는 경우
     */
    public void addImage(PostImage image) {
        if (this.images.size() >= MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGE_COUNT + "개까지 첨부 가능합니다");
        }
        this.images.add(image);
    }

    /**
     * 모든 이미지를 삭제합니다.
     */
    public void clearImages() {
        this.images.clear();
    }

    /**
     * 이미지 목록을 조회합니다 (불변 리스트 반환).
     *
     * @return 이미지 목록
     */
    public List<PostImage> getImages() {
        return Collections.unmodifiableList(this.images);
    }

    // === Private 검증 메서드 ===

    private static void validateTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목은 " + MAX_TITLE_LENGTH + "자 이내여야 합니다");
        }
    }

    private static void validateAnonymousOption(Board board) {
        if (!board.getAllowsAnonymous()) {
            throw new IllegalArgumentException("이 게시판에서는 익명 게시글을 작성할 수 없습니다");
        }
    }

    private static void validateQuestionOption(Board board) {
        if (!board.getAllowsQuestionTag()) {
            throw new IllegalArgumentException("이 게시판에서는 질문 태그를 사용할 수 없습니다");
        }
    }

    private static void validateVisibilityOption(Board board) {
        if (board.getCode() != BoardCode.NOTICES) {
            throw new IllegalArgumentException("준회원 공개 옵션은 공지사항에서만 사용 가능합니다");
        }
    }

    private boolean isAuthor(User user) {
        return this.author != null && this.author.getId().equals(user.getId());
    }
}
