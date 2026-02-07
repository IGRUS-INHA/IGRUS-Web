-- 행사 테이블에 soft delete 컬럼 추가
ALTER TABLE events
    ADD COLUMN event_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN event_deleted_at TIMESTAMP(6) NULL,
    ADD COLUMN event_deleted_by BIGINT NULL;

CREATE INDEX idx_events_deleted ON events(event_deleted);
