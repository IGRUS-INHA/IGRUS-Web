package igrus.web.board.domain;

import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 첨부파일
@Entity
@Table(name = "attachment")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    // 소속 게시글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;
    // 업로드한 사용자

    @Column(name = "url", nullable = false)
    private String url;
    // 파일 URL (외부 저장소)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
