-- Post 테이블에 like_count 컬럼 추가

ALTER TABLE posts ADD COLUMN posts_like_count INT NOT NULL DEFAULT 0;

-- 인덱스 생성 (좋아요 수 기준 정렬을 위해)
CREATE INDEX idx_posts_like_count ON posts(posts_like_count DESC);
