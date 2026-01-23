-- V3__add_soft_delete_columns.sql
-- soft delete 컬럼 추가

-- users 테이블
ALTER TABLE users
    ADD COLUMN users_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN users_deleted_at DATETIME NULL,
    ADD COLUMN users_deleted_by BIGINT NULL;

-- positions 테이블
ALTER TABLE positions
    ADD COLUMN positions_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN positions_deleted_at DATETIME NULL,
    ADD COLUMN positions_deleted_by BIGINT NULL;

-- password_credentials 테이블
ALTER TABLE password_credentials
    ADD COLUMN password_credentials_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN password_credentials_deleted_at DATETIME NULL,
    ADD COLUMN password_credentials_deleted_by BIGINT NULL;

-- 인덱스 추가 (soft delete 쿼리 최적화)
CREATE INDEX idx_users_deleted ON users(users_deleted);
CREATE INDEX idx_positions_deleted ON positions(positions_deleted);
CREATE INDEX idx_password_credentials_deleted ON password_credentials(password_credentials_deleted);
