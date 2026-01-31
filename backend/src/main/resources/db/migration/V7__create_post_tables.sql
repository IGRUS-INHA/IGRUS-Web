-- V7: Post tables creation
-- 게시글 및 게시글 이미지 테이블 생성

-- posts 테이블
CREATE TABLE posts (
    posts_id BIGINT NOT NULL AUTO_INCREMENT,
    posts_board_id BIGINT NOT NULL,
    posts_author_id BIGINT NOT NULL,
    posts_title VARCHAR(100) NOT NULL,
    posts_content TEXT NOT NULL,
    posts_view_count INT NOT NULL DEFAULT 0,
    posts_is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    posts_is_question BOOLEAN NOT NULL DEFAULT FALSE,
    posts_is_visible_to_associate BOOLEAN NOT NULL DEFAULT FALSE,
    posts_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    posts_deleted_at TIMESTAMP(6),
    posts_deleted_by BIGINT,
    posts_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    posts_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    posts_created_by BIGINT,
    posts_updated_by BIGINT,
    PRIMARY KEY (posts_id),
    INDEX idx_posts_board_id (posts_board_id),
    INDEX idx_posts_author_id (posts_author_id),
    INDEX idx_posts_created_at (posts_created_at),
    INDEX idx_posts_board_question (posts_board_id, posts_is_question),
    INDEX idx_posts_board_deleted (posts_board_id, posts_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가 (이름 명확화)
ALTER TABLE posts
    ADD CONSTRAINT fk_posts_board FOREIGN KEY (posts_board_id) REFERENCES boards(boards_id),
    ADD CONSTRAINT fk_posts_author FOREIGN KEY (posts_author_id) REFERENCES users(users_id);

-- post_images 테이블
CREATE TABLE post_images (
    post_images_id BIGINT NOT NULL AUTO_INCREMENT,
    post_images_post_id BIGINT NOT NULL,
    post_images_image_url VARCHAR(500) NOT NULL,
    post_images_display_order INT NOT NULL DEFAULT 0,
    post_images_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    post_images_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    post_images_created_by BIGINT,
    post_images_updated_by BIGINT,
    PRIMARY KEY (post_images_id),
    INDEX idx_post_images_post_id (post_images_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가 (이름 명확화)
ALTER TABLE post_images
    ADD CONSTRAINT fk_post_images_post FOREIGN KEY (post_images_post_id) REFERENCES posts(posts_id) ON DELETE CASCADE;
