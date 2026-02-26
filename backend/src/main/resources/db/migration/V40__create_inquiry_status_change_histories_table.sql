-- =============================================================================
-- V40: 문의 상태 변경 감사 이력 테이블 생성
-- FK 없음 — soft-delete 및 AFTER_COMMIT 리스너 호환 (V25 패턴)
-- =============================================================================

CREATE TABLE inquiry_status_change_histories (
    inquiry_status_change_histories_id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_status_change_histories_inquiry_id            BIGINT,
    inquiry_status_change_histories_changed_by_id         BIGINT,
    inquiry_status_change_histories_changed_by_student_id VARCHAR(20),
    inquiry_status_change_histories_change_type           VARCHAR(50)  NOT NULL,
    inquiry_status_change_histories_previous_value        VARCHAR(255) NOT NULL,
    inquiry_status_change_histories_new_value             VARCHAR(255) NOT NULL,
    inquiry_status_change_histories_created_at            TIMESTAMP(6) NOT NULL,
    inquiry_status_change_histories_updated_at            TIMESTAMP(6) NOT NULL,
    inquiry_status_change_histories_created_by            BIGINT,
    inquiry_status_change_histories_updated_by            BIGINT
);

CREATE INDEX idx_isch_inquiry_id    ON inquiry_status_change_histories (inquiry_status_change_histories_inquiry_id);
CREATE INDEX idx_isch_changed_by_id ON inquiry_status_change_histories (inquiry_status_change_histories_changed_by_id);
CREATE INDEX idx_isch_change_type   ON inquiry_status_change_histories (inquiry_status_change_histories_change_type);
CREATE INDEX idx_isch_created_at    ON inquiry_status_change_histories (inquiry_status_change_histories_created_at);
