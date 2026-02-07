-- Post 테이블에 comment_count 컬럼 추가

ALTER TABLE posts ADD COLUMN posts_comment_count INT NOT NULL DEFAULT 0;

-- 기존 댓글 데이터 기반 초기값 동기화
UPDATE posts p SET p.posts_comment_count = (
    SELECT COUNT(*) FROM comments c
    WHERE c.comments_post_id = p.posts_id AND c.comments_deleted = FALSE
);
