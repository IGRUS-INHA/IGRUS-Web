-- User-Position 다대다 관계를 위한 중간 테이블 생성 및 마이그레이션

-- 1. 중간 테이블 생성
CREATE TABLE user_positions (
    user_positions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_positions_user_id BIGINT NOT NULL,
    user_positions_position_id BIGINT NOT NULL,
    user_positions_assigned_at DATETIME(6) NOT NULL,
    user_positions_created_at DATETIME(6) NOT NULL,
    user_positions_updated_at DATETIME(6) NOT NULL,
    user_positions_created_by BIGINT,
    user_positions_updated_by BIGINT,

    CONSTRAINT fk_user_positions_user
        FOREIGN KEY (user_positions_user_id) REFERENCES users(users_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_positions_position
        FOREIGN KEY (user_positions_position_id) REFERENCES positions(positions_id) ON DELETE CASCADE,
    CONSTRAINT uk_user_positions UNIQUE (user_positions_user_id, user_positions_position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 기존 데이터 마이그레이션 (users_position_id가 있는 경우)
INSERT INTO user_positions (user_positions_user_id, user_positions_position_id,
    user_positions_assigned_at, user_positions_created_at, user_positions_updated_at)
SELECT users_id, users_position_id, NOW(6), NOW(6), NOW(6)
FROM users WHERE users_position_id IS NOT NULL;

-- 3. users 테이블에서 FK 컬럼 삭제
ALTER TABLE users DROP FOREIGN KEY fk_users_position;
ALTER TABLE users DROP COLUMN users_position_id;

-- 4. 인덱스 생성
CREATE INDEX idx_user_positions_user_id ON user_positions(user_positions_user_id);
CREATE INDEX idx_user_positions_position_id ON user_positions(user_positions_position_id);
