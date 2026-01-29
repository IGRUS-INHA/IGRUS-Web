-- V10: Comment tables creation
-- 댓글, 댓글 좋아요, 댓글 신고 테이블 생성

-- 댓글 테이블
CREATE TABLE comments (
    comments_id BIGINT NOT NULL AUTO_INCREMENT,
    comments_post_id BIGINT NOT NULL,
    comments_parent_comment_id BIGINT NULL,
    comments_author_id BIGINT NOT NULL,
    comments_content VARCHAR(500) NOT NULL,
    comments_is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    comments_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    comments_deleted_at TIMESTAMP(6) NULL,
    comments_deleted_by BIGINT NULL,
    comments_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comments_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comments_created_by BIGINT NULL,
    comments_updated_by BIGINT NULL,
    PRIMARY KEY (comments_id),
    INDEX idx_comments_post (comments_post_id),
    INDEX idx_comments_parent (comments_parent_comment_id),
    INDEX idx_comments_author (comments_author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 댓글 좋아요 테이블
CREATE TABLE comment_likes (
    comment_likes_id BIGINT NOT NULL AUTO_INCREMENT,
    comment_likes_comment_id BIGINT NOT NULL,
    comment_likes_user_id BIGINT NOT NULL,
    comment_likes_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comment_likes_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comment_likes_created_by BIGINT NULL,
    comment_likes_updated_by BIGINT NULL,
    PRIMARY KEY (comment_likes_id),
    CONSTRAINT uk_comment_likes_comment_user UNIQUE (comment_likes_comment_id, comment_likes_user_id),
    INDEX idx_comment_likes_user (comment_likes_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 댓글 신고 테이블
CREATE TABLE comment_reports (
    comment_reports_id BIGINT NOT NULL AUTO_INCREMENT,
    comment_reports_comment_id BIGINT NOT NULL,
    comment_reports_reporter_id BIGINT NOT NULL,
    comment_reports_reason TEXT NOT NULL,
    comment_reports_status ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
    comment_reports_resolved_at TIMESTAMP(6) NULL,
    comment_reports_resolved_by BIGINT NULL,
    comment_reports_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    comment_reports_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    comment_reports_created_by BIGINT NULL,
    comment_reports_updated_by BIGINT NULL,
    PRIMARY KEY (comment_reports_id),
    INDEX idx_comment_reports_status (comment_reports_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가 (이름 명확화)
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post FOREIGN KEY (comments_post_id) REFERENCES posts(posts_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_parent FOREIGN KEY (comments_parent_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_author FOREIGN KEY (comments_author_id) REFERENCES users(users_id);

ALTER TABLE comment_likes
    ADD CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_likes_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_likes_user FOREIGN KEY (comment_likes_user_id) REFERENCES users(users_id);

ALTER TABLE comment_reports
    ADD CONSTRAINT fk_comment_reports_comment FOREIGN KEY (comment_reports_comment_id) REFERENCES comments(comments_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_reports_reporter FOREIGN KEY (comment_reports_reporter_id) REFERENCES users(users_id),
    ADD CONSTRAINT fk_comment_reports_resolver FOREIGN KEY (comment_reports_resolved_by) REFERENCES users(users_id);
