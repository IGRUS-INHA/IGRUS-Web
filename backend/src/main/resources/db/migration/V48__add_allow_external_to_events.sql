-- events 테이블에 외부인 신청 허용 여부 컬럼 추가 (DECISION-05: 기본값 false)
ALTER TABLE events ADD COLUMN event_allow_external BOOLEAN NOT NULL DEFAULT FALSE;
