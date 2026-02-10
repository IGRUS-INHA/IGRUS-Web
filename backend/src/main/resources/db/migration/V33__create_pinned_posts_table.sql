-- 메인 페이지 고정 게시글 테이블

CREATE TABLE pinned_posts (
    pinned_posts_id BIGINT NOT NULL AUTO_INCREMENT,
    pinned_posts_post_id BIGINT NOT NULL,
    pinned_posts_display_order INT NOT NULL,
    pinned_posts_pinned_by BIGINT NOT NULL,
    pinned_posts_version BIGINT,
    pinned_posts_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_posts_deleted_at TIMESTAMP(6),
    pinned_posts_deleted_by BIGINT,
    pinned_posts_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    pinned_posts_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    pinned_posts_created_by BIGINT,
    pinned_posts_updated_by BIGINT,
    PRIMARY KEY (pinned_posts_id),
    INDEX idx_pinned_posts_display_order (pinned_posts_display_order),
    INDEX idx_pinned_posts_deleted (pinned_posts_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE pinned_posts
    ADD CONSTRAINT fk_pinned_posts_post FOREIGN KEY (pinned_posts_post_id) REFERENCES posts(posts_id),
    ADD CONSTRAINT fk_pinned_posts_pinned_by FOREIGN KEY (pinned_posts_pinned_by) REFERENCES users(users_id);
