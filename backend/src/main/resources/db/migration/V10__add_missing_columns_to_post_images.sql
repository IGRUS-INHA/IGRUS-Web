-- V10: Add missing BaseEntity columns to post_images table
-- post_images 테이블에 BaseEntity 상속으로 인해 필요한 컬럼 추가

ALTER TABLE post_images
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    ADD COLUMN created_by BIGINT,
    ADD COLUMN updated_by BIGINT;
