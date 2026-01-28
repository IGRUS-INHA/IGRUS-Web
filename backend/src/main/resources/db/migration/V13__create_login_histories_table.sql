-- V11: Create login_histories table for login history tracking
-- 로그인 히스토리 테이블 생성 (보안 감사 및 분석 목적)

CREATE TABLE login_histories (
    login_histories_id BIGINT NOT NULL AUTO_INCREMENT,
    login_histories_user_id BIGINT,
    login_histories_student_id VARCHAR(8) NOT NULL,
    login_histories_ip_address VARCHAR(45) NOT NULL,
    login_histories_user_agent VARCHAR(500),
    login_histories_success BOOLEAN NOT NULL,
    login_histories_failure_reason VARCHAR(50),
    login_histories_attempted_at TIMESTAMP(6) NOT NULL,
    login_histories_created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    login_histories_updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    login_histories_created_by BIGINT,
    login_histories_updated_by BIGINT,
    PRIMARY KEY (login_histories_id),
    CONSTRAINT fk_login_histories_user FOREIGN KEY (login_histories_user_id) REFERENCES users(users_id),
    INDEX idx_login_histories_user_id (login_histories_user_id),
    INDEX idx_login_histories_student_id (login_histories_student_id),
    INDEX idx_login_histories_success (login_histories_success),
    INDEX idx_login_histories_attempted_at (login_histories_attempted_at),
    INDEX idx_login_histories_ip_address (login_histories_ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 복합 인덱스 (분석 쿼리 최적화)
CREATE INDEX idx_login_histories_student_success ON login_histories(login_histories_student_id, login_histories_success);
CREATE INDEX idx_login_histories_ip_success ON login_histories(login_histories_ip_address, login_histories_success);
