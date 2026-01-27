package igrus.web.community.post.domain;

import igrus.web.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 게시글 조회 기록 엔티티.
 * 로그인 사용자의 게시글 조회 이력을 저장합니다.
 * 통계 및 분석 목적으로 사용됩니다.
 */
@Entity
@Table(name = "post_views")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostView {

    /** 조회 기록 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 조회된 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 조회한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    /** 조회 시각 */
    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    private PostView(Post post, User viewer) {
        this.post = post;
        this.viewer = viewer;
        this.viewedAt = Instant.now();
    }

    /**
     * 조회 기록을 생성합니다.
     *
     * @param post   조회된 게시글
     * @param viewer 조회한 사용자
     * @return 생성된 조회 기록
     */
    public static PostView create(Post post, User viewer) {
        return new PostView(post, viewer);
    }
}
