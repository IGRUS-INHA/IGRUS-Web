-- =============================================================================
-- V38: 2축 상태 모델 마이그레이션
-- 단일 event_status → event_registration_status + event_status (리팩토링)
-- =============================================================================

-- 1) 등록 상태 컬럼 추가
ALTER TABLE events
    ADD COLUMN event_registration_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';

-- 2) 기존 데이터 마이그레이션: event_status 값에 따라 registration_status 설정
UPDATE events SET event_registration_status = 'NOT_STARTED' WHERE event_status = 'UPCOMING';
UPDATE events SET event_registration_status = 'OPEN' WHERE event_status = 'OPEN';
UPDATE events SET event_registration_status = 'CLOSED' WHERE event_status IN ('CLOSED', 'ONGOING', 'COMPLETED');

-- 3) event_status 컬럼에서 OPEN/CLOSED → UPCOMING으로 변환
-- OPEN/CLOSED는 등록 축의 상태이므로, 행사 축에서는 아직 UPCOMING
UPDATE events SET event_status = 'UPCOMING' WHERE event_status IN ('OPEN', 'CLOSED');

-- 4) 인덱스 추가
CREATE INDEX idx_events_registration_status ON events(event_registration_status);

-- 5) 수동 재오픈 감사 이력 테이블 (EVT-INV-14)
CREATE TABLE event_reopen_histories (
    event_reopen_histories_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_reopen_histories_event_id BIGINT NOT NULL,
    event_reopen_histories_reason TEXT NOT NULL,
    event_reopen_histories_reopened_by BIGINT NOT NULL,
    event_reopen_histories_reopened_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_event_reopen_histories_event FOREIGN KEY (event_reopen_histories_event_id)
        REFERENCES events(event_id) ON DELETE CASCADE
);

CREATE INDEX idx_event_reopen_histories_event_id ON event_reopen_histories(event_reopen_histories_event_id);
