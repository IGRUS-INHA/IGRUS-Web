-- 사용자 정지 이력 테이블
-- PRD의 UserSuspension 테이블에 대응

CREATE TABLE user_suspensions (
    user_suspensions_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_suspensions_user_id BIGINT NOT NULL,
    user_suspensions_reason VARCHAR(500) NOT NULL,
    user_suspensions_suspended_at DATETIME(6) NOT NULL,
    user_suspensions_suspended_until DATETIME(6) NOT NULL,
    user_suspensions_suspended_by BIGINT NOT NULL,
    user_suspensions_lifted_at DATETIME(6),
    user_suspensions_lifted_by BIGINT,
    user_suspensions_created_at DATETIME(6) NOT NULL,
    user_suspensions_updated_at DATETIME(6) NOT NULL,
    user_suspensions_created_by BIGINT,
    user_suspensions_updated_by BIGINT,

    CONSTRAINT fk_user_suspensions_user
        FOREIGN KEY (user_suspensions_user_id) REFERENCES users(users_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_user_suspensions_user_id ON user_suspensions(user_suspensions_user_id);
CREATE INDEX idx_user_suspensions_suspended_at ON user_suspensions(user_suspensions_suspended_at);
CREATE INDEX idx_user_suspensions_suspended_until ON user_suspensions(user_suspensions_suspended_until);
CREATE INDEX idx_user_suspensions_lifted_at ON user_suspensions(user_suspensions_lifted_at);
