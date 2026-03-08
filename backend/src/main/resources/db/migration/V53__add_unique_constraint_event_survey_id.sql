-- 설문 1:1 연결 제약 추가 (하나의 설문은 하나의 행사에만 연결 가능)
-- MySQL: nullable 컬럼의 UNIQUE 제약은 다수의 NULL을 허용함
ALTER TABLE events ADD CONSTRAINT uk_events_survey_id UNIQUE (event_survey_id);
