-- 계정 상태 변경 감사 이력 테이블
-- 감사 목적 영구 보관: DELETE 불가 정책
CREATE TABLE account_status_change_histories (
    account_status_change_histories_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_status_change_histories_user_id BIGINT,
    account_status_change_histories_user_student_id VARCHAR(20),
    account_status_change_histories_changed_by_id BIGINT,
    account_status_change_histories_changed_by_student_id VARCHAR(20),
    account_status_change_histories_change_type VARCHAR(50) NOT NULL,
    account_status_change_histories_previous_value VARCHAR(255) NOT NULL,
    account_status_change_histories_new_value VARCHAR(255) NOT NULL,
    account_status_change_histories_reason VARCHAR(500),
    account_status_change_histories_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_status_change_histories_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    account_status_change_histories_created_by BIGINT,
    account_status_change_histories_updated_by BIGINT,
    CONSTRAINT fk_asch_user FOREIGN KEY (account_status_change_histories_user_id) REFERENCES users(users_id),
    CONSTRAINT fk_asch_changed_by FOREIGN KEY (account_status_change_histories_changed_by_id) REFERENCES users(users_id)
);

CREATE INDEX idx_asch_user_id ON account_status_change_histories (account_status_change_histories_user_id);
CREATE INDEX idx_asch_changed_by_id ON account_status_change_histories (account_status_change_histories_changed_by_id);
CREATE INDEX idx_asch_change_type ON account_status_change_histories (account_status_change_histories_change_type);
CREATE INDEX idx_asch_created_at ON account_status_change_histories (account_status_change_histories_created_at);
