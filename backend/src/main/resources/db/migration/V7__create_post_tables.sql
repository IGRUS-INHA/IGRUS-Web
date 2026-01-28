-- V7: Post tables creation
-- 게시글 및 게시글 이미지 테이블 생성

-- posts 테이블
CREATE TABLE posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    is_question BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible_to_associate BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    deleted_by BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BIGINT,
    updated_by BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_board FOREIGN KEY (board_id) REFERENCES boards(id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(users_id),
    INDEX idx_posts_board_id (board_id),
    INDEX idx_posts_author_id (author_id),
    INDEX idx_posts_created_at (created_at),
    INDEX idx_posts_board_question (board_id, is_question),
    INDEX idx_posts_board_deleted (board_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- post_images 테이블
CREATE TABLE post_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_post_images_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
