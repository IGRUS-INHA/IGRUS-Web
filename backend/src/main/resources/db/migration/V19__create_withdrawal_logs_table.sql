-- 회원 탈퇴 로그 테이블
CREATE TABLE withdrawal_logs (
    withdrawal_logs_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    withdrawal_logs_user_id BIGINT NOT NULL,
    withdrawal_logs_reason VARCHAR(500) NOT NULL,
    withdrawal_logs_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawal_logs_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    withdrawal_logs_created_by BIGINT,
    withdrawal_logs_updated_by BIGINT,
    CONSTRAINT fk_withdrawal_logs_user FOREIGN KEY (withdrawal_logs_user_id) REFERENCES users(users_id)
);

CREATE INDEX idx_withdrawal_logs_user_id ON withdrawal_logs(withdrawal_logs_user_id);
