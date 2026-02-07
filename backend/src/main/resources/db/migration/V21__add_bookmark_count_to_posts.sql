-- Post 테이블에 bookmark_count 컬럼 추가

ALTER TABLE posts ADD COLUMN posts_bookmark_count INT NOT NULL DEFAULT 0;

-- 기존 북마크 데이터 기반 초기값 동기화
UPDATE posts p SET p.posts_bookmark_count = (
    SELECT COUNT(*) FROM bookmarks b WHERE b.bookmarks_post_id = p.posts_id
);
