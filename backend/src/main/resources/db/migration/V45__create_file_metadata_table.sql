-- V45: 파일 메타데이터 테이블 생성
-- S3 Presigned URL 기반 이미지 업로드/다운로드를 위한 메타데이터 관리

CREATE TABLE file_metadata (
    file_metadata_id                BIGINT NOT NULL AUTO_INCREMENT,
    file_metadata_object_key        VARCHAR(500) NOT NULL,
    file_metadata_uploader_user_id  BIGINT NOT NULL,
    file_metadata_original_file_name VARCHAR(255) NOT NULL,
    file_metadata_content_type      VARCHAR(100) NOT NULL,
    file_metadata_file_size         BIGINT NOT NULL,
    file_metadata_status            VARCHAR(20) NOT NULL,
    file_metadata_completed_at      TIMESTAMP(6) NULL,
    file_metadata_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    file_metadata_deleted_at        TIMESTAMP(6),
    file_metadata_deleted_by        BIGINT,
    file_metadata_created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    file_metadata_updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    file_metadata_created_by        BIGINT,
    file_metadata_updated_by        BIGINT,
    PRIMARY KEY (file_metadata_id),
    UNIQUE KEY uk_file_metadata_object_key (file_metadata_object_key),
    INDEX idx_file_metadata_status_created_at (file_metadata_status, file_metadata_created_at),
    INDEX idx_file_metadata_uploader_user_id (file_metadata_uploader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FK 별도 추가
ALTER TABLE file_metadata
    ADD CONSTRAINT fk_file_metadata_uploader_user_id FOREIGN KEY (file_metadata_uploader_user_id) REFERENCES users(users_id);
