-- V9: Create post_views table for view history tracking
-- 게시글 조회 기록 테이블 생성 (통계 및 분석 목적)

CREATE TABLE post_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    viewer_id BIGINT NOT NULL,
    viewed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_post_views_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_views_viewer FOREIGN KEY (viewer_id) REFERENCES users(users_id),
    INDEX idx_post_views_post_id (post_id),
    INDEX idx_post_views_viewer_id (viewer_id),
    INDEX idx_post_views_viewed_at (viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
