-- 방문 기록 테이블
CREATE TABLE visit_logs (
    visit_logs_id          BIGINT       NOT NULL AUTO_INCREMENT,
    visit_logs_user_id     BIGINT       NULL,
    visit_logs_visited_at  TIMESTAMP(6) NOT NULL,
    visit_logs_created_at  TIMESTAMP(6) NOT NULL,
    visit_logs_updated_at  TIMESTAMP(6) NOT NULL,
    visit_logs_created_by  BIGINT       NULL,
    visit_logs_updated_by  BIGINT       NULL,
    PRIMARY KEY (visit_logs_id),
    CONSTRAINT fk_visit_logs_user FOREIGN KEY (visit_logs_user_id) REFERENCES users(users_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_visit_logs_user_id ON visit_logs(visit_logs_user_id);
CREATE INDEX idx_visit_logs_visited_at ON visit_logs(visit_logs_visited_at);
