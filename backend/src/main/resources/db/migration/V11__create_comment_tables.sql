-- 댓글 테이블
CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    comments_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    comments_deleted_at TIMESTAMP(6) NULL,
    comments_deleted_by BIGINT NULL,
    comments_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comments_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comments_created_by BIGINT NULL,
    comments_updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users(users_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 댓글 좋아요 테이블
CREATE TABLE comment_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment_likes_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comment_likes_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comment_likes_created_by BIGINT NULL,
    comment_likes_updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_like_user FOREIGN KEY (user_id) REFERENCES users(users_id),
    CONSTRAINT uk_comment_like UNIQUE (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 댓글 신고 테이블
CREATE TABLE comment_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
    resolved_at TIMESTAMP(6) NULL,
    resolved_by BIGINT NULL,
    comment_reports_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comment_reports_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comment_reports_created_by BIGINT NULL,
    comment_reports_updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_report_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(users_id),
    CONSTRAINT fk_comment_report_resolver FOREIGN KEY (resolved_by) REFERENCES users(users_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_comment_post ON comments(post_id);
CREATE INDEX idx_comment_parent ON comments(parent_comment_id);
CREATE INDEX idx_comment_author ON comments(author_id);
CREATE INDEX idx_comment_like_user ON comment_likes(user_id);
CREATE INDEX idx_comment_report_status ON comment_reports(status);
