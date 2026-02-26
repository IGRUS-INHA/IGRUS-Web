-- =============================================================================
-- V39: event_status_change_histories 테이블 생성 + event_reopen_histories 데이터 마이그레이션
-- =============================================================================

-- 1) 통합 이력 테이블 생성 (FK 없음 — V25 패턴 준수)
CREATE TABLE event_status_change_histories (
    event_status_change_histories_id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_status_change_histories_event_id              BIGINT,
    event_status_change_histories_changed_by_id         BIGINT,
    event_status_change_histories_changed_by_student_id VARCHAR(20),
    event_status_change_histories_change_type           VARCHAR(50)  NOT NULL,
    event_status_change_histories_previous_value        VARCHAR(255) NOT NULL,
    event_status_change_histories_new_value             VARCHAR(255) NOT NULL,
    event_status_change_histories_reason                TEXT,
    event_status_change_histories_created_at            TIMESTAMP(6) NOT NULL,
    event_status_change_histories_updated_at            TIMESTAMP(6) NOT NULL,
    event_status_change_histories_created_by            BIGINT,
    event_status_change_histories_updated_by            BIGINT
);

CREATE INDEX idx_esch_event_id      ON event_status_change_histories (event_status_change_histories_event_id);
CREATE INDEX idx_esch_changed_by_id ON event_status_change_histories (event_status_change_histories_changed_by_id);
CREATE INDEX idx_esch_change_type   ON event_status_change_histories (event_status_change_histories_change_type);
CREATE INDEX idx_esch_created_at    ON event_status_change_histories (event_status_change_histories_created_at);

-- 2) 기존 event_reopen_histories 데이터 마이그레이션
--    change_type = 'REGISTRATION_REOPENED', previous = 'CLOSED', new = 'OPEN'
INSERT INTO event_status_change_histories (
    event_status_change_histories_event_id,
    event_status_change_histories_changed_by_id,
    event_status_change_histories_changed_by_student_id,
    event_status_change_histories_change_type,
    event_status_change_histories_previous_value,
    event_status_change_histories_new_value,
    event_status_change_histories_reason,
    event_status_change_histories_created_at,
    event_status_change_histories_updated_at,
    event_status_change_histories_created_by
)
SELECT
    event_reopen_histories_event_id,
    event_reopen_histories_reopened_by,
    NULL,
    'REGISTRATION_REOPENED',
    'CLOSED',
    'OPEN',
    event_reopen_histories_reason,
    event_reopen_histories_reopened_at,
    event_reopen_histories_reopened_at,
    event_reopen_histories_reopened_by
FROM event_reopen_histories;

-- 3) 기존 테이블 삭제
DROP TABLE event_reopen_histories;
