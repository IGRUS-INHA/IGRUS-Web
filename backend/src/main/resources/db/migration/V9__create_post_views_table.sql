-- V9: Create post_views table for view history tracking
-- 게시글 조회 기록 테이블 생성 (통계 및 분석 목적)

CREATE TABLE post_views (
    post_views_id BIGINT NOT NULL AUTO_INCREMENT,
    post_views_post_id BIGINT NOT NULL,
    post_views_viewer_id BIGINT NOT NULL,
    post_views_viewed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    post_views_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    post_views_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    post_views_created_by BIGINT,
    post_views_updated_by BIGINT,
    PRIMARY KEY (post_views_id),
    INDEX idx_post_views_post_id (post_views_post_id),
    INDEX idx_post_views_viewer_id (post_views_viewer_id),
    INDEX idx_post_views_viewed_at (post_views_viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가 (이름 명확화)
ALTER TABLE post_views
    ADD CONSTRAINT fk_post_views_post FOREIGN KEY (post_views_post_id) REFERENCES posts(posts_id),
    ADD CONSTRAINT fk_post_views_viewer FOREIGN KEY (post_views_viewer_id) REFERENCES users(users_id);
