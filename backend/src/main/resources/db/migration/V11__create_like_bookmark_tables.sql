-- Like/Bookmark 테이블 생성
-- 좋아요 및 북마크 기능을 위한 테이블 스키마

-- 좋아요 테이블
CREATE TABLE likes (
    likes_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    likes_post_id BIGINT NOT NULL,
    likes_user_id BIGINT NOT NULL,
    likes_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    likes_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    likes_created_by BIGINT,
    likes_updated_by BIGINT,

    CONSTRAINT uk_likes_post_user UNIQUE (likes_post_id, likes_user_id),
    CONSTRAINT fk_likes_post FOREIGN KEY (likes_post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_user FOREIGN KEY (likes_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 북마크 테이블
CREATE TABLE bookmarks (
    bookmarks_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bookmarks_post_id BIGINT NOT NULL,
    bookmarks_user_id BIGINT NOT NULL,
    bookmarks_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    bookmarks_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    bookmarks_created_by BIGINT,
    bookmarks_updated_by BIGINT,

    CONSTRAINT uk_bookmarks_post_user UNIQUE (bookmarks_post_id, bookmarks_user_id),
    CONSTRAINT fk_bookmarks_post FOREIGN KEY (bookmarks_post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (bookmarks_user_id) REFERENCES users(users_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_likes_user ON likes(likes_user_id);
CREATE INDEX idx_likes_post ON likes(likes_post_id);
CREATE INDEX idx_likes_created_at ON likes(likes_created_at DESC);

CREATE INDEX idx_bookmarks_user ON bookmarks(bookmarks_user_id);
CREATE INDEX idx_bookmarks_post ON bookmarks(bookmarks_post_id);
CREATE INDEX idx_bookmarks_created_at ON bookmarks(bookmarks_created_at DESC);
