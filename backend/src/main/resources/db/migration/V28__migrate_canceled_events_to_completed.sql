-- CANCELED 상태가 Java EventStatus enum에서 제거되므로,
-- 기존 CANCELED 행사를 COMPLETED(최종 종료 상태)로 변환합니다.
UPDATE events SET event_status = 'COMPLETED' WHERE event_status = 'CANCELED';
