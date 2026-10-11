-- 행사 신청 취소 요청: 신청자가 사유를 적어 취소를 요청하고 운영진이 처리한다.
-- 요청 중에도 신청 상태는 유지되며, 취소 처리 후에도 두 컬럼은 알림 이력으로 남는다.
ALTER TABLE event_registrations ADD COLUMN event_registrations_cancel_reason VARCHAR(500) NULL;
ALTER TABLE event_registrations ADD COLUMN event_registrations_cancel_requested_at TIMESTAMP NULL;
