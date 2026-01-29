-- V8: Add version column for optimistic locking
-- 낙관적 락을 위한 version 컬럼 추가 (조회수 동시성 처리)

ALTER TABLE posts ADD COLUMN posts_version BIGINT NOT NULL DEFAULT 0;
